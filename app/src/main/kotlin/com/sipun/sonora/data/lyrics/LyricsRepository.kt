package com.sipun.sonora.data.lyrics

import android.content.Context
import com.sipun.sonora.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

internal class LyricsRepository(context: Context) {
    private val client = LrcLibClient()
    private val cache = LyricsCache(File(context.cacheDir, "lyrics"))

    suspend fun get(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val key = listOf(song.title, song.artist, song.album)
            .joinToString("|") { it.trim().lowercase() }
        cache.get(key)?.takeIf { it.lines.isNotEmpty() }?.let { return@withContext it }

        var lastFailure: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val result = client.fetch(song.title, song.artist, song.album)
                if (result != null && result.lines.isNotEmpty()) {
                    cache.put(key, result)
                    return@withContext result
                }
                lastFailure = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastFailure = e
            }

            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }

        lastFailure?.let { throw it }
        null
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 500L
    }
}
