package com.artemiy.player.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artemiy.player.data.AppDatabase
import com.artemiy.player.data.Mix
import com.artemiy.player.data.Recap
import com.artemiy.player.data.Song
import com.artemiy.player.data.buildMixes
import com.artemiy.player.data.buildRecap
import com.artemiy.player.data.statDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val playHistoryDao = AppDatabase.get(app).playHistoryDao()

    var mixes by mutableStateOf<List<Mix>>(emptyList())
        private set

    /** Days with at least one play so far — mixes show once this reaches MIX_MIN_STAT_DAYS. */
    var statDays by mutableStateOf(0)
        private set

    var quickPicks by mutableStateOf<List<Song>>(emptyList())
        private set

    var recentlyAdded by mutableStateOf<List<Song>>(emptyList())
        private set

    /** The last 50 files added to the library, newest first — "see all" of [recentlyAdded]. */
    var recentlyAddedAll by mutableStateOf<List<Song>>(emptyList())
        private set

    /** Null until a full calendar week with plays has passed. */
    var recap by mutableStateOf<Recap?>(null)
        private set

    fun refresh(allSongs: List<Song>) {
        if (allSongs.isEmpty()) return
        viewModelScope.launch {
            val plays = playHistoryDao.allPlays()
            val skips = playHistoryDao.allSkips()
            val topPlayed = playHistoryDao.topPlayed(limit = 10)
            val now = System.currentTimeMillis()
            val songsById = allSongs.associateBy { it.id }
            val newest = allSongs.sortedByDescending { it.dateAddedMs }
            // The heavy counting happens off the main thread; the results land on it.
            val builtMixes = withContext(Dispatchers.Default) { buildMixes(allSongs, plays, skips, now) }
            val builtRecap = withContext(Dispatchers.Default) { buildRecap(allSongs, plays, now) }
            statDays = statDays(plays)
            mixes = builtMixes
            quickPicks = topPlayed.mapNotNull { songsById[it.songId] }
            recentlyAdded = newest.take(8)
            recentlyAddedAll = newest.take(50)
            recap = builtRecap
        }
    }
}
