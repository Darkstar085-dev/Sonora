package com.sipun.sonora.player

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = SonoraPreferences(appContext)
    private val musicRepository = AndroidMusicRepository(appContext)
    private val artworkLoader = ArtworkLoader(appContext)
    private val artworkScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artworkJob: Job? = null
    private var positionUpdateJob: Job? = null
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null
    private var pendingQueueShuffled = false
    private var pendingExternalUri: Uri? = null
    private var restoredLastPlayed = false
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, SonoraPlaybackService::class.java)),
    ).buildAsync()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateState()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.value = _state.value.copy(artworkData = null)
            updateState()
            loadCurrentArtwork()
        }
    }

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    restoreLastPlayedIfNeeded()
                    pendingQueue?.let { (songs, index) ->
                        pendingQueue = null
                        if (pendingQueueShuffled) {
                            pendingQueueShuffled = false
                            playQueueShuffled(songs)
                        } else {
                            playQueue(songs, index)
                        }
                    }
                    pendingExternalUri?.let { uri ->
                        pendingExternalUri = null
                        playExternal(uri)
                    }
                    updateState()
                    if (mediaController.isPlaying) startPositionUpdates()
                }
            },
            context.mainExecutor,
        )
    }

    fun playExternal(uri: Uri) {
        val mediaController = controller ?: run {
            pendingExternalUri = uri
            return
        }

        artworkScope.launch {
            val metadata = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(appContext, uri)
                    MediaMetadata.Builder()
                        .setTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                        .setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                        .setAlbumTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                        .build()
                } catch (_: Exception) {
                    MediaMetadata.Builder().build()
                } finally {
                    retriever.release()
                }
            }

            val librarySongs = runCatching { musicRepository.songs() }.getOrDefault(emptyList())
            val queueIndex = librarySongs.indexOfFirst { it.uri == uri.toString() }
            if (queueIndex >= 0) {
                mediaController.setMediaItems(
                    librarySongs.map(::toMediaItem),
                    queueIndex,
                    C.TIME_UNSET,
                )
            } else {
                mediaController.setMediaItem(
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaId(uri.toString().hashCode().toLong().toString())
                        .setMediaMetadata(metadata)
                        .build(),
                )
            }
            mediaController.prepare()
            mediaController.play()
            loadCurrentArtwork()
            updateState()
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val mediaController = controller ?: run {
            pendingQueue = songs to startIndex
            return
        }
        mediaController.setMediaItems(
            songs.map(::toMediaItem),
            startIndex.coerceIn(0, songs.lastIndex),
            C.TIME_UNSET,
        )
        mediaController.prepare()
        mediaController.play()
        loadCurrentArtwork()
        updateState(songs)
    }

    fun playQueueShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        val mediaController = controller ?: run {
            pendingQueue = songs to 0
            pendingQueueShuffled = true
            return
        }
        mediaController.setMediaItems(
            songs.map(::toMediaItem),
            songs.indices.random(),
            C.TIME_UNSET,
        )
        mediaController.shuffleModeEnabled = true
        mediaController.prepare()
        mediaController.play()
        loadCurrentArtwork()
        updateState(songs)
    }

    fun playNext(song: Song) {
        controller?.let { mediaController ->
            val item = toMediaItem(song)
            val insertIndex = (mediaController.currentMediaItemIndex + 1).coerceAtLeast(0)
            mediaController.addMediaItem(insertIndex, item)
            updateState()
        }
    }

    fun addToQueue(song: Song) {
        controller?.let { mediaController ->
            mediaController.addMediaItem(toMediaItem(song))
            updateState()
        }
    }

    fun togglePlayPause() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else if (mediaController.currentMediaItem != null) {
                mediaController.play()
            } else {
                preferences.lastPlayed()?.let { song ->
                    val position = if (preferences.resumePlayback()) {
                        preferences.lastPlayedPositionMs()
                    } else {
                        0L
                    }
                    artworkScope.launch {
                        if (mediaController.currentMediaItem != null) return@launch
                        setQueueStartingWithSong(mediaController, song, position)
                        mediaController.prepare()
                        mediaController.play()
                        loadCurrentArtwork()
                        updateState()
                    }
                }
            }
        }
        updateState()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        updateState()
    }

    fun skipNext() {
        controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()
        updateState()
    }

    fun skipPrevious() {
        controller?.let {
            if (it.currentPosition > 3_000L) it.seekTo(0L)
            else if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem()
        }
        updateState()
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
        updateState()
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        updateState()
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
        updateState()
    }

    fun refresh() {
        updateState()
    }

    fun updateCurrentSong(song: Song) {
        val mediaController = controller ?: return
        val index = mediaController.currentMediaItemIndex
        if (index < 0) return
        val position = mediaController.currentPosition
        val wasPlaying = mediaController.isPlaying
        _state.value = _state.value.copy(artworkData = null)
        mediaController.replaceMediaItem(index, toMediaItem(song))
        mediaController.seekTo(position)
        if (wasPlaying) mediaController.play()
        updateState(_state.value.queue.map { if (it.id == song.id) song else it })
        loadCurrentArtwork()
    }

    fun release() {
        artworkJob?.cancel()
        positionUpdateJob?.cancel()
        artworkScope.cancel()
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
    }

    private fun startPositionUpdates() {
        if (positionUpdateJob?.isActive == true) return

        positionUpdateJob = artworkScope.launch {
            while (true) {
                updatePlaybackPosition()
                delay(250L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun updatePlaybackPosition() {
        val mediaController = controller ?: return
        val duration = mediaController.duration
            .takeIf { it != C.TIME_UNSET }
            ?.coerceAtLeast(0L)
            ?: 0L
        val position = mediaController.currentPosition.coerceAtLeast(0L)
        val isPlaying = mediaController.isPlaying

        _state.value = _state.value.copy(
            isPlaying = isPlaying,
            positionMs = position,
            durationMs = duration,
        )

        _state.value.currentSong?.let { song ->
            preferences.saveLastPlayed(
                song = song,
                positionMs = position,
                durationMs = duration,
                isPlaying = isPlaying,
                shuffleEnabled = mediaController.shuffleModeEnabled,
                repeatMode = when (mediaController.repeatMode) {
                    Player.REPEAT_MODE_ONE -> 2
                    Player.REPEAT_MODE_ALL -> 1
                    else -> 0
                },
            )
            com.sipun.sonora.widget.SonoraWidgetProvider.updateAllWidgets(appContext)
        }
    }

    private fun restoreLastPlayedIfNeeded() {
        if (restoredLastPlayed || !preferences.resumePlayback()) return
        val mediaController = controller ?: return
        if (mediaController.currentMediaItem != null) {
            restoredLastPlayed = true
            return
        }

        val song = preferences.lastPlayed() ?: return
        restoredLastPlayed = true
        artworkScope.launch {
            if (mediaController.currentMediaItem != null) return@launch
            setQueueStartingWithSong(
                mediaController,
                song,
                preferences.lastPlayedPositionMs().coerceAtLeast(0L),
            )
            mediaController.prepare()
            loadCurrentArtwork()
        }
    }

    private suspend fun setQueueStartingWithSong(
        mediaController: MediaController,
        song: Song,
        positionMs: Long,
    ) {
        val librarySongs = runCatching { musicRepository.songs() }.getOrDefault(emptyList())
        val queueIndex = librarySongs.indexOfFirst { it.uri == song.uri }
            .takeIf { it >= 0 }
            ?: librarySongs.indexOfFirst { it.id == song.id }

        if (queueIndex >= 0) {
            mediaController.setMediaItems(
                librarySongs.map(::toMediaItem),
                queueIndex,
                positionMs.coerceAtLeast(0L),
            )
        } else {
            mediaController.setMediaItem(
                toMediaItem(song),
                positionMs.coerceAtLeast(0L),
            )
        }
    }

    private fun loadCurrentArtwork() {
        val mediaController = controller ?: return
        val item = mediaController.currentMediaItem ?: return
        if (item.mediaMetadata.artworkData != null) return

        artworkJob?.cancel()
        artworkJob = artworkScope.launch {
            val artwork = withContext(Dispatchers.IO) {
                artworkLoader.extractEmbeddedArtwork(item.localConfiguration?.uri)
            } ?: return@launch

            val current = mediaController.currentMediaItem ?: return@launch
            if (current.mediaId != item.mediaId) return@launch

            val metadata = current.mediaMetadata.buildUpon()
                .setArtworkData(
                    artwork,
                    MediaMetadata.PICTURE_TYPE_FRONT_COVER,
                )
                .build()
            mediaController.replaceMediaItem(
                mediaController.currentMediaItemIndex,
                current.buildUpon()
                    .setMediaMetadata(metadata)
                    .build(),
            )
            _state.value = _state.value.copy(artworkData = artwork)
            updateState()
        }
    }

    private fun toMediaItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri?.let(Uri::parse))
                    .build(),
            )
            .build()

    private fun updateState(queue: List<Song>? = null) {
        val mediaController = controller ?: return
        val currentItem = mediaController.currentMediaItem
        val resolvedQueue = queue ?: _state.value.queue
        val currentSong = resolvedQueue.firstOrNull { it.id.toString() == currentItem?.mediaId }
            ?: currentItem?.toSong()
            ?: _state.value.currentSong

        val isPlaying = mediaController.isPlaying
        val position = mediaController.currentPosition.coerceAtLeast(0L)
        val duration = mediaController.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        val shuffleEnabled = mediaController.shuffleModeEnabled
        val repeatModeInt = when (mediaController.repeatMode) {
            Player.REPEAT_MODE_ONE -> 2
            Player.REPEAT_MODE_ALL -> 1
            else -> 0
        }

        val audioFormat = mediaController.currentTracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.getTrackFormat(0)
        val audioBitrate = audioFormat?.bitrate?.takeIf { it > 0 } ?: 0
        val audioSampleRate = audioFormat?.sampleRate?.takeIf { it > 0 } ?: 0
        val audioChannelCount = audioFormat?.channelCount?.takeIf { it > 0 } ?: 0
        val audioBitDepth = audioFormat?.initializationData
            ?.firstOrNull()
            ?.takeIf { it.size > 5 }
            ?.get(5)?.toInt()?.and(0xff)
            ?.takeIf { it == 16 || it == 24 || it == 32 } ?: 0

        currentSong?.let {
            preferences.saveLastPlayed(
                song = it,
                positionMs = position,
                durationMs = duration,
                isPlaying = isPlaying,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatModeInt,
            )
        }
        _state.value = PlayerState(
            currentSong = currentSong,
            artworkData = _state.value.artworkData,
            isPlaying = isPlaying,
            positionMs = position,
            durationMs = duration,
            queue = resolvedQueue,
            queueIndex = currentSong?.let(resolvedQueue::indexOf) ?: -1,
            hasPrevious = mediaController.hasPreviousMediaItem(),
            hasNext = mediaController.hasNextMediaItem(),
            shuffleEnabled = shuffleEnabled,
            repeatMode = when (repeatModeInt) {
                2 -> RepeatMode.ONE
                1 -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
            audioBitrate = audioBitrate,
            audioSampleRate = audioSampleRate,
            audioChannelCount = audioChannelCount,
            audioBitDepth = audioBitDepth,
            audioMimeType = audioFormat?.sampleMimeType,
        )
        com.sipun.sonora.widget.SonoraWidgetProvider.updateAllWidgets(appContext)
    }

    private fun MediaItem.toSong(): Song {
        val metadata = mediaMetadata
        val id = mediaId.toLongOrNull() ?: 0L
        val uri = localConfiguration?.uri?.toString().orEmpty()
        return Song(
            id = id,
            title = metadata.title?.toString().orEmpty().ifBlank { "Unknown title" },
            artist = metadata.artist?.toString().orEmpty().ifBlank { "Unknown artist" },
            album = metadata.albumTitle?.toString().orEmpty().ifBlank { "Unknown album" },
            durationMs = 0L,
            uri = uri,
            albumArtUri = metadata.artworkUri?.toString(),
        )
    }

    private companion object
}
