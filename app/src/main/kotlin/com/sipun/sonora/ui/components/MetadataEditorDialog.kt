package com.sipun.sonora.ui.components

import com.sipun.sonora.ui.theme.accentGradientTint
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sipun.sonora.R
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.media.AudioMetadataEditor
import com.sipun.sonora.data.media.EditableSongMetadata
import com.sipun.sonora.domain.model.Song
import kotlinx.coroutines.launch

@Composable
fun MetadataEditorDialog(song: Song, onDismiss: () -> Unit, onSaved: (Song) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(song) { mutableStateOf(song.title) }
    var artist by remember(song) { mutableStateOf(song.artist) }
    var album by remember(song) { mutableStateOf(song.album) }
    var albumArtist by remember { mutableStateOf("") }
    var genre by remember(song) { mutableStateOf(song.genre.orEmpty()) }
    var year by remember(song) { mutableStateOf(song.year?.toString().orEmpty()) }
    var track by remember(song) { mutableStateOf(song.trackNumber?.toString().orEmpty()) }
    var disc by remember { mutableStateOf("") }
    var composer by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var grouping by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var copyright by remember { mutableStateOf("") }
    var bpm by remember { mutableStateOf("") }
    var artworkUri by remember { mutableStateOf<Uri?>(null) }
    var embeddedArtwork by remember(song) { mutableStateOf<ByteArray?>(null) }
    var artworkChanged by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saveRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(song.id) {
        runCatching { AudioMetadataEditor.read(context, song) }
            .onSuccess { metadata ->
                title = metadata.title
                artist = metadata.artist
                album = metadata.album
                albumArtist = metadata.albumArtist
                genre = metadata.genre
                year = metadata.year?.toString().orEmpty()
                track = metadata.track?.toString().orEmpty()
                disc = metadata.disc?.toString().orEmpty()
                composer = metadata.composer
                comment = metadata.comment
                grouping = metadata.grouping
                lyrics = metadata.lyrics
                copyright = metadata.copyright
                bpm = metadata.bpm?.toString().orEmpty()
                embeddedArtwork = metadata.artworkData
            }
        loading = false
    }

    val writeAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            saveRequest++
        } else {
            saving = false
        }
    }

    val mediaManagementLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val sender = AudioMetadataEditor.getWriteRequestIntentSender(
            context,
            Uri.parse(song.uri),
        )
        if (sender != null) {
            writeAccessLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else {
            saveRequest++
        }
    }

    LaunchedEffect(saveRequest) {
        if (saveRequest == 0) return@LaunchedEffect
        scope.launch {
            val result = runCatching {
                AudioMetadataEditor.save(
                    context,
                    song,
                    EditableSongMetadata(
                        title, artist, album, albumArtist, genre,
                        year.toIntOrNull(), track.toIntOrNull(), disc.toIntOrNull(),
                        composer, comment, grouping, lyrics, copyright, bpm.toIntOrNull(),
                    ),
                    artworkUri,
                    artworkChanged,
                )
            }
            saving = false
            result.onSuccess {
                onSaved(
                    song.copy(
                        title = title,
                        artist = artist,
                        album = album,
                        genre = genre.ifBlank { null },
                        year = year.toIntOrNull(),
                        trackNumber = track.toIntOrNull(),
                        albumArtUri = if (artworkChanged && artworkUri == null) null else song.albumArtUri,
                    )
                )
                AndroidMusicRepository.notifyMetadataChanged()
            }.onFailure {
                if (it is com.sipun.sonora.data.media.MediaWriteAccessRequiredException) {
                    val sender = AudioMetadataEditor.getWriteRequestIntentSender(
                        context,
                        Uri.parse(song.uri)
                    )
                    if (sender != null) {
                        saving = true
                        writeAccessLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    } else {
                        error = "Sonora does not have write access to this media file."
                    }
                } else {
                    val cause = it.cause
                    error = it.message
                        ?: cause?.message
                                ?: (it::class.simpleName
                            ?: "Unknown error") + " while saving metadata."
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            artworkUri = uri
            embeddedArtwork = null
            artworkChanged = true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.edit_metadata)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val preview: Any? = when {
                        artworkChanged -> artworkUri ?: embeddedArtwork
                        embeddedArtwork != null -> embeddedArtwork
                        else -> null
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.metadata_artwork))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (preview != null) {
                                AsyncImage(
                                    model = preview,
                                    contentDescription = null,
                                    modifier = Modifier.size(84.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(Icons.Default.Album, null, modifier = Modifier.size(84.dp).accentGradientTint())
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Button(onClick = { picker.launch("image/*") }) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.accentGradientTint())
                                    Text(
                                        if (preview == null) stringResource(R.string.metadata_add_artwork)
                                        else stringResource(R.string.metadata_change_artwork),
                                        modifier = Modifier.padding(start = 6.dp),
                                    )
                                }
                                if (preview != null) {
                                    TextButton(onClick = {
                                        artworkUri = null
                                        embeddedArtwork = null
                                        artworkChanged = true
                                    }) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.accentGradientTint())
                                        Text(stringResource(R.string.metadata_remove_artwork))
                                    }
                                }
                            }
                        }
                    }
                }

                fun field(
                    label: Int,
                    value: String,
                    update: (String) -> Unit,
                    singleLine: Boolean = true
                ) {
                    item {
                        OutlinedTextField(
                            value = value,
                            onValueChange = update,
                            label = { Text(stringResource(label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = singleLine,
                        )
                    }
                }

                field(R.string.metadata_title, title, { title = it })
                field(R.string.metadata_artist, artist, { artist = it })
                field(R.string.metadata_album, album, { album = it })
                field(R.string.metadata_album_artist, albumArtist, { albumArtist = it })
                field(R.string.metadata_genre, genre, { genre = it })
                field(R.string.metadata_year, year, { year = it.filter(Char::isDigit).take(4) })
                field(R.string.metadata_track, track, { track = it.filter(Char::isDigit).take(3) })
                field(R.string.metadata_disc, disc, { disc = it.filter(Char::isDigit).take(3) })
                field(R.string.metadata_composer, composer, { composer = it })
                field(R.string.metadata_grouping, grouping, { grouping = it })
                field(R.string.metadata_copyright, copyright, { copyright = it })
                field(R.string.metadata_bpm, bpm, { bpm = it.filter(Char::isDigit).take(4) })
                field(R.string.metadata_comment, comment, { comment = it }, false)
                field(R.string.metadata_lyrics, lyrics, { lyrics = it }, false)
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && !loading && title.isNotBlank(),
                onClick = {
                    saving = true
                    val managementIntent = AudioMetadataEditor.getMediaManagementIntent(context)
                    if (managementIntent != null) {
                        mediaManagementLauncher.launch(managementIntent)
                    } else {
                        val sender = AudioMetadataEditor.getWriteRequestIntentSender(
                            context,
                            Uri.parse(song.uri),
                        )
                        if (sender != null) {
                            writeAccessLauncher.launch(
                                IntentSenderRequest.Builder(sender).build(),
                            )
                        } else {
                            saveRequest++
                        }
                    }
                },
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp))
                else Text(stringResource(R.string.metadata_save))
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(stringResource(R.string.metadata_save_failed, message)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { error = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }
}
