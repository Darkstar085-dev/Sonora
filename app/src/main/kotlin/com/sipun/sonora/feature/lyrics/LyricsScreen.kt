package com.sipun.sonora.feature.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.R
import com.sipun.sonora.data.lyrics.Lyrics
import com.sipun.sonora.data.lyrics.LyricsRepository
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.SonoraBackButton
import com.sipun.sonora.ui.theme.InterFont
import com.sipun.sonora.ui.theme.LocalAccentBrush
import com.sipun.sonora.ui.theme.accentGradientTint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(playerController: PlayerController, onBack: () -> Unit) {
    val state by playerController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repository = remember(context) { LyricsRepository(context.applicationContext) }
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var loading by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            playerController.refresh()
            delay(250)
        }
    }

    LaunchedEffect(state.currentSong?.id, retry) {
        val song = state.currentSong ?: return@LaunchedEffect
        loading = true
        lyrics = null
        try {
            lyrics = repository.get(song)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lyrics = null
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.lyrics_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    SonoraBackButton(onClick = onBack)
                },
                actions = {
                    IconButton(onClick = { retry++ }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.lyrics_retry),
                            modifier = Modifier.accentGradientTint()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val song = state.currentSong

        if (song == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.lyrics_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    SongArtworkImage(
                        song = song,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = null,
                    )
                }

                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 20.dp),
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            when {
                loading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                lyrics == null || lyrics!!.lines.isEmpty() -> Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { retry++ },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(LocalAccentBrush.current),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(stringResource(R.string.lyrics_retry))
                    }
                }

                else -> LyricsCard(
                    lyrics = lyrics!!,
                    positionMs = state.positionMs,
                    onSeek = { seekMs -> playerController.seekTo(seekMs) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LyricsCard(
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var active by remember(lyrics) { mutableIntStateOf(-1) }

    LaunchedEffect(positionMs, lyrics) {
        if (!lyrics.synced) return@LaunchedEffect
        val index = lyrics.lines.indexOfLast {
            (it.startMs ?: Long.MAX_VALUE) <= positionMs
        }
        if (index != active) active = index
    }

    LaunchedEffect(active) {
        if (active < 0) {
            listState.animateScrollToItem(0, 0)
            return@LaunchedEffect
        }
        if (active == 0) {
            listState.animateScrollToItem(0, 0)
            return@LaunchedEffect
        }

        if (listState.isScrollInProgress) {
            snapshotFlow { listState.isScrollInProgress }.first { !it }
        }

        var viewportHeight =
            listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        if (viewportHeight <= 0) {
            snapshotFlow { listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset }
                .first { it > 0 }
            viewportHeight =
                listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        }

        val activeItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }
        val itemSize = activeItem?.size
            ?: listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size
            ?: with(density) { 56.dp.roundToPx() }
        val centerOffset = -((viewportHeight - itemSize) / 2)
        listState.animateScrollToItem(active, centerOffset)
    }

    val topFadeAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / with(density) { 48.dp.toPx() })
                    .coerceIn(0f, 1f)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cardHeight = maxHeight
            val bottomPadding = (cardHeight * 0.45f).coerceAtLeast(160.dp)
            val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

            val topFadeDp = 72.dp
            val bottomFadeDp = 84.dp
            val topFadePx = with(density) { topFadeDp.toPx() }
            val bottomFadePx = with(density) { bottomFadeDp.toPx() }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val h = size.height
                        if (h > 0f) {
                            val topRatio = ((topFadePx / h) * topFadeAlpha).coerceIn(0f, 0.4f)
                            val bottomRatio = ((h - bottomFadePx) / h).coerceIn(0.6f, 1f)

                            if (topRatio > 0.001f) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        (topRatio * 0.25f) to Color.Black.copy(alpha = 0.15f),
                                        (topRatio * 0.6f) to Color.Black.copy(alpha = 0.6f),
                                        topRatio to Color.Black,
                                        bottomRatio to Color.Black,
                                        (bottomRatio + (1f - bottomRatio) * 0.4f) to Color.Black.copy(
                                            alpha = 0.6f
                                        ),
                                        (bottomRatio + (1f - bottomRatio) * 0.75f) to Color.Black.copy(
                                            alpha = 0.15f
                                        ),
                                        1.0f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            } else {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Black,
                                        bottomRatio to Color.Black,
                                        (bottomRatio + (1f - bottomRatio) * 0.4f) to Color.Black.copy(
                                            alpha = 0.6f
                                        ),
                                        (bottomRatio + (1f - bottomRatio) * 0.75f) to Color.Black.copy(
                                            alpha = 0.15f
                                        ),
                                        1.0f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        }
                    },
                contentPadding = PaddingValues(
                    start = 28.dp,
                    end = 24.dp,
                    top = 36.dp,
                    bottom = bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val isActive = index == active
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "lyrics_text_color",
                    )
                    val markerProgress = animateFloatAsState(
                        targetValue = if (isActive) 1f else 0f,
                        animationSpec = tween(durationMillis = 260),
                        label = "lyrics_marker_progress",
                    ).value

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = line.startMs != null) {
                                line.startMs?.let { onSeek(it) }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(5.dp)
                                .height(48.dp)
                                .graphicsLayer {
                                    scaleX = markerProgress
                                    alpha = markerProgress
                                }
                                .clip(RoundedCornerShape(5.dp))
                                .background(LocalAccentBrush.current),
                        )
                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = InterFont,
                                fontSize = 22.sp,
                                lineHeight = 32.sp,
                            ),
                            fontWeight = if (isActive) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            color = textColor,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(topFadeDp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = topFadeAlpha }
                    .background(
                        Brush.verticalGradient(
                            0.0f to containerColor,
                            0.35f to containerColor.copy(alpha = 0.85f),
                            0.7f to containerColor.copy(alpha = 0.4f),
                            1.0f to Color.Transparent,
                        )
                    ),
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(bottomFadeDp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.3f to containerColor.copy(alpha = 0.4f),
                            0.65f to containerColor.copy(alpha = 0.85f),
                            1.0f to containerColor,
                        )
                    ),
            )
        }
    }
}
