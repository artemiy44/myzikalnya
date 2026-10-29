package com.artemiy.player.ui.nowplaying

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.artemiy.player.ui.components.pressScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.Song
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.lyrics.hasRomanization
import com.artemiy.player.playback.OutputKind
import com.artemiy.player.playback.PlaySource
import com.artemiy.player.playback.SourceArt
import com.artemiy.player.playback.SourcePlace
import com.artemiy.player.ui.components.placeholderArtBrush
import com.artemiy.player.ui.components.rememberBlurredCollage
import com.artemiy.player.ui.home.drawMixMotif
import com.artemiy.player.ui.home.mixBrush
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.rememberTextMeasurer
import com.artemiy.player.playback.openOutputSwitcher
import com.artemiy.player.playback.rememberOutputDevice
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.PlayPauseIcon
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.components.marquee
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The "Экспрессивный" Now Playing style, after Material 3 Expressive: the cover across the whole
 * width, melting into the blurred background; a big left-aligned title; a wavy seek bar; three
 * chunky connected transport buttons; and a "Играет из …" bar at the bottom that pulls up into
 * the queue. No volume slider — that's part of the look. Everything else (background, lyrics,
 * queue, menus) is the same shared parts as the classic style.
 */

@Composable
internal fun ExpressiveNowPlaying(
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
    playingFrom: PlaySource?,
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
    backgroundMode: NowPlayingBackgroundMode,
    blurIntensity: LiveBlurIntensity,
    lyricsRomanization: Boolean,
    onToggleLyricsRomanization: () -> Unit,
    lyricsTapPlays: Boolean,
    lyricsLoading: Boolean,
    memory: NowPlayingMemory,
    /** How far the player is open (0 = closed, 1 = open) — it rides up from the bottom as a card. */
    expand: () -> Float = { 1f },
    /** Where the mini player bar is — the card grows out of it. */
    mini: com.artemiy.player.ui.components.MiniPlayerAnchors? = null,
    /** The "Диск" layout: three square buttons along the bottom and a plate that spins with the song. */
    dial: Boolean = false,
    /** "Диск" with its background in a card up top (otherwise over the whole screen). */
    dialCard: Boolean = true,
) {
    val cardLook = dial && dialCard
    val wide = !dial && LocalExpressiveWide.current
    var wideQueueOpen by remember { mutableStateOf(memory.expressiveQueueOpen) }
    var showLyrics by memory::expressiveLyrics
    var showAddToQueuePicker by remember { mutableStateOf(false) }
    // The "Диск" style has no bottom sheet: its queue is a panel opened by a button.
    var dialQueueOpen by remember { mutableStateOf(memory.expressiveQueueOpen) }
    // A lyric line held down: the share card opens from it.
    var shareFrom by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Opening: the app behind dims a little, and the mini player bar grows into the player — a
    // card with big round corners (in the bar's own color at first) stretching up to the top of
    // the screen, the corners straightening out as it arrives; its pieces then pop into place one
    // after another, each with a small overshoot (see popIn). Closing runs it all backwards.
    val miniColor = mini?.color ?: PlayerColors.SurfaceDim
    val bar = mini?.bar
    val active = LocalNowPlayingActive.current
    val cardCornerPx = with(density) { CARD_CORNER.toPx() }
    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (active) SCRIM_ALPHA * expand() else 0f }
            .background(Color.Black),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val e = if (active) expand() else 1f
                if (e < 1f) {
                    val from = bar ?: androidx.compose.ui.geometry.Rect(0f, size.height, size.width, size.height)
                    val top = from.top * (1f - e)
                    val bottom = from.bottom + (size.height - from.bottom) * e
                    // From the floating card's sides too (expressive tabs); the flat bar spans the width anyway.
                    val left = from.left * (1f - e)
                    val right = from.right + (size.width - from.right) * e
                    // Rounded all the way up, straightening only in the last moment of arriving.
                    val corner = cardCornerPx * ((1f - e) / CORNER_SETTLE).coerceAtMost(1f)
                    shape = androidx.compose.foundation.shape.GenericShape { _, _ ->
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                androidx.compose.ui.geometry.Rect(left, top, right, bottom),
                                androidx.compose.ui.geometry.CornerRadius(corner),
                            ),
                        )
                    }
                    clip = true
                    // Comes up over the mini player quickly, rather than covering it at once.
                    alpha = (e / CARD_FADE_IN).coerceAtMost(1f)
                }
            },
    ) {
    PlayerSurface(
        card = cardLook,
        song = song,
        mode = backgroundMode,
        intensity = blurIntensity,
        openFrom = bar,
        openProgress = { if (active) expand() else 1f },
        openTint = miniColor,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // A little extra lift, so the bar doesn't sit right on the screen's bottom edge.
            val barHeight = (if (dial) 8.dp else 78.dp) + navBottom
            // The wide look's three buttons stand at the foot; the queue sheet comes up from just above them.
            val widePillsArea = navBottom + 14.dp + WIDE_PILL_HEIGHT + 12.dp

            val dialQueuePanel: @Composable (Modifier) -> Unit = { panelModifier ->
                    DialQueuePanel(
                        // Lit modes wear the app's accent where the play tile does (the card, or no blur);
                        // over a full-screen blur they stay plain, like the white play tile.
                        accentModes = cardLook || backgroundMode == NowPlayingBackgroundMode.NONE,
                        manualQueue = manualQueue,
                        continueQueue = continueQueue,
                        shuffleEnabled = shuffleEnabled,
                        repeatEnabled = repeatEnabled,
                        infinitePlayEnabled = infinitePlayEnabled,
                        onQueueItemClick = onQueueItemClick,
                        onClearManualQueue = onClearManualQueue,
                        onMoveInQueue = onMoveInQueue,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onToggleInfinitePlay = onToggleInfinitePlay,
                        onAddSongsClick = { showAddToQueuePicker = true },
                        songActions = remember(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist) {
                            QueueSongActions(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
                        },
                        modifier = panelModifier,
                    )
                }

            if (dial) DialContent(
                song = song,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                showLyrics = showLyrics,
                lyrics = lyrics,
                lyricsRomanization = lyricsRomanization,
                lyricsLoading = lyricsLoading,
                lyricsTapPlays = lyricsTapPlays,
                onSeek = onSeek,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onLineLongClick = { shareFrom = it },
                expand = expand,
                active = active,
                barHeight = barHeight,
                statusTop = statusTop,
                playingFrom = playingFrom,
                card = cardLook,
                backgroundMode = backgroundMode,
                blurIntensity = blurIntensity,
                onClose = onClose,
                onToggleRomanization = onToggleLyricsRomanization,
                onToggleLyrics = { showLyrics = !showLyrics },
                onDeviceClick = { openOutputSwitcher(context) },
                queueOpen = dialQueueOpen,
                onQueueOpenChange = { dialQueueOpen = it; memory.expressiveQueueOpen = it },
                queuePanel = dialQueuePanel,
            ) else if (wide) WideContent(
                song = song,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                showLyrics = showLyrics,
                lyrics = lyrics,
                lyricsRomanization = lyricsRomanization,
                lyricsLoading = lyricsLoading,
                lyricsTapPlays = lyricsTapPlays,
                onSeek = onSeek,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onLineLongClick = { shareFrom = it },
                expand = expand,
                active = active,
                statusTop = statusTop,
                barHeight = widePillsArea + 16.dp,
            ) else Column(modifier = Modifier.fillMaxSize()) {
                // Everything above the title: the cover sitting at the bottom of this space, or the
                // lyrics filling all of it.
                Box(modifier = Modifier.fillMaxWidth().weight(1f).popIn(expand, active, order = 0, grow = true)) {
                    Crossfade(targetState = showLyrics, animationSpec = tween(300), label = "coverOrLyrics") { lyricsShown ->
                        if (lyricsShown) {
                            if (LocalNowPlayingActive.current) com.artemiy.player.ui.components.KeepScreenOn()
                            val topPx = with(density) { (statusTop + 36.dp).roundToPx() }
                            LyricsView(
                                lyrics = lyrics,
                                positionMs = positionMs,
                                isPlaying = isPlaying,
                                topFadePx = { topPx },
                                bottomFadePx = { 0 },
                                anchorTopPx = topPx,
                                anchorBottomPx = 0,
                                onLineClick = { timeMs ->
                                    onSeek(timeMs)
                                    if (lyricsTapPlays && !isPlaying) onTogglePlayPause()
                                },
                                onLineLongClick = { shareFrom = it },
                                showRomanization = lyricsRomanization,
                                loading = lyricsLoading,
                                contentPadding = PaddingValues(
                                    start = 24.dp,
                                    end = 24.dp,
                                    top = statusTop + 36.dp + FADE_SPAN,
                                    bottom = FADE_SPAN,
                                ),
                            )
                        } else {
                            // A rounded square as wide as the seek bar, centered in the space above
                            // the title; on pause it eases back a little and rounds off more.
                            val coverScale by animateFloatAsState(if (isPlaying) 1f else 0.92f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow), label = "coverScale")
                            val chosenCorner = LocalCoverCorner.current
                            val coverCorner by animateDpAsState(chosenCorner ?: if (isPlaying) 28.dp else 40.dp, spring(stiffness = Spring.StiffnessLow), label = "coverCorner")
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 24.dp, end = 24.dp, top = statusTop + 28.dp, bottom = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AlbumArt(
                                    uri = song?.uri,
                                    size = ART_SIZE_FULL,
                                    crossfade = true,
                                    modifier = Modifier
                                        .size(min(maxWidth, maxHeight))
                                        .graphicsLayer { scaleX = coverScale; scaleY = coverScale }
                                        .clip(RoundedCornerShape(coverCorner)),
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Box(modifier = Modifier.popIn(expand, active, order = 1)) {
                    ExpressiveTitleRow(
                        song = song,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                    )
                    }
                    WavySeekBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        onSeek = onSeek,
                        modifier = Modifier.padding(top = 14.dp).popIn(expand, active, order = 2),
                    )
                    ExpressiveTransport(
                        isPlaying = isPlaying,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        modifier = Modifier.padding(top = 18.dp).popIn(expand, active, order = 3),
                    )
                }
                // Some air between the buttons and the bottom bar.
                Spacer(modifier = Modifier.height(barHeight + 30.dp))
            }

            // Close handle and (with lyrics open) the romanization toggle, at the top of the screen.
            // (The "Диск" card draws its own, inside the card.)
            // (In the "Диск" full-screen look it gives way to the open queue panel, which covers it.)
            val handleAlpha by animateFloatAsState(if (dial && dialQueueOpen) 0f else 1f, tween(200), label = "closeHandle")
            if (!cardLook) Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + 10.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .graphicsLayer { alpha = handleAlpha }
                    .clip(RoundedCornerShape(3.dp))
                    .background(PlayerColors.TextPrimary.copy(alpha = 0.7f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (handleAlpha > 0.5f) onClose() },
            )
            if (!cardLook && showLyrics && lyrics?.hasRomanization() == true) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = statusTop + 4.dp, end = 16.dp)) {
                    RomanizationToggle(enabled = lyricsRomanization, onToggle = onToggleLyricsRomanization)
                }
            }

            if (wide) WidePills(
                lyricsActive = showLyrics,
                onLyricsClick = { showLyrics = !showLyrics },
                onDeviceClick = { openOutputSwitcher(context) },
                queueOpen = wideQueueOpen,
                onQueueClick = { wideQueueOpen = !wideQueueOpen },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navBottom + 14.dp).popIn(expand, active, order = 3),
            )

            if (!dial) QueueSheet(
                // Stops well short of the top, so reaching for the top of the open queue doesn't
                // land on the player itself and swipe the whole thing shut.
                fullHeight = maxHeight - statusTop - 64.dp,
                barHeight = if (wide) 0.dp else barHeight,
                playingFrom = playingFrom,
                song = song,
                lyricsActive = showLyrics,
                onLyricsClick = { showLyrics = !showLyrics },
                onDeviceClick = { openOutputSwitcher(context) },
                manualQueue = manualQueue,
                continueQueue = continueQueue,
                shuffleEnabled = shuffleEnabled,
                repeatEnabled = repeatEnabled,
                infinitePlayEnabled = infinitePlayEnabled,
                onQueueItemClick = onQueueItemClick,
                onClearManualQueue = onClearManualQueue,
                onMoveInQueue = onMoveInQueue,
                onRemoveFromQueue = onRemoveFromQueue,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onToggleInfinitePlay = onToggleInfinitePlay,
                onAddSongsClick = { showAddToQueuePicker = true },
                songActions = remember(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist) {
                    QueueSongActions(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
                },
                navBottom = navBottom,
                initiallyOpen = memory.expressiveQueueOpen,
                onOpenChange = { memory.expressiveQueueOpen = it; wideQueueOpen = it },
                pills = wide,
                externalOpen = wideQueueOpen,
                modifier = Modifier.align(Alignment.BottomCenter).popIn(expand, active, order = 4),
            )
        }

        if (showAddToQueuePicker) {
            AddToQueuePicker(
                songs = allSongs,
                actions = remember(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist) {
                    QueueSongActions(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
                },
                onDismiss = { showAddToQueuePicker = false },
            )
        }

        // The share card: a page over everything, like the queue picker.
        shareFrom?.let { start ->
            if (song != null && lyrics != null) ShareLyricsDialog(song, lyrics, start) { shareFrom = null } else shareFrom = null
        }
    }
    }
    }
}

