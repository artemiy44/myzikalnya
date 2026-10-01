package com.artemiy.player.ui.library

import com.artemiy.player.ui.theme.LocalBarsInset
import com.artemiy.player.ui.theme.barsInset
import com.artemiy.player.ui.components.pressScale
import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import com.artemiy.player.ui.components.HeroBar
import com.artemiy.player.ui.components.monthYear
import com.artemiy.player.ui.components.indexLetter
import com.artemiy.player.ui.components.rememberScrollTarget
import com.artemiy.player.ui.components.FastScroller
import com.artemiy.player.ui.components.fadesWithHeader
import com.artemiy.player.ui.home.drawGenreMotif
import com.artemiy.player.ui.library.ToneBackdrop
import com.artemiy.player.ui.components.inAlbumOrder
import com.artemiy.player.ui.components.groupedCard
import com.artemiy.player.ui.components.staggeredEntrance
import com.artemiy.player.ui.components.rememberEntrance
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.itemsIndexed
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
    val yearsGridState = rememberLazyGridState()
    val genresGridState = rememberLazyGridState()
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

    // The folders of the music, found once per library.
    val folderTree = remember(songs) { buildFolderTree(songs) }

    // The app's own language, for the fast scroller's "September 2026".
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    fun push(r: LibraryRoute) {
        backStack.add(r)
    }

    // Each page of the Library stack, animated: pushing slides in, going back (arrow or the
    // predictive back gesture) slides away with the page underneath showing.
    AnimatedBackStack(stack = backStack, onBack = { backStack.removeAt(backStack.lastIndex) }) { route ->
        // Artist/album/playlist pages run their header art up under the status bar themselves.
        val edgeToEdge = route is LibraryRoute.ArtistDetail || route is LibraryRoute.AlbumDetail || route is LibraryRoute.PlaylistDetail ||
            route is LibraryRoute.YearDetail || route is LibraryRoute.GenreDetail
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.Background)
                .then(if (edgeToEdge) Modifier else Modifier.statusBarsPadding()),
        ) {
            // The big title folds into a slim bar as the page scrolls (see CollapsingHeader).
            val pageTitle = when (val r = route) {
                LibraryRoute.Home -> stringResource(R.string.tab_library)
                LibraryRoute.Playlists -> stringResource(R.string.playlists)
                LibraryRoute.Artists -> stringResource(R.string.artists)
                LibraryRoute.Albums -> stringResource(R.string.albums)
                LibraryRoute.Songs -> stringResource(R.string.tracks)
                LibraryRoute.Years -> stringResource(R.string.years)
                LibraryRoute.Genres -> stringResource(R.string.genres)
                LibraryRoute.Folders -> startFolder(folderTree).name.ifBlank { stringResource(R.string.folders) }
                is LibraryRoute.FolderDetail -> r.path.lastOrNull().orEmpty()
                is LibraryRoute.YearDetail, is LibraryRoute.GenreDetail -> "" // their own hero header
                is LibraryRoute.AlbumDetail -> r.album
                is LibraryRoute.PlaylistDetail -> r.name
                is LibraryRoute.ArtistDetail -> "" // handled by its own hero header
            }
            val pageTrailing: (@Composable () -> Unit)? = when (route) {
                LibraryRoute.Playlists -> {
                    {
                        Icon(
                            imageVector = AppIcons.Add,
                            contentDescription = stringResource(R.string.new_playlist),
                            tint = PlayerColors.TextPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) {
                                    showCreatePlaylist = true
                                },
                        )
                    }
                }
                else -> null
            }
            val goBack = { backStack.removeAt(backStack.lastIndex); Unit }
            val pageContent: @Composable ColumnScope.() -> Unit = {

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
                    // One album however its tags spell it: "twenty one pilots" and "twenty One Pilots"
                    // on songs of the same "Vessel" are one album, shown in the usual spelling.
                    val albumGroups = remember(songs, artistNamesVersion) {
                        songs.groupBy { albumKey(it.album, it.artist) }
                            .map { (_, list) -> AlbumGroup(mostCommon(list.map { it.album }), mostCommon(list.map { it.artist }), list) }
                    }
                    // Newest year first; the songs without one lead the list.
                    val yearGroups = remember(songs) {
                        songs.groupBy { it.year }
                            .map { (year, list) -> TagGroup(year?.toString(), year?.toString(), list) }
                            .sortedWith(compareBy<TagGroup>({ it.key != null }, { -(it.key?.toIntOrNull() ?: 0) }))
                    }
                    // One tile per genre however its tags are spelled or in what language (GenreNames);
                    // a song with several genres is in each. Biggest first; songs without one lead.
                    val genreGroups = remember(songs) {
                        val tagged = songs.flatMap { song -> com.artemiy.player.data.GenreNames.entriesOf(song).map { (key, raw) -> Triple(key, raw, song) } }
                        val byGenre = tagged.groupBy { it.first }.map { (key, list) ->
                            TagGroup(key, com.artemiy.player.data.GenreNames.displayName(key, list.map { it.second }), list.map { it.third }.distinct())
                        }
                        val untagged = songs.filter { com.artemiy.player.data.GenreNames.entriesOf(it).isEmpty() }
                        (listOfNotNull(untagged.takeIf { it.isNotEmpty() }?.let { TagGroup(null, null, it) }) + byGenre.sortedByDescending { it.songs.size })
                    }

                    // Long-press menus: a whole album at once, or one song of a card.
                    val albumMenu: @Composable (List<Song>, Boolean, () -> Unit) -> Unit = { list, expanded, dismiss ->
                        com.artemiy.player.ui.components.AlbumActionsMenuPopup(
                            songs = remember(list) { list.inAlbumOrder() },
                            expanded = expanded,
                            onDismiss = dismiss,
                            onPlay = { ordered -> if (ordered.isNotEmpty()) onSongClick(ordered.first(), ordered) },
                            onPlayNext = onPlayNext,
                            onAddAllToQueue = onAddAllToQueue,
                            onAddToPlaylist = onAddToPlaylist,
                            onGoToArtist = if (route is LibraryRoute.ArtistDetail) null else ({ list.firstOrNull()?.let(onGoToArtist) }),
                        )
                    }
                    val songMenu: @Composable (Song, Boolean, () -> Unit) -> Unit = { song, expanded, dismiss ->
                        com.artemiy.player.ui.components.SongActionsMenuPopup(
                            song = song,
                            expanded = expanded,
                            onDismiss = dismiss,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = { onAddToPlaylist(listOf(it)) },
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist,
                        )
                    }
                    // A year's or a genre's page: the artist page's layout, with the tag as its name.
                    @Composable
                    fun TagDetail(group: TagGroup?, title: String, bigMark: String?, pictures: List<String>? = null) {
                        val tagSongs = group?.songs ?: emptyList()
                        val tagAlbums = remember(tagSongs) {
                            tagSongs.groupBy { albumKey(it.album, it.artist) }
                                .map { (_, list) -> mostCommon(list.map { it.album }) to list }
                                .sortedBy { it.first.lowercase() }
                        }
                        ArtistDetailScreen(
                            artist = "tag:" + (group?.key ?: title),
                            title = title,
                            bigMark = bigMark,
                            songSubtitle = { it.artist },
                            queueMessageRes = R.string.add_all_to_queue_msg,
                            genrePictures = pictures,
                            songs = tagSongs,
                            albums = tagAlbums,
                            albumMenu = albumMenu,
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
                                tagSongs.firstOrNull { it.album == album }?.let { push(LibraryRoute.AlbumDetail(it.album, it.artist)) }
                            },
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddAllToQueue = onAddAllToQueue,
                            onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                            onGoToAlbum = onGoToAlbum,
                        )
                    }

                    when (val r = route) {
                        LibraryRoute.Home -> LibraryHomeList(
                            tabs = settingsVm.libraryTabs.filter { it.shown }.map { it.tab },
                            counts = mapOf(
                                LibraryTab.PLAYLISTS to playlistsVm.playlists.size,
                                LibraryTab.ARTISTS to artistGroups.size,
                                LibraryTab.ALBUMS to albumGroups.size,
                                LibraryTab.TRACKS to songs.size,
                                LibraryTab.YEARS to yearGroups.count { it.key != null },
                                LibraryTab.GENRES to genreGroups.count { it.key != null },
                                LibraryTab.FOLDERS to folderTree.folderCount(),
                            ),
                            onOpen = { tab ->
                                push(
                                    when (tab) {
                                        LibraryTab.PLAYLISTS -> LibraryRoute.Playlists
                                        LibraryTab.ARTISTS -> LibraryRoute.Artists
                                        LibraryTab.ALBUMS -> LibraryRoute.Albums
                                        LibraryTab.TRACKS -> LibraryRoute.Songs
                                        LibraryTab.YEARS -> LibraryRoute.Years
                                        LibraryTab.GENRES -> LibraryRoute.Genres
                                        LibraryTab.FOLDERS -> LibraryRoute.Folders
                                    },
                                )
                            },
                            recentSongs = remember(songs) { songs.sortedByDescending { it.dateAddedMs }.take(12) },
                            onSongClick = { song, list -> onSongClick(song, list) },
                            songMenu = songMenu,
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
                                Box(modifier = Modifier.weight(1f)) {
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
                                    FastScroller(
                                        target = if (artistViewMode == ViewMode.LIST) rememberScrollTarget(artistsListState) else rememberScrollTarget(artistsGridState),
                                        label = { i ->
                                        filtered.getOrNull(i)?.let { g ->
                                            when (artistSort) {
                                                ArtistSort.NAME -> indexLetter(g.name)
                                                ArtistSort.COUNT -> g.songs.size.toString()
                                                ArtistSort.RECENT -> monthYear(g.songs.maxOf { it.dateAddedMs }, locale, dateSpan(filtered.map { a -> a.songs.maxOf { it.dateAddedMs } }))
                                            }
                                        }
                                    },
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
                                Box(modifier = Modifier.weight(1f)) {
                                when (albumViewMode) {
                                    ViewMode.LIST -> AlbumsList(
                                        groups = filtered,
                                        state = albumsListState,
                                        menu = albumMenu,
                                        onAlbumClick = { push(LibraryRoute.AlbumDetail(it.album, it.artist)) },
                                    )
                                    ViewMode.GRID_2, ViewMode.GRID_3 -> AlbumsGrid(
                                        groups = filtered,
                                        columns = if (albumViewMode == ViewMode.GRID_2) 2 else 3,
                                        state = albumsGridState,
                                        menu = albumMenu,
                                        onAlbumClick = { push(LibraryRoute.AlbumDetail(it.album, it.artist)) },
                                    )
                                }
                                    FastScroller(
                                        target = if (albumViewMode == ViewMode.LIST) rememberScrollTarget(albumsListState) else rememberScrollTarget(albumsGridState),
                                        label = { i ->
                                        filtered.getOrNull(i)?.let { g ->
                                            when (albumSort) {
                                                AlbumSort.NAME -> indexLetter(g.album)
                                                AlbumSort.COUNT -> g.songs.size.toString()
                                                AlbumSort.ARTIST -> indexLetter(g.artist)
                                                AlbumSort.RECENT -> monthYear(g.songs.maxOf { it.dateAddedMs }, locale, dateSpan(filtered.map { a -> a.songs.maxOf { it.dateAddedMs } }))
                                            }
                                        }
                                    },
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
                                    // Leaves with the folding header like the search field above it —
                                    // otherwise its lower edge peeked out from under the bar.
                                    Box(modifier = Modifier.fadesWithHeader()) {
                                    PlayShuffleRow(
                                        onPlay = { onSongClick(filtered.first(), filtered) },
                                        onShuffle = { filtered.shuffled().let { onSongClick(it.first(), it) } },
                                    )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
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
                                    FastScroller(
                                        target = if (songViewMode == ViewMode.LIST) rememberScrollTarget(songsListState) else rememberScrollTarget(songsGridState),
                                        label = { i ->
                                        filtered.getOrNull(i)?.let { song ->
                                            when (songSort) {
                                                SongSort.RECENT -> monthYear(song.dateAddedMs, locale, dateSpan(filtered.map { it.dateAddedMs }))
                                                SongSort.RELEASE_DATE -> song.year?.toString() ?: "?"
                                                SongSort.TITLE -> indexLetter(song.title)
                                                SongSort.ARTIST -> indexLetter(song.artist)
                                            }
                                        }
                                    },
                                    )
                                }
                            }
                        }

                        LibraryRoute.Folders, is LibraryRoute.FolderDetail -> {
                            val node = if (r is LibraryRoute.FolderDetail) folderTree.find(r.path) else startFolder(folderTree)
                            if (node != null) {
                                FolderPage(
                                    node = node,
                                    state = rememberLazyListState(),
                                    onOpenFolder = { push(LibraryRoute.FolderDetail(it.path)) },
                                    onPlayAll = { list -> if (list.isNotEmpty()) onSongClick(list.first(), list) },
                                    onSongClick = { song, list -> onSongClick(song, list) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onAddToPlaylist = { song -> onAddToPlaylist(listOf(song)) },
                                    onGoToAlbum = onGoToAlbum,
                                    onGoToArtist = onGoToArtist,
                                )
                            }
                        }

                        LibraryRoute.Years -> TagGrid(
                            groups = yearGroups,
                            state = yearsGridState,
                            onOpen = { push(LibraryRoute.YearDetail(it.key?.toIntOrNull())) },
                            name = { it.name ?: stringResource(R.string.no_year) },
                        ) { group -> TileMark(if (group.key == null) "?" else shortYear(group.key)) }

                        LibraryRoute.Genres -> TagGrid(
                            groups = genreGroups,
                            state = genresGridState,
                            onOpen = { push(LibraryRoute.GenreDetail(it.key)) },
                            name = { it.name ?: stringResource(R.string.no_genre) },
                        ) { group ->
                            if (group.name == null) {
                                TileMark("?")
                            } else {
                                val seed = remember(group.name) { group.name.lowercase().hashCode() }
                                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                    drawGenreMotif(group.name, seed)
                                }
                            }
                        }

                        is LibraryRoute.YearDetail -> {
                            val group = yearGroups.firstOrNull { it.key == r.year?.toString() }
                            TagDetail(
                                group = group,
                                title = r.year?.toString() ?: stringResource(R.string.no_year),
                                bigMark = r.year?.let { shortYear(it.toString()) } ?: "?",
                            )
                        }

                        is LibraryRoute.GenreDetail -> {
                            val group = genreGroups.firstOrNull { it.key == r.key }
                            TagDetail(
                                group = group,
                                title = group?.name ?: stringResource(R.string.no_genre),
                                bigMark = if (r.key == null) "?" else null,
                                // The genre's own picture (not whatever its songs' first tags say); songs
                                // without a genre have none — just the "?".
                                pictures = if (r.key == null) emptyList() else listOfNotNull(group?.name),
                            )
                        }

                        is LibraryRoute.ArtistDetail -> {
                            val artistSongs = artistGroups.firstOrNull { it.name == r.artist }?.songs ?: emptyList()
                            val artistAlbums = remember(artistSongs) {
                                artistSongs.groupBy { ArtistNames.key(it.album) }
                                    .map { (_, list) -> mostCommon(list.map { it.album }) to list }
                                    .sortedBy { it.first.lowercase() }
                            }
                            ArtistDetailScreen(
                                artist = r.artist,
                                songs = artistSongs,
                                albums = artistAlbums,
                                albumMenu = albumMenu,
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
                                    val albumArtist = artistSongs.firstOrNull { ArtistNames.key(it.album) == ArtistNames.key(album) }?.artist ?: r.artist
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
                                .firstOrNull { albumKey(it.album, it.artist) == albumKey(r.album, r.artist) }
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
                            val barShown = if (playlistViewMode == ViewMode.LIST) {
                                com.artemiy.player.ui.components.rememberHeroBarShown(playlistListState)
                            } else {
                                com.artemiy.player.ui.components.rememberHeroBarShown(playlistGridState)
                            }
                            // Header, then search/sort — all scrolling away together with the songs.
                            val header: @Composable () -> Unit = {
                                Column {
                                    PlaylistHero(
                                        barShown = barShown,
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
                            Box(modifier = Modifier.fillMaxSize()) {
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
                            HeroBar(shown = barShown, title = r.name, onBack = { backStack.removeAt(backStack.lastIndex) })
                            }
                        }
                    }
                }
            }
                    }
            if (edgeToEdge) {
                pageContent()
            } else {
                com.artemiy.player.ui.components.CollapsingHeader(
                    title = pageTitle,
                    bigHeader = { LibraryHeader(title = pageTitle, showBack = route != LibraryRoute.Home, onBack = goBack, trailing = pageTrailing) },
                    onBack = if (route != LibraryRoute.Home) goBack else null,
                    trailing = pageTrailing,
                    content = pageContent,
                )
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
