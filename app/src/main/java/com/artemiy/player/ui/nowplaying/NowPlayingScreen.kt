package com.artemiy.player.ui.nowplaying

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.Song
import com.artemiy.player.lyrics.LyricLine
import com.artemiy.player.lyrics.LyricVoice
import com.artemiy.player.lyrics.RubySegment
import com.artemiy.player.lyrics.hasRomanization
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.MinimalSlider
import com.artemiy.player.ui.components.SongActionsMenuPopup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.artemiy.player.ui.components.rememberAlbumArtBitmap
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private enum class CenterMode { Art, Lyrics, Queue }

/** The muted gray used for secondary text/icons on this screen. Normally exactly
 * [PlayerColors.TextSecondary]; over an album-art background it's lifted toward white just enough
 * to stay readable when the art behind it is bright (see [readableSecondaryColor]). */
private val LocalAdaptiveSecondaryColor = compositionLocalOf { PlayerColors.TextSecondary }

/** Minimum contrast ratio (WCAG formula) the gray must keep against the background. The plain
 * gray on the app's flat dark background is ~6:1, so this only kicks in over bright art. */
private const val SECONDARY_MIN_CONTRAST = 3.5f

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
) {
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

    // Same scrim strength on every Now Playing tab (Art/Lyrics/Queue) — this used to be lower on
    // the Lyrics tab specifically, which made the background visibly lighten when opening it.
    val scrimAlpha = 0.62f
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.92f,
        animationSpec = tween(300),
        label = "artScale",
    )

    // No Haze anymore, anywhere in this screen. Studied a real working Apple-Music-style player
    // (github.com/shouryadixitisverycool/Flamingo) and its whole "living blur" is a completely
    // different, much cheaper trick: blur the album art ONCE in software (not live, not per
    // frame) at a tiny resolution, then just pan/zoom that already-blurred static bitmap
    // (Ken Burns style). No continuous backdrop capture of scrolling content at all — the
    // "smooth fade under the header/controls" people see in Apple Music isn't a blur behind
    // those elements either; it's the *list content itself* fading to transparent (per-row alpha)
    // as it nears the edges, revealing this same static background underneath. See
    // LiveBlurBackground below and the fadeInList() modifier further down.
    //
    // The small pre-blurred art bitmap is loaded once here (not inside LiveBlurBackground) because
    // it serves two purposes: it IS the live-blur background, and in both blur modes it's what we
    // measure to decide how bright the gray text has to be to stay readable over the art.
    val backgroundArt = if (nowPlayingBackgroundMode != NowPlayingBackgroundMode.NONE) {
        val bitmap = rememberLivingBackgroundBitmap(song?.uri)
        remember(bitmap) { bitmap?.let { BackgroundArt(it) } }
    } else {
        null
    }
    val backgroundScrimAlpha = when (nowPlayingBackgroundMode) {
        NowPlayingBackgroundMode.LIVE_BLUR -> liveBlurScrimAlpha(liveBlurIntensity)
        else -> scrimAlpha
    }
    val targetSecondaryColor by produceState(PlayerColors.TextSecondary, backgroundArt, backgroundScrimAlpha) {
        val art = backgroundArt
        value = if (art == null) {
            PlayerColors.TextSecondary
        } else {
            withContext(Dispatchers.Default) { readableSecondaryColor(art.bitmap, backgroundScrimAlpha) }
        }
    }
    // Eased so it doesn't snap while the background itself is still crossfading to the new track.
    val secondaryColor by animateColorAsState(targetSecondaryColor, tween(600), label = "secondaryColor")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background),
    ) {
      CompositionLocalProvider(LocalAdaptiveSecondaryColor provides secondaryColor) {
        when (nowPlayingBackgroundMode) {
            NowPlayingBackgroundMode.LIVE_BLUR -> LiveBlurBackground(
                art = backgroundArt,
                intensity = liveBlurIntensity,
            )

            NowPlayingBackgroundMode.STATIC_BLUR -> {
                // Real per-track background: the album art itself, blurred and dimmed.
                // Crossfades between tracks instead of cutting abruptly.
                Crossfade(
                    targetState = song?.uri,
                    animationSpec = tween(450),
                    modifier = Modifier.fillMaxSize(),
                ) { uri ->
                    AlbumArt(
                        uri = uri,
                        size = ART_SIZE_FULL,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(60.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PlayerColors.Background.copy(alpha = scrimAlpha)),
                )
            }

            NowPlayingBackgroundMode.NONE -> {
                // Just the app's own flat background — no album art at all.
            }
        }

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
                            onLineClick = onSeek,
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song?.title ?: "Ничего не играет",
                                    color = PlayerColors.TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.then(
                                        if (song != null) {
                                            Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) { onGoToAlbum(song) }
                                        } else Modifier,
                                    ),
                                )
                                Text(
                                    text = song?.artist ?: "",
                                    color = LocalAdaptiveSecondaryColor.current,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.then(
                                        if (song != null) {
                                            Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) { onGoToArtist(song) }
                                        } else Modifier,
                                    ),
                                )
                            }
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
                        }
                    }

                    val safeDuration = max(durationMs, 1L)
                    var isDragging by remember { mutableStateOf(false) }
                    var dragValue by remember { mutableStateOf(0f) }
                    val shownPosition = if (isDragging) dragValue else positionMs.toFloat()

                    MinimalSlider(
                        value = shownPosition.coerceIn(0f, safeDuration.toFloat()),
                        onValueChange = {
                            isDragging = true
                            dragValue = it
                        },
                        onValueChangeFinished = {
                            onSeek(dragValue.toLong())
                            isDragging = false
                        },
                        valueRange = 0f..safeDuration.toFloat(),
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = formatMs(shownPosition.toLong()), color = LocalAdaptiveSecondaryColor.current, fontSize = 11.sp)
                        Text(text = formatMs(durationMs), color = LocalAdaptiveSecondaryColor.current, fontSize = 11.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Предыдущий трек",
                            tint = PlayerColors.TextPrimary,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onSkipPrevious() },
                        )
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Играть",
                            tint = PlayerColors.TextPrimary,
                            modifier = Modifier
                                .padding(horizontal = 40.dp)
                                .size(58.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onTogglePlayPause() },
                        )
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Следующий трек",
                            tint = PlayerColors.TextPrimary,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onSkipNext() },
                        )
                    }

                    VolumeRow(modifier = Modifier.padding(top = 28.dp))

                    BottomQuickActionsRow(
                        modifier = Modifier.padding(top = 30.dp),
                        lyricsActive = centerMode == CenterMode.Lyrics,
                        queueActive = centerMode == CenterMode.Queue,
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

/**
 * Prepares the "живой блюр" background bitmap: crop to square, downscale HARD (blur cost scales
 * with pixel count, so a tiny source blurs in a fraction of a millisecond), boost saturation so
 * it doesn't read as washed-out, then run a classic software stack blur — no live
 * RenderEffect/`Modifier.blur()` anywhere in this, which is what caused banding on this device
 * every previous time this project tried a live-blurred version of the art. Runs once per song
 * change, off the main thread; the result is a small, already-blurred, static [Bitmap].
 */
@Composable
private fun rememberLivingBackgroundBitmap(uri: android.net.Uri?): android.graphics.Bitmap? {
    val source = rememberAlbumArtBitmap(uri, ART_SIZE_THUMB)
    var result by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(source) {
        val bmp = source
        result = if (bmp == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching { prepareLivingBackgroundBitmap(bmp) }.getOrNull()
            }
        }
    }
    return result
}

