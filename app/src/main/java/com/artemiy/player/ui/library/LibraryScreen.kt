package com.artemiy.player.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemiy.player.data.LibraryViewMode
import com.artemiy.player.data.PlaylistWithCount
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.songLongPressTrigger
import com.artemiy.player.ui.settings.SettingsViewModel
import com.artemiy.player.ui.theme.PlayerColors

sealed class LibraryRoute {
    data object Home : LibraryRoute()
    data object Playlists : LibraryRoute()
    data object Artists : LibraryRoute()
    data object Albums : LibraryRoute()
    data object Songs : LibraryRoute()
    data class ArtistDetail(val artist: String) : LibraryRoute()
    data class AlbumDetail(val album: String, val artist: String) : LibraryRoute()
    data class PlaylistDetail(val playlistId: Long, val name: String) : LibraryRoute()
}

private data class ArtistGroup(val name: String, val songs: List<Song>)
private data class AlbumGroup(val album: String, val artist: String, val songs: List<Song>)

/** Cycled by a single toolbar icon, in this order: list rows, then 2-wide grid, then 3-wide. */
internal enum class ViewMode(val icon: ImageVector, val description: String) {
    LIST(Icons.Filled.ViewList, "Список"),
    GRID_2(Icons.Filled.GridView, "Сетка (2)"),
    GRID_3(Icons.Filled.Apps, "Сетка (3)");

    fun next(): ViewMode = entries[(ordinal + 1) % entries.size]
}

private enum class ArtistSort(val label: String) { COUNT("По числу треков"), RECENT("Недавно добавленные"), NAME("По алфавиту") }
private enum class AlbumSort(val label: String) { RECENT("Недавно добавленные"), NAME("По алфавиту"), COUNT("По числу треков"), ARTIST("По артисту") }
private enum class PlaylistSongSort(val label: String) { ORDER("В порядке добавления"), TITLE("По названию"), ARTIST("По артисту"), RECENT("Недавно добавленные в медиатеку") }
private enum class SongSort(val label: String) { RECENT("Недавно добавленные"), RELEASE_DATE("По дате выпуска"), TITLE("По названию"), ARTIST("По артисту") }