/** Top corners of the player card while it's on its way up or down. */
private val CARD_CORNER = 32.dp

/** Over this first bit of the way the card fades in over the mini player. */
private const val CARD_FADE_IN = 0.1f

/** The card's corners straighten over this last bit of the way up. */
private const val CORNER_SETTLE = 0.04f

/** How dark the app behind gets under the open player. */
private const val SCRIM_ALPHA = 0.45f

/**
 * One piece of the expressive player coming in as it opens: the pieces take turns ([order] 0
 * first), each rising a little from below into place with a small springy overshoot and fading
 * in. [grow] instead scales it up from a bit smaller (the cover).
 */
internal fun Modifier.popIn(expand: () -> Float, active: Boolean, order: Int, grow: Boolean = false): Modifier = graphicsLayer {
    if (!active) return@graphicsLayer
    val start = POP_START + order * POP_STAGGER
    val t = ((expand() - start) / POP_SPAN).coerceIn(0f, 1f)
    if (t >= 1f) return@graphicsLayer
    val eased = easeOutBack(t)
    alpha = (t * 2.5f).coerceAtMost(1f)
    if (grow) {
        val scale = 0.82f + 0.18f * eased
        scaleX = scale
        scaleY = scale
    } else {
        translationY = (1f - eased) * POP_RISE.toPx()
    }
}

