package com.sipun.sonora.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun PlayingEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "playing_equalizer")
    val first = transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "equalizer_bar_one",
    )
    val second = transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            tween(
                510,
                delayMillis = 70,
                easing = FastOutSlowInEasing
            ), repeatMode = RepeatMode.Reverse
        ),
        label = "equalizer_bar_two",
    )
    val third = transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(
                460,
                delayMillis = 130,
                easing = FastOutSlowInEasing
            ), repeatMode = RepeatMode.Reverse
        ),
        label = "equalizer_bar_three",
    )
    val heights = if (isPlaying) listOf(first.value, second.value, third.value) else listOf(
        0.28f,
        0.28f,
        0.28f
    )

    Row(
        modifier = modifier
            .size(width = 30.dp, height = 24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
