package com.sipun.sonora.feature.library

import androidx.compose.ui.graphics.Color
import com.sipun.sonora.ui.theme.accentGradientTint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.content.ContextCompat
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.AddToPlaylistDialog
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.PlayingEqualizer

@Composable
fun LibraryScreen(playerController: PlayerController, onOpenNowPlaying: () -> Unit) {
    val context = LocalContext.current
    val metadataRefreshVersion by AndroidMusicRepository.refreshVersion.collectAsState()
    val repository = remember(context, metadataRefreshVersion) { AndroidMusicRepository(context) }
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val preferences = remember(context) { SonoraPreferences(context) }
    val playerState by playerController.state.collectAsState()
    var playlistSongId by remember { mutableStateOf<Long?>(null) }
    var favoriteIds by remember { mutableStateOf(preferences.favoriteIds()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }

    LaunchedEffect(granted, metadataRefreshVersion) {
        repository.invalidateCache()
        if (!granted) return@LaunchedEffect
        loading = true
        songs = repository.songs()
        loading = false
    }

    if (!granted) return LibraryPermission { launcher.launch(permission) }
    if (loading) return BoxedLibraryState()
    if (songs.isEmpty()) return EmptyLibraryContent()

    playlistSongId?.let { songId ->
        AddToPlaylistDialog(songId, preferences) {
            playlistSongId = null
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Your library", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Local music on this device",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    songs.size.toString() + if (songs.size == 1) " song" else " songs",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            Card(
                onClick = { playerController.playQueue(songs, index); onOpenNowPlaying() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongArtworkImage(
                        song = song,
                        modifier = Modifier
                            .size(56.dp)
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
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist + " • " + song.album,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (playerState.currentSong?.id == song.id) PlayingEqualizer(isPlaying = playerState.isPlaying, modifier = Modifier.padding(end = 4.dp))
                    IconButton(onClick = {
                        playlistSongId = song.id
                    }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist", modifier = Modifier.accentGradientTint()) }
                    IconButton(onClick = {
                        favoriteIds = favoriteIds.toMutableSet()
                            .also { ids -> if (!ids.add(song.id)) ids.remove(song.id) }
                            .also { ids -> preferences.toggleFavorite(song.id) }
                    }) {
                        Icon(
                            if (song.id in favoriteIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",
                            tint = if (song.id in favoriteIds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.accentGradientTint(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryPermission(onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            null,
            tint = Color.White,
            modifier = Modifier.size(48.dp).accentGradientTint()
        )
        Text(
            "Access your music",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "Sonora needs audio access to discover music stored on this device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        Button(onClick = onRequest) { Text("Allow access") }
    }
}

@Composable
private fun BoxedLibraryState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text("Scanning your music…", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EmptyLibraryContent() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.AudioFile,
            null,
            tint = Color.White,
            modifier = Modifier.size(48.dp).accentGradientTint()
        )
        Text(
            "No music found",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "Add supported audio files to this device and open Sonora again.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
