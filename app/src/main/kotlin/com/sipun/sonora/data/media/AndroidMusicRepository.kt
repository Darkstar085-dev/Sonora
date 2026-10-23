package com.sipun.sonora.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.sipun.sonora.data.preferences.LibraryPreferences
import com.sipun.sonora.domain.MusicRepository
import com.sipun.sonora.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.jaudiotagger.kt.AudioTagger
import org.jaudiotagger.kt.tag.FieldKey
import java.io.File

class AndroidMusicRepository : MusicRepository {
    private val contentResolver: ContentResolver
    private val context: Context?

    constructor(context: Context) {
        this.contentResolver = context.contentResolver
        this.context = context.applicationContext
        appContext = context.applicationContext
    }

    constructor(contentResolver: ContentResolver) {
        this.contentResolver = contentResolver
        this.context = null
    }

    @Volatile
    private var cachedSongs: List<Song>? = null

    companion object {
        private val _refreshVersion = MutableStateFlow(0L)
        val refreshVersion = _refreshVersion.asStateFlow()

        @Volatile
        private var appContext: Context? = null

        fun notifyMetadataChanged() {
            _refreshVersion.update { it + 1L }
        }
    }

    override suspend fun songs(): List<Song> {
        cachedSongs?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedSongs ?: querySongs().also { cachedSongs = it }
        }
    }

    fun invalidateCache() {
        cachedSongs = null
    }

    override suspend fun albums(): List<String> =
        songs().map(Song::album).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

    override suspend fun artists(): List<String> =
        songs().map(Song::artist).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

    suspend fun getAvailableFolders(): List<String> = withContext(Dispatchers.IO) {
        val folders = mutableSetOf<String>()
        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null,
        )?.use { cursor ->
            val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relIdx = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val data = if (dataIdx >= 0) cursor.getString(dataIdx).orEmpty() else ""
                val rel = if (relIdx >= 0) cursor.getString(relIdx).orEmpty() else ""
                val folder = data.substringBeforeLast('/', "").ifBlank { rel }
                if (folder.isNotBlank()) {
                    folders.add(folder)
                }
            }
        }
        folders.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun querySongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val resolvedContext = context ?: appContext
        val libraryPreferences = resolvedContext?.let { LibraryPreferences(it) }
        val excludedFolders = libraryPreferences?.excludedFolders() ?: emptySet()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val genre = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val track = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val year = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val relativePath = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val displayName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val dataPath = if (dataIndex >= 0) cursor.getString(dataIndex).orEmpty() else ""
                val relativePathValue = cursor.getString(relativePath).orEmpty()
                val folderPath = dataPath.substringBeforeLast('/', "").ifBlank { relativePathValue }

                val isExcluded = excludedFolders.any { excluded ->
                    val cleanExcluded = excluded.trim()
                    if (cleanExcluded.isBlank() || cleanExcluded == "/" || cleanExcluded == "/storage/emulated/0") {
                        return@any false
                    }
                    dataPath.equals(cleanExcluded, ignoreCase = true) ||
                            dataPath.startsWith("$cleanExcluded/", ignoreCase = true) ||
                            relativePathValue.equals(cleanExcluded, ignoreCase = true) ||
                            relativePathValue.startsWith("$cleanExcluded/", ignoreCase = true) ||
                            folderPath.equals(cleanExcluded, ignoreCase = true) ||
                            folderPath.startsWith("$cleanExcluded/", ignoreCase = true)
                }
                if (isExcluded) continue

                val songId = cursor.getLong(id)
                val currentAlbumId = cursor.getLong(albumId)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songId,
                )
                val displayNameValue = cursor.getString(displayName).cleanMetadata("Unknown title")
                val mediaStoreMetadata = MediaStoreMetadata(
                    title = cursor.getString(title).cleanMetadata("Unknown title")
                        .ifBlank { displayNameValue },
                    artist = cursor.getString(artist).cleanMetadata("Unknown artist"),
                    album = cursor.getString(album).cleanAlbum(folderPath, "Unknown album"),
                    genre = cursor.getString(genre).cleanMetadata("").takeIf { it.isNotBlank() },
                    trackNumber = cursor.getInt(track).takeIf { it > 0 },
                    year = cursor.getInt(year).takeIf { it > 0 },
                )
                val metadata = if (displayNameValue.endsWith(".wav", ignoreCase = true)) {
                    readWavMetadata(contentResolver, uri, mediaStoreMetadata) ?: mediaStoreMetadata
                } else {
                    mediaStoreMetadata
                }

                songs += Song(
                    id = songId,
                    title = metadata.title,
                    artist = metadata.artist,
                    album = metadata.album,
                    genre = metadata.genre,
                    durationMs = cursor.getLong(duration),
                    uri = uri.toString(),
                    albumArtUri = if (currentAlbumId > 0) {
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            currentAlbumId,
                        ).toString()
                    } else {
                        null
                    },
                    trackNumber = metadata.trackNumber,
                    year = metadata.year,
                    folder = folderPath.ifBlank { null },
                    dateAddedSeconds = cursor.getLong(dateAdded),
                )
            }
        }
        return songs
    }
}

private data class MediaStoreMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String?,
    val trackNumber: Int?,
    val year: Int?,
)

private fun readWavMetadata(
    contentResolver: ContentResolver,
    uri: Uri,
    fallback: MediaStoreMetadata,
): MediaStoreMetadata? {
    val temp = File.createTempFile("sonora-wav-", ".wav")
    return try {
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        val tag = AudioTagger.read(Path(temp.absolutePath)).tag
        fun value(key: FieldKey): String = tag.first(key).orEmpty()

        MediaStoreMetadata(
            title = value(FieldKey.TITLE).ifBlank { fallback.title },
            artist = value(FieldKey.ARTIST).ifBlank { fallback.artist },
            album = value(FieldKey.ALBUM).ifBlank { fallback.album },
            genre = value(FieldKey.GENRE).takeIf { it.isNotBlank() } ?: fallback.genre,
            trackNumber = parseTrackNumber(value(FieldKey.TRACK)) ?: fallback.trackNumber,
            year = value(FieldKey.YEAR).toIntOrNull() ?: fallback.year,
        )
    } catch (_: Exception) {
        null
    } finally {
        temp.delete()
    }
}

private fun parseTrackNumber(value: String): Int? =
    value.substringBefore('/').trim().toIntOrNull()

private fun String?.cleanMetadata(fallback: String): String {
    val value = orEmpty().trim()
    return if (value.isBlank() || value.equals("<unknown>", ignoreCase = true)) fallback else value
}

private fun String?.cleanAlbum(folderPath: String, fallback: String): String {
    val value = orEmpty().trim()
    if (value.isBlank() || value.equals("<unknown>", ignoreCase = true) || value.equals(
            "Music",
            ignoreCase = true
        )
    ) {
        val folderName = folderPath.substringAfterLast('/').ifBlank { folderPath }
        if (folderName.isNotBlank() && !folderName.equals(
                "0",
                ignoreCase = true
            ) && !folderName.equals("Music", ignoreCase = true) && !folderName.equals(
                "Download",
                ignoreCase = true
            )
        ) {
            return folderName
        }
        return fallback
    }
    return value
}
