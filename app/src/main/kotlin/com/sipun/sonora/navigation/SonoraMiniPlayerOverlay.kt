package com.sipun.sonora.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.PlayerState
import com.sipun.sonora.ui.components.SonoraMiniPlayer

@Composable
internal fun SonoraMiniPlayerOverlay(
    player: PlayerController,
    playerState: PlayerState,
    visible: Boolean,
    onOpenNowPlaying: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible && playerState.currentSong != null,
        enter = fadeIn(tween(220)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(260)
        ),
        exit = fadeOut(tween(160)) + slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = tween(180)
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SonoraMiniPlayer(playerController = player, onOpenNowPlaying = onOpenNowPlaying)
            }
        }
    }
}
