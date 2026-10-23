package com.sipun.sonora.domain

interface MusicRepository {
    suspend fun songs(): List<Song>
    suspend fun albums(): List<String>
    suspend fun artists(): List<String>
}