/** Past the target a little, then back — the springy "pop". */
private fun easeOutBack(t: Float): Float {
    val c1 = 1.6f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

private const val POP_START = 0.3f
private const val POP_STAGGER = 0.08f
private const val POP_SPAN = 0.42f
private val POP_RISE = 64.dp

/** The queue sheet reaches this far below the screen edge, so no seam can show under it. */
private val SHEET_OVERSCAN = 6.dp

@Composable
private fun ExpressiveTitleRow(
    song: Song?,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song?.title ?: stringResource(R.string.nothing_playing),
                color = PlayerColors.TextPrimary,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.marquee(LocalNowPlayingActive.current).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    song?.let(onGoToAlbum)
                },
            )
            Text(
                text = song?.artist ?: "",
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 19.sp,
                maxLines = 1,
                modifier = Modifier.marquee(LocalNowPlayingActive.current).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    song?.let(onGoToArtist)
                },
            )
            QualityBadge()
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

/**
 * Material 3 Expressive-style seek bar: the played part is a wave (flowing while music plays,
 * flattening out on pause), the thumb a short vertical bar, the rest a plain thin line ending in a
 * dot. Tap or drag anywhere on it; seeks on release. Time left is shown as "-m:ss".
 */
@Composable
private fun WavySeekBar(positionMs: Long, durationMs: Long, isPlaying: Boolean, onSeek: (Long) -> Unit, modifier: Modifier = Modifier, showTimes: Boolean = true, knob: Boolean = false, height: androidx.compose.ui.unit.Dp? = null) {
    val safeDuration = max(durationMs, 1L).toFloat()
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: (positionMs / safeDuration).coerceIn(0f, 1f)
    val onSeekNow by rememberUpdatedState(onSeek)
    val durationNow by rememberUpdatedState(safeDuration)

    val amplitude by animateDpAsState(if (isPlaying) 3.dp else 0.dp, tween(400), label = "waveAmplitude")
    val phase by if (isPlaying && LocalNowPlayingActive.current) {
        rememberInfiniteTransition(label = "wave").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
            label = "wavePhase",
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val played = PlayerColors.TextPrimary
    val rest = PlayerColors.TextPrimary.copy(alpha = 0.3f)

    Column(
        modifier = if (height != null) modifier.height(height) else modifier,
        verticalArrangement = if (height != null) Arrangement.SpaceBetween else Arrangement.Top,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (knob) 22.dp else 34.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekNow((f * durationNow).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragFraction?.let { onSeekNow((it * durationNow).toLong()) }
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null },
                    ) { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                },
        ) {
            val mid = size.height / 2
            val stroke = 4.dp.toPx()
            // The wide look ends the wave in a round knob (a bigger gap around it) instead of the short bar.
            val knobRadius = 9.dp.toPx()
            val gap = if (knob) knobRadius + 3.dp.toPx() else 5.dp.toPx()
            val edge = if (knob) knobRadius else stroke
            val thumbX = (size.width * fraction).coerceIn(edge, size.width - edge)
            val amp = amplitude.toPx()
            val wavelength = 40.dp.toPx()

            val waveEnd = thumbX - gap
            if (waveEnd > 0f) {
                // Calm at the start of the bar, swelling to a touch above the usual height by the
                // time it reaches the thumb (the ramp is measured from the left edge).
                val ramp = 170.dp.toPx()
                // …and rising once more over the last stretch before the thumb: the crests get taller toward the end.
                val tailLength = 120.dp.toPx()
                val tailStart = (waveEnd - tailLength).coerceAtLeast(0f)
                fun swell(px: Float): Float {
                    val t = (px / ramp).coerceIn(0f, 1f)
                    val tail = ((px - tailStart) / tailLength).coerceIn(0f, 1f)
                    return (0.2f + 0.95f * (t * t * (3f - 2f * t))) * (1f + 0.45f * (tail * tail * (3f - 2f * tail)))
                }
                val path = Path()
                var x = 0f
                path.moveTo(0f, mid + amp * swell(0f) * sin(phase))
                while (x < waveEnd) {
                    x = (x + 2f).coerceAtMost(waveEnd)
                    path.lineTo(x, mid + amp * swell(x) * sin(2f * PI.toFloat() * x / wavelength + phase))
                }
                drawPath(path, played, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val restStart = thumbX + gap
            if (restStart < size.width) {
                drawLine(rest, Offset(restStart, mid), Offset(size.width - stroke / 2, mid), strokeWidth = stroke, cap = StrokeCap.Round)
                drawCircle(played, radius = stroke * 0.6f, center = Offset(size.width - stroke / 2, mid))
            }
            if (knob) drawCircle(played, radius = knobRadius, center = Offset(thumbX, mid))
            else drawLine(played, Offset(thumbX, mid - 15.dp.toPx()), Offset(thumbX, mid + 15.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        if (showTimes) Row(modifier = Modifier.fillMaxWidth().padding(top = if (knob) 2.dp else 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val shownMs = (fraction * safeDuration).toLong()
            Text(text = formatMs(shownMs), color = LocalAdaptiveSecondaryColor.current, fontSize = 12.sp)
            Text(text = "-" + formatMs((durationMs - shownMs).coerceAtLeast(0)), color = LocalAdaptiveSecondaryColor.current, fontSize = 12.sp)
        }
    }
}

/**
 * Three chunky connected buttons, Material 3 Expressive style: the pressed one widens while its
 * neighbours give way and its corners tighten; the big white middle one also changes shape
 * between play (rounder) and pause (squarer).
 */
@Composable
private fun ExpressiveTransport(
    isPlaying: Boolean,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val prevPressed by prevInteraction.collectIsPressedAsState()
    val playPressed by playInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()
    val bounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val prevWeight by animateFloatAsState(if (prevPressed) 1.35f else 1f, bounce, label = "prevWeight")
    val playWeight by animateFloatAsState(if (playPressed) 2.3f else 2f, bounce, label = "playWeight")
    val nextWeight by animateFloatAsState(if (nextPressed) 1.35f else 1f, bounce, label = "nextWeight")
    val sideFill = PlayerColors.TextPrimary.copy(alpha = 0.14f)

    Row(modifier = modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        ExpressiveButton(
            weight = prevWeight,
            // The side facing play matches play's own corners while a song plays.
            corner = when {
                prevPressed -> 14.dp
                isPlaying -> 20.dp
                else -> 28.dp
            },
            // While a song plays, the outer side rounds off further — the row reads as one pill.
            outerCorner = when {
                prevPressed -> 16.dp
                isPlaying -> 42.dp
                else -> 28.dp
            },
            outerStart = true,
            fill = sideFill,
            interaction = prevInteraction,
            onClick = onSkipPrevious,
        ) {
            Icon(AppIcons.SkipBack, contentDescription = stringResource(R.string.cd_previous_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
        ExpressiveButton(
            weight = playWeight,
            corner = when {
                playPressed -> 14.dp
                // Playing: a squarer rounded rectangle; paused: a round pill.
                isPlaying -> 20.dp
                else -> 42.dp
            },
            fill = PlayerColors.TextPrimary,
            interaction = playInteraction,
            onClick = onTogglePlayPause,
        ) {
            PlayPauseIcon(isPlaying = isPlaying, tint = PlayerColors.Background, modifier = Modifier.size(34.dp))
        }
        ExpressiveButton(
            weight = nextWeight,
            // The side facing play matches play's own corners while a song plays.
            corner = when {
                nextPressed -> 14.dp
                isPlaying -> 20.dp
                else -> 28.dp
            },
            outerCorner = when {
                nextPressed -> 16.dp
                isPlaying -> 42.dp
                else -> 28.dp
            },
            outerStart = false,
            fill = sideFill,
            interaction = nextInteraction,
            onClick = onSkipNext,
        ) {
            Icon(AppIcons.SkipForward, contentDescription = stringResource(R.string.cd_next_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ExpressiveButton(
    weight: Float,
    corner: androidx.compose.ui.unit.Dp,
    fill: Color,
    /** The corners on the row's outer edge, when they differ from [corner]. */
    outerCorner: androidx.compose.ui.unit.Dp = corner,
    /** Whether the outer edge is this button's start (left) side, or its end. */
    outerStart: Boolean = true,
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val animatedCorner by animateDpAsState(corner, spring(stiffness = Spring.StiffnessMediumLow), label = "buttonCorner")
    val animatedOuter by animateDpAsState(outerCorner, spring(stiffness = Spring.StiffnessMediumLow), label = "buttonOuterCorner")
    val start = if (outerStart) animatedOuter else animatedCorner
    val end = if (outerStart) animatedCorner else animatedOuter
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxSize()
            // Drawn as a shape rather than clipped to one: a clip with unequal corners (these side buttons)
            // is a path clip, which some older GPUs (the Adreno 3xx of old phones) draw flickering or not at all.
            .background(fill, RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end))
            .clickable(interactionSource = interaction, indication = null, onClick = buttonTicked(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        // The side with the rounder corners has less shape: the middle of the button's mass sits toward the squarer
        // side, and an icon in the exact middle of the box looks pushed to the rounded edge. Nudged back to where the
        // eye puts the centre (the missing area of the corners, over the button's height of about 80 dp).
        val centreShift = 0.215f * (start.value * start.value - end.value * end.value) / 80f
        Box(modifier = Modifier.graphicsLayer { translationX = centreShift.dp.toPx() }) { content() }
    }
}

/**
 * The bottom "Играет из …" bar, which pulls up (or opens on tap) into a sheet over the player
 * holding the queue — the same queue as the classic style's, toggles and all.
 */
@Composable
private fun QueueSheet(
    fullHeight: androidx.compose.ui.unit.Dp,
    barHeight: androidx.compose.ui.unit.Dp,
    playingFrom: PlaySource?,
    song: Song?,
    lyricsActive: Boolean,
    onLyricsClick: () -> Unit,
    onDeviceClick: () -> Unit,
    manualQueue: List<Song>,
    continueQueue: List<Song>,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    infinitePlayEnabled: Boolean,
    onQueueItemClick: (Song) -> Unit,
    onClearManualQueue: () -> Unit,
    onMoveInQueue: (from: Int, to: Int) -> Unit,
    onRemoveFromQueue: (position: Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    onAddSongsClick: () -> Unit,
    songActions: QueueSongActions,
    navBottom: androidx.compose.ui.unit.Dp,
    initiallyOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    /** The "cover across the top" look: no bar of its own (the buttons that open it stay put at the screen's foot), and it's opened from outside. */
    pills: Boolean = false,
    /** With [pills]: whether the buttons want the sheet open. */
    externalOpen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val openness = remember { Animatable(if (initiallyOpen) 1f else 0f) }
    val travelPx = with(density) { (fullHeight - barHeight).toPx() }.coerceAtLeast(1f)

    fun settle(open: Boolean) {
        onOpenChange(open)
        scope.launch {
            // A tiny visibility threshold: the default stops the spring a few pixels short of its end.
            openness.animateTo(if (open) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 0.00005f))
        }
    }
    BackHandler(enabled = openness.targetValue > 0.5f && LocalNowPlayingActive.current) { settle(false) }
    if (pills) LaunchedEffect(externalOpen) { if (externalOpen != openness.targetValue > 0.5f) settle(externalOpen) }

    // The wide look: pulling the list down while it is at its top pulls the sheet down with it (and closes it),
    // so the whole sheet is a handle, not just its top edge — pulling there would otherwise close the player.
    val pullToClose = remember(travelPx) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (available.y < 0f && openness.value < 1f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                    scope.launch { openness.snapTo((openness.value - available.y / travelPx).coerceIn(0f, 1f)) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (available.y > 0f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                    scope.launch { openness.snapTo((openness.value - available.y / travelPx).coerceIn(0f, 1f)) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (openness.value in 0.001f..0.999f) {
                    settle(if (available.y > 600f) false else if (available.y < -600f) true else openness.value > 0.5f)
                    return available
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    val dragState = rememberDraggableState { delta ->
        scope.launch { openness.snapTo((openness.value - delta / travelPx).coerceIn(0f, 1f)) }
    }
    // In the cover's own color (a Material-style tone of it): lightly tinted and see-through
    // while closed, a solid sheet of it once pulled up.
    val tone by androidx.compose.animation.animateColorAsState(
        rememberArtTone(song?.uri, com.artemiy.player.ui.theme.LocalPlayerPalette.current.isLight),
        tween(600),
        label = "queueSheetTone",
    )
    val sheetFill = lerp(tone.copy(alpha = if (pills) 0f else 0.4f), tone.copy(alpha = 0.97f), openness.value)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(fullHeight + SHEET_OVERSCAN)
            .offset { IntOffset(0, ((1f - openness.value) * travelPx).roundToInt() + SHEET_OVERSCAN.roundToPx()) }
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(sheetFill)
            .padding(bottom = SHEET_OVERSCAN),
    ) {
        // The bar itself: handle + "Играет из" row. Dragging it moves the sheet; a tap toggles.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        settle(
                            when {
                                velocity < -600f -> true
                                velocity > 600f -> false
                                else -> openness.value > 0.5f
                            },
                        )
                    },
                )
                .then(
                    if (pills) Modifier else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        settle(openness.targetValue < 0.5f)
                    },
                )
                .padding(horizontal = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .graphicsLayer { if (pills) alpha = openness.value }
                    .clip(RoundedCornerShape(2.dp))
                    .background(PlayerColors.TextPrimary.copy(alpha = 0.35f)),
            )
            // A taller strip to take hold of (the handle alone is thin, and a miss closes the player instead).
            if (pills) Spacer(modifier = Modifier.height(36.dp))
            if (!pills) Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceArtThumb(art = playingFrom?.art, song = song, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)))
                val playingFromLabel = stringResource(R.string.playing_from)
                val queueLabel = stringResource(R.string.playing_from_queue)
                Text(
                    text = buildAnnotatedString {
                        // The resource's trailing space gets trimmed at build time, so it is put back here.
                        append(playingFromLabel.trimEnd())
                        if (!playingFromLabel.endsWith('：')) append(' ')
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PlayerColors.TextPrimary)) {
                            append(playingFrom?.name ?: queueLabel)
                        }
                    },
                    color = LocalAdaptiveSecondaryColor.current,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                // On: a soft round patch swells up behind the icon (springy), so it is plain that the lyrics are open.
                val lyricsGlow by animateFloatAsState(
                    targetValue = if (lyricsActive) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "lyricsGlow",
                )
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(40.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = buttonTicked(onLyricsClick)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer {
                                scaleX = 0.5f + 0.5f * lyricsGlow
                                scaleY = 0.5f + 0.5f * lyricsGlow
                                alpha = lyricsGlow.coerceIn(0f, 1f)
                            }
                            .background(PlayerColors.TextPrimary.copy(alpha = 0.18f), androidx.compose.foundation.shape.CircleShape),
                    )
                    Icon(
                        imageVector = AppIcons.LyricsBubble,
                        contentDescription = stringResource(R.string.lyrics),
                        tint = if (lyricsActive) PlayerColors.TextPrimary else LocalAdaptiveSecondaryColor.current,
                        modifier = Modifier.size(24.dp),
                    )
                }
                val output = rememberOutputDevice(active = LocalNowPlayingActive.current)
                Icon(
                    imageVector = when (output.kind) {
                        OutputKind.PHONE -> AppIcons.DevicePhone
                        OutputKind.HEADPHONES -> AppIcons.DeviceHeadphones
                        OutputKind.EARBUDS -> AppIcons.DeviceEarbuds
                        OutputKind.SPEAKER -> AppIcons.DeviceSpeaker
                        OutputKind.BLUETOOTH -> AppIcons.DeviceBluetooth
                        OutputKind.USB -> AppIcons.DeviceUsb
                        OutputKind.TV -> AppIcons.DeviceTv
                        OutputKind.CAR -> AppIcons.DeviceCar
                    },
                    contentDescription = output.name ?: stringResource(R.string.this_device),
                    tint = if (output.kind == OutputKind.PHONE) LocalAdaptiveSecondaryColor.current else PlayerColors.TextPrimary,
                    modifier = Modifier
                        .padding(start = 18.dp)
                        .size(24.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = buttonTicked(onDeviceClick)),
                )
            }
        }

        // The queue — only built once the sheet starts opening.
        if (openness.value > 0.001f) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp).then(if (pills) Modifier.nestedScroll(pullToClose) else Modifier)) {
                // The list runs on behind the play modes (they are see-through): the rows fade out only about half way down them.
                val modesRoom = navBottom + 16.dp + 56.dp
                val fadeEdgePx = with(density) { (navBottom + 16.dp + 38.dp).roundToPx() }
                QueueList(
                    manualQueue = manualQueue,
                    continueQueue = continueQueue,
                    onItemClick = onQueueItemClick,
                    onClearManualQueue = onClearManualQueue,
                    onAddSongsClick = onAddSongsClick,
                    onMove = onMoveInQueue,
                    onRemove = onRemoveFromQueue,
                    onDragActiveChange = {},
                    songActions = songActions,
                    topFadePx = { 0 },
                    bottomFadePx = { fadeEdgePx },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = modesRoom + 8.dp),
                    fadeSpan = 36.dp,
                )
                // The play modes under the list, in the same chunky connected style as the
                // transport buttons, lit in the cover's color.
                // A veil of the sheet's own colour under the buttons, thickest at the foot: the rows melt into it rather than
                // showing through the buttons as words.
                val veilHeight = modesRoom + 36.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(veilHeight)
                        .drawBehind {
                            val side = 20.dp.toPx()
                            drawRect(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    0f to tone.copy(alpha = 0f),
                                    0.4f to tone.copy(alpha = 0.8f),
                                    0.6f to tone.copy(alpha = 0.97f),
                                    1f to tone.copy(alpha = 0.97f),
                                ),
                                topLeft = Offset(-side, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width + 2 * side, size.height),
                            )
                        },
                )
                ExpressiveQueueModes(
                    song = song,
                    shuffleEnabled = shuffleEnabled,
                    repeatEnabled = repeatEnabled,
                    infinitePlayEnabled = infinitePlayEnabled,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleInfinitePlay = onToggleInfinitePlay,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navBottom + 16.dp),
                )
            }
        }
    }
}

