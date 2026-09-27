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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.util.lerp
import com.artemiy.player.ui.components.MINI_ART_CORNER
import com.artemiy.player.ui.components.MiniPlayerAnchors
import com.artemiy.player.ui.theme.PlayerColors
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
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
    lyricsLoading: Boolean = false,
    /** Classic style: how far the player is open (0 = still the mini player, 1 = open). */
    expand: () -> Float = { 1f },
    /** Classic style: where the mini player's pieces are — the player grows out of it. */
    mini: MiniPlayerAnchors? = null,
    memory: NowPlayingMemory = remember { NowPlayingMemory() },
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
            lyricsLoading = lyricsLoading,
            memory = memory,
        )
        return
    }
    var centerMode by memory::centerMode
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
    // While opening: the background comes in over the first half, the cover flies all the way,
    // and everything else only shows up over the last quarter.
    val chromeAlpha = { ((expand() - 0.75f) / 0.25f).coerceIn(0f, 1f) }
    var finalArtBounds by remember { mutableStateOf<Rect?>(null) }
    val miniCornerPx = with(LocalDensity.current) { MINI_ART_CORNER.toPx() }
    val artCornerPx = with(LocalDensity.current) { 16.dp.toPx() }
    // The sheet starts as the mini player bar itself (in its own color) and grows to the full screen.
    // The mini player's own colors are read here, before the player's palette takes over.
    val miniColor = PlayerColors.SurfaceDim
    val miniTitleColor = PlayerColors.TextPrimary
    val miniArtistColor = PlayerColors.TextSecondary
    var titleTarget by remember { mutableStateOf<Rect?>(null) }
    NowPlayingSurface(
        song = song,
        mode = nowPlayingBackgroundMode,
        intensity = liveBlurIntensity,
        openFrom = mini?.bar,
        openProgress = expand,
        openTint = miniColor,
    ) {
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
                        .graphicsLayer { alpha = chromeAlpha() }
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
                                .onGloballyPositioned { finalArtBounds = it.boundsInRoot() }
                                // Flying in from (or back to) the mini player's cover: drawn at the
                                // in-between position and size, its corners in between too.
                                .graphicsLayer {
                                    val p = FLIGHT_EASING.transform(expand())
                                    val to = finalArtBounds
                                    val from = mini?.art
                                    if (p < 1f && to != null && from != null && to.width > 0f) {
                                        // Aim for the cover as it'll actually sit — a touch smaller
                                        // while paused — so there's no jump when it lands.
                                        val endW = to.width * artScale
                                        val endH = to.height * artScale
                                        val endLeft = to.center.x - endW / 2
                                        val endTop = to.center.y - endH / 2
                                        val left = lerp(from.left, endLeft, p)
                                        val top = lerp(from.top, endTop, p)
                                        val width = lerp(from.width, endW, p)
                                        val height = lerp(from.height, endH, p)
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        translationX = left - to.left
                                        translationY = top - to.top
                                        scaleX = width / to.width
                                        scaleY = height / to.height
                                        // The clip is drawn before the scaling, so the on-screen radius
                                        // is divided back up by the scale.
                                        val onScreen = lerp(miniCornerPx, artCornerPx, p)
                                        shape = RoundedCornerShape(onScreen * to.width / width)
                                    } else {
                                        // Settled: the paused shrink happens here too, so the
                                        // rounded clip shrinks along with the picture.
                                        transformOrigin = TransformOrigin.Center
                                        scaleX = artScale
                                        scaleY = artScale
                                        shape = RoundedCornerShape(artCornerPx)
                                    }
                                    clip = true
                                },
                        )
                    }

                    CenterMode.Lyrics -> Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha() }) {
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
                            loading = lyricsLoading,
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
                    CenterMode.Queue -> Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha() }) {
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
                            // Opening/closing on lyrics or the queue: the mini player's cover and
                            // text fly to (and from) this header instead of the big cover.
                            artModifier = Modifier.flyFrom(mini?.art, expand, fromCenter = true),
                            textModifier = Modifier.flyFrom(
                                mini?.text,
                                expand,
                                fromCenter = false,
                                startScale = 13f / 17f,
                                fadeInUntil = TEXT_HANDOFF,
                                onTarget = { titleTarget = it },
                            ),
                            trailing = {
                                Box(modifier = Modifier.graphicsLayer { alpha = chromeAlpha() }) {
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
                            // Title and artist fly over from the mini player's text.
                            textModifier = Modifier.flyFrom(
                                mini?.text,
                                expand,
                                fromCenter = false,
                                startScale = MINI_TITLE_SCALE,
                                fadeInUntil = TEXT_HANDOFF,
                                onTarget = { titleTarget = it },
                            ),
                            trailing = {
                                if (song != null) Box(modifier = Modifier.graphicsLayer { alpha = chromeAlpha() }) {
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

                    SeekBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        modifier = Modifier.padding(top = 18.dp).graphicsLayer { alpha = chromeAlpha() },
                    )

                    TransportControls(
                        isPlaying = isPlaying,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 12.dp),
                        prevModifier = Modifier.graphicsLayer { alpha = chromeAlpha() },
                        // Play and next fly over from the mini player's buttons.
                        playModifier = Modifier.flyFrom(mini?.play, expand, fromCenter = true),
                        nextModifier = Modifier.flyFrom(mini?.next, expand, fromCenter = true),
                    )

                    VolumeRow(modifier = Modifier.padding(top = 28.dp).graphicsLayer { alpha = chromeAlpha() })

                    BottomQuickActionsRow(
                        modifier = Modifier.padding(top = 30.dp).graphicsLayer { alpha = chromeAlpha() },
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

        // A look-alike of the mini player's text — same size, weight and colors — riding along at
        // first and fading out as the player's own title fades in, so grabbing the bar doesn't
        // change how its text looks.
        val miniText = mini?.text
        // How much bigger the text it hands over to is: the Art view's title, or the header's.
        val handoffScale = if (centerMode == CenterMode.Art) 20f / 13f else 17f / 13f
        if (miniText != null) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(miniText.left.roundToInt(), miniText.top.roundToInt()) }
                    .width(with(LocalDensity.current) { miniText.width.toDp() })
                    .graphicsLayer {
                        alpha = (1f - expand() / TEXT_HANDOFF).coerceIn(0f, 1f)
                        val p = FLIGHT_EASING.transform(expand())
                        val to = titleTarget
                        if (to != null) {
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationX = (to.left - miniText.left) * p
                            translationY = (to.top - miniText.top) * p
                            val scale = lerp(1f, handoffScale, p)
                            scaleX = scale
                            scaleY = scale
                        }
                    },
            ) {
                Text(
                    text = song?.title ?: "Ничего не играет",
                    color = miniTitleColor,
                    fontSize = 13.sp,
                    lineHeight = 15.6.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song?.artist ?: "",
                    color = miniArtistColor,
                    fontSize = 11.sp,
                    lineHeight = 15.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

/** How the flying pieces (cover, text, buttons) move against the opening — evenly, in step with
 * the sheet's edge. (Easing them on their own made them run ahead of it.) */
internal val FLIGHT_EASING: Easing = LinearEasing

/** The mini player's title (13sp) against the big player's (20sp). */
private const val MINI_TITLE_SCALE = 13f / 20f

/** How far into the opening the mini player's text has handed over to the player's own. */
private const val TEXT_HANDOFF = 0.15f
