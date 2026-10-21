@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.playlists

import androidx.compose.ui.graphics.Color
import com.sipun.sonora.ui.theme.accentGradientTint
import com.sipun.sonora.ui.components.SonoraBackButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.sipun.sonora.feature.sorting.SongSortOption
import com.sipun.sonora.feature.sorting.SongSortScope
import com.sipun.sonora.feature.sorting.sortSongs
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongActionConfig
import com.sipun.sonora.ui.components.PlayingEqualizer
import com.sipun.sonora.ui.components.SongMoreButton
import com.sipun.sonora.ui.components.SongCollectionControls

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    val metadataRefreshVersion by AndroidMusicRepository.refreshVersion.collectAsState()
    val playerState by playerController.state.collectAsState()
    val repository = remember(context, metadataRefreshVersion) { AndroidMusicRepository(context) }
    val preferences = remember(context) { SonoraPreferences(context) }
    var playlist by remember {
        mutableStateOf(
            preferences.playlists().firstOrNull { it.id == playlistId })
    }
    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var sortOption by remember(playlistId) {
        mutableStateOf(SongSortOption.fromKey(preferences.sortOption(SongSortScope.PLAYLIST.key)))
    }

    LaunchedEffect(metadataRefreshVersion) {
        repository.invalidateCache()
        allSongs = repository.songs()
    }

    val songs =
        playlist?.songIds?.mapNotNull { id -> allSongs.firstOrNull { it.id == id } }.orEmpty()
    val history = preferences.listeningHistoryIds()
    val sortedSongs = remember(songs, sortOption, history) { sortSongs(songs, sortOption, history) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist") },
                navigationIcon = {
                    SonoraBackButton(onClick = onBack)
                },
            )
        },
    ) { padding ->
        if (playlist == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                Text("Playlist not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(72.dp).accentGradientTint()
                            )
                        }
                        Text(
                            playlist?.name ?: "Playlist",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Text(
                            songs.size.toString() + if (songs.size == 1) " song" else " songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        if (sortedSongs.isNotEmpty()) {
                            SongCollectionControls(
                                songs = sortedSongs,
                                playerController = playerController,
                                onOpenNowPlaying = onOpenNowPlaying,
                                scope = SongSortScope.PLAYLIST,
                                currentOption = sortOption,
                                onOptionSelected = { option ->
                                    sortOption = option
                                    preferences.setSortOption(SongSortScope.PLAYLIST.key, option.key)
                                },
                            )
                        }
                    }
                }
                itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
                    Card(
                        onClick = { playerController.playQueue(sortedSongs, index); onOpenNowPlaying() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                (index + 1).toString().padStart(2, '0'),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(34.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.artist + " • " + song.album,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (playerState.currentSong?.id == song.id) {
                                PlayingEqualizer(isPlaying = playerState.isPlaying, modifier = Modifier.padding(end = 8.dp))
                            }
                            SongMoreButton(
                                song = song,
                                preferences = preferences,
                                playerController = playerController,
                                actions = SongActionConfig(
                                    showPlaylist = false,
                                    showRemoveFromPlaylist = true,
                                    showPlayNext = true,
                                    showAddToQueue = true,
                                ),
                                onOpenAlbum = onOpenAlbum,
                                onOpenArtist = onOpenArtist,
                                onRemoveFromPlaylist = {
                                    preferences.removeFromPlaylist(playlistId, song.id)
                                    playlist =
                                        preferences.playlists().firstOrNull { it.id == playlistId }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