/** The "Диск" queue panel wears the app theme's colors rather than the player's (and the cover's). */
@Composable
internal fun QueueSheetPalette(appPalette: com.artemiy.player.ui.theme.PlayerPalette, content: @Composable () -> Unit) {
    com.artemiy.player.ui.theme.PaletteScope(appPalette) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAdaptiveSecondaryColor provides appPalette.textSecondary, content = content)
    }
}

/** The "Играет из" picture — see [SourceArt]. Unknown source: the current song's cover. */
@Composable
private fun SourceArtThumb(art: SourceArt?, song: Song?, modifier: Modifier = Modifier) {
    when (art) {
        is SourceArt.Cover -> AlbumArt(uri = art.uri, size = ART_SIZE_THUMB, modifier = modifier)
        is SourceArt.Collage -> {
            val collage = rememberBlurredCollage(art.songs)
            Box(modifier = modifier.background(placeholderArtBrush())) {
                if (collage != null) {
                    Image(
                        bitmap = remember(collage) { collage.asImageBitmap() },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        is SourceArt.MixCard -> {
            val textMeasurer = rememberTextMeasurer()
            Box(
                modifier = modifier
                    .background(mixBrush(art.colorIndex))
                    .drawBehind { drawMixMotif(art.motif, textMeasurer) },
            )
        }
        is SourceArt.Place -> Box(
            modifier = modifier.background(PlayerColors.TextPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (art.place) {
                    SourcePlace.SONGS -> AppIcons.Songs
                    SourcePlace.LIBRARY -> AppIcons.ViewGrid
                    SourcePlace.SEARCH -> AppIcons.Search
                    SourcePlace.MOOD -> AppIcons.Mood
                    SourcePlace.QUICK_PICKS -> AppIcons.Home
                    SourcePlace.RECENTLY_ADDED -> AppIcons.Add
                    SourcePlace.RECAP -> AppIcons.Sort
                },
                contentDescription = null,
                tint = PlayerColors.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        null -> AlbumArt(uri = song?.uri, size = ART_SIZE_THUMB, modifier = modifier)
    }
}

/**
 * A Material-style container color from the cover: its main hue (by the most colorful pixels),
 * toned down — deep on a dark player, pale on a light one — so the text on it stays readable.
 * A black-and-white cover gives a neutral gray.
 */
@Composable
private fun rememberArtTone(uri: android.net.Uri?, light: Boolean, accent: Boolean = false): Color {
    val bitmap = com.artemiy.player.ui.components.rememberAlbumArtBitmap(uri)
    val neutral = when {
        accent -> PlayerColors.TextPrimary
        light -> Color(0xFFE6E6EA)
        else -> Color(0xFF2A2A2E)
    }
    val tone by androidx.compose.runtime.produceState(neutral, bitmap, light, accent) {
        val bmp = bitmap ?: return@produceState
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { artTone(bmp, light, accent) } ?: neutral
    }
    return tone
}

private fun artTone(source: android.graphics.Bitmap, light: Boolean, accent: Boolean): Color? {
    val bmp = if (source.config == android.graphics.Bitmap.Config.HARDWARE) {
        source.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return null
    } else source
    val hsv = FloatArray(3)
    var x = 0.0
    var y = 0.0
    var satSum = 0.0
    var weightSum = 0.0
    val steps = 24
    for (i in 0 until steps) for (j in 0 until steps) {
        val px = bmp.getPixel(i * (bmp.width - 1) / (steps - 1), j * (bmp.height - 1) / (steps - 1))
        android.graphics.Color.colorToHSV(px, hsv)
        // Colorful, not-too-dark pixels decide the hue.
        val weight = (hsv[1] * hsv[2]).toDouble().let { it * it }
        if (weight < 0.01) continue
        val angle = Math.toRadians(hsv[0].toDouble())
        x += kotlin.math.cos(angle) * weight
        y += kotlin.math.sin(angle) * weight
        satSum += hsv[1] * weight
        weightSum += weight
    }
    if (weightSum < 0.5) return null
    val hue = ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0).toFloat()
    val saturation = (satSum / weightSum).toFloat()
    return when {
        // The bright version, for things lit up on top of the container tone.
        accent && light -> Color.hsl(hue, (saturation * 0.9f).coerceIn(0.35f, 0.75f), 0.4f)
        accent -> Color.hsl(hue, (saturation * 0.9f).coerceIn(0.35f, 0.8f), 0.74f)
        light -> Color.hsl(hue, (saturation * 0.55f).coerceIn(0.15f, 0.5f), 0.86f)
        else -> Color.hsl(hue, (saturation * 0.6f).coerceIn(0.15f, 0.55f), 0.22f)
    }
}

/**
 * Shuffle / repeat / endless play for the expressive queue: three connected buttons like the
 * transport ones. Off: see-through and round; on: filled in the cover's bright color and a bit
 * squarer. The pressed one widens a little while the others make way.
 */
@Composable
private fun ExpressiveQueueModes(
    song: Song?,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    infinitePlayEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val light = com.artemiy.player.ui.theme.LocalPlayerPalette.current.isLight
    val accent by androidx.compose.animation.animateColorAsState(rememberArtTone(song?.uri, light, accent = true), tween(600), label = "modesAccent")
    val onAccent = if (light) Color.White else Color(0xFF141416)
    val offFill = PlayerColors.TextPrimary.copy(alpha = 0.12f)
    val bounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    Row(modifier = modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(
            Triple(AppIcons.Shuffle to stringResource(R.string.shuffle), shuffleEnabled, onToggleShuffle),
            Triple(AppIcons.Repeat to stringResource(R.string.repeat), repeatEnabled, onToggleRepeat),
            Triple(AppIcons.Infinite to stringResource(R.string.endless_play), infinitePlayEnabled, onToggleInfinitePlay),
        ).forEachIndexed { index, (iconAndLabel, enabled, onClick) ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val weight by animateFloatAsState(if (pressed) 1.25f else 1f, bounce, label = "modeWeight")
            val fill by androidx.compose.animation.animateColorAsState(if (enabled) accent else offFill, tween(220), label = "modeFill")
            val tint by androidx.compose.animation.animateColorAsState(if (enabled) onAccent else PlayerColors.TextPrimary, tween(220), label = "modeTint")
            val inner = when {
                pressed -> 10.dp
                enabled -> 14.dp
                else -> 28.dp
            }
            ExpressiveButton(
                weight = weight,
                corner = inner,
                // The row's outer ends stay fully round.
                outerCorner = if (index == 1) inner else if (pressed) 14.dp else 28.dp,
                outerStart = index == 0,
                fill = fill,
                interaction = interaction,
                onClick = onClick,
            ) {
                Icon(iconAndLabel.first, contentDescription = iconAndLabel.second, tint = tint, modifier = Modifier.size(22.dp))
                if (index == 1 && LocalRepeatOne.current) {
                    Text("1", color = tint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/**
 * The expressive player's second look: the cover as in the usual one, the title and artist centred under it, then a dark
 * card with the play key, the skip keys and the wavy bar. The three small buttons under the card are [WidePills].
 */
@Composable
private fun WideContent(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    showLyrics: Boolean,
    lyrics: ParsedLyrics?,
    lyricsRomanization: Boolean,
    lyricsLoading: Boolean,
    lyricsTapPlays: Boolean,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onLineLongClick: (Int) -> Unit,
    expand: () -> Float,
    active: Boolean,
    statusTop: androidx.compose.ui.unit.Dp,
    barHeight: androidx.compose.ui.unit.Dp,
) {
    val density = LocalDensity.current
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).popIn(expand, active, order = 0, grow = true)) {
            Crossfade(targetState = showLyrics, animationSpec = tween(300), label = "wideCoverOrLyrics") { lyricsShown ->
                if (lyricsShown) {
                    if (LocalNowPlayingActive.current) com.artemiy.player.ui.components.KeepScreenOn()
                    val topPx = with(density) { (statusTop + 36.dp).roundToPx() }
                    val reachPx = with(density) { 20.dp.roundToPx() }
                    // The list goes on 20 dp below this space, behind the title (which is drawn after it), so the last line is
                    // not cut off in the middle above the title but fades out just as it reaches it.
                    Box(
                        modifier = Modifier.layout { measurable, constraints ->
                            val taller = constraints.maxHeight + reachPx
                            val placeable = measurable.measure(constraints.copy(minHeight = taller, maxHeight = taller))
                            layout(placeable.width, constraints.maxHeight) { placeable.place(0, 0) }
                        },
                    ) {
                        LyricsView(
                            lyrics = lyrics,
                            positionMs = positionMs,
                            isPlaying = isPlaying,
                            topFadePx = { topPx },
                            bottomFadePx = { 0 },
                            anchorTopPx = topPx,
                            anchorBottomPx = 0,
                            onLineClick = { timeMs ->
                                onSeek(timeMs)
                                if (lyricsTapPlays && !isPlaying) onTogglePlayPause()
                            },
                            onLineLongClick = onLineLongClick,
                            showRomanization = lyricsRomanization,
                            loading = lyricsLoading,
                            // The text goes on almost to the title (a short fade at the foot, not the long one).
                            bottomFadeSpan = 28.dp,
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = statusTop + 36.dp + FADE_SPAN, bottom = 8.dp),
                        )
                    }
                } else {
                    // Like the usual expressive look: a rounded square as wide as the card, in the middle of the space above the title.
                    val coverScale by animateFloatAsState(if (isPlaying) 1f else 0.92f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow), label = "wideCoverScale")
                    val chosenCorner = LocalCoverCorner.current
                    val coverCorner by animateDpAsState(chosenCorner ?: if (isPlaying) 28.dp else 40.dp, spring(stiffness = Spring.StiffnessLow), label = "wideCoverCorner")
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize().padding(start = 38.dp, end = 38.dp, top = statusTop + 28.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AlbumArt(
                            uri = song?.uri,
                            size = ART_SIZE_FULL,
                            crossfade = true,
                            modifier = Modifier
                                .size(min(maxWidth, maxHeight))
                                .graphicsLayer { scaleX = coverScale; scaleY = coverScale }
                                .clip(RoundedCornerShape(coverCorner)),
                        )
                    }
                }
            }
        }

        // The title and artist, in the middle, on the melt of the cover.
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 4.dp).popIn(expand, active, order = 1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = song?.title ?: stringResource(R.string.nothing_playing),
                color = PlayerColors.TextPrimary,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().marquee(LocalNowPlayingActive.current).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    song?.let(onGoToAlbum)
                },
            )
            Text(
                text = song?.artist ?: "",
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().marquee(LocalNowPlayingActive.current).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    song?.let(onGoToArtist)
                },
            )
            QualityBadge()
        }

        // The card: the play pill and "next" on the first row, "previous" and the bar on the second.
        val dark = !com.artemiy.player.ui.theme.LocalPlayerPalette.current.isLight
        val cardFill = if (dark) Color.Black.copy(alpha = 0.34f) else PlayerColors.TextPrimary.copy(alpha = 0.08f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp)
                .padding(top = 16.dp)
                .popIn(expand, active, order = 2)
                .background(cardFill, RoundedCornerShape(32.dp))
                .padding(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val playInteraction = remember { MutableInteractionSource() }
                val playPressed by playInteraction.collectIsPressedAsState()
                val playCorner by animateDpAsState(if (playPressed) 18.dp else if (isPlaying) 26.dp else 34.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "widePlayCorner")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(WIDE_KEY)
                        .pressScale(playInteraction, pressedScale = 0.97f)
                        .background(PlayerColors.TextPrimary, RoundedCornerShape(playCorner))
                        .clickable(interactionSource = playInteraction, indication = null, onClick = buttonTicked(onTogglePlayPause)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                        color = PlayerColors.Background,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
                WideKey(onClick = onSkipNext) {
                    Icon(AppIcons.SkipForward, contentDescription = stringResource(R.string.cd_next_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(25.dp))
                }
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                // The same gap between the key and the bar as between the bar and the card's edge.
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                WideKey(onClick = onSkipPrevious) {
                    Icon(AppIcons.SkipBack, contentDescription = stringResource(R.string.cd_previous_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(25.dp))
                }
                WavySeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                    // The line starts near the key's top corner and the times end at its bottom.
                    knob = true,
                    height = WIDE_KEY,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Room for the three small buttons at the foot.
        Spacer(modifier = Modifier.height(barHeight))
    }
}

private val WIDE_KEY = 58.dp

/** A key of the wide look's card: a soft four-bladed clover that rounds off into a plain soft square while pressed, and springs back. */
@Composable
private fun WideKey(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val round by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "wideKeyRound")
    val fill = PlayerColors.TextPrimary.copy(alpha = 0.16f)
    // The setting for plain keys: the soft square the clover rounds into, always.
    val plain = LocalExpressiveRoundKeys.current
    Box(
        modifier = Modifier
            .size(WIDE_KEY)
            .pressScale(interaction, pressedScale = 0.94f)
            .drawBehind { drawPath(cloverPath(size, if (plain) 1f else round), fill) }
            .clickable(interactionSource = interaction, indication = null, onClick = buttonTicked(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The key's outline as the radius from its centre, round the full circle. At [round] 0: four soft blades towards the corners
 * with the middle of each side drawn in; at 1: a soft square (a superellipse), the shape the keys had before the blades.
 */
internal fun cloverPath(size: androidx.compose.ui.geometry.Size, round: Float): Path {
    val half = size.minDimension / 2f
    val dip = 0.15f
    val base = half * 1.2f / (1f + dip)
    val path = Path()
    val steps = 160
    val n = 3.2f
    for (i in 0..steps) {
        val t = (2.0 * PI * i / steps).toFloat()
        val c = kotlin.math.cos(t)
        val sn = sin(t)
        val blades = base * (1f - dip * kotlin.math.cos(4f * t))
        val soft = half / Math.pow((Math.pow(kotlin.math.abs(c).toDouble(), n.toDouble()) + Math.pow(kotlin.math.abs(sn).toDouble(), n.toDouble())), 1.0 / n).toFloat()
        val r = blades + (soft - blades) * round.coerceIn(0f, 1.2f)
        val x = size.width / 2f + r * c
        val y = size.height / 2f + r * sn
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private val WIDE_PILL_HEIGHT = 48.dp

/**
 * The three small buttons at the foot of the wide look: lyrics, the playback device, the queue. Narrow, close together; the
 * middle one is nearly square, the outer ones are round on their outer side and nearly square towards the middle.
 */
@Composable
private fun WidePills(
    lyricsActive: Boolean,
    onLyricsClick: () -> Unit,
    onDeviceClick: () -> Unit,
    queueOpen: Boolean,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val output = rememberOutputDevice(active = LocalNowPlayingActive.current)
    Row(
        modifier = modifier.fillMaxWidth().height(WIDE_PILL_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        WidePill(onClick = onLyricsClick, lit = lyricsActive, label = stringResource(R.string.lyrics), side = -1) {
            Icon(AppIcons.LyricsBubble, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
        WidePill(onClick = onDeviceClick, lit = false, label = output.name ?: stringResource(R.string.this_device), side = 0) {
            Icon(
                imageVector = when (output.kind) {
                    OutputKind.PHONE -> AppIcons.DevicePhone
                    OutputKind.HEADPHONES -> AppIcons.DeviceHeadphones
                    OutputKind.EARBUDS -> AppIcons.DeviceEarbuds
                    OutputKind.SPEAKER -> AppIcons.DeviceSpeaker
                    OutputKind.BLUETOOTH -> AppIcons.DeviceBluetooth
                    OutputKind.USB -> AppIcons.DeviceUsb
                    OutputKind.TV -> AppIcons.DeviceTv
                    OutputKind.CAR -> AppIcons.DeviceCar
                },
                contentDescription = null,
                tint = PlayerColors.TextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        WidePill(onClick = onQueueClick, lit = queueOpen, label = stringResource(R.string.queue), side = 1) {
            Icon(AppIcons.Queue, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

/** One of those buttons: springy like the transport keys (it widens and squares off under a finger); [lit] brightens it. [side]: -1 left, 0 middle, 1 right. */
@Composable
private fun WidePill(
    onClick: () -> Unit,
    lit: Boolean,
    label: String,
    side: Int,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val width by animateDpAsState(if (pressed) 72.dp else 56.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "widePillWidth")
    val inner by animateDpAsState(if (pressed) 10.dp else 14.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "widePillInner")
    val outer by animateDpAsState(if (pressed) 18.dp else 24.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "widePillOuter")
    val shape = when (side) {
        -1 -> RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
        1 -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
    val fill by androidx.compose.animation.animateColorAsState(
        PlayerColors.TextPrimary.copy(alpha = if (lit) 0.30f else 0.14f),
        tween(200),
        label = "widePillFill",
    )
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(fill, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = buttonTicked(onClick))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}
