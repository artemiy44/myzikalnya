package com.artemiy.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.artemiy.player.data.PlayerStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import com.artemiy.player.ui.mood.LoadingBurst
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.artemiy.player.data.Mood
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.Song
import com.artemiy.player.data.querySongs
import com.artemiy.player.data.songsForMood
import com.artemiy.player.data.topGenres
import com.artemiy.player.playback.PlaybackViewModel
import com.artemiy.player.playback.PlayOrigin
import com.artemiy.player.playback.SourceArt
import com.artemiy.player.playback.SourcePlace
import com.artemiy.player.ui.components.AppTab
import com.artemiy.player.ui.components.MiniPlayer
import com.artemiy.player.ui.components.MiniPlayerAnchors
import com.artemiy.player.ui.nowplaying.LocalNowPlayingActive
import com.artemiy.player.ui.nowplaying.NowPlayingMemory
import com.artemiy.player.ui.components.PlayerBottomBar
import com.artemiy.player.ui.home.HomeScreen
import com.artemiy.player.ui.home.HomeViewModel
import com.artemiy.player.ui.library.AddToPlaylistDialog
import com.artemiy.player.ui.library.LibraryRoute
import com.artemiy.player.ui.library.LibraryScreen
import com.artemiy.player.ui.library.PlaylistsViewModel
import com.artemiy.player.ui.nowplaying.NowPlayingScreen
import com.artemiy.player.ui.mood.MoodScreen
import com.artemiy.player.ui.search.LyricsSearchViewModel
import com.artemiy.player.ui.search.SearchScreen
import com.artemiy.player.ui.settings.SettingsScreen
import com.artemiy.player.ui.settings.SettingsViewModel
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.components.LocalStatusBarIconsOverride
import com.artemiy.player.ui.icons.LocalIconSet
import com.artemiy.player.ui.theme.LocalPlayerPalette
import androidx.compose.runtime.CompositionLocalProvider
import com.artemiy.player.ui.theme.PlayerTheme
import com.artemiy.player.ui.theme.appPalette
import com.artemiy.player.ui.theme.ThemeMode
import android.app.UiModeManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: SettingsViewModel = viewModel()
            // Tell the system the app's own light/dark choice, so the next launch splash (drawn
            // before any of our code runs) matches it instead of flashing dark on a light theme.
            LaunchedEffect(settings.themeMode) {
                if (Build.VERSION.SDK_INT >= 31) {
                    getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
                        when (settings.themeMode) {
                            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                        },
                    )
                }
            }
            val palette = appPalette(settings.themeMode, settings.lightVariant, settings.darkVariant, settings.accent, isSystemInDarkTheme())
            CompositionLocalProvider(LocalIconSet provides settings.iconSet) {
                PlayerTheme(palette = palette, appTextScale = settings.fontScale) {
                    PlayerApp(settings)
                }
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
    // Jump to the chosen start tab once, as soon as the saved choice has been read — later changes
    // to the setting only apply to the next launch, not to the tab you're on right now.
    var startTabApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.startTabLoaded) {
        if (settings.startTabLoaded && !startTabApplied) {
            selectedTab = settings.startTab
            startTabApplied = true
        }
    }
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

    // How far the player is open, 0 (just the mini player) to 1. The classic player animates its
    // opening from the mini player on this — tapped, or dragged up with a finger; the expressive
    // one simply appears.
    // Kept within 0..1 — a spring overshooting past "open" would flash the cover at full size. And
    // run out to a hair from the end: by default a spring stops 1% short and snaps the rest, which
    // made the cover and text hop a few pixels as the animation finished.
    val nowPlayingExpand = remember { Animatable(0f, visibilityThreshold = 0.0005f).apply { updateBounds(0f, 1f) } }
    val scope = rememberCoroutineScope()
    val miniAnchors = remember { MiniPlayerAnchors() }
    // What the player was showing last time (lyrics, queue...), to open it the same way again.
    val nowPlayingMemory = remember { NowPlayingMemory() }
    val classicPlayer = settings.playerStyle == PlayerStyle.CLASSIC

    /** [velocity]: how fast a finger was moving it when let go, in "whole openings" per second —
     * the animation carries on from that speed instead of starting from standstill. */
    fun openNowPlaying(velocity: Float = 0f) {
        showNowPlaying = true
        scope.launch {
            if (classicPlayer) nowPlayingExpand.animateTo(1f, NOW_PLAYING_SPRING, initialVelocity = velocity)
            else nowPlayingExpand.snapTo(1f)
        }
    }

    fun closeNowPlaying(animated: Boolean = true, velocity: Float = 0f) {
        scope.launch {
            if (classicPlayer && animated) nowPlayingExpand.animateTo(0f, NOW_PLAYING_SPRING, initialVelocity = velocity)
            else nowPlayingExpand.snapTo(0f)
            showNowPlaying = false
        }
    }

    fun goToAlbum(song: Song) {
        closeNowPlaying(animated = false)
        selectedTab = AppTab.Library
        libraryBackStack.add(LibraryRoute.AlbumDetail(song.album, song.artist))
    }

    fun goToArtist(song: Song) {
        closeNowPlaying(animated = false)
        selectedTab = AppTab.Library
        libraryBackStack.add(LibraryRoute.ArtistDetail(song.artist))
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val songs = remember { mutableStateListOf<Song>() }

    fun playMood(mood: Mood) {
        val pool = songsForMood(songs, mood, settings.moodFolders[mood] ?: emptySet())
        if (pool.isNotEmpty()) {
            playback.play(pool.first(), pool, PlayOrigin("Настроение", SourceArt.Place(SourcePlace.MOOD)))
            openNowPlaying()
        }
    }


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

    // The launch cover (see the end of this function) stays up until the library is in.
    var libraryLoaded by remember { mutableStateOf(false) }
    var appReady by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(permissionGranted, settings.scanFolders) {
        if (permissionGranted) {
            // Off the main thread — it's what made the launch animation stutter.
            val fresh = withContext(Dispatchers.IO) { querySongs(context, settings.scanFolders) }
            songs.clear()
            songs.addAll(fresh)
            libraryLoaded = true
            lyricsSearch.sync(fresh, isPlaying = { playback.isPlaying })
        }
    }
    LaunchedEffect(libraryLoaded, permissionGranted) {
        if (!appReady && (libraryLoaded || !permissionGranted)) {
            // A moment more for Home to put its mixes and rows together.
            delay(300)
            appReady = true
        }
    }
    LaunchedEffect(Unit) {
        // Never keep the cover up for long, whatever happens.
        delay(4000)
        appReady = true
    }

    LaunchedEffect(rescanTrigger) {
        if (rescanTrigger > 0 && permissionGranted) {
            // Off the main thread — it's what made the launch animation stutter.
            val fresh = withContext(Dispatchers.IO) { querySongs(context, settings.scanFolders) }
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
        BackHandler { closeNowPlaying() }
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

    // The drag distance that opens the player all the way — most of the screen's height.
    val expandTravelPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() } * 0.8f

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
                    onOpen = { if (playback.currentSong != null) openNowPlaying() },
                    onTogglePlayPause = { playback.togglePlayPause() },
                    onSkipNext = { playback.skipNext() },
                    artVisible = !(showNowPlaying && classicPlayer),
                    anchors = miniAnchors,
                    // Drag it up to pull the classic player open with the finger.
                    modifier = Modifier.draggable(
                        orientation = Orientation.Vertical,
                        enabled = classicPlayer && playback.currentSong != null,
                        state = rememberDraggableState { delta ->
                            showNowPlaying = true
                            scope.launch {
                                nowPlayingExpand.snapTo((nowPlayingExpand.value - delta / expandTravelPx).coerceIn(0f, 1f))
                            }
                        },
                        onDragStopped = { velocity ->
                            val open = velocity < -800f || (velocity <= 800f && nowPlayingExpand.value > 0.35f)
                            val speed = -velocity / expandTravelPx
                            if (open) openNowPlaying(speed) else closeNowPlaying(velocity = speed)
                        },
                    ),
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
            // Switching tabs: the old one fades out quickly, the new one fades in rising slightly
            // from 96% — Material's "fade through".
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(220, delayMillis = 70)) + scaleIn(tween(220, delayMillis = 70), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(90)))
                },
                label = "tabs",
            ) { tab ->
                Box(modifier = Modifier.fillMaxSize().background(PlayerColors.Background)) {
                    when (tab) {
                        AppTab.Mood -> {
                            val genresByMood = remember(songs.size, settings.moodFolders) {
                                Mood.entries.associateWith { mood -> topGenres(songsForMood(songs, mood, settings.moodFolders[mood] ?: emptySet())) }
                            }
                            MoodScreen(onPlayMood = ::playMood, genresFor = { genresByMood[it].orEmpty() })
                        }
                        AppTab.Home -> HomeScreen(
                            mixes = home.mixes,
                            statDays = home.statDays,
                            quickPicks = home.quickPicks,
                            recentlyAdded = home.recentlyAdded,
                            recentlyAddedAll = home.recentlyAddedAll,
                            recap = home.recap,
                            onSongClick = { song, list, origin ->
                                playback.play(song, list, origin)
                                openNowPlaying()
                            },
                            onSaveMix = { mix ->
                                // A snapshot: the mix itself changes daily, the saved playlist doesn't.
                                val date = java.text.SimpleDateFormat("dd.MM", java.util.Locale("ru")).format(java.util.Date())
                                playlistsVm.createPlaylistWithSongs("${mix.title} · $date", mix.songs.map { it.id })
                                android.widget.Toast.makeText(context, "Сохранено в плейлисты", android.widget.Toast.LENGTH_SHORT).show()
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
                                playback.play(song, list, libraryBackStack.lastOrNull()?.let { libraryOrigin(it, list) })
                                openNowPlaying()
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
                                playback.play(song, list, PlayOrigin("Поиск", SourceArt.Place(SourcePlace.SEARCH)))
                                openNowPlaying()
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
        }
    }

    // The player is built once, a moment after there's first something to play, and from then on
    // only hidden and shown — building it from scratch on every open cost a visible stall right
    // as the opening animation started (and taking it apart, one at the end of closing).
    var nowPlayingPrepared by remember { mutableStateOf(false) }
    LaunchedEffect(appReady, playback.currentSong != null) {
        if (appReady && playback.currentSong != null && !nowPlayingPrepared) {
            delay(1500)
            nowPlayingPrepared = true
        }
    }

    if (showNowPlaying || nowPlayingPrepared) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Hidden: parked off screen and not drawn, rather than taken apart. Only the
                // placement changes when it's shown or hidden — nothing is rebuilt.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(if (showNowPlaying) 0 else -placeable.width * 2, 0)
                    }
                }
                .graphicsLayer { alpha = if (showNowPlaying) 1f else 0f }
                // Classic player: swipe it down to close — following the finger, like opening.
                // Lists inside (lyrics, queue) keep their own vertical scrolling.
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = classicPlayer && showNowPlaying,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            nowPlayingExpand.snapTo((nowPlayingExpand.value - delta / expandTravelPx).coerceIn(0f, 1f))
                        }
                    },
                    onDragStopped = { velocity ->
                        val close = velocity > 800f || (velocity >= -800f && nowPlayingExpand.value < 0.65f)
                        val speed = -velocity / expandTravelPx
                        if (close) closeNowPlaying(velocity = speed) else openNowPlaying(speed)
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {},
        ) {
          CompositionLocalProvider(LocalNowPlayingActive provides showNowPlaying) {
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
                onClose = { closeNowPlaying() },
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
                playerStyle = settings.playerStyle,
                playingFrom = playback.playingFrom,
                expand = { nowPlayingExpand.value },
                mini = miniAnchors,
                memory = nowPlayingMemory,
                lyricsLoading = playback.lyricsLoading,
            )
          }
        }
    }

    // Settings slide in over everything from the right, and back out.
    AnimatedVisibility(
        visible = showSettings,
        enter = slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)),
        exit = slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250)),
    ) {
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
                playerStyle = settings.playerStyle,
                onPlayerStyleChange = { settings.updatePlayerStyle(it) },
                themeMode = settings.themeMode,
                onThemeModeChange = { settings.updateThemeMode(it) },
                lightVariant = settings.lightVariant,
                onLightVariantChange = { settings.updateLightVariant(it) },
                darkVariant = settings.darkVariant,
                onDarkVariantChange = { settings.updateDarkVariant(it) },
                accent = settings.accent,
                onAccentChange = { settings.updateAccent(it) },
                startTab = settings.startTab,
                onStartTabChange = { settings.updateStartTab(it) },
                iconSet = settings.iconSet,
                onIconSetChange = { settings.updateIconSet(it) },
                onBack = { showSettings = false },
            )
        }
    }

    // Launch cover: the app's background with the "happy" burst playing, instead of watching the
    // screens fill in; it fades out (growing a touch) once everything's there.
    AnimatedVisibility(
        visible = !appReady,
        enter = EnterTransition.None,
        exit = fadeOut(tween(380)) + scaleOut(tween(380), targetScale = 1.08f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.Background)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center,
        ) {
            LoadingBurst(color = PlayerColors.AccentStandalone, modifier = Modifier.size(96.dp))
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

/** "Играет из" for songs started from a Library page. */
private fun libraryOrigin(route: com.artemiy.player.ui.library.LibraryRoute, songs: List<Song>): PlayOrigin = when (route) {
    is com.artemiy.player.ui.library.LibraryRoute.AlbumDetail -> PlayOrigin(route.album.ifBlank { "Альбом" }, SourceArt.Cover(songs.firstOrNull()?.uri))
    is com.artemiy.player.ui.library.LibraryRoute.ArtistDetail -> PlayOrigin(route.artist, SourceArt.Collage(songs))
    is com.artemiy.player.ui.library.LibraryRoute.PlaylistDetail -> PlayOrigin(route.name, SourceArt.Collage(songs))
    com.artemiy.player.ui.library.LibraryRoute.Songs -> PlayOrigin("Треки", SourceArt.Place(SourcePlace.SONGS))
    else -> PlayOrigin("Медиатека", SourceArt.Place(SourcePlace.LIBRARY))
}

/** Classic player opening and closing: one soft spring both ways, settling without a bounce —
 * and carried right to the end: the spring's own default is to stop 1% short and jump the rest,
 * which showed as the cover, title and buttons hopping a few pixels as it finished. */
private val NOW_PLAYING_SPRING = spring(dampingRatio = 1f, stiffness = 320f, visibilityThreshold = 0.0005f)
