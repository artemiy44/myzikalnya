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
import com.artemiy.player.data.SmartKind
import com.artemiy.player.data.buildSmartPlaylists
import kotlinx.coroutines.launch

class PlaylistsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).playlistDao()

    var playlists by mutableStateOf<List<PlaylistWithCount>>(emptyList())
        private set

    /** Each smart playlist's songs right now (see SmartPlaylists); kept for the playlists page's counts. */
    var smart by mutableStateOf<Map<SmartKind, List<Song>>>(emptyMap())
        private set

    init {
        refresh()
    }

    /** Works the smart playlists out again — what's been played since changes them. */
    fun refreshSmart(allSongs: List<Song>) {
        viewModelScope.launch { smart = computeSmart(allSongs) }
    }

    private suspend fun computeSmart(allSongs: List<Song>): Map<SmartKind, List<Song>> {
        val history = AppDatabase.get(getApplication()).playHistoryDao()
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            buildSmartPlaylists(allSongs, history.allPlays(), history.allSkips())
        }
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

    /** Creates a playlist already holding [songIds], in that order. */
    fun createPlaylistWithSongs(name: String, songIds: List<Long>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = dao.insertPlaylist(PlaylistEntity(name = trimmed, createdAt = System.currentTimeMillis()))
            songIds.distinct().forEachIndexed { position, songId ->
                dao.insertPlaylistSong(PlaylistSongEntity(id, songId, position))
            }
            refresh()
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
        // A smart playlist (negative id): worked out from the listening, not read from the table.
        SmartKind.of(playlistId)?.let { kind -> return computeSmart(allSongs)[kind].orEmpty() }
        val ids = dao.getSongIds(playlistId)
        val byId = allSongs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}
