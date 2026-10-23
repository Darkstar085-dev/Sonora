package com.sipun.sonora.player

import com.sipun.sonora.domain.Song

data class PlayerState(
    val currentSong: Song? = null,
    val artworkData: ByteArray? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val audioBitrate: Int = 0,
    val audioSampleRate: Int = 0,
    val audioChannelCount: Int = 0,
    val audioBitDepth: Int = 0,
    val audioMimeType: String? = null,
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}
