package com.sipun.sonora.data.preferences

import android.content.Context
import com.sipun.sonora.domain.Song

internal class PlaybackPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun saveLastPlayed(
        song: Song,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        isPlaying: Boolean = false,
        shuffleEnabled: Boolean = false,
        repeatMode: Int = 0,
    ) {
        val history = listeningHistoryIds().toMutableList()
        history.remove(song.id)
        history.add(0, song.id)
        preferences.edit()
            .putLong(LAST_PLAYED_ID, song.id)
            .putString(LAST_PLAYED_TITLE, song.title)
            .putString(LAST_PLAYED_ARTIST, song.artist)
            .putString(LAST_PLAYED_ALBUM, song.album)
            .putString(LAST_PLAYED_URI, song.uri)
            .putString(LAST_PLAYED_ART, song.albumArtUri)
            .putLong(LAST_PLAYED_POSITION, positionMs.coerceAtLeast(0L))
            .putLong(LAST_PLAYED_DURATION, durationMs.coerceAtLeast(0L))
            .putBoolean(KEY_IS_PLAYING, isPlaying)
            .putBoolean(KEY_SHUFFLE_ENABLED, shuffleEnabled)
            .putInt(KEY_REPEAT_MODE, repeatMode)
            .putLong("${LAST_PLAYED_POSITION}_${song.id}", positionMs.coerceAtLeast(0L))
            .putString(LISTENING_HISTORY, history.take(MAX_LISTENING_HISTORY).joinToString(","))
            .apply()
    }

    fun isPlaying(): Boolean = preferences.getBoolean(KEY_IS_PLAYING, false)

    fun setIsPlaying(isPlaying: Boolean) {
        preferences.edit().putBoolean(KEY_IS_PLAYING, isPlaying).apply()
    }

    fun shuffleEnabled(): Boolean = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false)

    fun repeatMode(): Int = preferences.getInt(KEY_REPEAT_MODE, 0)

    fun lastPlayedDurationMs(): Long = preferences.getLong(LAST_PLAYED_DURATION, 0L)

    fun listeningHistoryIds(): List<Long> =
        preferences.getString(LISTENING_HISTORY, null)
            ?.split(',')
            ?.mapNotNull(String::toLongOrNull)
            .orEmpty()

    fun lastPlayed(): Song? {
        val uri = preferences.getString(LAST_PLAYED_URI, null) ?: return null
        return Song(
            id = preferences.getLong(LAST_PLAYED_ID, 0L),
            title = preferences.getString(LAST_PLAYED_TITLE, null).orEmpty()
                .ifBlank { "Unknown title" },
            artist = preferences.getString(LAST_PLAYED_ARTIST, null).orEmpty()
                .ifBlank { "Unknown artist" },
            album = preferences.getString(LAST_PLAYED_ALBUM, null).orEmpty()
                .ifBlank { "Unknown album" },
            durationMs = 0L,
            uri = uri,
            albumArtUri = preferences.getString(LAST_PLAYED_ART, null),
        )
    }

    fun lastPlayedPositionMs(): Long =
        preferences.getLong(LAST_PLAYED_POSITION, 0L)

    fun lastPlayedPositionMs(songId: Long): Long =
        preferences.getLong("${LAST_PLAYED_POSITION}_${songId}", 0L)

    fun resumePlayback(): Boolean =
        preferences.getBoolean(KEY_RESUME_PLAYBACK, false)

    fun setResumePlayback(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_RESUME_PLAYBACK, enabled).apply()
    }

    fun gaplessPlayback(): Boolean =
        preferences.getBoolean(KEY_GAPLESS_PLAYBACK, true)

    fun setGaplessPlayback(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_GAPLESS_PLAYBACK, enabled).apply()
    }

    fun crossfadeSeconds(): Int =
        preferences.getInt(KEY_CROSSFADE_SECONDS, 2).coerceIn(0, MAX_CROSSFADE_SECONDS)

    fun normalizeVolume(): Boolean =
        preferences.getBoolean(KEY_NORMALIZE_VOLUME, false)

    fun setNormalizeVolume(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NORMALIZE_VOLUME, enabled).apply()
    }

    fun keepScreenAwake(): Boolean =
        preferences.getBoolean(KEY_KEEP_SCREEN_AWAKE, false)

    fun setKeepScreenAwake(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_KEEP_SCREEN_AWAKE, enabled).apply()
    }

    fun setCrossfadeSeconds(seconds: Int) {
        preferences.edit()
            .putInt(KEY_CROSSFADE_SECONDS, seconds.coerceIn(0, MAX_CROSSFADE_SECONDS))
            .apply()
    }

    fun clearLastPlayed() {
        preferences.edit()
            .remove(LAST_PLAYED_ID)
            .remove(LAST_PLAYED_TITLE)
            .remove(LAST_PLAYED_ARTIST)
            .remove(LAST_PLAYED_ALBUM)
            .remove(LAST_PLAYED_URI)
            .remove(LAST_PLAYED_ART)
            .remove(LAST_PLAYED_POSITION)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "sonora_preferences"
        const val LAST_PLAYED_ID = "last_played_id"
        const val LAST_PLAYED_TITLE = "last_played_title"
        const val LAST_PLAYED_ARTIST = "last_played_artist"
        const val LAST_PLAYED_ALBUM = "last_played_album"
        const val LAST_PLAYED_URI = "last_played_uri"
        const val LAST_PLAYED_ART = "last_played_art"
        const val LAST_PLAYED_POSITION = "last_played_position"
        const val LAST_PLAYED_DURATION = "last_played_duration"
        const val KEY_IS_PLAYING = "key_is_playing"
        const val KEY_SHUFFLE_ENABLED = "key_shuffle_enabled"
        const val KEY_REPEAT_MODE = "key_repeat_mode"
        const val LISTENING_HISTORY = "listening_history"
        const val KEY_RESUME_PLAYBACK = "resume_playback"
        const val KEY_GAPLESS_PLAYBACK = "gapless_playback"
        const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
        const val KEY_NORMALIZE_VOLUME = "normalize_volume"
        const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        const val MAX_LISTENING_HISTORY = 10
        const val MAX_CROSSFADE_SECONDS = 12
    }
}
