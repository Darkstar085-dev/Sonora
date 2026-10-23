package com.sipun.sonora.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.ui.theme.LocalAccentBrush
import com.sipun.sonora.ui.theme.accentGradientTint
import com.sipun.sonora.domain.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.PlayerState

@Composable
internal fun LibraryLottieCard(
    songCount: Int,
    nowPlayingSong: Song?,
    state: PlayerState,
    player: PlayerController,
    onOpenNowPlaying: () -> Unit,
    onPlayAll: () -> Unit,
) {
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val repeatRotation = animateFloatAsState(
        targetValue = when (state.repeatMode) {
            com.sipun.sonora.player.RepeatMode.OFF -> 0f
            com.sipun.sonora.player.RepeatMode.ALL -> 180f
            com.sipun.sonora.player.RepeatMode.ONE -> 360f
        },
        animationSpec = tween(320),
        label = "home_repeat_rotation",
    ).value
    val shuffleScale = animateFloatAsState(
        targetValue = if (state.shuffleEnabled) 1.10f else 1f,
        animationSpec = tween(220),
        label = "home_shuffle_scale",
    ).value

    Card(
        onClick = { if (nowPlayingSong != null) onOpenNowPlaying() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        if (nowPlayingSong != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(nowPlayingSong, Modifier.size(116.dp))
                    Column(
                        Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            if (state.isPlaying) "Now playing" else "Paused",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            nowPlayingSong.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            nowPlayingSong.artist,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                ExpressiveHomeSeekBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    enabled = state.durationMs > 0L,
                    onSeek = player::seekTo,
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        formatTime(state.positionMs),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        formatTime(state.durationMs),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val controlBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    val activeBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

                    IconButton(
                        onClick = player::cycleRepeat,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.repeatMode == com.sipun.sonora.player.RepeatMode.OFF) {
                                    controlBackground
                                } else {
                                    activeBackground
                                }
                            ),
                    ) {
                        Icon(
                            if (state.repeatMode == com.sipun.sonora.player.RepeatMode.ONE) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            modifier = Modifier
                                .graphicsLayer { rotationZ = repeatRotation }
                                .then(
                                    if (state.repeatMode != com.sipun.sonora.player.RepeatMode.OFF) Modifier.accentGradientTint()
                                    else Modifier
                                ),
                            tint = if (state.repeatMode == com.sipun.sonora.player.RepeatMode.OFF) {
                                MaterialTheme.colorScheme.onSurface
                            } else Color.White,
                        )
                    }
                    IconButton(
                        onClick = player::skipPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(controlBackground),
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    FilledIconButton(
                        onClick = player::togglePlayPause,
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(LocalAccentBrush.current),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent, contentColor = Color.White),
                    ) {
                        AnimatedContent(
                            targetState = state.isPlaying,
                            label = "home_play_pause_transition",
                        ) { playing ->
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White,
                            )
                        }
                    }
                    IconButton(
                        onClick = player::skipNext,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(controlBackground),
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = player::toggleShuffle,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.shuffleEnabled) activeBackground else controlBackground
                            ),
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = shuffleScale
                                    scaleY = shuffleScale
                                }
                                .then(if (state.shuffleEnabled) Modifier.accentGradientTint() else Modifier),
                            tint = if (state.shuffleEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StaticVinyl(
                    modifier = Modifier.size(108.dp),
                )
                Column(
                    Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(R.string.your_library),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.song_count_other, songCount),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.ready_to_play),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                    FilledTonalButton(
                        onClick = onPlayAll,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(LocalAccentBrush.current),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.play_all))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExpressiveHomeSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val primary = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                }
            }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeek(
                            ((change.position.x / size.width).coerceIn(
                                0f,
                                1f
                            ) * durationMs).toLong()
                        )
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val thumbX = size.width * progress
            val wavePath = androidx.compose.ui.graphics.Path()
            val waveLength = 34.dp.toPx()
            val amplitude = 4.dp.toPx()
            wavePath.moveTo(0f, centerY)

            var x = 0f
            while (x <= thumbX) {
                val phase = (x / waveLength) * (2f * kotlin.math.PI).toFloat()
                wavePath.lineTo(x, centerY + kotlin.math.sin(phase) * amplitude)
                x += 2.dp.toPx()
            }

            if (thumbX > 0f) {
                drawPath(
                    path = wavePath,
                    color = primary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
            if (thumbX < size.width) {
                drawLine(
                    color = track,
                    start = androidx.compose.ui.geometry.Offset(thumbX, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            drawLine(
                color = primary,
                start = androidx.compose.ui.geometry.Offset(thumbX, centerY - 11.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(thumbX, centerY + 11.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun StaticVinyl(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.40f
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(
                        Color(0xFF303036),
                        Color(0xFF111114),
                    ),
                ),
                radius = radius,
            )
            repeat(7) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.045f),
                    radius = radius * (0.30f + index * 0.10f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
            drawCircle(
                color = primary,
                radius = radius * 0.28f,
            )
            drawCircle(
                color = Color(0xFF17171A),
                radius = radius * 0.07f,
            )
        }
    }
}

internal fun formatTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
internal fun OverviewMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(
        modifier
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Icon(
                    it,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp).accentGradientTint(),
                )
            }
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun OverviewMetricDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(44.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

