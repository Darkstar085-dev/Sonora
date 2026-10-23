@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.sipun.sonora.domain.Song
import com.sipun.sonora.feature.sorting.SongSortOption
import com.sipun.sonora.feature.sorting.SongSortScope
import com.sipun.sonora.feature.sorting.sortSongs
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.PlayingEqualizer
import com.sipun.sonora.ui.components.SongActionConfig
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.SongCollectionControls
import com.sipun.sonora.ui.components.SongMoreButton
import com.sipun.sonora.ui.components.SonoraBackButton

@Composable
fun AlbumDetailScreen(
    albumName: String,
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    val metadataRefreshVersion by AndroidMusicRepository.refreshVersion.collectAsState()
    val playerState by playerController.state.collectAsState()
    val repository = remember(context, metadataRefreshVersion) { AndroidMusicRepository(context) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val preferences =
        remember(context) { com.sipun.sonora.data.preferences.SonoraPreferences(context) }
    var sortOption by remember(albumName) {
        mutableStateOf(SongSortOption.fromKey(preferences.sortOption(SongSortScope.ALBUM.key)))
    }

    LaunchedEffect(albumName, metadataRefreshVersion) {
        repository.invalidateCache()
        songs = repository.songs().filter { it.album.equals(albumName, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Album") },
                navigationIcon = {
                    SonoraBackButton(onClick = onBack)
                },
            )
        },
    ) { padding ->
        if (songs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val history = preferences.listeningHistoryIds()
            val sortedSongs =
                remember(songs, sortOption, history) { sortSongs(songs, sortOption, history) }
            val first = sortedSongs.first()
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.72f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            SongArtworkImage(
                                song = first,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Text(
                            first.album,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Text(
                            first.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            if (songs.size == 1) "1 song" else songs.size.toString() + " songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        SongCollectionControls(
                            songs = sortedSongs,
                            playerController = playerController,
                            onOpenNowPlaying = onOpenNowPlaying,
                            scope = SongSortScope.ALBUM,
                            currentOption = sortOption,
                            onOptionSelected = { option ->
                                sortOption = option
                                preferences.setSortOption(SongSortScope.ALBUM.key, option.key)
                            },
                        )
                    }
                }
                item {
                    Text(
                        "Songs",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
                    Card(
                        onClick = {
                            playerController.playQueue(sortedSongs, index)
                            onOpenNowPlaying()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                (index + 1).toString().padStart(2, '0'),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(34.dp),
                            )
                            Column(Modifier.weight(1f)) {
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
                            }
                            if (playerState.currentSong?.id == song.id) {
                                PlayingEqualizer(
                                    isPlaying = playerState.isPlaying,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            SongMoreButton(
                                song = song,
                                preferences = preferences,
                                playerController = playerController,
                                actions = SongActionConfig(
                                    showAlbum = false,
                                    showPlayNext = true,
                                    showAddToQueue = true,
                                ),
                                onOpenArtist = onOpenArtist,
                            )
                        }
                    }
                }
            }
        }
    }
}
