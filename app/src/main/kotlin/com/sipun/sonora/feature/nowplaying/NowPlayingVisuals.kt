@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.sipun.sonora.feature.nowplaying

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation

@Composable
internal fun FavoriteQuickAction(
    favorite: Boolean,
    composition: com.airbnb.lottie.LottieComposition?,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(78.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(54.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying && composition != null) {
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.size(46.dp),
                        )
                    } else {
                        Icon(
                            if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Add to Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExpressiveSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
) {
    val progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(220),
        label = "expressive_seek_progress",
    ).value
    val wavePhase = if (isPlaying) {
        rememberInfiniteTransition(label = "seekbar_wave").animateFloat(
            initialValue = 0f,
            targetValue = (2f * kotlin.math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(3_000, easing = LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
            ),
            label = "seekbar_wave_phase",
        ).value
    } else {
        0f
    }
    val primary = if (isPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(enabled, durationMs) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
                .pointerInput(enabled, durationMs) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val centerY = size.height / 2f
                val trackStart = 0f
                val trackEnd = size.width
                val thumbX = trackStart + (trackEnd - trackStart) * animatedProgress

                val wavePath = androidx.compose.ui.graphics.Path()
                val waveLength = 34.dp.toPx()
                val amplitude = 4.dp.toPx()
                val angularLength = (2f * kotlin.math.PI).toFloat()
                val startPhase = (trackStart / waveLength) * angularLength + wavePhase
                val startY = centerY + kotlin.math.sin(startPhase) * amplitude
                wavePath.moveTo(trackStart, startY)
                var x = trackStart + 2.dp.toPx()
                while (x <= thumbX) {
                    val phase = (x / waveLength) * angularLength + wavePhase
                    wavePath.lineTo(
                        x,
                        centerY + kotlin.math.sin(phase) * amplitude,
                    )
                    x += 2.dp.toPx()
                }
                if (thumbX > trackStart) {
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

                if (thumbX < trackEnd) {
                    drawLine(
                        color = track,
                        start = androidx.compose.ui.geometry.Offset(thumbX, centerY),
                        end = androidx.compose.ui.geometry.Offset(trackEnd, centerY),
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
}

internal data class AudioInfo(
    val bitrate: String,
    val format: String,
    val sampleRate: String,
)

internal fun loadAudioInfo(context: android.content.Context, uriString: String): AudioInfo? {
    val uri = Uri.parse(uriString)
    return runCatching {
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                extractor.setDataSource(descriptor.fileDescriptor)

                val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                var bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toLongOrNull()?.takeIf { it > 0 }
                var sampleRate = 0

                for (index in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(index)
                    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (bitrate == null && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                                .takeIf { it > 0 }
                        }
                        break
                    }
                }

                AudioInfo(
                    bitrate = bitrate?.div(1000)?.takeIf { it > 0 }?.let { "$it kbps" } ?: "—",
                    format = audioFormatLabel(mimeType),
                    sampleRate = if (sampleRate > 0) {
                        val khz = sampleRate / 1000f
                        if (khz % 1f == 0f) {
                            "${khz.toInt()} kHz"
                        } else {
                            "$khz kHz"
                        }
                    } else "—",
                )
            }
        } finally {
            extractor.release()
            retriever.release()
        }
    }.getOrNull()
}

internal fun audioFormatLabel(mimeType: String?): String = when (mimeType?.lowercase()) {
    "audio/mpeg" -> "MP3"
    "audio/mp4", "audio/x-m4a" -> "M4A"
    "audio/flac" -> "FLAC"
    "audio/ogg", "audio/vorbis" -> "OGG"
    "audio/wav", "audio/x-wav" -> "WAV"
    "audio/aac", "audio/aacp" -> "AAC"
    "audio/opus" -> "OPUS"
    else -> mimeType?.substringAfterLast('/')?.uppercase()?.takeIf { it.isNotBlank() } ?: "AUDIO"
}

@Composable
internal fun AudioInfoPill(info: AudioInfo, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioInfoItem(Icons.Default.MusicNote, info.bitrate)
            AudioInfoDivider()
            AudioInfoItem(Icons.Default.Description, info.format)
            AudioInfoDivider()
            AudioInfoItem(Icons.Default.GraphicEq, info.sampleRate)
        }
    }
}

@Composable
internal fun AudioInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
internal fun AudioInfoDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    )
}
