package com.sipun.sonora.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.preferences.SonoraPlaylist
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.ui.theme.LocalAccentBrush
import com.sipun.sonora.ui.theme.accentGradientTint

@Composable
internal fun PlaylistList(
    playlists: List<SonoraPlaylist>,
    openPlaylist: (String) -> Unit,
    onRefresh: () -> Unit,
    preferences: SonoraPreferences,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your playlists", style = MaterialTheme.typography.titleLarge)
                Text(
                    playlists.size.toString() + if (playlists.size == 1) " playlist" else " playlists",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            var showCreate by remember { mutableStateOf(false) }
            IconButton(onClick = { showCreate = true }) {
                Icon(
                    Icons.Default.Add,
                    "Create playlist",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.accentGradientTint()
                )
            }
            if (showCreate) {
                CreatePlaylistDialog(
                    onDismiss = { showCreate = false },
                    onCreate = {
                        preferences.createPlaylist(it)
                        showCreate = false
                        onRefresh()
                    },
                )
            }
        }

        if (playlists.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistPlay,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(56.dp)
                        .accentGradientTint()
                )
                Text(
                    "No playlists yet",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    "Create a playlist to organize your music.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 132.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        onClick = { openPlaylist(playlist.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(58.dp)
                                    .background(
                                        LocalAccentBrush.current,
                                        RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistPlay,
                                    null,
                                    tint = Color.White,
                                )
                            }
                            Column(
                                Modifier
                                    .padding(start = 12.dp)
                                    .weight(1f)
                            ) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    playlist.songIds.size.toString() + if (playlist.songIds.size == 1) " song" else " songs",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                preferences.deletePlaylist(playlist.id)
                                onRefresh()
                            }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    "Delete playlist",
                                    modifier = Modifier.accentGradientTint()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