private fun prepareLivingBackgroundBitmap(source: android.graphics.Bitmap): android.graphics.Bitmap {
    val size = minOf(source.width, source.height)
    val xOffset = (source.width - size) / 2
    val yOffset = (source.height - size) / 2
    val square = android.graphics.Bitmap.createBitmap(source, xOffset, yOffset, size, size)
    val targetPx = 72
    val small = if (size > targetPx) {
        android.graphics.Bitmap.createScaledBitmap(square, targetPx, targetPx, true)
    } else {
        square
    }

    val saturated = android.graphics.Bitmap.createBitmap(
        small.width,
        small.height,
        android.graphics.Bitmap.Config.ARGB_8888,
    )
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(
            android.graphics.ColorMatrix().apply { setSaturation(2.2f) },
        )
    }
    android.graphics.Canvas(saturated).drawBitmap(small, 0f, 0f, paint)

    return stackBlur(saturated, radius = 14)
}

/**
 * Classic "stack blur" (Mario Klingemann's well-known, widely-reused algorithm) — plain software
 * box-style blur over raw pixels, no RenderEffect/GPU shader involved at all. Only ever runs on a
 * ~72x72px bitmap here, so even this pure-Kotlin implementation finishes near-instantly.
 */
private fun stackBlur(bitmap: android.graphics.Bitmap, radius: Int): android.graphics.Bitmap {
    if (radius < 1) return bitmap
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val div = radius * 2 + 1
    val divSum = (div + 1) shr 1
    val divSum2 = divSum * divSum
    val mulLookup = IntArray(256 * divSum2) { it / divSum2 }
    val stack = Array(div) { IntArray(3) }

    var minY: IntArray
    val vMin = IntArray(maxOf(w, h))

    var y = 0
    while (y < h) {
        var rSum = 0; var gSum = 0; var bSum = 0
        var rOut = 0; var gOut = 0; var bOut = 0
        var rIn = 0; var gIn = 0; var bIn = 0
        var i = 0
        while (i < div) {
            val x = (i - radius).coerceIn(0, w - 1)
            val p = pixels[y * w + x]
            val s = stack[i]
            s[0] = (p shr 16) and 0xFF
            s[1] = (p shr 8) and 0xFF
            s[2] = p and 0xFF
            val weight = radius + 1 - kotlin.math.abs(i - radius)
            rSum += s[0] * weight; gSum += s[1] * weight; bSum += s[2] * weight
            if (i <= radius) { rOut += s[0]; gOut += s[1]; bOut += s[2] }
            else { rIn += s[0]; gIn += s[1]; bIn += s[2] }
            i++
        }
        var stackPointer = radius
        var x = 0
        while (x < w) {
            pixels[y * w + x] = (pixels[y * w + x] and -0x1000000) or
                (mulLookup[rSum] shl 16) or (mulLookup[gSum] shl 8) or mulLookup[bSum]
            rSum -= rOut; gSum -= gOut; bSum -= bOut
            var stackStart = stackPointer - radius + div
            if (stackStart >= div) stackStart -= div
            val sOut = stack[stackStart]
            rOut -= sOut[0]; gOut -= sOut[1]; bOut -= sOut[2]
            if (y == 0) vMin[x] = minOf(x + radius + 1, w - 1)
            val p = pixels[y * w + vMin[x]]
            sOut[0] = (p shr 16) and 0xFF; sOut[1] = (p shr 8) and 0xFF; sOut[2] = p and 0xFF
            rIn += sOut[0]; gIn += sOut[1]; bIn += sOut[2]
            rSum += rIn; gSum += gIn; bSum += bIn
            stackPointer++
            if (stackPointer >= div) stackPointer = 0
            val sIn = stack[stackPointer]
            rOut += sIn[0]; gOut += sIn[1]; bOut += sIn[2]
            rIn -= sIn[0]; gIn -= sIn[1]; bIn -= sIn[2]
            x++
        }
        y++
    }

    minY = vMin.copyOf()
    x@ for (x0 in 0 until w) {
        var rSum = 0; var gSum = 0; var bSum = 0
        var rOut = 0; var gOut = 0; var bOut = 0
        var rIn = 0; var gIn = 0; var bIn = 0
        var i = 0
        while (i < div) {
            val yy = (i - radius).coerceIn(0, h - 1) * w
            val s = stack[i]
            val p = pixels[yy + x0]
            s[0] = (p shr 16) and 0xFF; s[1] = (p shr 8) and 0xFF; s[2] = p and 0xFF
            val weight = radius + 1 - kotlin.math.abs(i - radius)
            rSum += s[0] * weight; gSum += s[1] * weight; bSum += s[2] * weight
            if (i <= radius) { rOut += s[0]; gOut += s[1]; bOut += s[2] }
            else { rIn += s[0]; gIn += s[1]; bIn += s[2] }
            i++
        }
        var stackPointer = radius
        var yy = 0
        while (yy < h) {
            val idx = yy * w + x0
            pixels[idx] = (pixels[idx] and -0x1000000) or
                (mulLookup[rSum] shl 16) or (mulLookup[gSum] shl 8) or mulLookup[bSum]
            rSum -= rOut; gSum -= gOut; bSum -= bOut
            var stackStart = stackPointer - radius + div
            if (stackStart >= div) stackStart -= div
            val sOut = stack[stackStart]
            rOut -= sOut[0]; gOut -= sOut[1]; bOut -= sOut[2]
            if (x0 == 0) minY[yy] = minOf(yy + radius + 1, h - 1) * w
            val p = pixels[minY[yy] + x0]
            sOut[0] = (p shr 16) and 0xFF; sOut[1] = (p shr 8) and 0xFF; sOut[2] = p and 0xFF
            rIn += sOut[0]; gIn += sOut[1]; bIn += sOut[2]
            rSum += rIn; gSum += gIn; bSum += bIn
            stackPointer++
            if (stackPointer >= div) stackPointer = 0
            val sIn = stack[stackPointer]
            rOut += sIn[0]; gOut += sIn[1]; bOut += sIn[2]
            rIn -= sIn[0]; gIn -= sIn[1]; bIn -= sIn[2]
            yy++
        }
    }

    val out = bitmap.copy(bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    return out
}

/** Wrapper so the bitmap can be passed to composables that should skip recomposition on the
 * ~100ms position ticks — a raw [android.graphics.Bitmap] isn't a type Compose treats as stable. */
@androidx.compose.runtime.Immutable
private class BackgroundArt(val bitmap: android.graphics.Bitmap)

/** Lower intensity = more scrim = the blurred art reads as a softer, more muted wash. */
private fun liveBlurScrimAlpha(intensity: LiveBlurIntensity): Float = when (intensity) {
    LiveBlurIntensity.MUTED -> 0.66f
    LiveBlurIntensity.NORMAL -> 0.52f
    LiveBlurIntensity.VIVID -> 0.38f
}

/**
 * [PlayerColors.TextSecondary], lifted toward white only as far as needed to keep
 * [SECONDARY_MIN_CONTRAST] against the brightest part of the background the text can land on.
 *
 * Measures the art as it actually appears on screen: each pixel is first darkened by the scrim
 * (blended in sRGB, the way the scrim layer is drawn), then only the central vertical strip is
 * sampled — a tall phone screen crops a square cover to roughly its middle 45%, and Ken Burns
 * zooms in further. Uses the 85th-percentile luminance rather than the average, so one bright
 * region on an otherwise dark cover still counts — gray text can end up on exactly that region.
 */
private fun readableSecondaryColor(bitmap: android.graphics.Bitmap, scrimAlpha: Float): Color {
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return PlayerColors.TextSecondary
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val scrim = PlayerColors.Background
    val x0 = (w * 0.25f).toInt()
    val x1 = (w * 0.75f).toInt().coerceAtLeast(x0 + 1)
    val lums = FloatArray((x1 - x0) * h)
    var n = 0
    for (y in 0 until h) {
        for (x in x0 until x1) {
            val p = pixels[y * w + x]
            val r = ((p shr 16) and 0xFF) / 255f * (1f - scrimAlpha) + scrim.red * scrimAlpha
            val g = ((p shr 8) and 0xFF) / 255f * (1f - scrimAlpha) + scrim.green * scrimAlpha
            val b = (p and 0xFF) / 255f * (1f - scrimAlpha) + scrim.blue * scrimAlpha
            lums[n++] = Color(r, g, b).luminance()
        }
    }
    lums.sort()
    val backgroundLum = lums[((n - 1) * 0.85f).toInt()]

    fun contrast(textLum: Float) = (textLum + 0.05f) / (backgroundLum + 0.05f)
    var t = 0f
    while (t <= 1f) {
        val candidate = lerp(PlayerColors.TextSecondary, PlayerColors.TextPrimary, t)
        if (contrast(candidate.luminance()) >= SECONDARY_MIN_CONTRAST) return candidate
        t += 0.05f
    }
    return PlayerColors.TextPrimary
}

/**
 * The "живой блюр" background: the album art itself, pre-blurred once (see above) and then
 * slowly panned/zoomed — a "Ken Burns" effect, the same trick a real, well-regarded open-source
 * Apple-Music-style player (github.com/shouryadixitisverycool/Flamingo) uses. No per-frame blur
 * recompute at all; the bitmap is already-blurred, so this is just a cheap continuous
 * scale/translate on a `graphicsLayer`.
 */
@Composable
private fun LiveBlurBackground(art: BackgroundArt?, intensity: LiveBlurIntensity) {
    val scrimAlpha = liveBlurScrimAlpha(intensity)

    Box(modifier = Modifier.fillMaxSize().background(PlayerColors.Background)) {
        val bmp = art?.bitmap
        if (bmp != null) {
            // Two fully independent phases (scale vs. pan), each always read at a straight 1x
            // multiplier — the exact bug that made the old color-blob version "teleport" was
            // multiplying a single shared phase before feeding it to sin/cos, which breaks the
            // smooth wrap RepeatMode.Restart otherwise gives for free.
            val scaleTransition = rememberInfiniteTransition(label = "kenBurnsScale")
            val scalePhase by scaleTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2 * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(26_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "scalePhase",
            )
            val panTransition = rememberInfiniteTransition(label = "kenBurnsPan")
            val panPhase by panTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2 * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(34_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "panPhase",
            )
            val panAmplitudePx = with(LocalDensity.current) { 26.dp.toPx() }

            Crossfade(
                targetState = bmp,
                animationSpec = tween(600),
                modifier = Modifier.fillMaxSize(),
            ) { frame ->
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = 1.2f + 0.08f * kotlin.math.sin(scalePhase)
                            scaleX = scale
                            scaleY = scale
                            translationX = panAmplitudePx * kotlin.math.cos(panPhase)
                            translationY = panAmplitudePx * kotlin.math.sin(panPhase)
                        },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.Background.copy(alpha = scrimAlpha)),
        )
    }
}

