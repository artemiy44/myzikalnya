package com.artemiy.player.ui.library

import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.inAppFont
import com.artemiy.player.ui.components.AppDialog
import com.artemiy.player.ui.components.AppDropdownMenu
import com.artemiy.player.ui.components.AppMenuItem
import com.artemiy.player.ui.components.DialogButtons
import com.artemiy.player.ui.components.DialogMessage
import com.artemiy.player.ui.components.DialogTextField
import com.artemiy.player.ui.components.DialogTitle
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
import com.artemiy.player.data.ArtistNames
import com.artemiy.player.data.LibraryViewMode
import com.artemiy.player.data.artists
import com.artemiy.player.data.PlaylistWithCount
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.AnimatedBackStack
import com.artemiy.player.ui.components.BlurredCollageArt
import com.artemiy.player.ui.components.COLLAGE_HERO_HEIGHT
import com.artemiy.player.ui.components.CircleIconButton
import com.artemiy.player.ui.components.HeroOverArt
import com.artemiy.player.ui.components.HeroTextShadow
import com.artemiy.player.ui.components.PlayPillButton
import com.artemiy.player.ui.components.rememberArrowTint
import com.artemiy.player.ui.components.rememberBlurredCollage
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
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
internal enum class ViewMode(@androidx.annotation.StringRes val descriptionRes: Int) {
    LIST(R.string.view_list),
    GRID_2(R.string.view_grid_2),
    GRID_3(R.string.view_grid_3);

    /** Read at draw time — the icon depends on the chosen icon set. */
    val icon: ImageVector
        @Composable get() = when (this) {
            LIST -> AppIcons.ViewList
            GRID_2 -> AppIcons.ViewGrid
            GRID_3 -> AppIcons.ViewGridDense
        }

    fun next(): ViewMode = entries[(ordinal + 1) % entries.size]
}

