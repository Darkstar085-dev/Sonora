@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.ui.components

import com.sipun.sonora.ui.theme.accentGradientTint
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.data.media.AudioMetadataEditor
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import kotlinx.coroutines.launch

@Composable
fun SongMoreButton(
    song: Song,
    preferences: SonoraPreferences,
    playerController: PlayerController? = null,
    modifier: Modifier = Modifier,
    actions: SongActionConfig = SongActionConfig(),
    onOpenAlbum: ((String) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onChanged: () -> Unit = {},
) {
    var showMore by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showEditMetadata by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onChanged()
    }
    val isFavorite = song.id in preferences.favoriteIds()

    IconButton(
        onClick = { showMore = true },
        modifier = modifier,
    ) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), modifier = Modifier.accentGradientTint())
    }

    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.more_options),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )

                if (actions.showPlayNext && playerController != null) {
                    SongMoreOption(Icons.Default.PlayArrow, stringResource(R.string.play_next)) {
                        showMore = false
                        playerController.playNext(song)
                    }
                }

                if (actions.showAddToQueue && playerController != null) {
                    SongMoreOption(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        stringResource(R.string.add_to_queue)
                    ) {
                        showMore = false
                        playerController.addToQueue(song)
                    }
                }

                if (actions.showPlaylist) {
                    SongMoreOption(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        stringResource(R.string.add_to_playlist)
                    ) {
                        showMore = false
                        showPlaylistDialog = true
                    }
                }

                if (actions.showFavorite) {
                    SongMoreOption(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(
                            R.string.add_to_favorites
                        ),
                    ) {
                        showMore = false
                        preferences.toggleFavorite(song.id)
                        onChanged()
                    }
                }

                if (actions.showRemoveFromPlaylist) {
                    if (actions.showPlaylist || actions.showFavorite) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    SongMoreOption(
                        Icons.Default.RemoveCircleOutline,
                        stringResource(R.string.remove_from_playlist)
                    ) {
                        showMore = false
                        onRemoveFromPlaylist?.invoke()
                    }
                }

                if (actions.showAlbum && onOpenAlbum != null) {
                    SongMoreOption(Icons.Default.Album, stringResource(R.string.open_album)) {
                        showMore = false
                        onOpenAlbum(song.album)
                    }
                }

                if (actions.showArtist && onOpenArtist != null) {
                    SongMoreOption(Icons.Default.Person, stringResource(R.string.open_artist)) {
                        showMore = false
                        onOpenArtist(song.artist)
                    }
                }

                if (actions.showEditMetadata) {
                    SongMoreOption(Icons.Default.Edit, stringResource(R.string.edit_metadata)) {
                        showMore = false
                        showEditMetadata = true
                    }
                }

                if (actions.showInfo) {
                    SongMoreOption(Icons.Default.Info, stringResource(R.string.song_info)) {
                        showMore = false
                        showInfoDialog = true
                    }
                }

                if (actions.showRemoveFromDevice) {
                    SongMoreOption(
                        Icons.Default.Delete,
                        stringResource(R.string.remove_from_library),
                        destructive = true,
                    ) {
                        showMore = false
                        showRemoveConfirmation = true
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(song.id, preferences) {
            showPlaylistDialog = false
            onChanged()
        }
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text(stringResource(R.string.remove_from_library_question)) },
            text = { Text(stringResource(R.string.remove_from_library_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = false
                        scope.launch {
                            val uri = Uri.parse(song.uri)
                            val deleteRequest = AudioMetadataEditor.getDeleteRequestIntentSender(context, uri)
                            if (deleteRequest != null) {
                                deleteRequestLauncher.launch(
                                    IntentSenderRequest.Builder(deleteRequest).build()
                                )
                                return@launch
                            }
                            if (AudioMetadataEditor.delete(context, uri)) {
                                onChanged()
                            }
                        }
                    },
                ) {
                    Text(
                        stringResource(R.string.action_remove),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRemoveConfirmation = false
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showEditMetadata) {
        MetadataEditorDialog(
            song = song,
            onDismiss = { showEditMetadata = false },
            onSaved = { updatedSong ->
                showEditMetadata = false
                if (playerController?.state?.value?.currentSong?.id == updatedSong.id) {
                    playerController.updateCurrentSong(updatedSong)
                }
                onChanged()
            },
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.song_info)) },
            text = {
                Column {
                    Text(song.title, style = MaterialTheme.typography.titleMedium)
                    Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(song.album, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (song.durationMs > 0L) {
                        Text(
                            formatDuration(song.durationMs),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    song.folder?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfoDialog = false
                }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
private fun SongMoreOption(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.primary
    } else {
        LocalContentColor.current
    }
    ListItem(
        headlineContent = { Text(label, color = contentColor) },
        leadingContent = { Icon(icon, null, tint = contentColor, modifier = Modifier.accentGradientTint()) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