/** How far (in px, converted from [FADE_SPAN]) before an edge a row starts fading — fixed and the
 * same on all four edges (Lyrics top/bottom, Queue top/bottom), regardless of how tall the
 * floating panel sitting at that edge happens to be. */
private val FADE_SPAN = 72.dp

/**
 * Fades ONE list row's own alpha down as it nears the top/bottom edges of its LazyColumn's
 * viewport — applied per-item, not as a single mask over the whole list. The whole-list
 * `BlendMode.DstIn` + `CompositingStrategy.Offscreen` mask approach (a technique that works fine
 * in reference code elsewhere) produced zero visible effect on this device/build no matter how it
 * was wired up — the same category of "this normally-reliable technique silently no-ops here"
 * problem this project already hit twice with `Modifier.blur()` on solid shapes. This is the
 * fallback: plain per-item `alpha`, computed from [state]'s own live layout info (so it updates
 * every scroll frame via the draw phase, no recomposition), matched to this item by
 * [absoluteIndex] — its position among ALL items in the LazyColumn, not just within one
 * `item`/`itemsIndexed` block (Compose numbers items sequentially across the whole list in
 * declaration order). [topPx]/[bottomPx] are read lazily for the same reason.
 *
 * [topPx]/[bottomPx] only mark WHERE the edge of the floating panel sits (so a row reaches
 * alpha 0 exactly as it slides under it, never before or after) — the fade's own length is
 * always [FADE_SPAN], not the panel's height. Coupling the two (an earlier version did) made
 * the fade under the tall control panel comically slow/mushy compared to the short, crisp one
 * under the mini-header, even though both used the same formula.
 *
 * IMPORTANT coordinate gotcha that caused the fade to end early/late by a constant offset: a
 * `LazyListItemInfo.offset` of 0 is NOT the top of the viewport — per Compose's own docs it's
 * the point right AFTER `beforeContentPadding` (top content padding), and `visibleItemsInfo`
 * offsets are relative to that same zero point. Comparing that offset directly against
 * [topPx]/[bottomPx] (which are the header/panel's raw pixel heights, i.e. measured from the
 * true top/bottom of the screen) was off by exactly the content padding amount. Anchoring to
 * `info.viewportStartOffset`/`viewportEndOffset` instead — which already bake in
 * `beforeContentPadding` per Compose's own definition — fixes that for good.
 */
