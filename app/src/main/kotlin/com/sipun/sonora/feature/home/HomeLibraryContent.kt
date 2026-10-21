package com.sipun.sonora.feature.home

import androidx.compose.ui.graphics.Color
import com.sipun.sonora.ui.theme.accentGradientTint
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
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.feature.sorting.SongSortOption
import com.sipun.sonora.feature.sorting.SongSortScope
import com.sipun.sonora.feature.sorting.sortSongs
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongActionConfig
import com.sipun.sonora.ui.components.PlayingEqualizer
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.SongMoreButton
import com.sipun.sonora.ui.components.SongSortButton

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
    var sortOption by remember(preferences) {
        mutableStateOf(SongSortOption.fromKey(preferences.sortOption(SongSortScope.SONGS.key)))
    }
    val playerState by player.state.collectAsStateWithLifecycle()
    val history = preferences.listeningHistoryIds()
    val sortedSongs = remember(songs, sortOption, history) { sortSongs(songs, sortOption, history) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { player.playQueueShuffled(sortedSongs) }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle songs",
                        tint = Color.White, modifier = Modifier.accentGradientTint())
                }
                SongSortButton(
                    scope = SongSortScope.SONGS,
                    currentOption = sortOption,
                    onOptionSelected = { option ->
                        sortOption = option
                        preferences.setSortOption(SongSortScope.SONGS.key, option.key)
                    },
                )
            }
        }
        itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
            SongCard(
                song = song,
                player = player,
                isCurrentSong = playerState.currentSong?.id == song.id,
                isPlaying = playerState.isPlaying,
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
    val playerState by player.state.collectAsStateWithLifecycle()
    if (songs.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.FavoriteBorder,
                null,
                tint = Color.White,
                modifier = Modifier.size(56.dp).accentGradientTint()
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
                isCurrentSong = playerState.currentSong?.id == song.id,
                isPlaying = playerState.isPlaying,
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
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    preferences: SonoraPreferences,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    actions: SongActionConfig = SongActionConfig(),
    onChanged: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
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
            if (isCurrentSong) PlayingEqualizer(isPlaying = isPlaying, modifier = Modifier.padding(end = 8.dp))
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
