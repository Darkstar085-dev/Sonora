package com.sipun.sonora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongActionConfig
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.SongMoreButton
import java.text.Normalizer

@Composable
internal fun SongList(
    songs: List<Song>,
    player: PlayerController,
    preferences: SonoraPreferences,
    open: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val sortedSongs = remember(songs) { songs.sortedWith(songTitleComparator) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
    ) {
        itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
            SongCard(
                song = song,
                player = player,
                onClick = { player.playQueue(sortedSongs, index); open() },
                preferences = preferences,
                onOpenAlbum = openAlbum,
                onOpenArtist = openArtist,
                actions = SongActionConfig(
                    showRemoveFromDevice = true,
                    showEditMetadata = true,
                ),
                onChanged = onChanged,
            )
        }
    }
}

private val songTitleComparator = Comparator<Song> { first, second ->
    naturalCompare(first.title, second.title)
        .takeIf { it != 0 }
        ?: naturalCompare(first.artist, second.artist)
            .takeIf { it != 0 }
        ?: naturalCompare(first.album, second.album)
            .takeIf { it != 0 }
        ?: first.id.compareTo(second.id)
}

private fun naturalCompare(first: String, second: String): Int {
    val left = normalizeForSort(first)
    val right = normalizeForSort(second)
    var leftIndex = 0
    var rightIndex = 0

    while (leftIndex < left.length && rightIndex < right.length) {
        val leftChar = left[leftIndex]
        val rightChar = right[rightIndex]

        if (leftChar.isDigit() && rightChar.isDigit()) {
            val leftStart = leftIndex
            val rightStart = rightIndex
            while (leftIndex < left.length && left[leftIndex].isDigit()) leftIndex++
            while (rightIndex < right.length && right[rightIndex].isDigit()) rightIndex++

            val leftNumber = left.substring(leftStart, leftIndex).trimStart('0').ifBlank { "0" }
            val rightNumber = right.substring(rightStart, rightIndex).trimStart('0').ifBlank { "0" }

            if (leftNumber.length != rightNumber.length) {
                return leftNumber.length.compareTo(rightNumber.length)
            }

            val numericCompare = leftNumber.compareTo(rightNumber)
            if (numericCompare != 0) return numericCompare
            continue
        }

        val charCompare = leftChar.lowercaseChar().compareTo(rightChar.lowercaseChar())
        if (charCompare != 0) return charCompare

        leftIndex++
        rightIndex++
    }

    return left.length.compareTo(right.length)
}

private fun normalizeForSort(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)
        .filterNot { Character.getType(it.code) == Character.FORMAT.toInt() }
        .trim()


@Composable
internal fun FavoriteList(
    songs: List<Song>,
    player: PlayerController,
    preferences: SonoraPreferences,
    open: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
    onChanged: () -> Unit,
) {
    if (songs.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.FavoriteBorder,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "No favorites yet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                "Tap the heart on a song to save it here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongCard(
                song = song,
                player = player,
                onClick = { player.playQueue(songs, index); open() },
                preferences = preferences,
                onOpenAlbum = openAlbum,
                onOpenArtist = openArtist,
                onChanged = onChanged,
            )
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    player: PlayerController,
    onClick: () -> Unit,
    preferences: SonoraPreferences,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    actions: SongActionConfig = SongActionConfig(),
    onChanged: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song, Modifier.size(56.dp))
        Column(
            Modifier
                .padding(start = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist + " • " + song.album,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SongMoreButton(
            song = song,
            preferences = preferences,
            playerController = player,
            actions = SongActionConfig(
                showPlayNext = true,
                showAddToQueue = true,
                showEditMetadata = actions.showEditMetadata,
                showRemoveFromDevice = actions.showRemoveFromDevice,
            ),
            onOpenAlbum = onOpenAlbum,
            onOpenArtist = onOpenArtist,
            onChanged = onChanged,
        )
    }
}

@Composable
internal fun AlbumList(songs: List<Song>, openAlbum: (String) -> Unit) {
    val albums = songs.groupBy(Song::album)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(albums.keys.sorted(), key = { it }) { album ->
            val tracks = albums.getValue(album)
            Card(
                onClick = { openAlbum(album) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        SongArtworkImage(
                            song = tracks.first(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            album,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            tracks.first().artist + " • " + songCountLabel(tracks.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArtistList(songs: List<Song>, openArtist: (String) -> Unit) {
    val artists = songs.groupBy(Song::artist)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(artists.keys.sorted(), key = { it }) { artist ->
            val tracks = artists.getValue(artist)
            Card(
                onClick = { openArtist(artist) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        tracks.firstOrNull()?.let {
                            SongArtworkImage(
                                song = it,
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = "Artist artwork",
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            artist,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            songCountLabel(tracks.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun Artwork(song: Song, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        SongArtworkImage(
            song = song,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