private fun Modifier.fadeInList(
    state: LazyListState,
    absoluteIndex: Int,
    topPx: () -> Int,
    bottomPx: () -> Int,
): Modifier = this.graphicsLayer {
    val info = state.layoutInfo
    val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == absoluteIndex } ?: return@graphicsLayer
    val span = FADE_SPAN.toPx().coerceAtLeast(1f)
    val topEdge = topPx().toFloat() + info.viewportStartOffset
    val bottomEdge = info.viewportEndOffset - bottomPx().toFloat()
    val center = itemInfo.offset + itemInfo.size / 2f
    val topAlpha = ((center - topEdge) / span).coerceIn(0f, 1f)
    val bottomAlpha = ((bottomEdge - center) / span).coerceIn(0f, 1f)
    alpha = minOf(topAlpha, bottomAlpha)
}

@Composable
private fun LyricsView(
    lyrics: ParsedLyrics?,
    positionMs: Long,
    isPlaying: Boolean,
    topFadePx: () -> Int,
    bottomFadePx: () -> Int,
    anchorTopPx: Int,
    anchorBottomPx: Int,
    onLineClick: (timeMs: Long) -> Unit,
    showRomanization: Boolean,
    contentPadding: PaddingValues = PaddingValues(top = 16.dp, bottom = 220.dp),
) {
    // The controller only reports a fresh position every ~100ms, which is far too coarse for a
    // per-word karaoke sweep — it'd visibly step instead of glide. Interpolate every frame
    // between polls using elapsed wall-clock time, and resync to the real value on every poll
    // (or on seek/pause/play) so drift never exceeds one poll interval.
    var smoothPositionMs by remember { mutableStateOf(positionMs) }
    LaunchedEffect(positionMs, isPlaying) {
        val anchorReal = android.os.SystemClock.elapsedRealtime()
        smoothPositionMs = positionMs
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            withFrameMillis { }
            smoothPositionMs = positionMs + (android.os.SystemClock.elapsedRealtime() - anchorReal)
        }
    }
    when (lyrics) {
        null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Текст для этого трека не найден",
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        is ParsedLyrics.Unsynced -> {
            // One list item per line (not one giant text block) so fadeInList() can fade each
            // line out under the header/controls like synced lyrics do.
            val listState = rememberLazyListState()
            val textLines = remember(lyrics.text) { lyrics.text.lines() }
            val rubyLines = lyrics.rubyLines?.takeIf { showRomanization }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                itemsIndexed(textLines) { index, textLine ->
                    val ruby = rubyLines?.getOrNull(index)
                    Box(modifier = Modifier.fillMaxWidth().fadeInList(listState, index, topFadePx, bottomFadePx)) {
                        if (ruby != null) UnsyncedRubyLine(ruby) else UnsyncedLine(textLine)
                    }
                }
            }
        }

        is ParsedLyrics.Synced -> {
            // -1 during an instrumental intro, before the first line starts — must stay -1, not
            // be clamped to 0, or line 0 lights up (big font, autoscroll, gray first word) early.
            val activeIndex = remember(lyrics, positionMs) {
                lyrics.lines.indexOfLast { it.timeMs <= positionMs }
            }
            val listState = rememberLazyListState()
            var lastAnchorBottomPx by remember { mutableStateOf(anchorBottomPx) }
            LaunchedEffect(activeIndex, anchorTopPx, anchorBottomPx) {
                val panelToggled = anchorBottomPx != lastAnchorBottomPx
                lastAnchorBottomPx = anchorBottomPx
                val info = listState.layoutInfo
                val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == activeIndex }
                if (itemInfo != null) {
                    // Anchored within the gap that's actually visible between the header and
                    // the control panel (or the screen bottom when the panel is hidden): dead
                    // center of it while the panel is up, a bit above center otherwise. No
                    // artificial padding backs this — near the start/end of a song the scroll
                    // simply clamps instead of faking empty space to reach the anchor.
                    val viewportHeight = info.viewportSize.height
                    val gap = (viewportHeight - anchorTopPx - anchorBottomPx).coerceAtLeast(0)
                    val fraction = if (anchorBottomPx > 0) 0.5f else 0.4f
                    val anchor = anchorTopPx + gap * fraction
                    // offset is measured from the end of the top content padding, not from the
                    // top of the list (viewportStartOffset == -beforeContentPadding).
                    val itemCenterOnScreen = itemInfo.offset + itemInfo.size / 2f - info.viewportStartOffset
                    // Panel show/hide: a quick, eased glide to the new spot. Line changes keep
                    // the default spring they've always had.
                    listState.animateScrollBy(
                        itemCenterOnScreen - anchor,
                        if (panelToggled) tween(320, easing = FastOutSlowInEasing) else spring(),
                    )
                } else {
                    // Big jump (e.g. track just changed) — land roughly nearby first.
                    listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                }
            }
            LazyColumn(
                state = listState,
                // No clipToBounds(): the list is inset by the screen's 22dp side padding, so a
                // hard clip at its bounds visibly cut the active line's glow off at the sides.
                // LazyColumn's own scroll clip still clips top/bottom but allows ~30dp of
                // sideways overdraw — enough to reach the screen edge. Glow bleeding upward
                // toward the header is a non-issue since fadeInList() takes rows there to alpha 0.
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val active = index == activeIndex
                    val alpha by animateFloatAsState(if (active) 1f else 0.35f, label = "lineAlpha")
                    // Every line is laid out at the sung size and inactive ones are only shrunk
                    // visually, so a line wraps the same way whether it's being sung or not —
                    // re-laying it out at a bigger font made words jump to the next row mid-song.
                    val scale by animateFloatAsState(if (active) 1f else INACTIVE_LYRIC_SCALE, label = "lineScale")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadeInList(listState, index, topFadePx, bottomFadePx)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onLineClick(line.timeMs) },
                    ) {
                        val nextLineStartMs = lyrics.lines.getOrNull(index + 1)?.timeMs
                        SungLine(
                            line = line,
                            active = active,
                            alpha = alpha,
                            scale = scale,
                            positionMs = smoothPositionMs,
                            nextLineStartMs = nextLineStartMs,
                            showRomanization = showRomanization,
                        )
                        line.secondary.forEach { secondary ->
                            if (secondary.voice != null && secondary.voice != line.voice) {
                                // The other singer, at the same moment: a full line on their side.
                                SungLine(
                                    line = secondary,
                                    active = active,
                                    alpha = alpha,
                                    scale = scale,
                                    positionMs = smoothPositionMs,
                                    nextLineStartMs = nextLineStartMs,
                                    showRomanization = showRomanization,
                                )
                            } else {
                                val alignEnd = line.voice == LyricVoice.V2
                                Box(modifier = Modifier.lyricScale(scale, alignEnd)) {
                                    SecondaryLyricLine(
                                        line = secondary,
                                        positionMs = smoothPositionMs,
                                        alpha = alpha,
                                        fontSize = (LYRIC_SIZE * 0.62f).sp,
                                        alignEnd = alignEnd,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Font size every synced lyric line is laid out at (the size of the line being sung). */
private const val LYRIC_SIZE = 31f

/** Lines not being sung are drawn at 28/31 of that, by scaling — see the item in [LyricsView]. */
private const val INACTIVE_LYRIC_SCALE = 28f / 31f

/** Visual-only scale, anchored to the side the line is aligned to so it shrinks toward it. */
private fun Modifier.lyricScale(scale: Float, alignEnd: Boolean): Modifier = graphicsLayer {
    scaleX = scale
    scaleY = scale
    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (alignEnd) 1f else 0f, 0.5f)
}

/**
 * One sung line: the line itself (karaoke sweep + glow while active; with romanization on, each
 * word gets its reading printed right above it) and its background vocals (smaller, dimmer,
 * below, with their own sweep). A `v2` line sits on the right, everything else on the left.
 */
@Composable
private fun SungLine(
    line: LyricLine,
    active: Boolean,
    alpha: Float,
    scale: Float,
    positionMs: Long,
    nextLineStartMs: Long?,
    showRomanization: Boolean,
) = Column(modifier = Modifier.fillMaxWidth().lyricScale(scale, line.voice == LyricVoice.V2)) {
    val fontSize = LYRIC_SIZE
    val alignEnd = line.voice == LyricVoice.V2
    val background = line.background
    val bottomPadding = if (background != null) 2.dp else 8.dp
    val readings = line.wordReadings?.takeIf { showRomanization }
    val ruby = line.ruby?.takeIf { showRomanization }
    when {
        line.words != null -> {
            // Word-synced lines keep the same per-word layout whether sung or not (only the
            // sweep is off), so their words wrap identically in both states.
            if (active) {
                // The glow pass is the exact same word-by-word composable, just recolored/blurred,
                // so it lines up pixel-for-pixel with the crisp text on top and sweeps forward in
                // lockstep instead of glowing ahead of what's been sung.
                Box {
                    WordSyncedLine(line, positionMs, alpha, fontSize.sp, nextLineStartMs, alignEnd, 8.dp, bottomPadding, readings = readings, glow = true)
                    WordSyncedLine(line, positionMs, alpha, fontSize.sp, nextLineStartMs, alignEnd, 8.dp, bottomPadding, readings = readings)
                }
            } else {
                WordSyncedLine(line, positionMs, alpha, fontSize.sp, nextLineStartMs, alignEnd, 8.dp, bottomPadding, readings = readings, sweep = false)
            }
        }
        ruby != null -> if (active) {
            Box {
                RubyLine(ruby, alpha, fontSize, alignEnd, bottomPadding, glow = true)
                RubyLine(ruby, alpha, fontSize, alignEnd, bottomPadding)
            }
        } else {
            RubyLine(ruby, alpha, fontSize, alignEnd, bottomPadding)
        }
        // Plain LRC has no per-word timing, so the whole active line glows at once.
        active -> Box {
            PlainLyricLine(line.text, alpha, fontSize, alignEnd, 8.dp, bottomPadding, glow = true)
            PlainLyricLine(line.text, alpha, fontSize, alignEnd, 8.dp, bottomPadding)
        }
        else -> PlainLyricLine(line.text, alpha, fontSize, alignEnd, 8.dp, bottomPadding)
    }
    if (background != null) {
        val mainLineEndMs = line.endTimeMs ?: line.words?.last()?.timeMs?.plus(400L) ?: nextLineStartMs ?: line.timeMs
        BackgroundVocals(background, active, alpha, fontSize, positionMs, mainLineEndMs, alignEnd)
    }
}

/** Background vocals: a small, dimmer line under the main one that grows a little from the
 * moment it starts until both it and the main line above it are done, then settles back. */
@Composable
private fun BackgroundVocals(
    background: LyricLine,
    lineActive: Boolean,
    alpha: Float,
    fontSize: Float,
    positionMs: Long,
    mainLineEndMs: Long,
    alignEnd: Boolean,
) {
    val words = background.words
    val startMs = words?.first()?.timeMs ?: background.timeMs
    val endMs = maxOf(background.endTimeMs ?: (words?.last()?.timeMs?.plus(400L)) ?: startMs, mainLineEndMs)
    val singing = positionMs in startMs..endMs
    // Same trick as whole lines: laid out at the grown size, shrunk visually when not singing.
    val baseSize = fontSize * 0.62f
    val grownSize = baseSize + 3f
    val scale by animateFloatAsState(if (singing) 1f else baseSize / grownSize, label = "backgroundScale")
    val backgroundAlpha = alpha * 0.85f
    Box(modifier = Modifier.lyricScale(scale, alignEnd)) {
        if (words != null) {
            WordSyncedLine(background, positionMs, backgroundAlpha, grownSize.sp, null, alignEnd, 6.dp, 8.dp, sweep = lineActive)
        } else {
            PlainLyricLine(background.text, backgroundAlpha, grownSize, alignEnd, 6.dp, 8.dp)
        }
    }
}

@Composable
private fun UnsyncedLine(text: String) {
    Text(
        text = text,
        color = PlayerColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun UnsyncedRubyLine(segments: List<RubySegment>) {
    FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        segments.forEach { segment ->
            Column(
                modifier = Modifier
                    .alignBy(LastBaseline)
                    .padding(end = if (segment.reading != null && !segment.text.last().isWhitespace()) 3.dp else 0.dp),
            ) {
                Text(
                    text = segment.reading ?: " ",
                    color = if (segment.reading == null) Color.Transparent else PlayerColors.TextPrimary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
                UnsyncedLine(segment.text)
            }
        }
    }
}

/** Size of a reading printed above its word, relative to the lyric's own font size. */
private const val READING_SCALE = 0.42f

@Composable
private fun ReadingText(reading: String?, fontSize: Float, glow: Boolean) {
    // Words without a reading still reserve the reading row, so every word in a row lines up.
    Text(
        text = reading ?: " ",
        color = if (glow || reading == null) Color.Transparent else PlayerColors.TextPrimary.copy(alpha = 0.8f),
        fontSize = (fontSize * READING_SCALE).sp,
        lineHeight = (fontSize * READING_SCALE * 1.2f).sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
    )
}

/** A romanized line without word timing: each word-sized piece with its reading above it. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RubyLine(
    segments: List<RubySegment>,
    alpha: Float,
    fontSize: Float,
    alignEnd: Boolean,
    bottomPadding: Dp,
    glow: Boolean = false,
) {
    val color = if (glow) PlayerColors.TextPrimary.copy(alpha = LYRIC_GLOW_ALPHA) else PlayerColors.TextPrimary
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(top = 8.dp, bottom = bottomPadding)
            .then(if (glow) Modifier.blur(LYRIC_GLOW_BLUR, BlurredEdgeTreatment.Unbounded) else Modifier),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            val text = if (index == segments.lastIndex) segment.text.trimEnd() else segment.text
            Column(
                modifier = Modifier
                    .alignBy(LastBaseline)
                    .padding(end = if (segment.reading != null && !segment.text.last().isWhitespace()) 4.dp else 0.dp),
            ) {
                ReadingText(segment.reading, fontSize, glow)
                Text(text = text, color = color, fontSize = fontSize.sp, lineHeight = fontSize.sp * 1.2f, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** A translation line sharing its parent's timestamp — smaller and dimmer underneath, as a
 * comment on the main line rather than a competing one. */
@Composable
private fun SecondaryLyricLine(
    line: LyricLine,
    positionMs: Long,
    alpha: Float,
    fontSize: androidx.compose.ui.unit.TextUnit,
    alignEnd: Boolean,
) {
    if (line.words != null) {
        WordSyncedLine(line, positionMs, alpha, fontSize, null, alignEnd, 8.dp, 8.dp)
    } else {
        Text(
            text = line.text,
            color = LocalAdaptiveSecondaryColor.current,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha)
                .padding(bottom = 6.dp),
        )
    }
}

/** Shared by the eLRC and plain-LRC glow passes so both look identical. */
private val LYRIC_GLOW_BLUR = 18.dp
private const val LYRIC_GLOW_ALPHA = 0.75f

@Composable
private fun PlainLyricLine(
    text: String,
    alpha: Float,
    fontSize: Float,
    alignEnd: Boolean,
    topPadding: Dp,
    bottomPadding: Dp,
    glow: Boolean = false,
) {
    Text(
        text = text,
        color = if (glow) PlayerColors.TextPrimary.copy(alpha = LYRIC_GLOW_ALPHA) else PlayerColors.TextPrimary,
        fontSize = fontSize.sp,
        lineHeight = fontSize.sp * 1.3f,
        fontWeight = FontWeight.ExtraBold,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(top = topPadding, bottom = bottomPadding)
            .then(if (glow) Modifier.blur(LYRIC_GLOW_BLUR, BlurredEdgeTreatment.Unbounded) else Modifier),
    )
}

/**
 * Renders a word-synced line the way Apple Music / Gramophone do it: rather than flipping each
 * word from dim to bright the instant its timestamp hits, the *currently singing* word sweeps
 * from bright to dim left-to-right as playback moves through its time window, so the highlight
 * looks like it travels through the word instead of jumping whole words at a time.
 *
 * [readings], when given, prints each word's romanization right above it. With [sweep] off every
 * word is one flat color (an inactive line that still needs the per-word layout for readings).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WordSyncedLine(
    line: LyricLine,
    positionMs: Long,
    alpha: Float,
    fontSize: androidx.compose.ui.unit.TextUnit,
    nextLineStartMs: Long?,
    alignEnd: Boolean,
    topPadding: Dp,
    bottomPadding: Dp,
    readings: List<String?>? = null,
    sweep: Boolean = true,
    glow: Boolean = false,
) {
    val words = line.words ?: return
    val sungColor = if (glow) PlayerColors.TextPrimary.copy(alpha = LYRIC_GLOW_ALPHA) else PlayerColors.TextPrimary
    // Not-yet-sung words *within the currently active line* stay fairly bright (unlike fully
    // inactive lines, which fade via the line-level `alpha`) — otherwise the whole active line
    // reads as dim/gray for most of its duration since only one short word is ever fully white.
    // In the glow pass, "not yet sung" is fully transparent instead — the glow must only sit
    // behind text that has actually been reached, never ahead of the sweep.
    val unsungColor = if (glow) Color.Transparent else PlayerColors.TextPrimary.copy(alpha = 0.55f)
    val sungStyle = TextStyle(color = sungColor, fontSize = fontSize, fontWeight = FontWeight.ExtraBold)
    val unsungStyle = TextStyle(color = unsungColor, fontSize = fontSize, fontWeight = FontWeight.ExtraBold)
    // Exactly one word is ever "in progress" at a time — found the same way the active *line* is
    // found (last word whose tag time has passed). Every other word is a flat solid color. This
    // guarantees only a single word animates even when the line wraps onto two visual rows, and
    // keeps already-sung/not-yet-sung words from flickering due to their own window's edge cases.
    val currentIndex = if (sweep) words.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0) else words.size
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(top = topPadding, bottom = bottomPadding)
            .then(if (glow) Modifier.blur(LYRIC_GLOW_BLUR, BlurredEdgeTreatment.Unbounded) else Modifier),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        words.forEachIndexed { index, word ->
            // The last word's trailing space would leave a gap against the right edge of a v2 line.
            val text = if (index == words.lastIndex) word.text.trimEnd() else word.text
            val reading = readings?.getOrNull(index)
            Column(
                modifier = Modifier
                    // Latin and Japanese glyphs sit at different heights in the same font; lining
                    // up the lyric text's baseline keeps "high" level with the kanji around it.
                    .then(if (readings != null) Modifier.alignBy(LastBaseline) else Modifier)
                    .padding(end = if (reading != null && text.lastOrNull()?.isWhitespace() == false) 4.dp else 0.dp),
            ) {
                if (readings != null) ReadingText(reading, fontSize.value, glow)
                when {
                    index < currentIndex -> Text(text = text, style = sungStyle)
                    index > currentIndex -> Text(text = text, style = unsungStyle)
                    else -> {
                        val wordEndMs = when {
                            index + 1 < words.size -> words[index + 1].timeMs
                            line.endTimeMs != null -> line.endTimeMs
                            nextLineStartMs != null -> nextLineStartMs
                            else -> word.timeMs + 400L
                        }
                        val progress = if (wordEndMs > word.timeMs) {
                            ((positionMs - word.timeMs).toFloat() / (wordEndMs - word.timeMs)).coerceIn(0f, 1f)
                        } else 1f
                        // A brush-based gradient here turned out to look gray throughout most of
                        // the word (its scaling wasn't behaving as a clean per-word wipe). Drawing
                        // the bright copy on top, hard-clipped to exactly `progress` of that word's
                        // own measured width, is a much more direct and reliably-correct reveal.
                        Box {
                            Text(text = text, style = unsungStyle)
                            Text(
                                text = text,
                                style = sungStyle,
                                modifier = Modifier.drawWithContent {
                                    clipRect(right = size.width * progress) { this@drawWithContent.drawContent() }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingMiniHeader(
    song: Song?,
    onCollapse: () -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCollapse() }
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            uri = song?.uri,
            size = ART_SIZE_THUMB,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = song?.title ?: "Ничего не играет",
                color = PlayerColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (song != null) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGoToAlbum(song) }
                    } else Modifier,
                ),
            )
            Text(
                text = song?.artist ?: "",
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (song != null) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGoToArtist(song) }
                    } else Modifier,
                ),
            )
        }
        trailing()
    }
}

/** The "⋮" next to the current song's title (Art view and Queue header). */
@Composable
private fun CurrentSongMenuButton(
    song: Song,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(start = 12.dp)) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Действия с треком",
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuExpanded = true },
        )
        SongActionsMenuPopup(
            song = song,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
        )
    }
}

/** Lyrics-view switch for the romanized lines — same see-through look as the Queue toggles. */
@Composable
private fun RomanizationToggle(enabled: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background((if (enabled) PlayerColors.AccentOnDark else PlayerColors.Surface).copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Translate,
            contentDescription = if (enabled) "Скрыть романизацию" else "Показать романизацию",
            tint = if (enabled) PlayerColors.AccentText else PlayerColors.TextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** One queue row plus a LazyColumn key that stays stable while rows are dragged around — the
 * same song can legitimately be queued twice, so the song id alone isn't unique. */
private class QueueEntry(val key: String, val song: Song)

private fun queueEntries(prefix: String, songs: List<Song>): List<QueueEntry> {
    val seen = HashMap<Long, Int>()
    return songs.map { song ->
        val occurrence = (seen[song.id] ?: 0) + 1
        seen[song.id] = occurrence
        QueueEntry("$prefix-${song.id}-$occurrence", song)
    }
}

private fun List<QueueEntry>.moved(fromKey: Any, toKey: Any): List<QueueEntry> {
    val from = indexOfFirst { it.key == fromKey }
    val to = indexOfFirst { it.key == toKey }
    if (from < 0 || to < 0) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private val QueueRemoveRed = Color(0xFFE5383B)

@Composable
private fun QueueList(
    manualQueue: List<Song>,
    continueQueue: List<Song>,
    onItemClick: (Song) -> Unit,
    onClearManualQueue: () -> Unit,
    onAddSongsClick: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (position: Int) -> Unit,
    onDragActiveChange: (Boolean) -> Unit,
    topFadePx: () -> Int,
    bottomFadePx: () -> Int,
    state: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    // Local copies so a drag can reorder rows live under the finger; the player's real queue is
    // only changed once, when the drag ends. Any change coming back from the player (the move
    // itself, a track change, infinite-play top-ups) resets them to the player's truth.
    var manualRows by remember(manualQueue) { mutableStateOf(queueEntries("m", manualQueue)) }
    var continueRows by remember(continueQueue) { mutableStateOf(queueEntries("c", continueQueue)) }
    val haptics = LocalHapticFeedback.current
    // Rows only reorder within their own section — "Очередь" and "Далее по очереди" have different
    // meanings in the player, so a drag can't cross the header between them.
    val reorderState = rememberReorderableLazyListState(state) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        when {
            fromKey.startsWith("m-") && toKey.startsWith("m-") -> manualRows = manualRows.moved(fromKey, toKey)
            fromKey.startsWith("c-") && toKey.startsWith("c-") -> continueRows = continueRows.moved(fromKey, toKey)
            else -> return@rememberReorderableLazyListState
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun commitMove(key: String) {
        val manual = key.startsWith("m-")
        val original = queueEntries(if (manual) "m" else "c", if (manual) manualQueue else continueQueue)
        val rows = if (manual) manualRows else continueRows
        val from = original.indexOfFirst { it.key == key }
        val to = rows.indexOfFirst { it.key == key }
        if (from < 0 || to < 0 || from == to) return
        val offset = if (manual) 0 else manualQueue.size
        onMove(offset + from, offset + to)
    }

    // Absolute positions of the fade-eligible rows among ALL items in this LazyColumn (needed by
    // fadeInList() below) — index 0 is the "Очередь" header row, 1 is "Добавить треки", so the
    // manual rows start at 2; continue rows start after those plus their own header row (only
    // present when there are any).
    val manualQueueStart = 2
    val continueQueueStart = manualQueueStart + manualRows.size + 1
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
    ) {
        item(key = "header-queue") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeInList(state, 0, topFadePx, bottomFadePx)
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Очередь", color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (manualQueue.isNotEmpty()) {
                    Text(
                        text = "Очистить",
                        color = LocalAdaptiveSecondaryColor.current,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClearManualQueue() },
                    )
                }
            }
        }
        item(key = "header-add") {
            AddSongsToQueueRow(onClick = onAddSongsClick, modifier = Modifier.fadeInList(state, 1, topFadePx, bottomFadePx))
        }
        itemsIndexed(manualRows, key = { _, entry -> entry.key }) { i, entry ->
            ReorderableItem(reorderState, key = entry.key) { isDragging ->
                SwipeToRemove(onRemove = { onRemove(manualQueue.indexOfFirstEntry(entry.key, "m")) }) {
                    QueueRow(
                        item = entry.song,
                        isDragging = isDragging,
                        onClick = { onItemClick(entry.song) },
                        dragHandleModifier = Modifier.draggableHandle(
                            onDragStarted = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragActiveChange(true)
                            },
                            onDragStopped = {
                                commitMove(entry.key)
                                onDragActiveChange(false)
                            },
                        ),
                        modifier = Modifier.fadeInList(state, manualQueueStart + i, topFadePx, bottomFadePx),
                    )
                }
            }
        }
        if (continueRows.isNotEmpty()) {
            item(key = "header-continue") {
                Text(
                    text = "Далее по очереди",
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fadeInList(state, manualQueueStart + manualRows.size, topFadePx, bottomFadePx)
                        .padding(top = 18.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(continueRows, key = { _, entry -> entry.key }) { i, entry ->
                ReorderableItem(reorderState, key = entry.key) { isDragging ->
                    SwipeToRemove(onRemove = {
                        onRemove(manualQueue.size + continueQueue.indexOfFirstEntry(entry.key, "c"))
                    }) {
                        QueueRow(
                            item = entry.song,
                            isDragging = isDragging,
                            onClick = { onItemClick(entry.song) },
                            dragHandleModifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDragActiveChange(true)
                                },
                                onDragStopped = {
                                    commitMove(entry.key)
                                    onDragActiveChange(false)
                                },
                            ),
                            modifier = Modifier.fadeInList(state, continueQueueStart + i, topFadePx, bottomFadePx),
                        )
                    }
                }
            }
        }
    }
}

