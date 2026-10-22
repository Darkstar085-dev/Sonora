package com.sipun.sonora.data.lyrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

internal class LrcLibClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(title: String, artist: String, album: String): Lyrics? {
        val exactUrl = "https://lrclib.net/api/get?track_name=" + enc(title) +
            "&artist_name=" + enc(artist) + "&album_name=" + enc(album)
        var best = runCatching { requestExact(exactUrl) }.getOrNull()
            ?.takeIf { !it.instrumental && it.hasLyrics() }
        var bestScore = best?.let {
            it.matchScore(title, artist, album) + it.preferenceScore(title, artist, album)
        } ?: Int.MIN_VALUE

        val titles = titleVariants(title)
        val artists = artistVariants(artist)
        val urls = linkedSetOf<String>()
        titles.forEach { candidateTitle ->
            artists.take(3).forEach { candidateArtist ->
                urls += searchUrl(candidateTitle, candidateArtist)
            }
        }
        titles.forEach { candidateTitle -> urls += searchUrl(candidateTitle, null) }

        for (url in urls.take(MAX_SEARCHES)) {
            val responses = runCatching { requestSearchResults(url) }.getOrNull() ?: continue
            val candidate = responses
                .asSequence()
                .filter { !it.instrumental && it.hasLyrics() }
                .map { response -> response to response.matchScore(title, artist, album) }
                .filter { (_, score) -> score >= MIN_MATCH_SCORE }
                .map { (response, score) -> response to (score + response.preferenceScore(title, artist, album)) }
                .maxByOrNull { it.second }
                ?: continue
            if (candidate.second > bestScore) {
                best = candidate.first
                bestScore = candidate.second
            }
        }
        return best?.toLyrics(title, artist, album)
    }

    private fun requestExact(url: String): Response? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            json.decodeFromString<Response>(
                connection.inputStream.bufferedReader().use { it.readText() },
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun requestSearchResults(url: String): List<Response> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            json.decodeFromString<List<Response>>(
                connection.inputStream.bufferedReader().use { it.readText() },
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun searchUrl(title: String, artist: String?): String =
        "https://lrclib.net/api/search?track_name=" + enc(title) +
            (artist?.takeIf { it.isNotBlank() }?.let { "&artist_name=" + enc(it) } ?: "")

    private fun titleVariants(value: String): List<String> {
        val cleaned = value
            .replace(
                Regex("""\s*\((?i:from\s+["']?.*|remaster(?:ed)?[^)]*|official[^)]*)\)\s*"""),
                " ",
            )
            .replace(Regex("""\s*\[(?i:remaster(?:ed)?[^]]*|official[^]]*)\]\s*"""), " ")
            .replace(Regex("""\s+-\s+(?i:remaster(?:ed)?|official audio|lyrics video).*$"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return listOf(value.trim(), cleaned)
            .filter { it.isNotBlank() }
            .distinctBy { it.normalized() }
    }

    private fun artistVariants(value: String): List<String> {
        val primaryArtists = value.split(
            Regex(
                """\s*(?:,|;|&|\band\b|\bfeat\.?\b|\bft\.?\b|\bx\b)\s*""",
                RegexOption.IGNORE_CASE,
            ),
        ).map(String::trim).filter(String::isNotBlank)
        return (listOf(value.trim()) + primaryArtists)
            .filter(String::isNotBlank)
            .distinctBy { it.normalized() }
    }

    private fun Response.hasLyrics(): Boolean =
        !plainLyrics.isNullOrBlank() || !syncedLyrics.isNullOrBlank()

    private fun Response.toLyrics(title: String, artist: String, album: String): Lyrics? {
        if (instrumental) return null
        val (selectedText, selectedSynced) = selectLyricsText(title, artist, album) ?: return null
        if (selectedSynced) {
            val synced = LyricsParser.parse(selectedText)
            if (synced.lines.isNotEmpty()) return synced
        }
        val lines = selectedText.lines().filter(String::isNotBlank).map(::LyricsLine)
        return lines.takeIf { it.isNotEmpty() }?.let { Lyrics(it, false) }
    }

    private fun Response.preferenceScore(title: String, artist: String, album: String): Int {
        val selection = selectLyricsText(title, artist, album) ?: return 0
        val (lyrics, synced) = selection
        return (if (synced) 4 else 0) + languageScore(lyrics)
    }

    private fun Response.selectLyricsText(
        title: String,
        artist: String,
        album: String,
    ): Pair<String, Boolean>? {
        val synced = syncedLyrics?.takeIf(String::isNotBlank)
        val plain = plainLyrics?.takeIf(String::isNotBlank)
        if (synced == null) return plain?.let { it to false }
        if (plain == null) return synced to true

        if (languageScore(plain) > languageScore(synced) + 4) {
            return plain to false
        }
        return synced to true
    }

    private fun languageScore(lyrics: String): Int {
        val devanagariCount = lyrics.count { it.code in 0x0900..0x097F }
        val latinCount = lyrics.count { it in 'a'..'z' || it in 'A'..'Z' }
        return when {
            devanagariCount >= 12 -> 30
            latinCount > 0 && hasRomanizedHindi(lyrics) -> 18
            latinCount > 0 -> 8
            else -> 0
        }
    }

    private fun hasRomanizedHindi(value: String): Boolean {
        val tokens = value.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z ]"), " ")
            .split(Regex("\\s+"))
            .toSet()
        val HindiWords = setOf(
            "hai", "hain", "ho", "hoon", "hu", "main", "mein", "tum", "tujh",
            "tera", "teri", "mere", "meri", "dil", "ishq", "pyaar", "pyar",
            "mohabbat", "aankhon", "saath", "kaise", "kyun", "kya", "chale",
            "jaane", "zindagi", "khuda", "sanam", "aaja", "aao", "chahun",
        )
        return tokens.intersect(HindiWords).size >= 2
    }

    private fun Response.matchScore(title: String, artist: String, album: String): Int {
        val wantedTitles = titleVariants(title).map { it.normalized() }.filter(String::isNotBlank).toSet()
        val candidateTitle = trackName.normalized()
        if (wantedTitles.isEmpty() || candidateTitle.isBlank()) return Int.MIN_VALUE
        val titleExact = candidateTitle in wantedTitles
        val bestTitle = wantedTitles.maxByOrNull { candidate ->
            when {
                candidateTitle == candidate -> 100
                candidateTitle.contains(candidate) || candidate.contains(candidateTitle) -> 50
                else -> candidate.split(' ').intersect(candidateTitle.split(' ').toSet()).size
            }
        }.orEmpty()
        val wantedTokens = bestTitle.split(' ').filter(String::isNotBlank).toSet()
        val candidateTokens = candidateTitle.split(' ').filter(String::isNotBlank).toSet()
        val titleOverlap = wantedTokens.intersect(candidateTokens).size
        val titleClose = !titleExact &&
            (candidateTitle.contains(bestTitle) || bestTitle.contains(candidateTitle)) &&
            minOf(wantedTokens.size, candidateTokens.size) >= 2 &&
            titleOverlap >= minOf(2, wantedTokens.size)
        if (!titleExact && !titleClose) return Int.MIN_VALUE

        val wantedArtist = artist.normalized()
        val candidateArtist = artistName.normalized()
        val wantedAlbum = album.normalized()
        val candidateAlbum = albumName.normalized()
        var score = if (titleExact) 8 else 4
        if (wantedArtist.isNotBlank() && candidateArtist.isNotBlank()) {
            if (candidateArtist == wantedArtist) score += 6
            else {
                val wantedTokens = wantedArtist.split(' ').filter(String::isNotBlank).toSet()
                val candidateTokens = candidateArtist.split(' ').filter(String::isNotBlank).toSet()
                val overlap = wantedTokens.intersect(candidateTokens).size
                if (overlap > 0) score += minOf(4, overlap * 2)
            }
        }
        if (wantedAlbum.isNotBlank() && candidateAlbum == wantedAlbum) score += 2
        return score
    }

    private fun String?.normalized(): String = Normalizer
        .normalize(this.orEmpty().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("""\([^)]*\)"""), " ")
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
        .replace(Regex("""\s+"""), " ")

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val CONNECT_TIMEOUT_MS = 2_500
        const val READ_TIMEOUT_MS = 3_500
        const val MAX_SEARCHES = 2
        const val MIN_MATCH_SCORE = 6
    }
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
