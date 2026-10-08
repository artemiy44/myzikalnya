package com.artemiy.player

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
import com.artemiy.player.ui.i18n.withAppLanguage
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.artemiy.player.ui.components.ArtistChoiceDialog
import com.artemiy.player.data.ArtistNames
import com.artemiy.player.data.artists
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
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(newBase.withAppLanguage())
    }

    override fun onResume() {
        super.onResume()
        com.artemiy.player.ui.theme.WallpaperAccents.refresh(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.artemiy.player.ui.theme.WallpaperAccents.refresh(this)
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
            CompositionLocalProvider(
                LocalIconSet provides settings.iconSet,
                com.artemiy.player.ui.theme.LocalUiStyle provides settings.uiStyle,
                com.artemiy.player.ui.nowplaying.LocalLrcGapDots provides settings.lrcGapDots,
                com.artemiy.player.ui.components.LocalHeroBleed provides (settings.heroStyle == com.artemiy.player.data.HeroStyle.VIVID),
            ) {
                PlayerTheme(palette = palette, appTextScale = settings.fontScale, font = settings.appFont) {
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
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    // Tapping the tab that's already open: back to its first page (or its top). Counted per tab.
    val tabRootRequests = remember { mutableStateMapOf<AppTab, Int>() }
    // Each tab's own state (scroll positions, a search typed in...) survives switching to
    // another tab and back.
    val tabStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
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
    var showSettings by rememberSaveable { mutableStateOf(false) }
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
    // Opening the player (e.g. from a search result) puts away the keyboard left up by the
    // search field, instead of leaving it hanging over the player.
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(showNowPlaying) {
        if (showNowPlaying) {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    /** [velocity]: how fast a finger was moving it when let go, in "whole openings" per second —
     * the animation carries on from that speed instead of starting from standstill. */
    // On its way down (swiped or closed): the back gesture leaves it alone.
    var nowPlayingClosing by remember { mutableStateOf(false) }

    fun openNowPlaying(velocity: Float = 0f) {
        showNowPlaying = true
        nowPlayingClosing = false
        scope.launch {
            nowPlayingExpand.animateTo(1f, if (classicPlayer) NOW_PLAYING_SPRING else EXPRESSIVE_SPRING, initialVelocity = velocity)
        }
    }

    fun closeNowPlaying(animated: Boolean = true, velocity: Float = 0f) {
        nowPlayingClosing = true
        scope.launch {
            if (animated) nowPlayingExpand.animateTo(0f, if (classicPlayer) NOW_PLAYING_SPRING else EXPRESSIVE_SPRING, initialVelocity = velocity)
            else nowPlayingExpand.snapTo(0f)
            showNowPlaying = false
        }
    }

    fun goToAlbum(song: Song) {
        closeNowPlaying(animated = false)
        selectedTab = AppTab.Library
        libraryBackStack.add(LibraryRoute.AlbumDetail(song.album, song.artist))
    }

    fun goToArtistNamed(name: String) {
        closeNowPlaying(animated = false)
        selectedTab = AppTab.Library
        libraryBackStack.add(LibraryRoute.ArtistDetail(name))
    }

    // Several artists on one song: ask which one's page to open.
    var artistChoice by remember { mutableStateOf<List<String>?>(null) }

    fun goToArtist(song: Song) {
        val names = song.artists()
        if (names.size > 1) artistChoice = names else goToArtistNamed(names.firstOrNull() ?: song.artist)
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val songs = remember { mutableStateListOf<Song>() }

    // Starting something new replaces the queue. When the queue was set up with some care —
    // shuffle or endless play on, or 20+ songs lined up — ask first, so a stray tap doesn't throw
    // it away. (Adding to the queue never asks: it doesn't replace anything.)
    var pendingPlay by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun startPlaying(action: () -> Unit) {
        val lined = playback.manualQueue.size + playback.continueQueue.size
        val careful = playback.currentSong != null &&
            (playback.shuffleEnabled || playback.infinitePlayEnabled || lined >= QUEUE_WORTH_ASKING)
        if (careful) pendingPlay = action else action()
    }

    fun playMood(mood: Mood) {
        val pool = songsForMood(songs, mood, settings.moodFolders[mood] ?: emptySet())
        if (pool.isNotEmpty()) {
            startPlaying {
                playback.play(pool.first(), pool, PlayOrigin(context.getString(R.string.tab_mood), SourceArt.Place(SourcePlace.MOOD)))
                openNowPlaying()
            }
        }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // First launch: the welcome screens ask for this themselves, in their own step.
    LaunchedEffect(settings.onboardingDone) {
        if (settings.onboardingDone == true && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The launch cover (see the end of this function) stays up until the library is in.
    var libraryLoaded by remember { mutableStateOf(false) }
    var appReady by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(permissionGranted, settings.scanFolders, settings.minDurationSec) {
        if (permissionGranted) {
            // Off the main thread — it's what made the launch animation stutter.
            val fresh = withContext(Dispatchers.IO) { querySongs(context, settings.scanFolders, settings.minDurationSec * 1000L) }
            // Each artist's usual spelling, so "Eve" and "EVE" end up as one artist.
            // …and one spelling per artist line and album everywhere it's shown.
            val spelled = withContext(Dispatchers.Default) { ArtistNames.learn(fresh); ArtistNames.withUsualSpellings(fresh) }
            songs.clear()
            songs.addAll(spelled)
            libraryLoaded = true
            lyricsSearch.sync(spelled, isPlaying = { playback.isPlaying })
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
            val fresh = withContext(Dispatchers.IO) { querySongs(context, settings.scanFolders, settings.minDurationSec * 1000L) }
            // Each artist's usual spelling, so "Eve" and "EVE" end up as one artist.
            // …and one spelling per artist line and album everywhere it's shown.
            val spelled = withContext(Dispatchers.Default) { ArtistNames.learn(fresh); ArtistNames.withUsualSpellings(fresh) }
            songs.clear()
            songs.addAll(spelled)
            lyricsSearch.sync(spelled, isPlaying = { playback.isPlaying })
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

    // Back from the open player: while the back gesture is being made, the player already starts
    // going down (the first bit of its closing, following the finger); letting go closes it the
    // rest of the way, carrying on from there — or, when the gesture is called off, it settles
    // back open.
    // (Not while the expressive player's queue sheet is pulled up: back closes that first.)
    if (showNowPlaying) {
        androidx.activity.compose.PredictiveBackHandler(
            enabled = !nowPlayingClosing && (classicPlayer || !nowPlayingMemory.expressiveQueueOpen),
        ) { progress ->
            // Picked up from wherever the player is right now (it may still be opening) — never
            // pulled back up to "almost open" first.
            val start = nowPlayingExpand.value
            try {
                progress.collect { event -> nowPlayingExpand.snapTo(minOf(start, 1f - BACK_PEEK * event.progress)) }
                closeNowPlaying()
            } catch (e: kotlinx.coroutines.CancellationException) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    nowPlayingExpand.animateTo(1f, NOW_PLAYING_SPRING)
                }
                throw e
            }
        }
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
    // The back gesture on Settings' main page: the page follows the finger off to the right with
    // the app showing underneath it, like every other page here; let go to close it, or it springs
    // back. (Sub-pages have their own handler, which takes over while they're open.)
    val settingsPeek = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(showSettings) { if (showSettings) settingsPeek.snapTo(0f) }
    if (showSettings) {
        androidx.activity.compose.PredictiveBackHandler { progress ->
            try {
                progress.collect { event -> settingsPeek.snapTo(event.progress) }
                settingsPeek.animateTo(1f, tween(240))
                showSettings = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { settingsPeek.animateTo(0f, tween(200)) }
            }
        }
    }

    // The drag distance that opens the player all the way — most of the screen's height.
    val expandTravelPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() } * 0.8f

    // What shows around the shrunken page: the page colour, darkened a bit more than the page
    // itself (so it reads as depth, not as a lighter strip), by the same 0..1 as the player.
    val backdropColor = PlayerColors.Background
    val screenCornerPx = com.artemiy.player.ui.components.rememberScreenCornerRadiusPx()
    // How far the app is covered by something sliding over it: the player opening, or Settings
    // (which also lets go as the back gesture pulls it away). The page under it shrinks and dims.
    val settingsCover by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showSettings) 1f else 0f,
        animationSpec = tween(280),
        label = "settingsCover",
    )
    val coverDepth = { maxOf(nowPlayingExpand.value, settingsCover * (1f - settingsPeek.value)) }
    CompositionLocalProvider(LocalStatusBarIconsOverride provides statusBarOverride) {
    Scaffold(
        modifier = Modifier.drawBehind {
            drawRect(backdropColor)
            val depth = coverDepth()
            if (depth > 0f) drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f * depth))
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Column {
                if (!com.artemiy.player.ui.theme.expressiveUi) Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PlayerColors.Border),
                )
                MiniPlayer(
                    title = playback.currentSong?.title ?: stringResource(R.string.nothing_playing),
                    artist = playback.currentSong?.artist ?: stringResource(R.string.no_track_chosen),
                    albumArtUri = playback.currentSong?.uri,
                    isPlaying = playback.isPlaying,
                    onOpen = { if (playback.currentSong != null) openNowPlaying() },
                    onTogglePlayPause = { playback.togglePlayPause() },
                    onSkipNext = { playback.skipNext() },
                    onSkipPrevious = if (classicPlayer) null else ({ playback.skipPrevious() }),
                    artVisible = !(showNowPlaying && classicPlayer),
                    anchors = miniAnchors,
                    textScrolls = !showNowPlaying,
                    progress = { if (playback.durationMs > 0) playback.positionMs.toFloat() / playback.durationMs else 0f },
                    progressShown = !showNowPlaying,
                    // Drag it up to pull the classic player open with the finger.
                    modifier = Modifier.draggable(
                        orientation = Orientation.Vertical,
                        enabled = playback.currentSong != null,
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
                PlayerBottomBar(selected = selectedTab, onSelect = { tab ->
                    if (tab != selectedTab) {
                        selectedTab = tab
                    } else if (tab == AppTab.Library && libraryBackStack.size > 1) {
                        libraryBackStack.removeRange(1, libraryBackStack.size)
                    } else {
                        tabRootRequests[tab] = (tabRootRequests[tab] ?: 0) + 1
                    }
                })
            }
        }
    ) { innerPadding ->
        // Expressive: the tab pages run on under the floating mini player and tab bar (nothing
        // behind those), padding their scrolling ends instead; classic keeps them above the bars.
        val floatingBars = com.artemiy.player.ui.theme.expressiveUi
        CompositionLocalProvider(
            com.artemiy.player.ui.theme.LocalBarsInset provides if (floatingBars) innerPadding.calculateBottomPadding() else 0.dp,
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (floatingBars) 0.dp else innerPadding.calculateBottomPadding())
                // Depth: while the player opens (or is dragged, or backed out of), the page under
                // it shrinks a little and dims — drawn from the same 0..1 as the player itself.
                .graphicsLayer {
                    val p = nowPlayingExpand.value
                    val s = 1f - 0.06f * coverDepth()
                    scaleX = s
                    scaleY = s
                    // Round it like the phone's own screen corners (none on a square screen) — the
                    // radius grows and fades with the opening instead of switching on and off.
                    val radius = screenCornerPx * maxOf((p / 0.33f).coerceIn(0f, 1f), settingsCover * (1f - settingsPeek.value))
                    if (radius > 0.5f) {
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
                        clip = true
                    }
                }
                .drawWithContent {
                    drawContent()
                    val depth = coverDepth()
                    if (depth > 0f) drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f * depth))
                },
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
                tabStates.SaveableStateProvider(tab.name) {
                Box(modifier = Modifier.fillMaxSize().background(PlayerColors.Background)) {
                    when (tab) {
                        AppTab.Mood -> {
                            val genresByMood = remember(songs.size, settings.moodFolders) {
                                Mood.entries.associateWith { mood -> topGenres(songsForMood(songs, mood, settings.moodFolders[mood] ?: emptySet())) }
                            }
                            MoodScreen(onPlayMood = ::playMood, genresFor = { genresByMood[it].orEmpty() })
                        }
                        AppTab.Home -> HomeScreen(
                            rootRequest = tabRootRequests[AppTab.Home] ?: 0,
                            mixes = home.mixes,
                            statDays = home.statDays,
                            quickPicks = home.quickPicks,
                            recentlyAdded = home.recentlyAdded,
                            recentlyAddedAll = home.recentlyAddedAll,
                            recap = home.recap,
                            onSongClick = { song, list, origin ->
                                startPlaying {
                                    playback.play(song, list, origin)
                                    openNowPlaying()
                                }
                            },
                            onSaveMix = { mix ->
                                // A snapshot: the mix itself changes daily, the saved playlist doesn't.
                                val date = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault()).format(java.util.Date())
                                playlistsVm.createPlaylistWithSongs("${mix.title} · $date", mix.songs.map { it.id })
                                android.widget.Toast.makeText(context, context.getString(R.string.saved_to_playlists), android.widget.Toast.LENGTH_SHORT).show()
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
                                val origin = libraryBackStack.lastOrNull()?.let { libraryOrigin(it, list, context) }
                                startPlaying {
                                    playback.play(song, list, origin)
                                    openNowPlaying()
                                }
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
                                startPlaying {
                                    playback.play(song, list, PlayOrigin(context.getString(R.string.tab_search), SourceArt.Place(SourcePlace.SEARCH)))
                                    openNowPlaying()
                                }
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
                    enabled = showNowPlaying,
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
          // While the player is on its way open or shut (not fully open), touches starting inside
          // it are swallowed — a tap meant for the page underneath could otherwise land on a
          // button or slider mid-animation (the volume jumped to 100% like that).
          Box(
              modifier = Modifier
                  .fillMaxSize()
                  .pointerInput(Unit) {
                      awaitEachGesture {
                          val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                          if (nowPlayingExpand.value < 0.999f) {
                              down.consume()
                              do {
                                  val event = awaitPointerEvent(PointerEventPass.Initial)
                                  event.changes.forEach { it.consume() }
                              } while (event.changes.any { it.pressed })
                          }
                      }
                  },
          ) {
          CompositionLocalProvider(
              LocalNowPlayingActive provides showNowPlaying,
              com.artemiy.player.ui.nowplaying.LocalRepeatOne provides playback.repeatOne,
              com.artemiy.player.ui.nowplaying.LocalQualityLabel provides (if (settings.qualityBadge) playback.qualityLabel else null),
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
                .graphicsLayer {
                    // The back gesture: the page pulls away from the middle — shrinking and
                    // rounded like the screen, melting away over the last stretch.
                    val p = settingsPeek.value
                    val s = 1f - 0.12f * p
                    scaleX = s
                    scaleY = s
                    alpha = 1f - ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    val radius = screenCornerPx * (p / 0.2f).coerceIn(0f, 1f)
                    if (radius > 0.5f) {
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
                        clip = true
                    }
                }
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
                minDurationSec = settings.minDurationSec,
                notificationRepeatButton = settings.notificationRepeatButton,
                onNotificationRepeatButtonChange = { settings.updateNotificationRepeatButton(it) },
                onMinDurationChange = { settings.updateMinDurationSec(it) },
                onToggleScanFolder = { folder -> settings.toggleScanFolder(folder) },
                infinitePlayMode = settings.infinitePlayMode,
                onInfinitePlayModeChange = { settings.updateInfinitePlayMode(it) },
                nowPlayingBackgroundMode = settings.nowPlayingBackgroundMode,
                onNowPlayingBackgroundModeChange = { settings.updateNowPlayingBackgroundMode(it) },
                liveBlurIntensity = settings.liveBlurIntensity,
                onLiveBlurIntensityChange = { settings.updateLiveBlurIntensity(it) },
                lyricsTapPlays = settings.lyricsTapPlays,
                onLyricsTapPlaysChange = { settings.updateLyricsTapPlays(it) },
                lrcGapDots = settings.lrcGapDots,
                onLrcGapDotsChange = { settings.updateLrcGapDots(it) },
                playerStyle = settings.playerStyle,
                onPlayerStyleChange = { settings.updatePlayerStyle(it) },
                heroStyle = settings.heroStyle,
                onHeroStyleChange = { settings.updateHeroStyle(it) },
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
                appFont = settings.appFont,
                onAppFontChange = { settings.updateAppFont(it) },
                libraryTabs = settings.libraryTabs,
                extras = com.artemiy.player.ui.settings.ExtraSettings(
                    fadeMode = settings.fadeMode,
                    onFadeModeChange = { settings.updateFadeMode(it) },
                    qualityBadge = settings.qualityBadge,
                    onQualityBadgeChange = { settings.updateQualityBadge(it) },
                ),
                songs = songs,
                onDataRestored = { playlistsVm.refresh(); home.refresh(songs) },
                onLibraryTabsChange = { settings.updateLibraryTabs(it) },
                uiStyle = settings.uiStyle,
                onUiStyleChange = { settings.updateUiStyle(it) },
                onShowOnboarding = { settings.updateOnboardingDone(false) },
                onBack = { showSettings = false },
                keptArtists = settings.keptArtists,
                onAddKeptArtist = { settings.addKeptArtist(it) },
                onRemoveKeptArtist = { settings.removeKeptArtist(it) },
                splitArtistLines = remember(songs.size, settings.keptArtists) {
                    songs.map { it.artist }.distinct().filter { ArtistNames.split(it).size > 1 }.sortedBy { it.lowercase() }
                },
            )
        }
    }

    // The welcome screens: on first launch, and again when asked for from "О приложении" — first
    // the language (the screen restarts in it), then the burst's hello and the setup steps.
    var languageStepDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.onboardingDone) { if (settings.onboardingDone == true) languageStepDone = false }
    if (settings.onboardingDone == false && !languageStepDone) {
        com.artemiy.player.ui.onboarding.LanguageScreen(onPicked = { language ->
            val before = com.artemiy.player.ui.i18n.LanguagePrefs.get(context)
            com.artemiy.player.ui.i18n.LanguagePrefs.set(context, language)
            languageStepDone = true
            if (before != language) (context as? android.app.Activity)?.recreate()
        })
    } else if (settings.onboardingDone == false) {
        com.artemiy.player.ui.onboarding.OnboardingScreen(
            state = com.artemiy.player.ui.onboarding.OnboardingState(
                musicAllowed = permissionGranted,
                libraryLoaded = libraryLoaded,
                songCount = songs.size,
                availableFolders = settings.availableScanFolders,
                selectedFolders = settings.scanFolders,
                themeMode = settings.themeMode,
                playerStyle = settings.playerStyle,
                uiStyle = settings.uiStyle,
                notificationsNeedAsking = Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED,
            ),
            actions = com.artemiy.player.ui.onboarding.OnboardingActions(
                requestMusic = { permissionLauncher.launch(audioPermission) },
                loadFolders = { settings.loadAvailableScanFolders() },
                toggleFolder = { settings.toggleScanFolder(it) },
                setThemeMode = { settings.updateThemeMode(it) },
                setPlayerStyle = { settings.updatePlayerStyle(it) },
                setUiStyle = { settings.updateUiStyle(it) },
                requestNotifications = {
                    if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                finish = {
                    settings.updateOnboardingDone(true)
                    showSettings = false
                },
            ),
        )
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
            LoadingBurst(color = PlayerColors.AccentMark, modifier = Modifier.size(96.dp))
        }
    }

    pendingPlay?.let { play ->
        com.artemiy.player.ui.components.AppDialog(onDismiss = { pendingPlay = null }) {
            com.artemiy.player.ui.components.DialogTitle(stringResource(R.string.replace_queue_q))
            com.artemiy.player.ui.components.DialogMessage(stringResource(R.string.replace_queue_msg))
            com.artemiy.player.ui.components.DialogButtons(
                dismissLabel = stringResource(R.string.cancel),
                onDismiss = { pendingPlay = null },
                confirmLabel = stringResource(R.string.replace),
                onConfirm = {
                    pendingPlay = null
                    play()
                },
            )
        }
    }

    artistChoice?.let { names ->
        ArtistChoiceDialog(
            names = names,
            onPick = { name ->
                artistChoice = null
                goToArtistNamed(name)
            },
            onDismiss = { artistChoice = null },
        )
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
private fun libraryOrigin(route: com.artemiy.player.ui.library.LibraryRoute, songs: List<Song>, context: android.content.Context): PlayOrigin = when (route) {
    is com.artemiy.player.ui.library.LibraryRoute.AlbumDetail -> PlayOrigin(route.album.ifBlank { context.getString(R.string.album) }, SourceArt.Cover(songs.firstOrNull()?.uri))
    is com.artemiy.player.ui.library.LibraryRoute.ArtistDetail -> PlayOrigin(route.artist, SourceArt.Collage(songs))
    is com.artemiy.player.ui.library.LibraryRoute.PlaylistDetail -> PlayOrigin(route.name, SourceArt.Collage(songs))
    com.artemiy.player.ui.library.LibraryRoute.Songs -> PlayOrigin(context.getString(R.string.tracks), SourceArt.Place(SourcePlace.SONGS))
    else -> PlayOrigin(context.getString(R.string.tab_library), SourceArt.Place(SourcePlace.LIBRARY))
}

/** Classic player opening and closing: one soft spring both ways, settling without a bounce —
 * and carried right to the end: the spring's own default is to stop 1% short and jump the rest,
 * which showed as the cover, title and buttons hopping a few pixels as it finished. */
private val NOW_PLAYING_SPRING = spring(dampingRatio = 1f, stiffness = 320f, visibilityThreshold = 0.0005f)

/** The expressive player's card: a little softer and slower than the classic sheet, so its
 * pieces have time to pop in one after another. */
private val EXPRESSIVE_SPRING = spring(dampingRatio = 1f, stiffness = 240f, visibilityThreshold = 0.0005f)

/** From how many songs lined up a queue is worth a "replace it?" question. */
private const val QUEUE_WORTH_ASKING = 20

/** How far the player sinks toward the mini player while the back gesture is made. */
private const val BACK_PEEK = 0.12f
