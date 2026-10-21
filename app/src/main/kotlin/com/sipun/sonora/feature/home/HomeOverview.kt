package com.sipun.sonora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.ui.theme.LocalAccentBrush
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.PlayerState
import com.sipun.sonora.ui.components.SongMoreButton

@Composable
internal fun Overview(
    songs: List<Song>,
    favoriteCount: Int,
    playlistCount: Int,
    currentSongId: Long?,
    playerState: PlayerState,
    player: PlayerController,
    preferences: SonoraPreferences,
    open: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
) {
    val albums = songs.map(Song::album).distinct()
    val artists = songs.map(Song::artist).distinct()
    val recentlyAdded =
        remember(songs) { songs.sortedWith(compareByDescending<Song> { it.dateAddedSeconds }.thenByDescending { it.id }) }
    val songsById = remember(songs) { songs.associateBy(Song::id) }
    val listeningHistory = remember(currentSongId, songs) {
        preferences.listeningHistoryIds().mapNotNull(songsById::get).take(5)
    }
    val libraryArtwork =
        songsById[currentSongId] ?: recentlyAdded.firstOrNull() ?: songs.firstOrNull()

    val nowPlayingSong = playerState.currentSong
    val showNowPlaying = nowPlayingSong != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
    ) {
        item {
            LibraryLottieCard(
                songCount = songs.size,
                nowPlayingSong = nowPlayingSong,
                state = playerState,
                player = player,
                onOpenNowPlaying = open,
                onPlayAll = {
                    player.playQueueShuffled(songs)
                },
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverviewMetric(
                        stringResource(R.string.tab_songs),
                        songs.size,
                        Modifier.weight(1f),
                        Icons.Default.MusicNote,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_albums),
                        albums.size,
                        Modifier.weight(1f),
                        Icons.Default.Album,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_artists),
                        artists.size,
                        Modifier.weight(1f),
                        Icons.Default.Person,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_favorites),
                        favoriteCount,
                        Modifier.weight(1f),
                        Icons.Default.Favorite,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_playlists),
                        playlistCount,
                        Modifier.weight(1f),
                        Icons.AutoMirrored.Filled.PlaylistPlay,
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.recently_added),
                style = MaterialTheme.typography.titleLarge
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 10.dp),
            ) {
                items(recentlyAdded.take(10), key = { it.id }) { song ->
                    Box(Modifier.width(188.dp)) {
                        Card(
                            onClick = { player.playQueue(songs, songs.indexOf(song)); open() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Box {
                                    Artwork(
                                        song,
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp)),
                                    )
                                    FilledIconButton(
                                        onClick = {
                                            player.playQueue(songs, songs.indexOf(song))
                                            open()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(LocalAccentBrush.current),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.White,
                                        )
                                    }
                                }
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                                Text(
                                    song.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        SongMoreButton(
                            song = song,
                            preferences = preferences,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            onOpenAlbum = openAlbum,
                            onOpenArtist = openArtist,
                        )
                    }
                }
            }
        }
        if (listeningHistory.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.continue_listening),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.size(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column {
                        listeningHistory.forEachIndexed { index, song ->
                            val isCurrentSong = song.id == currentSongId
                            val durationMs = if (isCurrentSong) {
                                playerState.durationMs
                            } else {
                                song.durationMs
                            }
                            val positionMs = if (isCurrentSong) {
                                playerState.positionMs
                            } else {
                                preferences.lastPlayedPositionMs(song.id)
                            }
                            val progress = if (durationMs > 0L) {
                                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            Card(
                                onClick = { player.playQueue(songs, songs.indexOf(song)); open() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Artwork(
                                        song,
                                        Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                    )
                                    Column(
                                        Modifier
                                            .padding(start = 12.dp)
                                            .weight(1f),
                                    ) {
                                        Text(
                                            song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            song.artist,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(7.dp))
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                ),
                                        ) {
                                            if (progress > 0f) {
                                                Box(
                                                    Modifier
                                                        .fillMaxWidth(progress)
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(LocalAccentBrush.current),
                                                )
                                            }
                                        }
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            player.playQueue(songs, songs.indexOf(song))
                                            open()
                                        },
                                        modifier = Modifier
                                            .padding(start = 10.dp)
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(LocalAccentBrush.current),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            modifier = Modifier.size(26.dp),
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                            if (index < listeningHistory.lastIndex) HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }
    }
}