private enum class ArtistSort(val labelRes: Int) { COUNT(R.string.sort_by_count), RECENT(R.string.sort_recent), NAME(R.string.sort_by_name) }
private enum class AlbumSort(val labelRes: Int) { RECENT(R.string.sort_recent), NAME(R.string.sort_by_name), COUNT(R.string.sort_by_count), ARTIST(R.string.sort_by_artist) }
private enum class PlaylistSongSort(val labelRes: Int) { ORDER(R.string.sort_added_order), TITLE(R.string.sort_by_title), ARTIST(R.string.sort_by_artist), RECENT(R.string.sort_recent_library) }
private enum class SongSort(val labelRes: Int) { RECENT(R.string.sort_recent), RELEASE_DATE(R.string.sort_release_date), TITLE(R.string.sort_by_title), ARTIST(R.string.sort_by_artist) }

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

    fun push(r: LibraryRoute) {
        backStack.add(r)
    }

    // Each page of the Library stack, animated: pushing slides in, going back (arrow or the
    // predictive back gesture) slides away with the page underneath showing.
    AnimatedBackStack(stack = backStack, onBack = { backStack.removeAt(backStack.lastIndex) }) { route ->
        // Artist/album/playlist pages run their header art up under the status bar themselves.
        val edgeToEdge = route is LibraryRoute.ArtistDetail || route is LibraryRoute.AlbumDetail || route is LibraryRoute.PlaylistDetail
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.Background)
                .then(if (edgeToEdge) Modifier else Modifier.statusBarsPadding()),
        ) {
            if (!edgeToEdge) {
                LibraryHeader(
                    title = when (val r = route) {
                        LibraryRoute.Home -> stringResource(R.string.tab_library)
                        LibraryRoute.Playlists -> stringResource(R.string.playlists)
                        LibraryRoute.Artists -> stringResource(R.string.artists)
                        LibraryRoute.Albums -> stringResource(R.string.albums)
                        LibraryRoute.Songs -> stringResource(R.string.tracks)
                        is LibraryRoute.AlbumDetail -> r.album
                        is LibraryRoute.PlaylistDetail -> r.name
                        is LibraryRoute.ArtistDetail -> "" // handled by its own hero header
                    },
                    showBack = route != LibraryRoute.Home,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    trailing = when (route) {
                        LibraryRoute.Playlists -> {
                            {
                                Icon(
                                    imageVector = AppIcons.Add,
                                    contentDescription = stringResource(R.string.new_playlist),
                                    tint = PlayerColors.TextPrimary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            showCreatePlaylist = true
                                        },
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
                                text = stringResource(R.string.music_access_needed),
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
                                Text(stringResource(R.string.allow_access))
                            }
                        }
                    }
                }

                songs.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.no_tracks_found), color = PlayerColors.TextSecondary)
                    }
                }

                else -> {
                    // A song credited to several artists ("Ado & Eve") is listed under each of them.
                    val keptArtists = ArtistNames.userKept
                    val artistNamesVersion = ArtistNames.version
                    val artistGroups = remember(songs, keptArtists, artistNamesVersion) {
                        songs.flatMap { song -> song.artists().map { it to song } }
                            .groupBy({ it.first }, { it.second })
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
                                    placeholder = stringResource(R.string.search_artists),
                                    sortOptions = ArtistSort.entries,
                                    sortOptionLabel = { stringResource(it.labelRes) },
                                    currentSort = stringResource(artistSort.labelRes),
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
                                    placeholder = stringResource(R.string.search_albums),
                                    sortOptions = AlbumSort.entries,
                                    sortOptionLabel = { stringResource(it.labelRes) },
                                    currentSort = stringResource(albumSort.labelRes),
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
                                    placeholder = stringResource(R.string.search_tracks),
                                    sortOptions = SongSort.entries,
                                    sortOptionLabel = { stringResource(it.labelRes) },
                                    currentSort = stringResource(songSort.labelRes),
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
                                    // Albums are keyed by their songs' full artist line ("Ado & Eve"),
                                    // not by this one artist.
                                    val albumArtist = artistSongs.firstOrNull { it.album == album }?.artist ?: r.artist
                                    push(LibraryRoute.AlbumDetail(album, albumArtist))
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
                            val collage = rememberBlurredCollage(playlistSongs)
                            // Header, then search/sort — all scrolling away together with the songs.
                            val header: @Composable () -> Unit = {
                                Column {
                                    PlaylistHero(
                                        name = r.name,
                                        songCount = playlistSongs.size,
                                        collage = collage,
                                        onBack = { backStack.removeAt(backStack.lastIndex) },
                                        onPlay = { if (filtered.isNotEmpty()) onSongClick(filtered.first(), filtered) },
                                        onShuffle = { if (filtered.isNotEmpty()) filtered.shuffled().let { onSongClick(it.first(), it) } },
                                        onRename = { showRenamePlaylist = true },
                                        onDelete = { showDeletePlaylist = true },
                                    )
                                    if (playlistSongs.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.playlist_empty),
                                            color = PlayerColors.TextSecondary,
                                            fontSize = 14.sp,
                                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                            textAlign = TextAlign.Center,
                                        )
                                    } else {
                                        ListToolbar(
                                            query = playlistQuery,
                                            onQueryChange = { playlistQuery = it },
                                            placeholder = stringResource(R.string.search_playlist),
                                            sortOptions = PlaylistSongSort.entries,
                                            sortOptionLabel = { stringResource(it.labelRes) },
                                            currentSort = stringResource(playlistSort.labelRes),
                                            onSortSelect = { playlistSort = it },
                                            viewMode = playlistViewMode,
                                            onViewModeCycle = { settingsVm.setViewMode("playlist", LibraryViewMode.valueOf(playlistViewMode.next().name)) },
                                        )
                                    }
                                }
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
                                    header = header,
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
                                    header = header,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylist) {
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            initialName = "",
            confirmLabel = stringResource(R.string.create),
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
            title = stringResource(R.string.rename_playlist),
            initialName = openPlaylist.name,
            confirmLabel = stringResource(R.string.save),
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
            title = stringResource(R.string.delete_playlist_q, openPlaylist.name),
            message = stringResource(R.string.delete_playlist_msg),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { showDeletePlaylist = false },
            onConfirm = {
                playlistsVm.deletePlaylist(openPlaylist.playlistId)
                showDeletePlaylist = false
                backStack.removeAt(backStack.lastIndex)
            },
        )
    }
}

/** A playlist's header — same build as an artist's: its songs' covers as a blurred collage, name
 * and song count over it, then shuffle / play / playlist actions. */
