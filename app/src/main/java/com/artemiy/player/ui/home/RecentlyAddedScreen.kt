package com.artemiy.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemiy.player.data.LibraryViewMode
import com.artemiy.player.data.Song
import com.artemiy.player.ui.library.LibraryHeader
import com.artemiy.player.ui.library.ListToolbar
import com.artemiy.player.ui.library.PlayShuffleRow
import com.artemiy.player.ui.library.SongList
import com.artemiy.player.ui.library.SongsGrid
import com.artemiy.player.ui.library.ViewMode
import com.artemiy.player.ui.settings.SettingsViewModel
import com.artemiy.player.ui.theme.PlayerColors

/** The last 50 files added to the library, newest first — like "Треки", but the order is fixed. */
@Composable
fun RecentlyAddedScreen(
    songs: List<Song>,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    val settingsVm: SettingsViewModel = viewModel()
    val viewMode = ViewMode.valueOf(settingsVm.viewMode("recent").name)
    var query by remember { mutableStateOf("") }
    val filtered = remember(songs, query) {
        songs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
    }
    Column(modifier = Modifier.fillMaxSize().background(PlayerColors.Background).statusBarsPadding()) {
        LibraryHeader(title = "Недавно добавленные", showBack = true, onBack = onBack)
        ListToolbar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Поиск",
            sortOptions = emptyList<Unit>(),
            sortOptionLabel = { "" },
            currentSort = "",
            onSortSelect = {},
            viewMode = viewMode,
            onViewModeCycle = { settingsVm.setViewMode("recent", LibraryViewMode.valueOf(viewMode.next().name)) },
        )
        if (filtered.isNotEmpty()) {
            PlayShuffleRow(
                onPlay = { onSongClick(filtered.first(), filtered) },
                onShuffle = { filtered.shuffled().let { onSongClick(it.first(), it) } },
            )
        }
        when (viewMode) {
            ViewMode.LIST -> SongList(
                songs = filtered,
                state = rememberLazyListState(),
                onSongClick = { song -> onSongClick(song, filtered) },
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
            )
            ViewMode.GRID_2, ViewMode.GRID_3 -> SongsGrid(
                songs = filtered,
                columns = if (viewMode == ViewMode.GRID_2) 2 else 3,
                state = rememberLazyGridState(),
                onSongClick = { song -> onSongClick(song, filtered) },
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
            )
        }
    }
}
