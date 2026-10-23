package com.sipun.sonora.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.sipun.sonora.domain.Song
import com.sipun.sonora.player.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SongArtworkImage(
    song: Song,
    modifier: Modifier = Modifier,
    artworkData: ByteArray? = null,
    contentDescription: String? = "Album artwork",
) {
    val context = LocalContext.current
    val artworkLoader = remember(context) { ArtworkLoader(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var embeddedArtwork by remember(song.id, song.uri, artworkData) {
        mutableStateOf(artworkData)
    }
    var useEmbeddedArtwork by remember(song.id, song.uri, song.albumArtUri, artworkData) {
        mutableStateOf(artworkData != null || song.albumArtUri == null)
    }

    fun loadEmbeddedArtwork() {
        if (embeddedArtwork != null) {
            useEmbeddedArtwork = true
            return
        }
        scope.launch {
            embeddedArtwork = withContext(Dispatchers.IO) {
                artworkLoader.extractEmbeddedArtwork(Uri.parse(song.uri))
            }
            useEmbeddedArtwork = true
        }
    }

    LaunchedEffect(song.id, song.uri, song.albumArtUri, artworkData) {
        embeddedArtwork = artworkData
        useEmbeddedArtwork = artworkData != null || song.albumArtUri == null
        if (song.albumArtUri == null && artworkData == null) {
            loadEmbeddedArtwork()
        }
    }

    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Album,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(0.62f),
        )

        val model = if (useEmbeddedArtwork) embeddedArtwork else song.albumArtUri
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (!useEmbeddedArtwork && artworkData == null) {
                        loadEmbeddedArtwork()
                    }
                },
            )
        }
    }
}
