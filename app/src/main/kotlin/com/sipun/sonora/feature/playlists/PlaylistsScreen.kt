package com.sipun.sonora.feature.playlists

import androidx.compose.ui.graphics.Color
import com.sipun.sonora.ui.theme.accentGradientTint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.theme.LocalAccentBrush

@Composable
fun PlaylistsScreen(
    playerController: PlayerController,
    onOpenPlaylist: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { SonoraPreferences(context) }
    var playlists by remember { mutableStateOf(preferences.playlists()) }
    var showCreate by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.your_playlists),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    if (playlists.size == 1) {
                        stringResource(R.string.playlist_count_one)
                    } else {
                        stringResource(R.string.playlist_count_other, playlists.size)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = { showCreate = true },
                modifier = Modifier.background(
                    LocalAccentBrush.current,
                    RoundedCornerShape(50),
                ),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) { Icon(Icons.Default.Add, stringResource(R.string.action_create_playlist), modifier = Modifier.accentGradientTint()) }
        }

        if (playlists.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.LibraryMusic,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp).accentGradientTint()
                )
                Text(
                    stringResource(R.string.create_first_playlist),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    stringResource(R.string.build_collections),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(240)) + slideInVertically(initialOffsetY = { it / 12 }, animationSpec = tween(260)),
                    ) {
                        Card(
                            onClick = { onOpenPlaylist(playlist.id) },
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            shape = RoundedCornerShape(22.dp),
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
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                , modifier = Modifier.accentGradientTint())
                            }
                            Column(
                                Modifier
                                    .padding(start = 12.dp)
                                    .weight(1f)
                            ) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (playlist.songIds.size == 1) {
                                        stringResource(R.string.song_count_one)
                                    } else {
                                        stringResource(
                                            R.string.song_count_other,
                                            playlist.songIds.size
                                        )
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                preferences.deletePlaylist(playlist.id)
                                playlists = preferences.playlists()
                            }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    stringResource(R.string.action_delete_playlist)
                                , modifier = Modifier.accentGradientTint())
                            }
                        }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = {
                preferences.createPlaylist(it)
                playlists = preferences.playlists()
                showCreate = false
            },
        )
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(
                    stringResource(R.string.action_create)
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
