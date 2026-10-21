package com.sipun.sonora.ui.components

import com.sipun.sonora.ui.theme.accentGradientTint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.theme.LocalAccentBrush

@Composable
fun SonoraMiniPlayer(
    playerController: PlayerController,
    onOpenNowPlaying: () -> Unit,
) {
    val state by playerController.state.collectAsStateWithLifecycle()
    val song = state.currentSong ?: return
    val progress = animateFloatAsState(
        targetValue = if (state.durationMs > 0L) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(140),
        label = "mini_player_progress",
    ).value

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 5.dp,
        onClick = onOpenNowPlaying,
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 7.dp, end = 6.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    SongArtworkImage(
                        song = song,
                        modifier = Modifier.fillMaxSize(),
                        artworkData = state.artworkData,
                    )
                }

                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = playerController::skipPrevious,
                    enabled = state.hasPrevious,
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(
                    onClick = playerController::togglePlayPause,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LocalAccentBrush.current),
                ) {
                    AnimatedContent(targetState = state.isPlaying, label = "mini_player_play_pause") { playing ->
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (playing) "Pause" else "Play",
                            modifier = Modifier.size(26.dp),
                            tint = Color.White,
                        )
                    }
                }
                IconButton(
                    onClick = playerController::skipNext,
                    enabled = state.hasNext,
                ) {
                    Icon(Icons.Default.SkipNext, "Next", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(LocalAccentBrush.current),
                )
            }
        }
    }
}
