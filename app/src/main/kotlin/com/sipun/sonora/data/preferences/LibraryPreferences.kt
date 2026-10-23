package com.sipun.sonora.data.preferences

import android.content.Context

internal class LibraryPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun favoriteIds(): Set<Long> =
        preferences.getStringSet(FAVORITE_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toLongOrNull)
            .toSet()

    fun toggleFavorite(songId: Long): Boolean {
        val favorites = favoriteIds().toMutableSet()
        val isFavorite = if (favorites.add(songId)) true else {
            favorites.remove(songId)
            false
        }
        preferences.edit()
            .putStringSet(FAVORITE_IDS, favorites.map(Long::toString).toSet())
            .apply()
        return isFavorite
    }

    fun playlists(): List<SonoraPlaylist> =
        preferences.getStringSet(PLAYLISTS, emptySet())
            .orEmpty()
            .mapNotNull { encoded ->
                val parts = encoded.split('|', limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val ids = parts[2].split(',').mapNotNull(String::toLongOrNull)
                SonoraPlaylist(parts[0], parts[1], ids)
            }
            .sortedBy { it.name.lowercase() }

    fun createPlaylist(name: String): SonoraPlaylist? {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return null
        val playlist = SonoraPlaylist(
            id = "playlist_" + System.currentTimeMillis(),
            name = cleanName,
            songIds = emptyList(),
        )
        savePlaylists(playlists() + playlist)
        return playlist
    }

    fun deletePlaylist(id: String) {
        savePlaylists(playlists().filterNot { it.id == id })
    }

    fun addToPlaylist(playlistId: String, songId: Long) {
        savePlaylists(
            playlists().map {
                if (it.id == playlistId && songId !in it.songIds) {
                    it.copy(songIds = it.songIds + songId)
                } else {
                    it
                }
            },
        )
    }

    fun removeFromPlaylist(playlistId: String, songId: Long) {
        savePlaylists(
            playlists().map {
                if (it.id == playlistId) {
                    it.copy(songIds = it.songIds.filterNot { id -> id == songId })
                } else {
                    it
                }
            },
        )
    }

    fun sortOption(scope: String): String =
        preferences.getString("$SORT_OPTION_PREFIX$scope", DEFAULT_SORT_OPTION)
            ?: DEFAULT_SORT_OPTION

    fun setSortOption(scope: String, option: String) {
        preferences.edit().putString("$SORT_OPTION_PREFIX$scope", option).apply()
    }

    fun excludedFolders(): Set<String> =
        preferences.getStringSet(EXCLUDED_FOLDERS, emptySet())
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "/" && it != "/storage/emulated/0" }
            .toSet()

    fun setExcludedFolders(folders: Set<String>) {
        preferences.edit().putStringSet(EXCLUDED_FOLDERS, folders).apply()
    }

    fun addExcludedFolder(folder: String) {
        val clean = folder.trim()
        if (clean.isBlank() || clean == "/" || clean == "/storage/emulated/0") return
        val set = excludedFolders().toMutableSet()
        if (set.add(clean)) {
            setExcludedFolders(set)
        }
    }

    fun removeExcludedFolder(folder: String) {
        val set = excludedFolders().toMutableSet()
        if (set.remove(folder)) {
            setExcludedFolders(set)
        }
    }

    private fun savePlaylists(playlists: List<SonoraPlaylist>) {
        preferences.edit()
            .putStringSet(
                PLAYLISTS,
                playlists.map { playlist ->
                    playlist.id + "|" + playlist.name.replace("|", " ") + "|" +
                            playlist.songIds.joinToString(",")
                }.toSet(),
            )
            .apply()
    }

    private companion object {
        const val FILE_NAME = "sonora_preferences"
        const val FAVORITE_IDS = "favorite_song_ids"
        const val PLAYLISTS = "playlists"
        const val EXCLUDED_FOLDERS = "excluded_folders"
        const val SORT_OPTION_PREFIX = "sort_option_"
        const val DEFAULT_SORT_OPTION = "TITLE_ASC"
    }
}
