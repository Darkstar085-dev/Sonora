package com.sipun.sonora.data.lyrics

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.sipun.sonora.domain.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.jaudiotagger.kt.AudioTagger
import org.jaudiotagger.kt.tag.FieldKey
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal class LyricsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val client = LrcLibClient()
    private val cache = LyricsCache(File(appContext.cacheDir, "lyrics"))
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshingKeys = ConcurrentHashMap.newKeySet<String>()

    suspend fun get(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val key = "lrclib-v2|" + listOf(song.title, song.artist, song.album)
            .joinToString("|") { it.trim().lowercase(Locale.ROOT) }

        cache.get(key)?.takeIf { it.lines.isNotEmpty() }?.let { cached ->
            refreshInBackground(key, song)
            return@withContext cached
        }

        val online = try {
            fetchAndCache(key, song)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (online != null) return@withContext online

        val sidecar = readSidecarLyrics(song.uri)
        if (sidecar != null && sidecar.lines.isNotEmpty()) return@withContext sidecar

        val embedded = readEmbeddedLyrics(song.uri)
        if (embedded?.synced == true && embedded.lines.isNotEmpty()) {
            return@withContext embedded
        }
        if (embedded != null && embedded.lines.isNotEmpty()) {
            return@withContext embedded
        }
        null
    }

    private fun refreshInBackground(key: String, song: Song) {
        if (!refreshingKeys.add(key)) return
        refreshScope.launch {
            try {
                fetchAndCache(key, song)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                refreshingKeys.remove(key)
            }
        }
    }

    private suspend fun fetchAndCache(key: String, song: Song): Lyrics? {
        val result = client.fetch(song.title, song.artist, song.album)
        if (result != null && result.lines.isNotEmpty()) {
            cache.put(key, result)
            return result
        }
        return null
    }

    private fun readSidecarLyrics(uriString: String): Lyrics? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val sidecarText = when (uri.scheme) {
            "file" -> {
                val audioFile = runCatching { File(uri.path ?: return null) }.getOrNull() ?: return null
                File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
                    .takeIf { it.isFile && it.canRead() }
                    ?.let { file -> runCatching { file.readText() }.getOrNull() }
            }
            "content" -> {
                val resolver = appContext.contentResolver
                val metadata = runCatching {
                    resolver.query(
                        uri,
                        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (!cursor.moveToFirst()) null else {
                            val name = cursor.getString(0).orEmpty()
                            val directory = cursor.getString(1)
                            if (name.isBlank()) null else name to directory
                        }
                    }
                }.getOrNull() ?: return null
                val lrcName = metadata.first.substringBeforeLast('.', metadata.first) + ".lrc"
                val selection = if (metadata.second.isNullOrBlank()) {
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                } else {
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                }
                val args = if (metadata.second.isNullOrBlank()) arrayOf(lrcName)
                    else arrayOf(lrcName, metadata.second)
                runCatching {
                    resolver.query(
                        MediaStore.Files.getContentUri("external"),
                        arrayOf(MediaStore.MediaColumns._ID),
                        selection,
                        args,
                        null,
                    )?.use { cursor ->
                        if (!cursor.moveToFirst()) null else {
                            val id = cursor.getLong(0)
                            val lrcUri = Uri.withAppendedPath(
                                MediaStore.Files.getContentUri("external"),
                                id.toString(),
                            )
                            resolver.openInputStream(lrcUri)?.bufferedReader()?.use { it.readText() }
                        }
                    }
                }.getOrNull()
            }
            else -> null
        }
        return sidecarText?.let { runCatching { LyricsParser.parse(it) }.getOrNull() }
            ?.takeIf { it.lines.isNotEmpty() }
    }

    private fun readEmbeddedLyrics(uriString: String): Lyrics? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val temporaryFile = createTemporaryAudioFile(appContext.contentResolver, uri) ?: return null
        return try {
            runCatching {
                val tag = AudioTagger.read(Path(temporaryFile.absolutePath)).tag
                val rawLyrics = tag.first(FieldKey.LYRICS)
                    ?.takeIf { it.isNotBlank() }
                    ?: return@runCatching null
                LyricsParser.parse(rawLyrics).takeIf { it.lines.isNotEmpty() }
            }.getOrNull()
        } finally {
            temporaryFile.delete()
        }
    }

    private fun createTemporaryAudioFile(resolver: ContentResolver, uri: Uri): File? {
        val extension = extensionFor(resolver, uri)
        val file = runCatching {
            File.createTempFile("sonora-lyrics-", ".$extension", appContext.cacheDir)
        }.getOrNull() ?: return null

        return try {
            val input = resolver.openInputStream(uri) ?: return null.also { file.delete() }
            input.use { source ->
                file.outputStream().use { output -> source.copyTo(output) }
            }
            file.takeIf { it.length() > 0L } ?: null.also { file.delete() }
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    private fun extensionFor(resolver: ContentResolver, uri: Uri): String {
        val pathExtension = uri.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.matches(Regex("[a-z0-9]{2,5}")) }
        if (pathExtension != null) return pathExtension
        return when (resolver.getType(uri)?.lowercase(Locale.ROOT)) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/flac" -> "flac"
            "audio/ogg", "application/ogg" -> "ogg"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/aac", "audio/aacp" -> "aac"
            "audio/opus" -> "opus"
            else -> "mp3"
        }
    }

}
