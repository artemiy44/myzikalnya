package com.artemiy.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.Song
import com.artemiy.player.data.querySongs
import com.artemiy.player.data.songsForMood
import com.artemiy.player.playback.PlaybackViewModel
import com.artemiy.player.ui.components.AppTab
import com.artemiy.player.ui.components.MiniPlayer
import com.artemiy.player.ui.components.PlayerBottomBar
import com.artemiy.player.ui.home.HomeScreen
import com.artemiy.player.ui.home.HomeViewModel
import com.artemiy.player.ui.library.AddToPlaylistDialog
import com.artemiy.player.ui.library.LibraryRoute
import com.artemiy.player.ui.library.LibraryScreen
import com.artemiy.player.ui.library.PlaylistsViewModel
import com.artemiy.player.ui.nowplaying.NowPlayingScreen
import com.artemiy.player.ui.search.LyricsSearchViewModel
import com.artemiy.player.ui.search.SearchScreen
import com.artemiy.player.ui.settings.SettingsScreen
import com.artemiy.player.ui.settings.SettingsViewModel
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.components.LocalStatusBarIconsOverride
import com.artemiy.player.ui.theme.LocalPlayerPalette
import androidx.compose.runtime.CompositionLocalProvider
import com.artemiy.player.ui.theme.PlayerTheme
import com.artemiy.player.ui.theme.appPalette
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: SettingsViewModel = viewModel()
            val palette = appPalette(settings.themeMode, settings.lightVariant, settings.darkVariant, settings.accent, isSystemInDarkTheme())
            PlayerTheme(palette = palette, appTextScale = settings.fontScale) {
                PlayerApp(settings)
            }
        }
    }
}

