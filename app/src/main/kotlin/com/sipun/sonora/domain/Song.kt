package com.sipun.sonora.domain

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val folder: String? = null,
    val dateAddedSeconds: Long = 0L,
)
