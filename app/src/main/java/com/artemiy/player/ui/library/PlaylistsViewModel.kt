package com.artemiy.player.ui.library

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artemiy.player.data.AppDatabase
import com.artemiy.player.data.PlaylistEntity
import com.artemiy.player.data.PlaylistSongEntity
import com.artemiy.player.data.PlaylistWithCount
import com.artemiy.player.data.Song
import kotlinx.coroutines.launch

class PlaylistsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).playlistDao()

    var playlists by mutableStateOf<List<PlaylistWithCount>>(emptyList())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { playlists = dao.getPlaylistsWithCount() }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = dao.insertPlaylist(PlaylistEntity(name = trimmed, createdAt = System.currentTimeMillis()))
            refresh()
            onCreated(id)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val position = dao.nextPosition(playlistId)
            dao.insertPlaylistSong(PlaylistSongEntity(playlistId, songId, position))
            refresh()
            onDone()
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            dao.removeSong(playlistId, songId)
            refresh()
        }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.renamePlaylist(playlistId, trimmed)
            refresh()
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            dao.deletePlaylist(playlistId)
            refresh()
        }
    }

    suspend fun getSongsForPlaylist(playlistId: Long, allSongs: List<Song>): List<Song> {
        val ids = dao.getSongIds(playlistId)
        val byId = allSongs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}