private val audioPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
private fun PlayerApp(settings: SettingsViewModel) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var rescanTrigger by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val playback: PlaybackViewModel = viewModel()
    val home: HomeViewModel = viewModel()
    val playlistsVm: PlaylistsViewModel = viewModel()
    val lyricsSearch: LyricsSearchViewModel = viewModel()

    // Hoisted above the Library tab (rather than owned by LibraryScreen itself) for two reasons:
    // it survives switching tabs and back instead of resetting to the Library home every time,
    // and it lets "перейти к альбому/исполнителю" from Home/Search actually land somewhere —
    // those tabs have no navigation stack of their own to push a detail screen onto.
    val libraryBackStack = remember { mutableStateListOf<LibraryRoute>(LibraryRoute.Home) }
    var addToPlaylistSongs by remember { mutableStateOf<List<Song>?>(null) }

    fun goToAlbum(song: Song) {
        showNowPlaying = false
        selectedTab = AppTab.Library
        libraryBackStack.add(LibraryRoute.AlbumDetail(song.album, song.artist))
    }

    fun goToArtist(song: Song) {
        showNowPlaying = false
        selectedTab = AppTab.Library
        libraryBackStack.add(LibraryRoute.ArtistDetail(song.artist))
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val songs = remember { mutableStateListOf<Song>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(permissionGranted, settings.scanFolders) {
        if (permissionGranted) {
            val fresh = querySongs(context, settings.scanFolders)
            songs.clear()
            songs.addAll(fresh)
            lyricsSearch.sync(fresh, isPlaying = { playback.isPlaying })
        }
    }

    LaunchedEffect(rescanTrigger) {
        if (rescanTrigger > 0 && permissionGranted) {
            val fresh = querySongs(context, settings.scanFolders)
            songs.clear()
            songs.addAll(fresh)
            lyricsSearch.sync(fresh, isPlaying = { playback.isPlaying })
        }
    }

    LaunchedEffect(selectedTab, songs.size) {
        if (selectedTab == AppTab.Home) {
            home.refresh(songs)
        }
    }

    LaunchedEffect(songs.size) {
        playback.setLibrary(songs.toList())
    }

    if (showNowPlaying) {
        BackHandler { showNowPlaying = false }
    }

    // Dark status/nav bar icons on a light theme — except over the Now Playing screen's blur
    // backgrounds, which stay dark whatever the theme is.
    val lightTheme = LocalPlayerPalette.current.isLight
    val lightBars = lightTheme && !(showNowPlaying && settings.nowPlayingBackgroundMode != NowPlayingBackgroundMode.NONE)
    // Album/artist pages put their cover art under the status bar and pick its icon color from
    // the art; overlays drawn on top of them (player, settings) go back to the theme's choice.
    val statusBarOverride = remember { mutableStateOf<Boolean?>(null) }
    val darkStatusIcons = if (showNowPlaying || showSettings) lightBars else statusBarOverride.value ?: lightBars
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkStatusIcons
            isAppearanceLightNavigationBars = lightBars
        }
    }
    if (showSettings) {
        BackHandler { showSettings = false }
    }

    CompositionLocalProvider(LocalStatusBarIconsOverride provides statusBarOverride) {
    Scaffold(
        containerColor = PlayerColors.Background,
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PlayerColors.Border),
                )
                MiniPlayer(
                    title = playback.currentSong?.title ?: "Ничего не играет",
                    artist = playback.currentSong?.artist ?: "Трек не выбран",
                    albumArtUri = playback.currentSong?.uri,
                    isPlaying = playback.isPlaying,
                    onOpen = { if (playback.currentSong != null) showNowPlaying = true },
                    onTogglePlayPause = { playback.togglePlayPause() },
                )
                PlayerBottomBar(selected = selectedTab, onSelect = { selectedTab = it })
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            when (selectedTab) {
                AppTab.Home -> HomeScreen(
                    quickPicks = home.quickPicks,
                    keepListening = home.keepListening,
                    recentlyAdded = home.recentlyAdded,
                    onSongClick = { song, list ->
                        playback.play(song, list)
                        showNowPlaying = true
                    },
                    onMoodClick = { mood ->
                        val pool = songsForMood(songs, mood, settings.moodFolders[mood] ?: emptySet())
                        if (pool.isNotEmpty()) {
                            playback.play(pool.first(), pool)
                            showNowPlaying = true
                        }
                    },
                    onSettingsClick = { showSettings = true },
                    onPlayNext = { song -> playback.playNext(song) },
                    onAddToQueue = { song -> playback.addToQueue(song) },
                    onAddToPlaylist = { song -> addToPlaylistSongs = listOf(song) },
                    onGoToAlbum = ::goToAlbum,
                    onGoToArtist = ::goToArtist,
                )
                AppTab.Library -> LibraryScreen(
                    permissionGranted = permissionGranted,
                    songs = songs,
                    backStack = libraryBackStack,
                    onRequestPermission = { permissionLauncher.launch(audioPermission) },
                    onSongClick = { song, list ->
                        playback.play(song, list)
                        showNowPlaying = true
                    },
                    onPlayNext = { song -> playback.playNext(song) },
                    onAddToQueue = { song -> playback.addToQueue(song) },
                    onAddAllToQueue = { list -> playback.addAllToQueue(list) },
                    onAddToPlaylist = { list -> addToPlaylistSongs = list },
                    onGoToAlbum = ::goToAlbum,
                    onGoToArtist = ::goToArtist,
                )
                AppTab.Search -> SearchScreen(
                    songs = songs,
                    onSongClick = { song, list ->
                        playback.play(song, list)
                        showNowPlaying = true
                    },
                    onPlayNext = { song -> playback.playNext(song) },
                    onAddToQueue = { song -> playback.addToQueue(song) },
                    onAddToPlaylist = { song -> addToPlaylistSongs = listOf(song) },
                    onGoToAlbum = ::goToAlbum,
                    onGoToArtist = ::goToArtist,
                    searchLyrics = { query -> lyricsSearch.search(query, songs.toList()) },
                    lyricsIndexProgress = lyricsSearch.indexProgress,
                )
            }
        }
    }

    if (showNowPlaying) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {},
        ) {
            NowPlayingScreen(
                song = playback.currentSong,
                isPlaying = playback.isPlaying,
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                manualQueue = playback.manualQueue,
                continueQueue = playback.continueQueue,
                shuffleEnabled = playback.shuffleEnabled,
                repeatEnabled = playback.repeatEnabled,
                infinitePlayEnabled = playback.infinitePlayEnabled,
                lyrics = playback.lyrics,
                onClose = { showNowPlaying = false },
                onTogglePlayPause = { playback.togglePlayPause() },
                onSkipNext = { playback.skipNext() },
                onSkipPrevious = { playback.skipPrevious() },
                onSeek = { ms -> playback.seekTo(ms) },
                onQueueItemClick = { song -> playback.playFromQueue(song) },
                onClearManualQueue = { playback.clearManualQueue() },
                onMoveInQueue = { from, to -> playback.moveInQueue(from, to) },
                onRemoveFromQueue = { position -> playback.removeFromQueue(position) },
                onToggleShuffle = { playback.toggleShuffle() },
                onToggleRepeat = { playback.toggleRepeat() },
                onToggleInfinitePlay = { playback.toggleInfinitePlay() },
                allSongs = songs,
                onPlayNext = { song -> playback.playNext(song) },
                onAddToQueue = { song -> playback.addToQueue(song) },
                onAddToPlaylist = { song -> addToPlaylistSongs = listOf(song) },
                onGoToAlbum = ::goToAlbum,
                onGoToArtist = ::goToArtist,
                nowPlayingBackgroundMode = settings.nowPlayingBackgroundMode,
                liveBlurIntensity = settings.liveBlurIntensity,
                lyricsRomanization = settings.lyricsRomanization,
                onToggleLyricsRomanization = { settings.toggleLyricsRomanization() },
                lyricsTapPlays = settings.lyricsTapPlays,
            )
        }
    }

    if (showSettings) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {},
        ) {
            SettingsScreen(
                fontScale = settings.fontScale,
                onFontScaleChange = { settings.updateFontScale(it) },
                songCount = songs.size,
                onRescanLibrary = {
                    rescanTrigger++
                    settings.loadAvailableScanFolders()
                },
                availableFolders = remember(songs.size) { songs.mapNotNull { it.folder }.distinct().sorted() },
                moodFolders = settings.moodFolders,
                onToggleMoodFolder = { mood, folder -> settings.toggleMoodFolder(mood, folder) },
                availableScanFolders = settings.availableScanFolders,
                scanFolders = settings.scanFolders,
                onToggleScanFolder = { folder -> settings.toggleScanFolder(folder) },
                infinitePlayMode = settings.infinitePlayMode,
                onInfinitePlayModeChange = { settings.updateInfinitePlayMode(it) },
                nowPlayingBackgroundMode = settings.nowPlayingBackgroundMode,
                onNowPlayingBackgroundModeChange = { settings.updateNowPlayingBackgroundMode(it) },
                liveBlurIntensity = settings.liveBlurIntensity,
                onLiveBlurIntensityChange = { settings.updateLiveBlurIntensity(it) },
                lyricsTapPlays = settings.lyricsTapPlays,
                onLyricsTapPlaysChange = { settings.updateLyricsTapPlays(it) },
                themeMode = settings.themeMode,
                onThemeModeChange = { settings.updateThemeMode(it) },
                lightVariant = settings.lightVariant,
                onLightVariantChange = { settings.updateLightVariant(it) },
                darkVariant = settings.darkVariant,
                onDarkVariantChange = { settings.updateDarkVariant(it) },
                accent = settings.accent,
                onAccentChange = { settings.updateAccent(it) },
                onBack = { showSettings = false },
            )
        }
    }

    addToPlaylistSongs?.let { songsToAdd ->
        fun addAllTo(playlistId: Long) {
            var remaining = songsToAdd.size
            songsToAdd.forEach { song ->
                playlistsVm.addSongToPlaylist(playlistId, song.id) {
                    remaining--
                    if (remaining == 0) addToPlaylistSongs = null
                }
            }
        }
        AddToPlaylistDialog(
            playlists = playlistsVm.playlists,
            onDismiss = { addToPlaylistSongs = null },
            onAddToPlaylist = { playlistId -> addAllTo(playlistId) },
            onCreatePlaylist = { name ->
                playlistsVm.createPlaylist(name) { id -> addAllTo(id) }
            },
        )
    }
    }
}