@Composable
fun LibraryScreen(
    permissionGranted: Boolean,
    songs: List<Song>,
    backStack: androidx.compose.runtime.snapshots.SnapshotStateList<LibraryRoute>,
    onRequestPermission: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddAllToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    val route = backStack.last()
    val playlistsVm: PlaylistsViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()

    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showRenamePlaylist by remember { mutableStateOf(false) }
    var showDeletePlaylist by remember { mutableStateOf(false) }

    // Hoisted so scroll position survives navigating into a detail screen and back.
    val artistsListState = rememberLazyListState()
    val artistsGridState = rememberLazyGridState()
    val albumsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val songsListState = rememberLazyListState()
    val songsGridState = rememberLazyGridState()

    var artistQuery by remember { mutableStateOf("") }
    var artistSort by remember { mutableStateOf(ArtistSort.COUNT) }
    // Persisted (DataStore, via SettingsViewModel) so the chosen list/grid style survives a
    // restart instead of resetting to each tab's default every time.
    val artistViewMode = ViewMode.valueOf(settingsVm.viewMode("artists").name)
    var albumQuery by remember { mutableStateOf("") }
    var albumSort by remember { mutableStateOf(AlbumSort.RECENT) }
    val albumViewMode = ViewMode.valueOf(settingsVm.viewMode("albums").name)
    var songQuery by remember { mutableStateOf("") }
    var songSort by remember { mutableStateOf(SongSort.RECENT) }
    val songViewMode = ViewMode.valueOf(settingsVm.viewMode("songs").name)
    val playlistViewMode = ViewMode.valueOf(settingsVm.viewMode("playlist").name)

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
    }

    fun push(r: LibraryRoute) {
        backStack.add(r)
    }

    // Artist/album pages run their cover art up under the status bar themselves.
    val edgeToEdge = route is LibraryRoute.ArtistDetail || route is LibraryRoute.AlbumDetail
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .then(if (edgeToEdge) Modifier else Modifier.statusBarsPadding()),
    ) {
        if (!edgeToEdge) {
            LibraryHeader(
                title = when (val r = route) {
                    LibraryRoute.Home -> "Медиатека"
                    LibraryRoute.Playlists -> "Плейлисты"
                    LibraryRoute.Artists -> "Артисты"
                    LibraryRoute.Albums -> "Альбомы"
                    LibraryRoute.Songs -> "Треки"
                    is LibraryRoute.AlbumDetail -> r.album
                    is LibraryRoute.PlaylistDetail -> r.name
                    is LibraryRoute.ArtistDetail -> "" // handled by its own hero header
                },
                showBack = backStack.size > 1,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                trailing = when (route) {
                    LibraryRoute.Playlists -> {
                        {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Новый плейлист",
                                tint = PlayerColors.TextPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        showCreatePlaylist = true
                                    },
                            )
                        }
                    }
                    is LibraryRoute.PlaylistDetail -> {
                        {
                            PlaylistMenuButton(
                                onRename = { showRenamePlaylist = true },
                                onDelete = { showDeletePlaylist = true },
                            )
                        }
                    }
                    else -> null
                },
            )
        }

        when {
            !permissionGranted -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Нужен доступ к музыке на устройстве",
                            color = PlayerColors.TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlayerColors.Accent,
                                contentColor = PlayerColors.OnAccent,
                            ),
                        ) {
                            Text("Разрешить доступ")
                        }
                    }
                }
            }

            songs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Треки не найдены", color = PlayerColors.TextSecondary)
                }
            }

            else -> {
                val artistGroups = remember(songs) {
                    songs.groupBy { it.artist }
                        .map { (artist, list) -> ArtistGroup(artist, list) }
                }
                val albumGroups = remember(songs) {
                    songs.groupBy { it.album to it.artist }
                        .map { (key, list) -> AlbumGroup(key.first, key.second, list) }
                }

                when (val r = route) {
                    LibraryRoute.Home -> LibraryHomeList(
                        playlistCount = playlistsVm.playlists.size,
                        artistCount = artistGroups.size,
                        albumCount = albumGroups.size,
                        songCount = songs.size,
                        recentSongs = remember(songs) { songs.sortedByDescending { it.dateAddedMs }.take(12) },
                        onOpenPlaylists = { push(LibraryRoute.Playlists) },
                        onOpenArtists = { push(LibraryRoute.Artists) },
                        onOpenAlbums = { push(LibraryRoute.Albums) },
                        onOpenSongs = { push(LibraryRoute.Songs) },
                        onSongClick = { song, list -> onSongClick(song, list) },
                    )

                    LibraryRoute.Playlists -> PlaylistsList(
                        playlists = playlistsVm.playlists,
                        onPlaylistClick = { p -> push(LibraryRoute.PlaylistDetail(p.id, p.name)) },
                    )

                    LibraryRoute.Artists -> {
                        val filtered = remember(artistGroups, artistQuery, artistSort) {
                            artistGroups
                                .filter { it.name.contains(artistQuery, ignoreCase = true) }
                                .let { list ->
                                    when (artistSort) {
                                        ArtistSort.NAME -> list.sortedBy { it.name.lowercase() }
                                        ArtistSort.COUNT -> list.sortedByDescending { it.songs.size }
                                        ArtistSort.RECENT -> list.sortedByDescending { g -> g.songs.maxOf { it.dateAddedMs } }
                                    }
                                }
                        }
                        Column(modifier = Modifier.fillMaxSize()) {
                            ListToolbar(
                                query = artistQuery,
                                onQueryChange = { artistQuery = it },
                                placeholder = "Поиск по артистам",
                                sortOptions = ArtistSort.entries,
                                sortOptionLabel = { it.label },
                                currentSort = artistSort.label,
                                onSortSelect = { artistSort = it },
                                viewMode = artistViewMode,
                                onViewModeCycle = { settingsVm.setViewMode("artists", LibraryViewMode.valueOf(artistViewMode.next().name)) },
                            )
                            when (artistViewMode) {
                                ViewMode.LIST -> ArtistsList(
                                    groups = filtered,
                                    state = artistsListState,
                                    onArtistClick = { push(LibraryRoute.ArtistDetail(it.name)) },
                                )
                                ViewMode.GRID_2, ViewMode.GRID_3 -> ArtistsGrid(
                                    groups = filtered,
                                    columns = if (artistViewMode == ViewMode.GRID_2) 2 else 3,
                                    state = artistsGridState,
                                    onArtistClick = { push(LibraryRoute.ArtistDetail(it.name)) },
                                )
                            }
                        }
                    }

                    LibraryRoute.Albums -> {
                        val filtered = remember(albumGroups, albumQuery, albumSort) {
                            albumGroups
                                .filter {
                                    it.album.contains(albumQuery, ignoreCase = true) ||
                                        it.artist.contains(albumQuery, ignoreCase = true)
                                }
                                .let { list ->
                                    when (albumSort) {
                                        AlbumSort.NAME -> list.sortedBy { it.album.lowercase() }
                                        AlbumSort.COUNT -> list.sortedByDescending { it.songs.size }
                                        AlbumSort.ARTIST -> list.sortedBy { it.artist.lowercase() }
                                        AlbumSort.RECENT -> list.sortedByDescending { g -> g.songs.maxOf { it.dateAddedMs } }
                                    }
                                }
                        }
                        Column(modifier = Modifier.fillMaxSize()) {
                            ListToolbar(
                                query = albumQuery,
                                onQueryChange = { albumQuery = it },
                                placeholder = "Поиск по альбомам",
                                sortOptions = AlbumSort.entries,
                                sortOptionLabel = { it.label },
                                currentSort = albumSort.label,
                                onSortSelect = { albumSort = it },
                                viewMode = albumViewMode,
                                onViewModeCycle = { settingsVm.setViewMode("albums", LibraryViewMode.valueOf(albumViewMode.next().name)) },
                            )
                            when (albumViewMode) {
                                ViewMode.LIST -> AlbumsList(
                                    groups = filtered,
                                    state = albumsListState,
                                    onAlbumClick = { push(LibraryRoute.AlbumDetail(it.album, it.artist)) },
                                )
                                ViewMode.GRID_2, ViewMode.GRID_3 -> AlbumsGrid(
                                    groups = filtered,
                                    columns = if (albumViewMode == ViewMode.GRID_2) 2 else 3,
                                    state = albumsGridState,
                                    onAlbumClick = { push(LibraryRoute.AlbumDetail(it.album, it.artist)) },
                                )
                            }
                        }
                    }

                    LibraryRoute.Songs -> {
                        val filtered = remember(songs, songQuery, songSort) {
                            songs
                                .filter {
                                    it.title.contains(songQuery, ignoreCase = true) ||
                                        it.artist.contains(songQuery, ignoreCase = true)
                                }
                                .let { list ->
                                    when (songSort) {
                                        SongSort.TITLE -> list.sortedBy { it.title.lowercase() }
                                        SongSort.ARTIST -> list.sortedBy { it.artist.lowercase() }
                                        SongSort.RECENT -> list.sortedByDescending { it.dateAddedMs }
                                        SongSort.RELEASE_DATE -> list.sortedByDescending { it.year ?: -1 }
                                    }
                                }
                        }
                        Column(modifier = Modifier.fillMaxSize()) {
                            ListToolbar(
                                query = songQuery,
                                onQueryChange = { songQuery = it },
                                placeholder = "Поиск по трекам",
                                sortOptions = SongSort.entries,
                                sortOptionLabel = { it.label },
                                currentSort = songSort.label,
                                onSortSelect = { songSort = it },
                                viewMode = songViewMode,
                                onViewModeCycle = { settingsVm.setViewMode("songs", LibraryViewMode.valueOf(songViewMode.next().name)) },
                            )
                            if (filtered.isNotEmpty()) {
                                PlayShuffleRow(
                                    onPlay = { onSongClick(filtered.first(), filtered) },
                                    onShuffle = { filtered.shuffled().let { onSongClick(it.first(), it) } },
                                )
                            }
                            when (songViewMode) {
                                ViewMode.LIST -> SongList(
                                    songs = filtered,
                                    state = songsListState,
                                    onSongClick = { song -> onSongClick(song, filtered) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                                    onGoToAlbum = onGoToAlbum,
                                    onGoToArtist = onGoToArtist,
                                )
                                ViewMode.GRID_2, ViewMode.GRID_3 -> SongsGrid(
                                    songs = filtered,
                                    columns = if (songViewMode == ViewMode.GRID_2) 2 else 3,
                                    state = songsGridState,
                                    onSongClick = { song -> onSongClick(song, filtered) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                                    onGoToAlbum = onGoToAlbum,
                                    onGoToArtist = onGoToArtist,
                                )
                            }
                        }
                    }

                    is LibraryRoute.ArtistDetail -> {
                        val artistSongs = artistGroups.firstOrNull { it.name == r.artist }?.songs ?: emptyList()
                        val artistAlbums = remember(artistSongs) {
                            artistSongs.groupBy { it.album }
                                .map { (album, list) -> album to list }
                                .sortedBy { it.first.lowercase() }
                        }
                        ArtistDetailScreen(
                            artist = r.artist,
                            songs = artistSongs,
                            albums = artistAlbums,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onPlayAll = { list -> if (list.isNotEmpty()) onSongClick(list.first(), list) },
                            onShuffleAll = { list ->
                                if (list.isNotEmpty()) {
                                    val shuffled = list.shuffled()
                                    onSongClick(shuffled.first(), shuffled)
                                }
                            },
                            onSongClick = { song, list -> onSongClick(song, list) },
                            onAlbumClick = { album ->
                                push(LibraryRoute.AlbumDetail(album, r.artist))
                            },
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddAllToQueue = onAddAllToQueue,
                            onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                            onGoToAlbum = onGoToAlbum,
                        )
                    }

                    is LibraryRoute.AlbumDetail -> {
                        val albumSongs = albumGroups
                            .firstOrNull { it.album == r.album && it.artist == r.artist }
                            ?.songs ?: emptyList()
                        AlbumDetailScreen(
                            album = r.album,
                            artist = r.artist,
                            songs = albumSongs,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onPlayAll = { list -> if (list.isNotEmpty()) onSongClick(list.first(), list) },
                            onShuffleAll = { list ->
                                if (list.isNotEmpty()) {
                                    val shuffled = list.shuffled()
                                    onSongClick(shuffled.first(), shuffled)
                                }
                            },
                            onSongClick = { song, list -> onSongClick(song, list) },
                            onAddAllClick = { list -> onAddToPlaylist(list) },
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                            onGoToArtist = onGoToArtist,
                        )
                    }

                    is LibraryRoute.PlaylistDetail -> {
                        var playlistSongs by remember(r.playlistId) { mutableStateOf<List<Song>>(emptyList()) }
                        // playlistsVm.playlists changes after every add/remove, so this re-reads
                        // the playlist right after a song is taken out of it.
                        LaunchedEffect(r.playlistId, songs, playlistsVm.playlists) {
                            playlistSongs = playlistsVm.getSongsForPlaylist(r.playlistId, songs)
                        }
                        var playlistQuery by remember(r.playlistId) { mutableStateOf("") }
                        var playlistSort by remember(r.playlistId) { mutableStateOf(PlaylistSongSort.ORDER) }
                        val playlistListState = rememberLazyListState()
                        val playlistGridState = rememberLazyGridState()
                        val removeFromPlaylist: (Song) -> Unit = { song -> playlistsVm.removeSongFromPlaylist(r.playlistId, song.id) }
                        if (playlistSongs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "В плейлисте пока нет треков", color = PlayerColors.TextSecondary)
                            }
                        } else {
                            val filtered = remember(playlistSongs, playlistQuery, playlistSort) {
                                playlistSongs
                                    .filter {
                                        it.title.contains(playlistQuery, ignoreCase = true) ||
                                            it.artist.contains(playlistQuery, ignoreCase = true)
                                    }
                                    .let { list ->
                                        when (playlistSort) {
                                            PlaylistSongSort.ORDER -> list
                                            PlaylistSongSort.TITLE -> list.sortedBy { it.title.lowercase() }
                                            PlaylistSongSort.ARTIST -> list.sortedBy { it.artist.lowercase() }
                                            PlaylistSongSort.RECENT -> list.sortedByDescending { it.dateAddedMs }
                                        }
                                    }
                            }
                            Column(modifier = Modifier.fillMaxSize()) {
                                ListToolbar(
                                    query = playlistQuery,
                                    onQueryChange = { playlistQuery = it },
                                    placeholder = "Поиск в плейлисте",
                                    sortOptions = PlaylistSongSort.entries,
                                    sortOptionLabel = { it.label },
                                    currentSort = playlistSort.label,
                                    onSortSelect = { playlistSort = it },
                                    viewMode = playlistViewMode,
                                    onViewModeCycle = { settingsVm.setViewMode("playlist", LibraryViewMode.valueOf(playlistViewMode.next().name)) },
                                )
                                if (filtered.isNotEmpty()) {
                                    PlayShuffleRow(
                                        onPlay = { onSongClick(filtered.first(), filtered) },
                                        onShuffle = { filtered.shuffled().let { onSongClick(it.first(), it) } },
                                    )
                                }
                                when (playlistViewMode) {
                                    ViewMode.LIST -> SongList(
                                        songs = filtered,
                                        state = playlistListState,
                                        onSongClick = { song -> onSongClick(song, filtered) },
                                        onPlayNext = onPlayNext,
                                        onAddToQueue = onAddToQueue,
                                        onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist,
                                        onRemoveFromPlaylist = removeFromPlaylist,
                                    )
                                    ViewMode.GRID_2, ViewMode.GRID_3 -> SongsGrid(
                                        songs = filtered,
                                        columns = if (playlistViewMode == ViewMode.GRID_2) 2 else 3,
                                        state = playlistGridState,
                                        onSongClick = { song -> onSongClick(song, filtered) },
                                        onPlayNext = onPlayNext,
                                        onAddToQueue = onAddToQueue,
                                        onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist,
                                        onRemoveFromPlaylist = removeFromPlaylist,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylist) {
        PlaylistNameDialog(
            title = "Новый плейлист",
            initialName = "",
            confirmLabel = "Создать",
            onDismiss = { showCreatePlaylist = false },
            onConfirm = { name ->
                playlistsVm.createPlaylist(name)
                showCreatePlaylist = false
            },
        )
    }

    val openPlaylist = route as? LibraryRoute.PlaylistDetail
    if (showRenamePlaylist && openPlaylist != null) {
        PlaylistNameDialog(
            title = "Переименовать плейлист",
            initialName = openPlaylist.name,
            confirmLabel = "Сохранить",
            onDismiss = { showRenamePlaylist = false },
            onConfirm = { name ->
                playlistsVm.renamePlaylist(openPlaylist.playlistId, name)
                // The header title comes from the route itself.
                backStack[backStack.lastIndex] = openPlaylist.copy(name = name.trim())
                showRenamePlaylist = false
            },
        )
    }
    if (showDeletePlaylist && openPlaylist != null) {
        ConfirmDialog(
            title = "Удалить плейлист «${openPlaylist.name}»?",
            message = "Сами треки останутся в медиатеке.",
            confirmLabel = "Удалить",
            onDismiss = { showDeletePlaylist = false },
            onConfirm = {
                playlistsVm.deletePlaylist(openPlaylist.playlistId)
                showDeletePlaylist = false
                backStack.removeAt(backStack.lastIndex)
            },
        )
    }
}

@Composable
private fun PlaylistMenuButton(onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Действия с плейлистом",
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Переименовать") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = { expanded = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text("Удалить плейлист") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PlayerColors.SurfaceDim)
                .padding(20.dp),
        ) {
            Text(text = title, color = PlayerColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(text = message, color = PlayerColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Отмена",
                    color = PlayerColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
                        .padding(end = 20.dp),
                )
                Text(
                    text = confirmLabel,
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onConfirm() },
                )
            }
        }
    }
}

@Composable
internal fun LibraryHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 20.dp, 20.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = PlayerColors.TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }
                    .padding(end = 12.dp),
            )
        }
        Text(
            text = title,
            color = PlayerColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun LibraryHomeList(
    playlistCount: Int,
    artistCount: Int,
    albumCount: Int,
    songCount: Int,
    recentSongs: List<Song>,
    onOpenPlaylists: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenSongs: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            LibraryRow("Плейлисты", playlistCount, Icons.Filled.PlaylistPlay, onOpenPlaylists)
            LibraryRow("Артисты", artistCount, Icons.Filled.Person, onOpenArtists)
            LibraryRow("Альбомы", albumCount, Icons.Filled.Album, onOpenAlbums)
            LibraryRow("Треки", songCount, Icons.Filled.MusicNote, onOpenSongs)
        }

        if (recentSongs.isNotEmpty()) {
            Text(
                text = "Недавно добавленные",
                color = PlayerColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                recentSongs.chunked(2).forEach { rowSongs ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowSongs.forEach { song ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        onSongClick(song, recentSongs)
                                    },
                            ) {
                                AlbumArt(
                                    uri = song.uri,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp)),
                                )
                                Text(
                                    text = song.title,
                                    color = PlayerColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                Text(
                                    text = song.artist,
                                    color = PlayerColors.TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (rowSongs.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(label: String, count: Int, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PlayerColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            color = PlayerColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        Text(text = "$count", color = PlayerColors.TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(end = 2.dp))
    }
}

@Composable
internal fun <T> ListToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    sortOptions: List<T>,
    sortOptionLabel: (T) -> String,
    currentSort: String,
    onSortSelect: (T) -> Unit,
    viewMode: ViewMode,
    onViewModeCycle: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(PlayerColors.Surface)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(16.dp))
            Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                if (query.isEmpty()) {
                    Text(text = placeholder, color = PlayerColors.TextTertiary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = PlayerColors.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(PlayerColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Icon(
            imageVector = viewMode.icon,
            contentDescription = "Вид: ${viewMode.description}",
            tint = PlayerColors.TextSecondary,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(20.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onViewModeCycle() },
        )
        // No sort options (a list with a fixed order) = no sort button at all.
        if (sortOptions.isNotEmpty()) Box {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = "Сортировка: $currentSort",
                tint = PlayerColors.TextSecondary,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(20.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuExpanded = true },
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                sortOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(sortOptionLabel(option)) },
                        onClick = {
                            onSortSelect(option)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsList(groups: List<ArtistGroup>, state: LazyListState, onArtistClick: (ArtistGroup) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = state) {
        items(groups, key = { it.name }) { group ->
            val coverUri = remember(group) { group.songs.minByOrNull { it.album.lowercase() }?.uri }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onArtistClick(group) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(
                    uri = coverUri,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(26.dp)),
                )
                Text(
                    text = group.name,
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(
                    text = "${group.songs.size}",
                    color = PlayerColors.TextTertiary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ArtistsGrid(groups: List<ArtistGroup>, columns: Int, state: LazyGridState, onArtistClick: (ArtistGroup) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        gridItems(groups, key = { it.name }) { group ->
            val coverUri = remember(group) { group.songs.minByOrNull { it.album.lowercase() }?.uri }
            Column(
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onArtistClick(group) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AlbumArt(
                    uri = coverUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(percent = 50)),
                )
                Text(
                    text = group.name,
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "${group.songs.size}",
                    color = PlayerColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun AlbumsList(groups: List<AlbumGroup>, state: LazyListState, onAlbumClick: (AlbumGroup) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = state) {
        items(groups, key = { "${it.album}|${it.artist}" }) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAlbumClick(group) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(
                    uri = group.songs.firstOrNull()?.uri,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = group.album.ifBlank { "Без альбома" },
                        color = PlayerColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = group.artist,
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${group.songs.size}",
                    color = PlayerColors.TextTertiary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun AlbumsGrid(groups: List<AlbumGroup>, columns: Int, state: LazyGridState, onAlbumClick: (AlbumGroup) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        gridItems(groups) { group ->
            Column(
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAlbumClick(group) },
            ) {
                AlbumArt(
                    uri = group.songs.firstOrNull()?.uri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Text(
                    text = group.album.ifBlank { "Без альбома" },
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = group.artist,
                    color = PlayerColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun SongsGrid(
    songs: List<Song>,
    columns: Int,
    state: LazyGridState,
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        gridItems(songs, key = { it.id }) { song ->
            var menuExpanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.songLongPressTrigger(
                    onClick = { onSongClick(song) },
                    onLongPress = { menuExpanded = true },
                ),
            ) {
                Box {
                    AlbumArt(
                        uri = song.uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    com.artemiy.player.ui.components.SongActionsMenuPopup(
                        song = song,
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                    )
                }
                Text(
                    text = song.title,
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = song.artist,
                    color = PlayerColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun SongList(
    songs: List<Song>,
    state: LazyListState = rememberLazyListState(),
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = state) {
        items(songs, key = { it.id }) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSongClick(song) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = PlayerColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                com.artemiy.player.ui.components.SongActionsMenu(
                    song = song,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onGoToAlbum = onGoToAlbum,
                    onGoToArtist = onGoToArtist,
                    onRemoveFromPlaylist = onRemoveFromPlaylist,
                    iconSize = 20.dp,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
internal fun PlayShuffleRow(onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlayShuffleButton(icon = Icons.Filled.PlayArrow, label = "Слушать", onClick = onPlay, modifier = Modifier.weight(1f))
        PlayShuffleButton(icon = Icons.Filled.Shuffle, label = "Перемешать", onClick = onShuffle, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PlayShuffleButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PlayerColors.Surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(17.dp))
        Text(text = label, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun PlaylistsList(playlists: List<PlaylistWithCount>, onPlaylistClick: (PlaylistWithCount) -> Unit) {
    if (playlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Плейлистов пока нет — создай первый значком «+» сверху",
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(playlists, key = { it.id }) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPlaylistClick(playlist) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlayerColors.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.PlaylistPlay, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = playlist.name,
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(text = "${playlist.songCount}", color = PlayerColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PlayerColors.SurfaceDim)
                .padding(20.dp),
        ) {
            Text(
                text = title,
                color = PlayerColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PlayerColors.Surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (name.isEmpty()) {
                    Text(text = "Название", color = PlayerColors.TextTertiary, fontSize = 14.sp)
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = PlayerColors.TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(PlayerColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Отмена",
                    color = PlayerColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
                        .padding(end = 20.dp),
                )
                Text(
                    text = confirmLabel,
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (name.isNotBlank()) onConfirm(name)
                        },
                )
            }
        }
    }
}
