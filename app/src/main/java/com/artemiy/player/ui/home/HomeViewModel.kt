package com.artemiy.player.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artemiy.player.data.AppDatabase
import com.artemiy.player.data.Song
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val playHistoryDao = AppDatabase.get(app).playHistoryDao()

    var quickPicks by mutableStateOf<List<Song>>(emptyList())
        private set

    var keepListening by mutableStateOf<List<Song>>(emptyList())
        private set

    var recentlyAdded by mutableStateOf<List<Song>>(emptyList())
        private set

    fun refresh(allSongs: List<Song>) {
        if (allSongs.isEmpty()) return
        viewModelScope.launch {
            val songsById = allSongs.associateBy { it.id }

            val topCounts = playHistoryDao.topPlayed(limit = 8)
            quickPicks = topCounts.mapNotNull { songsById[it.songId] }

            val recentIds = playHistoryDao.recentlyPlayed(limit = 8)
            keepListening = recentIds.mapNotNull { songsById[it] }

            recentlyAdded = allSongs.sortedByDescending { it.dateAddedMs }.take(8)
        }
    }
}
