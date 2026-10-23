package com.sipun.sonora.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.webkit.MimeTypeMap
import com.sipun.sonora.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.jaudiotagger.kt.AudioTagger
import org.jaudiotagger.kt.tag.Artwork
import org.jaudiotagger.kt.tag.FieldKey
import org.jaudiotagger.kt.tag.PictureTypes
import org.jaudiotagger.kt.tag.Tag
import java.io.File

data class EditableSongMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int?,
    val track: Int?,
    val disc: Int?,
    val composer: String,
    val comment: String,
    val grouping: String,
    val lyrics: String,
    val copyright: String,
    val bpm: Int?,
    val artworkData: ByteArray? = null,
)

class MediaWriteAccessRequiredException : Exception()

object AudioMetadataEditor {
    fun getMediaManagementIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || MediaStore.canManageMedia(context)) {
            return null
        }
        return Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
            data = Uri.parse("package:" + context.packageName)
        }
    }

    fun getDeleteRequestIntentSender(context: Context, uri: Uri): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(context)) {
            return null
        }
        return MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender
    }

    suspend fun delete(context: Context, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.delete(uri, null, null) > 0
            }.getOrDefault(false)
        }

    fun getWriteRequestIntentSender(context: Context, uri: Uri): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender
    }

    suspend fun read(context: Context, song: Song): EditableSongMetadata =
        withContext(Dispatchers.IO) {
            val audioUri = Uri.parse(song.uri)
            val temp = File.createTempFile(
                "sonora-read-",
                audioSuffix(context, audioUri),
                context.cacheDir,
            )
            try {
                context.contentResolver.openInputStream(audioUri).use { input ->
                    requireNotNull(input) { "Unable to read the audio file." }
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                val tag = AudioTagger.read(Path(temp.absolutePath)).tag
                fun value(key: FieldKey): String = tag.first(key).orEmpty()
                EditableSongMetadata(
                    title = value(FieldKey.TITLE).ifBlank { song.title },
                    artist = value(FieldKey.ARTIST).ifBlank { song.artist },
                    album = value(FieldKey.ALBUM).ifBlank { song.album },
                    albumArtist = value(FieldKey.ALBUM_ARTIST),
                    genre = value(FieldKey.GENRE).ifBlank { song.genre.orEmpty() },
                    year = value(FieldKey.YEAR).toIntOrNull() ?: song.year,
                    track = value(FieldKey.TRACK).toIntOrNull() ?: song.trackNumber,
                    disc = value(FieldKey.DISC_NO).toIntOrNull(),
                    composer = value(FieldKey.COMPOSER),
                    comment = value(FieldKey.COMMENT),
                    grouping = value(FieldKey.GROUPING),
                    lyrics = value(FieldKey.LYRICS),
                    copyright = value(FieldKey.COPYRIGHT),
                    bpm = value(FieldKey.BPM).toIntOrNull(),
                    artworkData = tag.artworks.firstOrNull()?.data,
                )
            } finally {
                temp.delete()
            }
        }

    suspend fun save(
        context: Context,
        song: Song,
        metadata: EditableSongMetadata,
        artworkUri: Uri?,
        artworkChanged: Boolean,
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val audioUri = Uri.parse(song.uri)
        val audioTemp = File.createTempFile(
            "sonora-edit-",
            audioSuffix(context, audioUri),
            context.cacheDir,
        )
        val imageTemp = if (artworkChanged && artworkUri != null) {
            File.createTempFile(
                "sonora-art-",
                imageSuffix(context, artworkUri),
                context.cacheDir,
            )
        } else null
        val normalizedImageTemp = if (imageTemp != null) {
            File.createTempFile("sonora-art-normalized-", ".jpg", context.cacheDir)
        } else null

        try {
            resolver.openInputStream(audioUri).use { input ->
                requireNotNull(input) { "Unable to read the audio file." }
                audioTemp.outputStream().use { output -> input.copyTo(output) }
            }

            val audioPath = Path(audioTemp.absolutePath)
            val audioFile = AudioTagger.read(audioPath)
            val tag: Tag = audioFile.tag
            writeField(tag, FieldKey.TITLE, metadata.title)
            writeField(tag, FieldKey.ARTIST, metadata.artist)
            writeField(tag, FieldKey.ALBUM, metadata.album)
            writeField(tag, FieldKey.ALBUM_ARTIST, metadata.albumArtist)
            writeField(tag, FieldKey.GENRE, metadata.genre)
            writeField(tag, FieldKey.YEAR, metadata.year?.toString().orEmpty())
            writeField(tag, FieldKey.TRACK, metadata.track?.toString().orEmpty())
            writeField(tag, FieldKey.DISC_NO, metadata.disc?.toString().orEmpty())
            writeField(tag, FieldKey.COMPOSER, metadata.composer)
            writeField(tag, FieldKey.COMMENT, metadata.comment)
            writeField(tag, FieldKey.GROUPING, metadata.grouping)
            writeField(tag, FieldKey.LYRICS, metadata.lyrics)
            writeField(tag, FieldKey.COPYRIGHT, metadata.copyright)
            writeField(tag, FieldKey.BPM, metadata.bpm?.toString().orEmpty())

            if (artworkChanged) {
                tag.clearArtworks()
                if (artworkUri != null && imageTemp != null) {
                    resolver.openInputStream(artworkUri).use { input ->
                        requireNotNull(input) { "Unable to read artwork." }
                        imageTemp.outputStream().use { output -> input.copyTo(output) }
                    }
                    val normalizedArtwork = requireNotNull(normalizedImageTemp)
                    normalizeArtworkToJpeg(imageTemp, normalizedArtwork)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(normalizedArtwork.absolutePath, bounds)
                    tag.setArtwork(
                        Artwork(
                            data = normalizedArtwork.readBytes(),
                            mimeType = "image/jpeg",
                            pictureType = PictureTypes.DEFAULT_ID,
                            width = bounds.outWidth,
                            height = bounds.outHeight,
                            colourDepth = 24,
                        ),
                    )
                }
            }

            AudioTagger.write(audioPath, tag)

            try {
                resolver.openOutputStream(audioUri, "wt").use { output ->
                    requireNotNull(output) { "Unable to write the audio file." }
                    audioTemp.inputStream().use { input -> input.copyTo(output) }
                }
            } catch (_: SecurityException) {
                throw MediaWriteAccessRequiredException()
            }

            val mediaStoreUpdated = runCatching {
                resolver.update(
                    audioUri,
                    ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, metadata.title)
                        put(MediaStore.Audio.Media.ARTIST, metadata.artist)
                        put(MediaStore.Audio.Media.ALBUM, metadata.album)
                        put(MediaStore.Audio.Media.YEAR, metadata.year ?: 0)
                        put(MediaStore.Audio.Media.TRACK, metadata.track ?: 0)
                    },
                    null,
                    null,
                ) > 0
            }.getOrDefault(false)

            resolver.notifyChange(audioUri, null, ContentResolver.NOTIFY_UPDATE)

            if (!mediaStoreUpdated) {
                rescanMediaStore(context, audioUri)
            }
        } finally {
            audioTemp.delete()
            imageTemp?.delete()
            normalizedImageTemp?.delete()
        }
    }

    private fun rescanMediaStore(context: Context, audioUri: Uri) {
        @Suppress("DEPRECATION")
        val path = runCatching {
            context.contentResolver.query(
                audioUri,
                arrayOf(MediaStore.Audio.Media.DATA),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

        if (!path.isNullOrBlank()) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                arrayOf(context.contentResolver.getType(audioUri)),
                null,
            )
        }
    }

    private fun normalizeArtworkToJpeg(source: File, target: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "Unable to decode artwork image."
        }

        val maxDimension = 2048
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = requireNotNull(BitmapFactory.decodeFile(source.absolutePath, options)) {
            "Unable to decode artwork image."
        }
        try {
            target.outputStream().use { output ->
                require(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                    "Unable to encode artwork image."
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun audioSuffix(context: Context, uri: Uri): String =
        mediaSuffix(context, uri, "audio")

    private fun imageSuffix(context: Context, uri: Uri): String =
        mediaSuffix(context, uri, "image", fallback = ".jpg")

    private fun mediaSuffix(
        context: Context,
        uri: Uri,
        mediaType: String,
        fallback: String? = null,
    ): String {
        val displayName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
        }.getOrNull()

        val name = displayName ?: uri.lastPathSegment
        val extension = name
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() && !it.contains('/') }

        if (extension != null) return ".$extension"

        val mimeType = context.contentResolver.getType(uri)
        val mimeExtension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.isNotBlank() }

        if (mimeExtension != null) return ".$mimeExtension"
        if (fallback != null) return fallback

        error("Unable to determine $mediaType file format.")
    }

    private fun writeField(tag: Tag, key: FieldKey, value: String) {
        if (value.isBlank()) tag.remove(key) else tag.set(key, value)
    }
}
