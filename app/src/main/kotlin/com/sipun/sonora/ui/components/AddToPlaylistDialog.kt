package com.sipun.sonora.ui.components

import com.sipun.sonora.ui.theme.accentGradientTint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.SonoraPreferences

@Composable
fun AddToPlaylistDialog(songId: Long, preferences: SonoraPreferences, onDismiss: () -> Unit) {
    var playlists by remember { mutableStateOf(preferences.playlists()) }
    var showCreate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        stringResource(R.string.create_playlist_to_save),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            val added = songId in playlist.songIds
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = {
                                    Text(
                                        if (added) {
                                            stringResource(R.string.already_added)
                                        } else if (playlist.songIds.size == 1) {
                                            stringResource(R.string.song_count_one)
                                        } else {
                                            stringResource(
                                                R.string.song_count_other,
                                                playlist.songIds.size
                                            )
                                        }
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.LibraryMusic,
                                        null,
                                        tint = if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    , modifier = Modifier.accentGradientTint())
                                },
                                trailingContent = {
                                    if (added) Text(
                                        stringResource(R.string.added),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    else IconButton(onClick = {
                                        preferences.addToPlaylist(playlist.id, songId)
                                        playlists = preferences.playlists()
                                    }) {
                                        Icon(
                                            Icons.Default.Add,
                                            stringResource(
                                                R.string.action_add_to_playlist,
                                                playlist.name
                                            )
                                        , modifier = Modifier.accentGradientTint())
                                    }
                                },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { showCreate = true },
                    modifier = Modifier.padding(top = 6.dp)
                ) { Text(stringResource(R.string.action_create_new_playlist)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.new_playlist)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.createPlaylist(name)
                            ?.let { preferences.addToPlaylist(it.id, songId) }
                        playlists = preferences.playlists()
                        showCreate = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreate = false
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
