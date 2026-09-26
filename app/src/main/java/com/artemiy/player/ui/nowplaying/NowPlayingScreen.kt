package com.artemiy.player.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.PlayerStyle
import com.artemiy.player.data.Song
import com.artemiy.player.playback.PlaySource
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.lyrics.hasRomanization
import com.artemiy.player.playback.openOutputSwitcher
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.AlbumArt
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    manualQueue: List<Song>,
    continueQueue: List<Song>,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    infinitePlayEnabled: Boolean,
    lyrics: ParsedLyrics?,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueueItemClick: (Song) -> Unit,
    onClearManualQueue: () -> Unit,
    onMoveInQueue: (from: Int, to: Int) -> Unit,
    onRemoveFromQueue: (position: Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    allSongs: List<Song>,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    nowPlayingBackgroundMode: NowPlayingBackgroundMode = NowPlayingBackgroundMode.LIVE_BLUR,
    liveBlurIntensity: LiveBlurIntensity = LiveBlurIntensity.NORMAL,
    lyricsRomanization: Boolean = true,
    onToggleLyricsRomanization: () -> Unit = {},
    lyricsTapPlays: Boolean = false,
    playerStyle: PlayerStyle = PlayerStyle.CLASSIC,
    playingFrom: PlaySource? = null,
) {
    if (playerStyle == PlayerStyle.EXPRESSIVE) {
        ExpressiveNowPlaying(
            song = song, isPlaying = isPlaying, positionMs = positionMs, durationMs = durationMs,
            manualQueue = manualQueue, continueQueue = continueQueue,
            shuffleEnabled = shuffleEnabled, repeatEnabled = repeatEnabled, infinitePlayEnabled = infinitePlayEnabled,
            lyrics = lyrics, playingFrom = playingFrom,
            onClose = onClose, onTogglePlayPause = onTogglePlayPause, onSkipNext = onSkipNext, onSkipPrevious = onSkipPrevious,
            onSeek = onSeek, onQueueItemClick = onQueueItemClick, onClearManualQueue = onClearManualQueue,
            onMoveInQueue = onMoveInQueue, onRemoveFromQueue = onRemoveFromQueue,
            onToggleShuffle = onToggleShuffle, onToggleRepeat = onToggleRepeat, onToggleInfinitePlay = onToggleInfinitePlay,
            allSongs = allSongs, onPlayNext = onPlayNext, onAddToQueue = onAddToQueue, onAddToPlaylist = onAddToPlaylist,
            onGoToAlbum = onGoToAlbum, onGoToArtist = onGoToArtist,
            backgroundMode = nowPlayingBackgroundMode, blurIntensity = liveBlurIntensity,
            lyricsRomanization = lyricsRomanization, onToggleLyricsRomanization = onToggleLyricsRomanization,
            lyricsTapPlays = lyricsTapPlays,
        )
        return
    }
    var centerMode by remember { mutableStateOf(CenterMode.Art) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsVisibleBeforeDrag by remember { mutableStateOf(true) }
    var showAddToQueuePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Reset to visible whenever the center content changes mode.
    LaunchedEffect(centerMode) {
        controlsVisible = true
    }

    // Lyrics: auto-hide after a few seconds of inactivity, but only while a song is actually
    // playing — pausing forces the controls back so they're not stuck hidden while paused.
    LaunchedEffect(centerMode, controlsVisible, isPlaying) {
        if (centerMode == CenterMode.Lyrics && controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }
    LaunchedEffect(isPlaying) {
        if (!isPlaying && centerMode == CenterMode.Lyrics) controlsVisible = true
    }

    val queueListState = rememberLazyListState()
    val queueNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -2f) controlsVisible = false
                else if (available.y > 2f) controlsVisible = true
                return Offset.Zero
            }
        }
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.92f,
        animationSpec = tween(300),
        label = "artScale",
    )
    NowPlayingSurface(song = song, mode = nowPlayingBackgroundMode, intensity = liveBlurIntensity) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // top only, not bottom — the control panel below already gets its own
                // navigationBarsPadding lower down; a bottom value here too double-reserves
                // space and brings back the "ledge" above the gesture bar.
                .padding(start = 22.dp, end = 22.dp, top = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LocalAdaptiveSecondaryColor.current)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onClose() },
                )
            }

            var controlsHeightPx by remember { mutableStateOf(0) }
            var headerHeightPx by remember { mutableStateOf(0) }
            var toggleRowHeightPx by remember { mutableStateOf(0) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (centerMode) {
                    CenterMode.Art -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 10.dp)
                            .padding(bottom = with(density) { controlsHeightPx.toDp() }),
                        contentAlignment = Alignment.Center,
                    ) {
                        AlbumArt(
                            uri = song?.uri,
                            size = ART_SIZE_FULL,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .scale(artScale)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    }

                    CenterMode.Lyrics -> {
                        LyricsView(
                            lyrics = lyrics,
                            positionMs = positionMs,
                            isPlaying = isPlaying,
                            topFadePx = { headerHeightPx },
                            // The control panel auto-hides while a Lyrics track plays — no panel
                            // up means nothing for the list to fade out from under, so the fade
                            // boundary collapses to the true bottom edge instead of staying
                            // pinned to the (now invisible) panel's last known height.
                            bottomFadePx = { if (controlsVisible) controlsHeightPx else 0 },
                            anchorTopPx = headerHeightPx,
                            anchorBottomPx = if (controlsVisible) controlsHeightPx else 0,
                            onLineClick = { timeMs ->
                                onSeek(timeMs)
                                if (lyricsTapPlays && !isPlaying) onTogglePlayPause()
                            },
                            showRomanization = lyricsRomanization,
                            // Extra FADE_SPAN at both ends: otherwise the first/last lines can't
                            // scroll out of the fade zone (nothing above/below them to scroll)
                            // and stay stuck half-faded, reading as gray.
                            contentPadding = PaddingValues(
                                top = with(density) { headerHeightPx.toDp() } + FADE_SPAN + 8.dp,
                                bottom = with(density) { controlsHeightPx.toDp() } + FADE_SPAN + 8.dp,
                            ),
                        )
                        if (!controlsVisible) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { controlsVisible = true },
                            )
                        }
                    }

                    // The toggle row floats fixed under the header (same idea as the header
                    // itself) — the list scrolls underneath BOTH of them and fades out as it
                    // approaches, exactly like Lyrics does under its header.
                    CenterMode.Queue -> Box(modifier = Modifier.fillMaxSize()) {
                        QueueList(
                            manualQueue = manualQueue,
                            continueQueue = continueQueue,
                            onItemClick = onQueueItemClick,
                            onClearManualQueue = onClearManualQueue,
                            onAddSongsClick = { showAddToQueuePicker = true },
                            onMove = onMoveInQueue,
                            onRemove = onRemoveFromQueue,
                            songActions = remember(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist) {
                                QueueSongActions(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
                            },
                            // The control panel covers the bottom of the list, so a picked-up
                            // song couldn't be dragged down there — hide it for the duration of
                            // the drag, then put it back the way it was.
                            onDragActiveChange = { active ->
                                if (active) {
                                    controlsVisibleBeforeDrag = controlsVisible
                                    controlsVisible = false
                                } else {
                                    controlsVisible = controlsVisibleBeforeDrag
                                }
                            },
                            state = queueListState,
                            topFadePx = { headerHeightPx + toggleRowHeightPx },
                            // Same reasoning as Lyrics — the panel hides on scroll in Queue too,
                            // so the fade should collapse with it instead of hanging around.
                            bottomFadePx = { if (controlsVisible) controlsHeightPx else 0 },
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(queueNestedScrollConnection),
                            contentPadding = PaddingValues(
                                top = with(density) { (headerHeightPx + toggleRowHeightPx).toDp() } + 8.dp,
                                bottom = with(density) { controlsHeightPx.toDp() } + 24.dp,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(top = with(density) { headerHeightPx.toDp() })
                                .onGloballyPositioned { toggleRowHeightPx = it.size.height },
                        ) {
                            QueueToggleRow(
                                shuffleEnabled = shuffleEnabled,
                                repeatEnabled = repeatEnabled,
                                infinitePlayEnabled = infinitePlayEnabled,
                                onToggleShuffle = onToggleShuffle,
                                onToggleRepeat = onToggleRepeat,
                                onToggleInfinitePlay = onToggleInfinitePlay,
                            )
                        }
                    }
                }

                // Floating mini-header — Lyrics/Queue only (Art shows title in the control panel
                // below instead). Overlaps the scrollable content instead of pushing it down — no
                // background/blur of its own at all now; the list underneath fades itself out via
                // fadeInList() before it gets here, so there's nothing to hide a hard edge from.
                if (centerMode != CenterMode.Art) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .onGloballyPositioned { headerHeightPx = it.size.height }
                            // Absorbs taps anywhere in this panel's bounds so they don't fall
                            // through to whatever's scrolling underneath (was letting stray taps
                            // near the header hit list rows behind it).
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .padding(top = 10.dp),
                    ) {
                        NowPlayingMiniHeader(
                            song = song,
                            onCollapse = { centerMode = CenterMode.Art },
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist,
                            trailing = {
                                when {
                                    centerMode == CenterMode.Lyrics && lyrics?.hasRomanization() == true -> RomanizationToggle(
                                        enabled = lyricsRomanization,
                                        onToggle = onToggleLyricsRomanization,
                                    )
                                    centerMode == CenterMode.Queue && song != null -> CurrentSongMenuButton(
                                        song = song,
                                        onPlayNext = onPlayNext,
                                        onAddToQueue = onAddToQueue,
                                        onAddToPlaylist = onAddToPlaylist,
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist,
                                    )
                                }
                            },
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(250)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onGloballyPositioned { controlsHeightPx = it.size.height }
                        // Same tap-absorption as the header above — otherwise taps landing in the
                        // gaps between the slider/buttons fall through to the list underneath.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .navigationBarsPadding(),
                ) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    // Redundant with NowPlayingMiniHeader once we're in Lyrics/Queue mode — that
                    // already shows title/artist up top. Only show it below the big cover in the
                    // plain Art view. (A "slides up into the mini header" transition animation is
                    // a nice follow-up, not this pass.)
                    if (centerMode == CenterMode.Art) {
                        SongTitleRow(
                            song = song,
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist,
                            trailing = {
                                if (song != null) {
                                    CurrentSongMenuButton(
                                        song = song,
                                        onPlayNext = onPlayNext,
                                        onAddToQueue = onAddToQueue,
                                        onAddToPlaylist = onAddToPlaylist,
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist,
                                    )
                                }
                            },
                        )
                    }

                    SeekBar(positionMs = positionMs, durationMs = durationMs, onSeek = onSeek, modifier = Modifier.padding(top = 18.dp))

                    TransportControls(
                        isPlaying = isPlaying,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 12.dp),
                    )

                    VolumeRow(modifier = Modifier.padding(top = 28.dp))

                    BottomQuickActionsRow(
                        modifier = Modifier.padding(top = 30.dp),
                        lyricsActive = centerMode == CenterMode.Lyrics,
                        queueActive = centerMode == CenterMode.Queue,
                        shuffleEnabled = shuffleEnabled,
                        repeatEnabled = repeatEnabled,
                        infinitePlayEnabled = infinitePlayEnabled,
                        onLyricsClick = {
                            centerMode = if (centerMode == CenterMode.Lyrics) CenterMode.Art else CenterMode.Lyrics
                        },
                        onDeviceClick = { openOutputSwitcher(context) },
                        onQueueClick = {
                            centerMode = if (centerMode == CenterMode.Queue) CenterMode.Art else CenterMode.Queue
                        },
                    )
                }
            }
            }
        }

        if (showAddToQueuePicker) {
            AddToQueuePicker(
                songs = allSongs,
                onAdd = onAddToQueue,
                onDismiss = { showAddToQueuePicker = false },
            )
        }
    }
}
