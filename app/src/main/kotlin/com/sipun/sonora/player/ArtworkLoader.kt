package com.sipun.sonora.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.io.files.Path
import org.jaudiotagger.kt.AudioTagger
import java.io.ByteArrayOutputStream
import java.io.File

internal class ArtworkLoader(private val context: Context) {
    companion object {
        private const val MAX_ARTWORK_SIZE = 512
        private const val MAX_ARTWORK_BYTES = 2 * 1024 * 1024
        private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
    }

    fun extractEmbeddedArtwork(uri: Uri?): ByteArray? {
        if (uri == null) return null
        val cacheKey = uri.toString()
        memoryCache[cacheKey]?.let { return it }

        val retriever = MediaMetadataRetriever()
        val result = try {
            retriever.setDataSource(context, uri)
            val embedded = retriever.embeddedPicture
            if (embedded != null) {
                if (embedded.size <= MAX_ARTWORK_BYTES) embedded else resizeArtwork(embedded)
            } else {
                extractWithJaudiotagger(uri)
            }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
        if (result != null) {
            memoryCache[cacheKey] = result
        }
        return result
    }

    fun loadArtwork(uri: Uri?): ByteArray? {
        if (uri == null) return null
        val cacheKey = uri.toString()
        memoryCache[cacheKey]?.let { return it }

        val result = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                if (bytes.size <= MAX_ARTWORK_BYTES) bytes else resizeArtwork(bytes)
            }
        } catch (_: Exception) {
            null
        }
        if (result != null) {
            memoryCache[cacheKey] = result
        }
        return result
    }

    private fun extractWithJaudiotagger(uri: Uri): ByteArray? {
        val suffix = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                        .substringAfterLast('.', "")
                        .takeIf { it.isNotBlank() }
                        ?.let { ".$it" }
                } else {
                    null
                }
            }
        }.getOrNull() ?: ".audio"

        val temp = File.createTempFile("sonora-artwork-", suffix, context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            AudioTagger.read(Path(temp.absolutePath)).tag.artworks
                .firstOrNull()?.data
                ?.let { if (it.size <= MAX_ARTWORK_BYTES) it else resizeArtwork(it) }
        } catch (_: Exception) {
            null
        } finally {
            temp.delete()
        }
    }

    private fun resizeArtwork(embedded: ByteArray): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
            ?: return null
        return try {
            val maxDimension = maxOf(bitmap.width, bitmap.height)
            val scaled = if (maxDimension > MAX_ARTWORK_SIZE) {
                val scale = MAX_ARTWORK_SIZE.toFloat() / maxDimension
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bitmap
            }
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, output)
                output.toByteArray()
            }.also {
                if (scaled !== bitmap) scaled.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
