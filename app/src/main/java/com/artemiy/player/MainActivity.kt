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
import com.artemiy.player.data.Song
import com.artemiy.player.data.querySongs
import com.artemiy.player.data.songsForMood
import com.artemiy.player.playback.PlaybackViewModel
import com.artemiy.player.ui.components.AppTab
import com.artemiy.player.ui.components.MiniPlayer
import com.artemiy.player.ui.components.PlayerBottomBar
import com.artemiy.player.ui.home.HomeScreen
import com.artemiy.player.ui.home.HomeViewModel
import com.artemiy.player.ui.library.LibraryScreen
import com.artemiy.player.ui.nowplaying.NowPlayingScreen
import com.artemiy.player.ui.search.SearchScreen
import com.artemiy.player.ui.settings.SettingsScreen
import com.artemiy.player.ui.settings.SettingsViewModel
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.PlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // App is dark-only for now (no light theme yet), so status/nav bar icons
        // must stay light regardless of the system's day/night setting.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            val settings: SettingsViewModel = viewModel()
            PlayerTheme(appTextScale = settings.fontScale) {
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
        }
    }

    LaunchedEffect(rescanTrigger) {
        if (rescanTrigger > 0 && permissionGranted) {
            val fresh = querySongs(context, settings.scanFolders)
            songs.clear()
            songs.addAll(fresh)
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
    if (showSettings) {
        BackHandler { showSettings = false }
    }

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
                )
                AppTab.Library -> LibraryScreen(
                    permissionGranted = permissionGranted,
                    songs = songs,
                    onRequestPermission = { permissionLauncher.launch(audioPermission) },
                    onSongClick = { song, list ->
                        playback.play(song, list)
                        showNowPlaying = true
                    },
                    onPlayNext = { song -> playback.playNext(song) },
                )
                AppTab.Search -> SearchScreen(
                    songs = songs,
                    onSongClick = { song, list ->
                        playback.play(song, list)
                        showNowPlaying = true
                    },
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
                onToggleShuffle = { playback.toggleShuffle() },
                onToggleRepeat = { playback.toggleRepeat() },
                onToggleInfinitePlay = { playback.toggleInfinitePlay() },
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
                onBack = { showSettings = false },
            )
        }
    }
}
