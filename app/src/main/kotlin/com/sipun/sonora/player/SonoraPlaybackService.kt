package com.sipun.sonora.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.mp3.Mp3InfoReplayGain
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sipun.sonora.MainActivity
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.SonoraPreferences
import kotlin.math.log10
import kotlin.math.pow

class SonoraPlaybackService : MediaSessionService() {
    private val preferences by lazy { SonoraPreferences(applicationContext) }
    private val transitionHandler = Handler(Looper.getMainLooper())
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            cancelCrossfade()
            activePlayer?.pause()
        }
    }
    private val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val defaultResult = super<MediaSession.Callback>.onConnect(session, controller)
            if (!defaultResult.isAccepted || controller.isTrusted) return defaultResult

            val playbackCommands = defaultResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_GET_TIMELINE)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
                .build()

            return MediaSession.ConnectionResult.accept(
                defaultResult.availableSessionCommands,
                playbackCommands,
            )
        }
    }
    private var mediaSession: MediaSession? = null
    private var activePlayer: ExoPlayer? = null
    private var crossfadePlayer: ExoPlayer? = null
    private var crossfadeFromIndex = C.INDEX_UNSET
    private var crossfadeTargetIndex = C.INDEX_UNSET
    private var crossfadeStartElapsedMs = 0L
    private var crossfadeDurationMs = 0L
    private val normalizationVolumes = mutableMapOf<ExoPlayer, Float>()
    private val normalizationEffects = mutableMapOf<ExoPlayer, LoudnessEnhancer?>()
    private val normalizationMetadata = mutableMapOf<ExoPlayer, Metadata>()
    private var externalPlaybackActive = false

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            audioBecomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        setForegroundServiceTimeoutMs(120_000L)

        val initialPlayer = buildPlayer(handleAudioFocus = true)
        activePlayer = initialPlayer
        initialPlayer.addListener(serviceListener)
        addNormalizationListener(initialPlayer)

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            .also { it.setSmallIcon(R.drawable.sonora_launcher_monochrome) }

        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, initialPlayer)
            .setCallback(mediaSessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

        addSession(mediaSession!!)
        transitionHandler.post(transitionRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (externalPlaybackActive) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private fun beginExternalPlayback() {
        if (externalPlaybackActive) return
        externalPlaybackActive = true
        triggerNotificationUpdate()
    }

    private fun endExternalPlayback() {
        if (!externalPlaybackActive) return
        externalPlaybackActive = false
        triggerNotificationUpdate()
    }

    override fun onDestroy() {
        unregisterReceiver(audioBecomingNoisyReceiver)
        transitionHandler.removeCallbacks(transitionRunnable)
        cancelCrossfade()
        mediaSession?.release()
        mediaSession = null
        releasePlayer(activePlayer)
        activePlayer = null
        super.onDestroy()
    }

    private fun buildPlayer(handleAudioFocus: Boolean): ExoPlayer =
        ExoPlayer.Builder(this)
            .setRenderersFactory(SonoraRenderersFactory(this))
            .setHandleAudioBecomingNoisy(false)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .setPauseAtEndOfMediaItems(
                preferences.crossfadeSeconds() == 0 && !preferences.gaplessPlayback(),
            )
            .build()

    private val serviceListener = object : Player.Listener {
        override fun onMediaItemTransition(
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int,
        ) {
            val active = activePlayer ?: return

            if (crossfadePlayer != null) {
                when {
                    active.currentMediaItemIndex == crossfadeTargetIndex &&
                            reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                        active.volume = 0f
                    }

                    active.currentMediaItemIndex != crossfadeFromIndex -> {
                        cancelCrossfade()
                        active.volume = normalizationVolumes[active] ?: 1f
                    }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) {
                beginExternalPlayback()
            } else if (playWhenReady &&
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS &&
                externalPlaybackActive
            ) {
                endExternalPlayback()
            }
            if (!playWhenReady &&
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                preferences.crossfadeSeconds() == 0 &&
                !preferences.gaplessPlayback()
            ) {
                transitionHandler.postDelayed(
                    { activePlayer?.play() },
                    NON_GAPLESS_DELAY_MS,
                )
            }
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
                beginExternalPlayback()
            } else if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                endExternalPlayback()
            }
        }

        override fun onTimelineChanged(
            timeline: androidx.media3.common.Timeline,
            reason: Int,
        ) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED && crossfadePlayer != null) {
                cancelCrossfade()
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (crossfadePlayer != null) cancelCrossfade()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            if (crossfadePlayer != null) cancelCrossfade()
        }
    }

    private val transitionRunnable = object : Runnable {
        override fun run() {
            val active = activePlayer

            if (active != null) {
                val crossfadeMs = preferences.crossfadeSeconds() * 1_000L
                active.pauseAtEndOfMediaItems = crossfadeMs == 0L && !preferences.gaplessPlayback()

                if (crossfadePlayer != null) {
                    updateCrossfade(active)
                } else if (crossfadeMs > 0L) {
                    maybePrepareCrossfade(active, crossfadeMs)
                } else {
                    normalizationMetadata[active]?.let { metadata ->
                        if (preferences.normalizeVolume()) applyNormalization(active, metadata)
                    }
                    val baseVolume = normalizationVolumes[active] ?: 1f
                    if (active.volume != baseVolume) active.volume = baseVolume
                }
            }

            transitionHandler.postDelayed(this, TRANSITION_TICK_MS)
        }
    }

    private fun maybePrepareCrossfade(active: ExoPlayer, crossfadeMs: Long) {
        if (!active.isPlaying || active.currentMediaItemIndex < 0) return

        val duration = active.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return

        val remaining = duration - active.currentPosition
        val prepareThreshold = crossfadeMs + PREPARE_BUFFER_MS
        if (remaining !in 0..prepareThreshold || !active.hasNextMediaItem()) return

        val nextIndex = active.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return

        val items = List(active.mediaItemCount) { active.getMediaItemAt(it) }
        val next = buildPlayer(handleAudioFocus = false)

        next.setMediaItems(items, nextIndex, 0L)
        addNormalizationListener(next)
        next.shuffleModeEnabled = active.shuffleModeEnabled
        next.repeatMode = active.repeatMode
        next.playbackParameters = active.playbackParameters
        next.volume = 0f
        next.prepare()

        crossfadePlayer = next
        crossfadeFromIndex = active.currentMediaItemIndex
        crossfadeTargetIndex = nextIndex
        crossfadeStartElapsedMs = 0L
        crossfadeDurationMs = crossfadeMs
    }

    private fun updateCrossfade(active: ExoPlayer) {
        val next = crossfadePlayer ?: return

        if (active.currentMediaItemIndex == crossfadeTargetIndex) {
            finalizeCrossfade(active, next)
            return
        }

        if (active.currentMediaItemIndex != crossfadeFromIndex) {
            cancelCrossfade()
            active.volume = 1f
            return
        }

        val remaining = active.duration - active.currentPosition
        if (crossfadeStartElapsedMs == 0L) {
            if (remaining <= crossfadeDurationMs) {
                next.play()
            }
            if (!next.isPlaying) return
            crossfadeStartElapsedMs = SystemClock.elapsedRealtime()
        } else if (!next.isPlaying) {
            return
        }

        val elapsed = SystemClock.elapsedRealtime() - crossfadeStartElapsedMs
        val progress = (elapsed.toFloat() / crossfadeDurationMs.coerceAtLeast(1L))
            .coerceIn(0f, 1f)

        val activeBaseVolume = normalizationVolumes[active] ?: 1f
        val nextBaseVolume = normalizationVolumes[next] ?: 1f
        active.volume = activeBaseVolume * (1f - progress)
        next.volume = nextBaseVolume * progress

        if (progress >= 1f) {
            finalizeCrossfade(active, next)
        }
    }

    private fun finalizeCrossfade(oldPlayer: ExoPlayer, nextPlayer: ExoPlayer) {
        nextPlayer.setAudioAttributes(audioAttributes, true)
        nextPlayer.volume = normalizationVolumes[nextPlayer] ?: 1f
        nextPlayer.addListener(serviceListener)
        mediaSession?.setPlayer(nextPlayer)
        activePlayer = nextPlayer

        oldPlayer.removeListener(serviceListener)
        releasePlayer(oldPlayer)

        crossfadePlayer = null
        crossfadeFromIndex = C.INDEX_UNSET
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeStartElapsedMs = 0L
        crossfadeDurationMs = 0L
    }

    private fun cancelCrossfade() {
        crossfadePlayer?.let { next ->
            releasePlayer(next)
        }
        crossfadePlayer = null
        crossfadeFromIndex = C.INDEX_UNSET
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeStartElapsedMs = 0L
        crossfadeDurationMs = 0L
        activePlayer?.volume = activePlayer?.let { normalizationVolumes[it] ?: 1f } ?: 1f
    }

    private fun addNormalizationListener(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onMetadata(metadata: Metadata) {
                normalizationMetadata[player] = metadata
                applyNormalization(player, metadata)
            }
        })
    }

    private fun applyNormalization(player: ExoPlayer, metadata: Metadata) {
        if (!preferences.normalizeVolume()) {
            clearNormalization(player)
            return
        }

        var gainDb: Float? = null
        var peak: Float? = null

        for (index in 0 until metadata.length()) {
            when (val entry = metadata[index]) {
                is TextInformationFrame -> {
                    when (val key = entry.description?.trim()?.uppercase()) {
                        "REPLAYGAIN_TRACK_GAIN", "REPLAYGAIN_ALBUM_GAIN" -> {
                            if (gainDb == null || key == "REPLAYGAIN_TRACK_GAIN") {
                                gainDb = parseGainDb(entry.values.first())
                            }
                        }

                        "REPLAYGAIN_TRACK_PEAK", "REPLAYGAIN_ALBUM_PEAK" -> {
                            if (peak == null || key == "REPLAYGAIN_TRACK_PEAK") {
                                peak = entry.values.first().toFloatOrNull()
                            }
                        }
                    }
                }

                is androidx.media3.extractor.metadata.vorbis.VorbisComment -> {
                    when (entry.key.trim().uppercase()) {
                        "REPLAYGAIN_TRACK_GAIN", "REPLAYGAIN_ALBUM_GAIN" -> {
                            if (gainDb == null || entry.key.equals("REPLAYGAIN_TRACK_GAIN", true)) {
                                gainDb = parseGainDb(entry.value)
                            }
                        }

                        "REPLAYGAIN_TRACK_PEAK", "REPLAYGAIN_ALBUM_PEAK" -> {
                            if (peak == null || entry.key.equals("REPLAYGAIN_TRACK_PEAK", true)) {
                                peak = entry.value.toFloatOrNull()
                            }
                        }
                    }
                }

                is Mp3InfoReplayGain -> {
                    val field = entry.field1 ?: entry.field2
                    if (field != null && field.name == Mp3InfoReplayGain.GainField.NAME_RADIO) {
                        gainDb = field.gain
                    }
                    if (entry.peak > 0f) peak = entry.peak
                }
            }
        }

        val gain = gainDb ?: run {
            clearNormalization(player)
            return
        }
        val safeGain = if (gain > 0f && peak != null && peak > 0f) {
            minOf(gain, -20f * log10(peak))
        } else {
            gain
        }

        if (safeGain <= 0f) {
            releaseNormalizationEffect(player)
            normalizationVolumes[player] = dbToLinear(safeGain)
            player.volume = normalizationVolumes[player] ?: 1f
        } else {
            normalizationVolumes[player] = 1f
            try {
                val enhancer =
                    normalizationEffects[player] ?: LoudnessEnhancer(player.audioSessionId).also {
                        normalizationEffects[player] = it
                    }
                enhancer.setTargetGain((safeGain * 100f).toInt())
                enhancer.enabled = true
                player.volume = 1f
            } catch (_: RuntimeException) {
                releaseNormalizationEffect(player)
                player.volume = 1f
            }
        }
    }

    private fun clearNormalization(player: ExoPlayer) {
        releaseNormalizationEffect(player)
        normalizationMetadata.remove(player)
        normalizationVolumes[player] = 1f
        player.volume = 1f
    }

    private fun releaseNormalizationEffect(player: ExoPlayer) {
        normalizationEffects.remove(player)?.let { effect ->
            try {
                effect.enabled = false
            } catch (_: RuntimeException) {
            }
            effect.release()
        }
    }

    private fun releasePlayer(player: ExoPlayer?) {
        if (player == null) return
        releaseNormalizationEffect(player)
        normalizationVolumes.remove(player)
        normalizationMetadata.remove(player)
        player.removeListener(serviceListener)
        player.stop()
        player.release()
    }

    private fun parseGainDb(value: String): Float? =
        value.trim().removeSuffix("dB").trim().toFloatOrNull()

    private fun dbToLinear(db: Float): Float =
        10f.pow(db / 20f).coerceIn(0f, 1f)

    private companion object {
        private const val NOTIFICATION_CHANNEL_ID = "sonora_playback"
        private const val TRANSITION_TICK_MS = 50L
        private const val PREPARE_BUFFER_MS = 3_000L
        private const val NON_GAPLESS_DELAY_MS = 350L
    }
}