/** Position of the row with [key] in the player's own (not locally reordered) list. */
private fun List<Song>.indexOfFirstEntry(key: String, prefix: String): Int =
    queueEntries(prefix, this).indexOfFirst { it.key == key }

/** Swipe left to remove: the row slides away over a red strip that only fills the part already
 * uncovered, so the row's own (transparent) content never sits on top of red. The label stays
 * put at the row's right edge and the strip just uncovers it, rather than sliding in with it. */
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onRemove()
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val revealedPx = -(runCatching { dismissState.requireOffset() }.getOrDefault(0f))
            if (revealedPx > 0f) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(with(LocalDensity.current) { revealedPx.toDp() })
                            .clip(RoundedCornerShape(10.dp))
                            .background(QueueRemoveRed),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "Убрать",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                // Measured at full width no matter how narrow the strip is, so
                                // it's never squeezed/re-laid-out; the strip's clip reveals it.
                                .wrapContentWidth(Alignment.End, unbounded = true)
                                .padding(end = 18.dp),
                        )
                    }
                }
            }
        },
    ) {
        content()
    }
}

@Composable
private fun AddSongsToQueueRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Same size/shape as a queue row's cover, same see-through fill as the toggle buttons above.
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PlayerColors.Surface.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(22.dp))
        }
        Text(
            text = "Добавить треки в очередь",
            color = PlayerColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun QueueRow(
    item: Song,
    isDragging: Boolean,
    onClick: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val liftFill = PlayerColors.Surface.copy(alpha = 0.55f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A picked-up row gets a card behind it, drawn wider than the row itself so the cover
            // and the drag handle both get the same breathing room instead of touching its edges.
            .drawBehind {
                if (isDragging) {
                    val inset = 10.dp.toPx()
                    drawRoundRect(
                        color = liftFill,
                        topLeft = Offset(-inset, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width + inset * 2, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    )
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            uri = item.uri,
            size = ART_SIZE_THUMB,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = item.title,
                color = PlayerColors.TextPrimary.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist,
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Перетащить",
            tint = LocalAdaptiveSecondaryColor.current,
            modifier = dragHandleModifier
                .padding(start = 12.dp, end = 4.dp)
                .size(22.dp),
        )
    }
}

@Composable
private fun QueueToggleRow(
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    infinitePlayEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QueueToggleButton(
            icon = Icons.Filled.Shuffle,
            description = "Перемешать",
            active = shuffleEnabled,
            onClick = onToggleShuffle,
            modifier = Modifier.weight(1f),
        )
        QueueToggleButton(
            icon = Icons.Filled.Repeat,
            description = "Повтор",
            active = repeatEnabled,
            onClick = onToggleRepeat,
            modifier = Modifier.weight(1f),
        )
        QueueToggleButton(
            icon = Icons.Filled.AllInclusive,
            description = "Бесконечное воспроизведение",
            active = infinitePlayEnabled,
            onClick = onToggleInfinitePlay,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QueueToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background((if (active) PlayerColors.AccentOnDark else PlayerColors.Surface).copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) PlayerColors.AccentText else PlayerColors.TextPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Full-screen search overlay for the Queue's "Добавить треки в очередь" row — same idea as the
 * Search tab, but tapping a result adds it to the end of the queue instead of playing it, and
 * the sheet stays open so you can queue up several before dismissing. */
@Composable
private fun AddToQueuePicker(songs: List<Song>, onAdd: (Song) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, songs) {
        val q = query.trim()
        if (q.isEmpty()) songs
        else songs.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }
    var addedIds by remember { mutableStateOf(emptySet<Long>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Добавить в очередь", color = PlayerColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    tint = PlayerColors.TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PlayerColors.Surface)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(18.dp))
                Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    if (query.isEmpty()) {
                        Text(text = "Название, исполнитель, альбом", color = PlayerColors.TextTertiary, fontSize = 15.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = PlayerColors.TextPrimary, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PlayerColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                itemsIndexed(results) { _, song ->
                    val added = song.id in addedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArt(uri = song.uri, size = ART_SIZE_THUMB, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)))
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(text = song.title, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = song.artist, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(
                            imageVector = if (added) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = if (added) "Добавлено" else "Добавить",
                            tint = if (added) PlayerColors.TextSecondary else PlayerColors.TextPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onAdd(song)
                                    addedIds = addedIds + song.id
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService<AudioManager>() }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1 }
    var volume by remember {
        mutableFloatStateOf((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat())
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.VolumeDown, contentDescription = null, tint = LocalAdaptiveSecondaryColor.current, modifier = Modifier.size(18.dp))
        MinimalSlider(
            value = volume,
            onValueChange = {
                volume = it
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
            },
            valueRange = 0f..maxVolume.toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        Icon(imageVector = Icons.Filled.VolumeUp, contentDescription = null, tint = LocalAdaptiveSecondaryColor.current, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BottomQuickActionsRow(
    modifier: Modifier = Modifier,
    lyricsActive: Boolean,
    queueActive: Boolean,
    onLyricsClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.FormatQuote,
            contentDescription = "Текст песни",
            tint = if (lyricsActive) PlayerColors.TextPrimary else LocalAdaptiveSecondaryColor.current,
            modifier = Modifier
                .size(24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLyricsClick() },
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(120.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDeviceClick() },
        ) {
            Icon(
                imageVector = Icons.Filled.Cast,
                contentDescription = "Устройство воспроизведения",
                tint = LocalAdaptiveSecondaryColor.current,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Это устройство",
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.QueueMusic,
            contentDescription = "Очередь",
            tint = if (queueActive) PlayerColors.TextPrimary else LocalAdaptiveSecondaryColor.current,
            modifier = Modifier
                .size(24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onQueueClick() },
        )
    }
}

private fun openOutputSwitcher(context: android.content.Context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            context.startActivity(Intent("android.settings.panel.action.media_output").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
