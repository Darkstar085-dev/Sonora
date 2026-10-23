package com.sipun.sonora.feature.sorting

import com.sipun.sonora.domain.Song
import java.text.Normalizer

enum class SongSortScope(val key: String) {
    SONGS("songs"),
    ALBUM("album"),
    ARTIST("artist"),
    PLAYLIST("playlist"),
}

enum class SongSortOption(val key: String, val label: String) {
    TITLE_ASC("TITLE_ASC", "Title (A → Z)"),
    TITLE_DESC("TITLE_DESC", "Title (Z → A)"),
    ARTIST_ASC("ARTIST_ASC", "Artist (A → Z)"),
    ARTIST_DESC("ARTIST_DESC", "Artist (Z → A)"),
    ALBUM_ASC("ALBUM_ASC", "Album (A → Z)"),
    ALBUM_DESC("ALBUM_DESC", "Album (Z → A)"),
    RECENTLY_ADDED("RECENTLY_ADDED", "Recently added"),
    RECENTLY_PLAYED("RECENTLY_PLAYED", "Recently played");

    companion object {
        fun fromKey(key: String): SongSortOption =
            entries.firstOrNull { it.key == key } ?: TITLE_ASC
    }
}

fun sortSongs(songs: List<Song>, option: SongSortOption, listeningHistoryIds: List<Long>): List<Song> =
    when (option) {
        SongSortOption.TITLE_ASC -> songs.sortedWith(compareByNatural(Song::title))
        SongSortOption.TITLE_DESC -> songs.sortedWith(compareByNatural(Song::title).reversed())
        SongSortOption.ARTIST_ASC -> songs.sortedWith(compareByNatural(Song::artist).then(compareByNatural(Song::title)))
        SongSortOption.ARTIST_DESC -> songs.sortedWith(compareByNatural(Song::artist).reversed().then(compareByNatural(Song::title)))
        SongSortOption.ALBUM_ASC -> songs.sortedWith(compareByNatural(Song::album).then(compareByNatural(Song::title)))
        SongSortOption.ALBUM_DESC -> songs.sortedWith(compareByNatural(Song::album).reversed().then(compareByNatural(Song::title)))
        SongSortOption.RECENTLY_ADDED -> songs.sortedByDescending(Song::dateAddedSeconds)
        SongSortOption.RECENTLY_PLAYED -> {
            val positions = listeningHistoryIds.withIndex().associate { it.value to it.index }
            songs.sortedBy { positions[it.id] ?: Int.MAX_VALUE }
        }
    }

private fun compareByNatural(selector: (Song) -> String): Comparator<Song> =
    Comparator { first, second -> naturalCompare(selector(first), selector(second)) }

private fun naturalCompare(first: String, second: String): Int {
    val left = normalize(first)
    val right = normalize(second)
    var li = 0
    var ri = 0
    while (li < left.length && ri < right.length) {
        if (left[li].isDigit() && right[ri].isDigit()) {
            val ls = li
            val rs = ri
            while (li < left.length && left[li].isDigit()) li++
            while (ri < right.length && right[ri].isDigit()) ri++
            val ln = left.substring(ls, li).trimStart('0').ifBlank { "0" }
            val rn = right.substring(rs, ri).trimStart('0').ifBlank { "0" }
            if (ln.length != rn.length) return ln.length.compareTo(rn.length)
            val numberCompare = ln.compareTo(rn)
            if (numberCompare != 0) return numberCompare
        } else {
            val charCompare = left[li].lowercaseChar().compareTo(right[ri].lowercaseChar())
            if (charCompare != 0) return charCompare
            li++
            ri++
        }
    }
    return left.length.compareTo(right.length)
}

private fun normalize(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)
        .filterNot { Character.getType(it.code) == Character.FORMAT.toInt() }
        .trim()
