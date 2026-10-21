@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.sipun.sonora.feature.nowplaying

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.R
import com.sipun.sonora.data.media.AudioMetadataEditor
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.RepeatMode
import com.sipun.sonora.ui.components.AddToPlaylistDialog
import com.sipun.sonora.ui.components.SongArtworkImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun NowPlayingScreen(
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenLyrics: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { SonoraPreferences(context) }
    val state by playerController.state.collectAsStateWithLifecycle()
    val artworkScale = animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.985f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 280f),
        label = "artwork_playback_scale",
    ).value
    val repeatRotation = animateFloatAsState(
        targetValue = when (state.repeatMode) {
            RepeatMode.OFF -> 0f
            RepeatMode.ALL -> 180f
            RepeatMode.ONE -> 360f
        },
        animationSpec = tween(320),
        label = "repeat_rotation",
    ).value
    val shuffleScale = animateFloatAsState(
        targetValue = if (state.shuffleEnabled) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 360f),
        label = "shuffle_scale",
    ).value
    val repeatTint = animateColorAsState(
        targetValue = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "repeat_tint",
    ).value
    val shuffleTint = animateColorAsState(
        targetValue = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "shuffle_tint",
    ).value
    val playPauseHaloTransition = rememberInfiniteTransition(label = "play_pause_halo")
    val playingHaloScale = playPauseHaloTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_150, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "play_pause_halo_scale",
    ).value
    val playingHaloAlpha = playPauseHaloTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_150, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "play_pause_halo_alpha",
    ).value
    val playPauseHaloScale = if (state.isPlaying) playingHaloScale else 0.94f
    val playPauseHaloAlpha = if (state.isPlaying) playingHaloAlpha else 0.10f
    val scope = rememberCoroutineScope()
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showArtist by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showEditMetadata by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var favorite by remember(state.currentSong?.id) {
        mutableStateOf(state.currentSong?.id?.let { it in preferences.favoriteIds() } == true)
    }
    val favoriteComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.fav),
    )
    val favoriteAnimation = rememberLottieAnimatable()
    var sleepTimerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var fileAudioInfo by remember {
        mutableStateOf(AudioInfo(bitrate = "—", format = "AUDIO", sampleRate = "—"))
    }
    val audioInfo = AudioInfo(
        bitrate = state.audioBitrate.takeIf { it > 0 }?.div(1000)?.let { "$it kbps" }
            ?: fileAudioInfo.bitrate,
        format = fileAudioInfo.format.takeUnless { it == "AUDIO" }
            ?: audioFormatLabel(state.audioMimeType),
        sampleRate = if (state.audioSampleRate > 0) {
            val khz = state.audioSampleRate / 1000f
            (if (khz % 1f == 0f) "${khz.toInt()}" else khz.toString()) + " kHz"
        } else fileAudioInfo.sampleRate,
    )
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                context,
                context.getString(R.string.remove_from_library),
                Toast.LENGTH_SHORT,
            ).show()
            playerController.skipNext()
        }
    }

    LaunchedEffect(state.currentSong?.uri) {
        state.currentSong?.uri?.let { uri ->
            loadAudioInfo(context, uri)?.let { loadedInfo ->
                fileAudioInfo = loadedInfo
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            playerController.refresh()
            delay(500)
        }
    }

    if (showPlaylistPicker) {
        state.currentSong?.let { song ->
            AddToPlaylistDialog(song.id, preferences) { showPlaylistPicker = false }
        }
    }

    if (showMore) {
        MoreOptionsSheet(
            onDismiss = { showMore = false },
            onAlbum = {
                showMore = false
                state.currentSong?.let { onOpenAlbum(it.album) }
            },
            onArtist = {
                showMore = false
                showArtist = true
            },
            onPlaylist = {
                showMore = false
                showPlaylistPicker = true
            },
            onRingtone = {
                showMore = false
                state.currentSong?.let { setAsRingtone(context, it) }
            },
            onSleepTimer = {
                showMore = false
                showSleepTimer = true
            },
            onSpeed = {
                showMore = false
                showSpeed = true
            },
            onEqualizer = {
                showMore = false
                openEqualizer(context)
            },
            onEditMetadata = {
                showMore = false
                showEditMetadata = true
            },
            onInfo = {
                showMore = false
                showInfo = true
            },
            onRemove = {
                showMore = false
                showRemoveConfirmation = true
            },
        )
    }

    if (showQueue) {
        QueueDialog(
            queue = state.queue,
            currentIndex = state.queueIndex,
            onSelect = { song ->
                val index = state.queue.indexOfFirst { it.id == song.id }
                if (index >= 0) playerController.playQueue(state.queue, index)
                showQueue = false
            },
            onDismiss = { showQueue = false },
        )
    }

    if (showArtist) {
        ArtistDialog(
            artist = state.currentSong?.artist.orEmpty(),
            songs = state.queue.filter { it.artist == state.currentSong?.artist },
            onSelect = { song ->
                val index = state.queue.indexOfFirst { it.id == song.id }
                if (index >= 0) playerController.playQueue(state.queue, index)
                showArtist = false
            },
            onDismiss = { showArtist = false },
        )
    }

    if (showEditMetadata) {
        state.currentSong?.let { song ->
            com.sipun.sonora.ui.components.MetadataEditorDialog(
                song = song,
                onDismiss = { showEditMetadata = false },
                onSaved = { updatedSong ->
                    showEditMetadata = false
                    playerController.updateCurrentSong(updatedSong)
                },
            )
        }
    }

    if (showInfo) {
        state.currentSong?.let { song ->
            SongInfoDialog(song, onDismiss = { showInfo = false })
        }
    }

    if (showSpeed) {
        PlaybackSpeedDialog(
            currentSpeed = 1f,
            onSelect = {
                playerController.setPlaybackSpeed(it)
                showSpeed = false
            },
            onDismiss = { showSpeed = false },
        )
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            onSelect = { minutes ->
                sleepTimerJob?.cancel()
                if (minutes != null) {
                    sleepTimerJob = scope.launch {
                        delay(minutes * 60_000L)
                        playerController.togglePlayPause()
                    }
                }
                showSleepTimer = false
            },
            onDismiss = { showSleepTimer = false },
        )
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text(stringResource(R.string.remove_from_library_question)) },
            text = { Text(stringResource(R.string.remove_from_library_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val song = state.currentSong
                        if (song != null) {
                            showRemoveConfirmation = false
                            scope.launch {
                                val uri = Uri.parse(song.uri)
                                val deleteRequest =
                                    AudioMetadataEditor.getDeleteRequestIntentSender(context, uri)
                                if (deleteRequest != null) {
                                    deleteRequestLauncher.launch(
                                        IntentSenderRequest.Builder(deleteRequest).build()
                                    )
                                    return@launch
                                }
                                val removed = AudioMetadataEditor.delete(context, uri)
                                Toast.makeText(
                                    context,
                                    if (removed) {
                                        context.getString(R.string.remove_from_library)
                                    } else {
                                        "Unable to remove this song"
                                    },
                                    Toast.LENGTH_SHORT,
                                ).show()
                                if (removed) playerController.skipNext()
                            }
                        }
                    },
                ) {
                    Text(
                        stringResource(R.string.action_remove),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {},
            )
        },
    ) { padding ->
        val song = state.currentSong
        val max = state.durationMs.coerceAtLeast(1L)

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = song?.title?.replace(Regex("\\s*[-–—]\\s*"), " ")?.trim()
                            ?: "Select a song from your library",
                        label = "song_title_transition",
                    ) { title ->
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.fillMaxWidth().basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1_000,
                                repeatDelayMillis = 1_000,
                            ),
                        )
                    }
                }
                AnimatedContent(targetState = song?.artist ?: "Sonora", label = "song_artist_transition") { artist ->
                    Text(
                        artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 0.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Crossfade(
                targetState = song?.id,
                label = "album_art_transition",
            ) { currentSongId ->
                Box(
                    Modifier
                        .fillMaxWidth(0.82f)
                        .widthIn(max = 340.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (currentSongId != null && song?.id == currentSongId) {
                        SongArtworkImage(
                            song = song,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(30.dp))
                                .graphicsLayer {
                                    scaleX = artworkScale
                                    scaleY = artworkScale
                                },
                            artworkData = state.artworkData,
                        )
                    }
                }
            }

            Spacer(Modifier.height(38.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))

                    ExpressiveSeekBar(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        isPlaying = state.isPlaying,
                        enabled = song != null && state.durationMs > 0,
                        onSeek = playerController::seekTo,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            formatPlaybackTime(state.positionMs),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            formatPlaybackTime(state.durationMs),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    AudioInfoPill(
                        info = audioInfo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = playerController::cycleRepeat) {
                            Icon(
                                if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                "Repeat " + state.repeatMode.name.lowercase(),
                                modifier = Modifier.graphicsLayer { rotationZ = repeatRotation },
                                tint = repeatTint,
                            )
                        }
                        IconButton(
                            onClick = playerController::skipPrevious,
                            enabled = state.hasPrevious || state.positionMs > 3_000L,
                        ) {
                            Icon(Icons.Default.SkipPrevious, "Previous")
                        }
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(92.dp)
                                    .graphicsLayer {
                                        scaleX = playPauseHaloScale
                                        scaleY = playPauseHaloScale
                                    }
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = playPauseHaloAlpha)
                                    ),
                            )
                            Box(
                                Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ),
                            )
                            FilledIconButton(
                                onClick = playerController::togglePlayPause,
                                enabled = song != null,
                                modifier = Modifier.size(72.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White,
                                ),
                            ) {
                                AnimatedContent(targetState = state.isPlaying, label = "play_pause_transition") { playing ->
                                    Icon(
                                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        if (playing) "Pause" else "Play",
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
                        IconButton(onClick = playerController::skipNext, enabled = state.hasNext) {
                            Icon(Icons.Default.SkipNext, "Next")
                        }
                        IconButton(onClick = playerController::toggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle,
                                "Shuffle",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = shuffleScale
                                    scaleY = shuffleScale
                                },
                                tint = shuffleTint,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuickAction(Icons.Default.Description, "Lyrics") { onOpenLyrics() }
                        QuickAction(Icons.AutoMirrored.Filled.QueueMusic, "Queue") {
                            showQueue = true
                        }
                        FavoriteQuickAction(
                            favorite = favorite,
                            composition = favoriteComposition,
                            isPlaying = favoriteAnimation.isPlaying,
                            progress = favoriteAnimation.progress,
                        ) {
                            song?.let { currentSong ->
                                val isFavorite = preferences.toggleFavorite(currentSong.id)
                                favorite = isFavorite
                                scope.launch {
                                    favoriteComposition?.let { composition ->
                                        if (isFavorite) {
                                            favoriteAnimation.animate(
                                                composition = composition,
                                                iterations = 1,
                                            )
                                        } else {
                                            favoriteAnimation.snapTo(
                                                composition = composition,
                                                progress = 0f,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        QuickAction(Icons.Default.MoreHoriz, "More") { showMore = true }
                    }
                }
            }
        }
    }
}
