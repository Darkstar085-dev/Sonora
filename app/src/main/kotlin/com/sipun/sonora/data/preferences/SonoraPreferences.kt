package com.sipun.sonora.data.preferences

import android.content.Context
import com.sipun.sonora.domain.Song

data class SonoraPlaylist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
)

/**
 * Compatibility facade over the focused preference stores.
 *
 * Library state and playback state remain in the same SharedPreferences file
 * and use the same keys so existing app data remains compatible.
 */
class SonoraPreferences(context: Context) {
    private val library = LibraryPreferences(context)
    private val playback = PlaybackPreferences(context)

    fun favoriteIds(): Set<Long> = library.favoriteIds()

    fun sortOption(scope: String): String = library.sortOption(scope)

    fun setSortOption(scope: String, option: String) = library.setSortOption(scope, option)

    fun saveLastPlayed(
        song: Song,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        isPlaying: Boolean = false,
        shuffleEnabled: Boolean = false,
        repeatMode: Int = 0,
    ) = playback.saveLastPlayed(
        song = song,
        positionMs = positionMs,
        durationMs = durationMs,
        isPlaying = isPlaying,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
    )

    fun isPlaying(): Boolean = playback.isPlaying()

    fun setIsPlaying(isPlaying: Boolean) = playback.setIsPlaying(isPlaying)

    fun shuffleEnabled(): Boolean = playback.shuffleEnabled()

    fun repeatMode(): Int = playback.repeatMode()

    fun lastPlayedDurationMs(): Long = playback.lastPlayedDurationMs()

    fun lastPlayedPositionMs(): Long = playback.lastPlayedPositionMs()

    fun lastPlayedPositionMs(songId: Long): Long =
        playback.lastPlayedPositionMs(songId)

    fun resumePlayback(): Boolean = playback.resumePlayback()

    fun setResumePlayback(enabled: Boolean) = playback.setResumePlayback(enabled)

    fun gaplessPlayback(): Boolean = playback.gaplessPlayback()

    fun setGaplessPlayback(enabled: Boolean) = playback.setGaplessPlayback(enabled)

    fun crossfadeSeconds(): Int = playback.crossfadeSeconds()

    fun setCrossfadeSeconds(seconds: Int) = playback.setCrossfadeSeconds(seconds)

    fun normalizeVolume(): Boolean = playback.normalizeVolume()

    fun setNormalizeVolume(enabled: Boolean) = playback.setNormalizeVolume(enabled)

    fun keepScreenAwake(): Boolean = playback.keepScreenAwake()

    fun setKeepScreenAwake(enabled: Boolean) = playback.setKeepScreenAwake(enabled)

    fun listeningHistoryIds(): List<Long> = playback.listeningHistoryIds()

    fun lastPlayed(): Song? = playback.lastPlayed()

    fun clearLastPlayed() = playback.clearLastPlayed()

    fun toggleFavorite(songId: Long): Boolean = library.toggleFavorite(songId)

    fun playlists(): List<SonoraPlaylist> = library.playlists()

    fun createPlaylist(name: String): SonoraPlaylist? = library.createPlaylist(name)

    fun deletePlaylist(id: String) = library.deletePlaylist(id)

    fun addToPlaylist(playlistId: String, songId: Long) =
        library.addToPlaylist(playlistId, songId)

    fun removeFromPlaylist(playlistId: String, songId: Long) =
        library.removeFromPlaylist(playlistId, songId)

    fun excludedFolders(): Set<String> = library.excludedFolders()
    fun setExcludedFolders(folders: Set<String>) = library.setExcludedFolders(folders)
    fun addExcludedFolder(folder: String) = library.addExcludedFolder(folder)
    fun removeExcludedFolder(folder: String) = library.removeExcludedFolder(folder)
}
