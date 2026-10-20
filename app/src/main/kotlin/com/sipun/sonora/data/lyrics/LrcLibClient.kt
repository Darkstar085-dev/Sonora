package com.sipun.sonora.data.lyrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal class LrcLibClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(title: String, artist: String, album: String): Lyrics? {
        val exactUrl = "https://lrclib.net/api/get?track_name=" + enc(title) +
            "&artist_name=" + enc(artist) + "&album_name=" + enc(album)
        val exact = runCatching { requestExact(exactUrl) }.getOrNull()
        if (exact != null) return exact

        val searchUrl = "https://lrclib.net/api/search?track_name=" + enc(title) +
            "&artist_name=" + enc(artist)
        return requestSearch(searchUrl, title, artist, album)
    }

    private fun requestExact(url: String): Lyrics? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            json.decodeFromString<Response>(
                connection.inputStream.bufferedReader().use { it.readText() },
            ).toLyrics()
        } finally {
            connection.disconnect()
        }
    }

    private fun requestSearch(
        url: String,
        title: String,
        artist: String,
        album: String,
    ): Lyrics? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val responses = json.decodeFromString<List<Response>>(
                connection.inputStream.bufferedReader().use { it.readText() },
            )
            responses
                .filter { !it.instrumental && it.hasLyrics() }
                .sortedByDescending { it.matchScore(title, artist, album) }
                .firstNotNullOfOrNull { it.toLyrics() }
        } finally {
            connection.disconnect()
        }
    }

    private fun Response.hasLyrics(): Boolean =
        !plainLyrics.isNullOrBlank() || !syncedLyrics.isNullOrBlank()

    private fun Response.toLyrics(): Lyrics? {
        if (instrumental) return null
        val synced = syncedLyrics?.takeIf(String::isNotBlank)?.let(LyricsParser::parse)
        if (synced != null && synced.lines.isNotEmpty()) return synced
        val plain = plainLyrics?.takeIf(String::isNotBlank) ?: return null
        val lines = plain.lines().filter(String::isNotBlank).map(::LyricsLine)
        return lines.takeIf { it.isNotEmpty() }?.let { Lyrics(it, false) }
    }

    private fun Response.matchScore(title: String, artist: String, album: String): Int {
        var score = 0
        if (trackName.normalized() == title.normalized()) score += 4
        if (artistName.normalized() == artist.normalized()) score += 4
        if (albumName.normalized() == album.normalized()) score += 1
        return score
    }

    private fun String?.normalized(): String = this.orEmpty().trim().lowercase()

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}

@Serializable
private data class Response(
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val instrumental: Boolean = false,
)
