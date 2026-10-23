package com.sipun.sonora.navigation

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.player.PlayerController

@Composable
fun SonoraApp(
    onCheckForUpdates: () -> Unit = {},
    externalAudioUri: Uri? = null,
    onExternalAudioConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val player = remember(context) { PlayerController(context.applicationContext) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val slideOffset = with(LocalDensity.current) { 72.dp.roundToPx() }
    val playerState by player.state.collectAsStateWithLifecycle()
    LaunchedEffect(externalAudioUri) {
        externalAudioUri?.let {
            player.playExternal(it)
            onExternalAudioConsumed()
            navController.navigate(SonoraRoute.NowPlaying.route) {
                launchSingleTop = true
            }
        }
    }

    val showMiniPlayer = currentRoute != null &&
            currentRoute != SonoraRoute.Home.route &&
            currentRoute != SonoraRoute.NowPlaying.route &&
            currentRoute != SonoraRoute.Lyrics.route &&
            playerState.currentSong != null

    val preferences = remember(context) { SonoraPreferences(context.applicationContext) }
    val keepScreenAwake = preferences.keepScreenAwake()
    val activity = context as? Activity

    DisposableEffect(keepScreenAwake, playerState.isPlaying) {
        if (activity != null) {
            if (keepScreenAwake && playerState.isPlaying) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SonoraNavGraph(
            navController = navController,
            player = player,
            onCheckForUpdates = onCheckForUpdates,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (targetState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeIn(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    ) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 360,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideInVertically(
                        initialOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 360,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                initialOffsetX = { it / 18 },
                                animationSpec = tween(240, easing = FastOutSlowInEasing)
                            )
                }
            },
            exitTransition = {
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeOut(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideOutVertically(
                        targetOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { -it / 24 },
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            )
                }
            },
            popEnterTransition = {
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                initialOffsetX = { -it / 20 },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                }
            },
            popExitTransition = {
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideOutVertically(
                        targetOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { it / 20 },
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            )
                }
            },
            predictivePopEnterTransition = { _ ->
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                initialOffsetX = { -it / 20 },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                }
            },
            predictivePopExitTransition = { _ ->
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideOutVertically(
                        targetOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { it / 20 },
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            )
                }
            },
        )

        SonoraMiniPlayerOverlay(
            player = player,
            playerState = playerState,
            visible = showMiniPlayer,
            onOpenNowPlaying = {
                navController.navigate(SonoraRoute.NowPlaying.route)
            },
        )
    }
}