@Composable
private fun PlaylistHero(
    name: String,
    songCount: Int,
    collage: android.graphics.Bitmap?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    HeroOverArt(
        topTint = rememberArrowTint(listOf(collage)),
        onBack = onBack,
        height = COLLAGE_HERO_HEIGHT,
        art = { BlurredCollageArt(collage) },
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = HeroTextShadow).inAppFont(),
        )
        Text(
            text = pluralStringResource(R.plurals.songs_count, songCount, songCount),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            style = TextStyle(shadow = HeroTextShadow).inAppFont(),
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(icon = AppIcons.Shuffle, description = stringResource(R.string.shuffle), onClick = onShuffle)
            PlayPillButton(onClick = onPlay, modifier = Modifier.padding(horizontal = 14.dp))
            Box {
                CircleIconButton(icon = AppIcons.More, description = stringResource(R.string.playlist_actions)) { menuExpanded = true }
                AppDropdownMenu(expanded = menuExpanded, onDismiss = { menuExpanded = false }) {
                    AppMenuItem(text = stringResource(R.string.rename), icon = AppIcons.Edit, onClick = { menuExpanded = false; onRename() })
                    AppMenuItem(text = stringResource(R.string.delete_playlist), icon = AppIcons.Delete, destructive = true, onClick = { menuExpanded = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppDialog(onDismiss = onDismiss) {
        DialogTitle(title)
        DialogMessage(message)
        DialogButtons(dismissLabel = stringResource(R.string.cancel), onDismiss = onDismiss, confirmLabel = confirmLabel, onConfirm = onConfirm, destructive = true)
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
                imageVector = AppIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
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
            LibraryRow(stringResource(R.string.playlists), playlistCount, AppIcons.Playlist, onOpenPlaylists)
            LibraryRow(stringResource(R.string.artists), artistCount, AppIcons.Artist, onOpenArtists)
            LibraryRow(stringResource(R.string.albums), albumCount, AppIcons.Album, onOpenAlbums)
            LibraryRow(stringResource(R.string.tracks), songCount, AppIcons.Songs, onOpenSongs)
        }

        if (recentSongs.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recently_added),
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
    sortOptionLabel: @Composable (T) -> String,
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
            Icon(imageVector = AppIcons.Search, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(16.dp))
            Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                if (query.isEmpty()) {
                    Text(text = placeholder, color = PlayerColors.TextTertiary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = PlayerColors.TextPrimary, fontSize = 13.sp).inAppFont(),
                    cursorBrush = SolidColor(PlayerColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Icon(
            imageVector = viewMode.icon,
            contentDescription = stringResource(R.string.view_mode, stringResource(viewMode.descriptionRes)),
            tint = PlayerColors.TextSecondary,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(20.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onViewModeCycle() },
        )
        // No sort options (a list with a fixed order) = no sort button at all.
        if (sortOptions.isNotEmpty()) Box {
            Icon(
                imageVector = AppIcons.Sort,
                contentDescription = stringResource(R.string.sort_mode, currentSort),
                tint = PlayerColors.TextSecondary,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(20.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuExpanded = true },
            )
            AppDropdownMenu(expanded = menuExpanded, onDismiss = { menuExpanded = false }) {
                sortOptions.forEach { option ->
                    AppMenuItem(
                        text = sortOptionLabel(option),
                        selected = sortOptionLabel(option) == currentSort,
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
                        text = group.album.ifBlank { stringResource(R.string.no_album) },
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
                    text = group.album.ifBlank { stringResource(R.string.no_album) },
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
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = if (header != null) 0.dp else 4.dp, bottom = 4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (header != null) {
            // Full width, reaching past the grid's side padding — the header draws edge to edge.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.layout { measurable, constraints ->
                    val extra = 40.dp.roundToPx()
                    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
                }) { header() }
            }
        }
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
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = state) {
        if (header != null) item(key = "header") { header() }
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
        PlayShuffleButton(icon = AppIcons.Play, label = stringResource(R.string.action_listen), onClick = onPlay, modifier = Modifier.weight(1f))
        PlayShuffleButton(icon = AppIcons.Shuffle, label = stringResource(R.string.shuffle), onClick = onShuffle, modifier = Modifier.weight(1f))
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
                text = stringResource(R.string.no_playlists),
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
                    Icon(imageVector = AppIcons.Playlist, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
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
    AppDialog(onDismiss = onDismiss) {
        DialogTitle(title)
        DialogTextField(value = name, onValueChange = { name = it }, placeholder = stringResource(R.string.name), modifier = Modifier.padding(top = 16.dp))
        DialogButtons(
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = onDismiss,
            confirmLabel = confirmLabel,
            onConfirm = { if (name.isNotBlank()) onConfirm(name) },
            confirmEnabled = name.isNotBlank(),
        )
    }
}
