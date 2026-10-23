package com.sipun.sonora.feature.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.R
import com.sipun.sonora.data.lyrics.Lyrics
import com.sipun.sonora.data.lyrics.LyricsRepository
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.SonoraBackButton
import com.sipun.sonora.ui.theme.LocalAccentBrush
import com.sipun.sonora.ui.theme.accentGradientTint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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
                    .padding(top = 8.dp, bottom = 20.dp),
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
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var active by remember(lyrics) { mutableIntStateOf(-1) }

    LaunchedEffect(positionMs, lyrics) {
        if (!lyrics.synced) return@LaunchedEffect
        val index = lyrics.lines.indexOfLast {
            (it.startMs ?: Long.MAX_VALUE) <= positionMs
        }
        if (index != active) active = index
    }

    LaunchedEffect(active) {
        if (active >= 0) {
            val layoutInfo = listState.layoutInfo
            val activeItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val lowerThreshold = layoutInfo.viewportStartOffset + (viewportHeight * 0.72f).toInt()
            val shouldScroll = activeItem == null ||
                    activeItem.offset + activeItem.size > lowerThreshold ||
                    activeItem.offset < layoutInfo.viewportStartOffset

            if (shouldScroll) {
                listState.animateScrollToItem((active - 3).coerceAtLeast(0))
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 28.dp,
                    end = 24.dp,
                    top = 36.dp,
                    bottom = 56.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isActive = index == active
                        val textColor by animateColorAsState(
                            targetValue = if (isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(durationMillis = 220),
                            label = "lyrics_text_color",
                        )
                        val markerProgress = animateFloatAsState(
                            targetValue = if (isActive) 1f else 0f,
                            animationSpec = tween(durationMillis = 260),
                            label = "lyrics_marker_progress",
                        ).value

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
                            style = MaterialTheme.typography.titleLarge,
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
                    .height(58.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.surface,
                            1f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        )
                    ),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0f),
                            1f to MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    ),
            )
        }
    }
}
