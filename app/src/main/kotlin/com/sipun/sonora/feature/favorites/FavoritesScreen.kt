package com.sipun.sonora.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.PlayingEqualizer

@Composable
fun FavoritesScreen(
    playerController: PlayerController,
    onOpenNowPlaying: () -> Unit,
) {
    val context = LocalContext.current
    val metadataRefreshVersion by AndroidMusicRepository.refreshVersion.collectAsState()
    val repository = remember(context, metadataRefreshVersion) { AndroidMusicRepository(context) }
    val preferences = remember(context) { SonoraPreferences(context) }
    val playerState by playerController.state.collectAsState()
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(metadataRefreshVersion) {
        repository.invalidateCache()
        songs = repository.songs().filter { it.id in preferences.favoriteIds() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Favorites", style = MaterialTheme.typography.headlineSmall)
                Text(
                    songs.size.toString() + if (songs.size == 1) " song" else " songs",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (songs.isNotEmpty()) {
                FilledIconButton(
                    onClick = {
                        playerController.playQueue(songs)
                        onOpenNowPlaying()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play favorites")
                }
            }
        }

        if (songs.isEmpty()) {
            EmptyFavorites()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    FavoriteSongCard(
                        song = song,
                        isCurrentSong = playerState.currentSong?.id == song.id,
                        isPlaying = playerState.isPlaying,
                        onClick = {
                            playerController.playQueue(songs, index)
                            onOpenNowPlaying()
                        },
                        onRemove = {
                            preferences.toggleFavorite(song.id)
                            songs = songs.filterNot { it.id == song.id }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteSongCard(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongArtworkImage(
                song = song,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
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
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyFavorites() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            "No favorites yet",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            "Tap the heart on a song to save it here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
