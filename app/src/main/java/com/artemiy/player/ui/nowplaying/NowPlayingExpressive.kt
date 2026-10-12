package com.artemiy.player.ui.nowplaying

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxScope
import com.artemiy.player.ui.library.AnimatedToneBackdrop
import com.artemiy.player.ui.library.ToneBackdrop
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.artemiy.player.ui.home.drawGenreMotif
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.data.artists
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import com.artemiy.player.ui.components.pressScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
                queuePanel = { panelModifier ->
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
                },
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
                            val coverCorner by animateDpAsState(if (isPlaying) 28.dp else 40.dp, spring(stiffness = Spring.StiffnessLow), label = "coverCorner")
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

            if (!dial) QueueSheet(
                // Stops well short of the top, so reaching for the top of the open queue doesn't
                // land on the player itself and swipe the whole thing shut.
                fullHeight = maxHeight - statusTop - 64.dp,
                barHeight = barHeight,
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
                onOpenChange = { memory.expressiveQueueOpen = it },
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
private fun Modifier.popIn(expand: () -> Float, active: Boolean, order: Int, grow: Boolean = false): Modifier = graphicsLayer {
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
private fun WavySeekBar(positionMs: Long, durationMs: Long, isPlaying: Boolean, onSeek: (Long) -> Unit, modifier: Modifier = Modifier, showTimes: Boolean = true) {
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

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
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
            val gap = 5.dp.toPx()
            val thumbX = (size.width * fraction).coerceIn(stroke, size.width - stroke)
            val amp = amplitude.toPx()
            val wavelength = 40.dp.toPx()

            val waveEnd = thumbX - gap
            if (waveEnd > 0f) {
                // Calm at the start of the bar, swelling to a touch above the usual height by the
                // time it reaches the thumb (the ramp is measured from the left edge).
                val ramp = 170.dp.toPx()
                fun swell(px: Float): Float {
                    val t = (px / ramp).coerceIn(0f, 1f)
                    return 0.2f + 0.95f * (t * t * (3f - 2f * t))
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
            drawLine(played, Offset(thumbX, mid - 15.dp.toPx()), Offset(thumbX, mid + 15.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        if (showTimes) Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
            .clip(RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end))
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
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
    val sheetFill = lerp(tone.copy(alpha = 0.4f), tone.copy(alpha = 0.97f), openness.value)

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
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    settle(openness.targetValue < 0.5f)
                }
                .padding(horizontal = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PlayerColors.TextPrimary.copy(alpha = 0.35f)),
            )
            Row(
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
                Icon(
                    imageVector = AppIcons.LyricsBubble,
                    contentDescription = stringResource(R.string.lyrics),
                    tint = if (lyricsActive) PlayerColors.TextPrimary else LocalAdaptiveSecondaryColor.current,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(24.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLyricsClick() },
                )
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
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDeviceClick() },
                )
            }
        }

        // The queue — only built once the sheet starts opening.
        if (openness.value > 0.001f) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp)) {
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
                    bottomFadePx = { 0 },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
                )
                // The play modes under the list, in the same chunky connected style as the
                // transport buttons, lit in the cover's color.
                ExpressiveQueueModes(
                    song = song,
                    shuffleEnabled = shuffleEnabled,
                    repeatEnabled = repeatEnabled,
                    infinitePlayEnabled = infinitePlayEnabled,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleInfinitePlay = onToggleInfinitePlay,
                    modifier = Modifier.padding(top = 4.dp, bottom = navBottom + 16.dp),
                )
            }
        }
    }
}

/** The "Диск" queue panel wears the app theme's colors rather than the player's (and the cover's). */
@Composable
private fun QueueSheetPalette(appPalette: com.artemiy.player.ui.theme.PlayerPalette, content: @Composable () -> Unit) {
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
 * The "Диск" middle of the player. Up top, "Играет из" small and the title big and two-toned (the
 * artist in gray under it). Along the bottom a grid of square buttons, two rows of three:
 *   [time]  [queue]  [plate]
 *   [play]  [back ]  [next ]
 * The plate turns while the song plays, rests on pause, and turned by a finger it winds the song.
 * The queue button opens the queue (a panel over the title, the grid staying in reach); held down
 * it brings up the lyrics instead, and held again puts them away.
 */
@Composable
private fun DialContent(
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
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onLineLongClick: (Int) -> Unit,
    expand: () -> Float,
    active: Boolean,
    barHeight: androidx.compose.ui.unit.Dp,
    statusTop: androidx.compose.ui.unit.Dp,
    playingFrom: PlaySource?,
    card: Boolean,
    backgroundMode: NowPlayingBackgroundMode,
    blurIntensity: LiveBlurIntensity,
    onClose: () -> Unit,
    onToggleRomanization: () -> Unit,
    onToggleLyrics: () -> Unit,
    onDeviceClick: () -> Unit,
    queueOpen: Boolean,
    onQueueOpenChange: (Boolean) -> Unit,
    queuePanel: @Composable (Modifier) -> Unit,
) {
    // However big the text is set (system size × the app's), the Dial keeps to a size its tiles and
    // card can hold; everywhere else in the app the setting applies in full.
    val outerDensity = LocalDensity.current
    val dialDensity = remember(outerDensity.density, outerDensity.fontScale) {
        androidx.compose.ui.unit.Density(outerDensity.density, minOf(outerDensity.fontScale, DIAL_MAX_FONT_SCALE))
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides dialDensity) {
    val density = LocalDensity.current
    // Winding the song with the plate makes the player blink "stopped" for a moment while it loads the
    // new spot; the play tile and the plate keep to what they showed when the finger went down, and
    // let go of it a little after the finger comes off.
    var windHold by remember { mutableStateOf(false) }
    var windHeldPlaying by remember { mutableStateOf(isPlaying) }
    var windingNow by remember { mutableStateOf(false) }
    // Keyed on "a finger is down": when it comes up the release is timed, and a finger coming down
    // again before that cancels it (otherwise the old timer let go in the middle of the new turn).
    LaunchedEffect(windingNow) {
        if (!windingNow && windHold) {
            kotlinx.coroutines.delay(900)
            windHold = false
        }
    }
    val shownPlaying = if (windHold) windHeldPlaying else isPlaying
    val onWinding: (Boolean) -> Unit = { winding ->
        if (winding && !windHold) windHeldPlaying = isPlaying
        if (winding) windHold = true
        windingNow = winding
    }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val appPalette = com.artemiy.player.ui.theme.LocalAppPalette.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePad = 16.dp
        val tileGap = 10.dp
        val tile = ((maxWidth - sidePad * 2 - tileGap * 2) / 3).coerceAtMost(124.dp)
        // The same air under the grid as at its sides (16 dp), on top of the system's own bottom inset.
        val groupBottom = barHeight + 8.dp
        // Without the card the timecode runs along the top of the grid (with it, along the card's bottom).
        val timeRow = if (card) 0.dp else 52.dp
        val gridHeight = tile * 2 + tileGap + timeRow + (if (card) 0.dp else tileGap)
        // What sits above the grid: lyrics, the queue panel (and the card) stop short of it.
        val aboveGrid = groupBottom + gridHeight + 12.dp
        // Inside the card everything is measured from the card's own top, not the screen's.
        val contentTop = if (card) 0.dp else statusTop
        // The grid sits on the app's own background when the card holds the picture, or when there is none.
        val onTheme = card || backgroundMode == NowPlayingBackgroundMode.NONE

        BackHandler(enabled = queueOpen && LocalNowPlayingActive.current) { onQueueOpenChange(false) }

        val queueShown by animateFloatAsState(
            if (queueOpen) 1f else 0f,
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            label = "dialQueue",
        )
        val panelLayer: @Composable BoxScope.() -> Unit = {
            // The queue panel, the same size in both looks: it rises over the title and stops above the tiles — in the card
            // an even frame all round, over the screen (the full-screen look) about as far from the edges, and
            // over the handle and the timecode in both, which come back when it closes.
            if (queueShown > 0.001f) {
                queuePanel(
                    Modifier
                        .align(Alignment.TopCenter)
                        .then(
                            if (card) Modifier.padding(DIAL_PANEL_FRAME)
                            else Modifier.padding(start = sidePad, end = sidePad, top = statusTop + 14.dp, bottom = groupBottom + tile * 2 + tileGap + 12.dp),
                        )
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = queueShown.coerceIn(0f, 1f)
                            translationY = (1f - queueShown) * 48.dp.toPx()
                        },
                )
            }
        }

        // The "screen": title (or lyrics), the queue panel, the close handle and the song's menu.
        val screenPart: @Composable BoxScope.() -> Unit = {
            Crossfade(targetState = showLyrics, animationSpec = tween(300), label = "dialTextOrLyrics") { lyricsShown ->
                if (lyricsShown) {
                    if (LocalNowPlayingActive.current) com.artemiy.player.ui.components.KeepScreenOn()
                    val topPx = with(density) { (contentTop + 36.dp).roundToPx() }
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = if (card) CARD_TIME_ROOM else aboveGrid)) {
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
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = contentTop + 36.dp + FADE_SPAN, bottom = FADE_SPAN),
                        )
                    }
                } else {
                    var menuOpen by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = contentTop + (if (card) 44.dp else 52.dp))
                            .popIn(expand, active, order = 1),
                    ) {
                        Column {
                            // "Играет из …": small and quiet, above the title.
                            val playingFromLabel = stringResource(R.string.playing_from)
                            val queueLabel = stringResource(R.string.playing_from_queue)
                            Text(
                                text = buildAnnotatedString {
                                    // The resource's trailing space gets trimmed at build time, so it is put back here.
                                    append(playingFromLabel.trimEnd())
                                    if (!playingFromLabel.endsWith('：')) append(' ')
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(playingFrom?.name ?: queueLabel) }
                                },
                                color = LocalAdaptiveSecondaryColor.current,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                            // Title and artist at the same size, the artist in the secondary color.
                            // A tap goes to the album / artist; a long press opens the song's menu.
                            val openMenu = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                menuOpen = true
                            }
                            DialFitText(
                                text = song?.title ?: stringResource(R.string.nothing_playing),
                                color = PlayerColors.TextPrimary,
                                maxLines = 3,
                                marqueeWhenTooLong = false,
                                onClick = { song?.let(onGoToAlbum) },
                                onLongClick = openMenu,
                                bracketOnNewLine = true,
                            )
                            // The artist: the first name (up to two lines), with the rest folded into a "+N" tile;
                            // a name too long even for that runs as a marquee.
                            val artistNames = remember(song?.artist) { song?.artists().orEmpty() }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DialFitText(
                                    text = artistNames.firstOrNull() ?: song?.artist.orEmpty(),
                                    color = LocalAdaptiveSecondaryColor.current,
                                    maxLines = 2,
                                    marqueeWhenTooLong = true,
                                    onClick = { song?.let(onGoToArtist) },
                                    onLongClick = openMenu,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (artistNames.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 10.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(LocalAdaptiveSecondaryColor.current.copy(alpha = 0.22f))
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { song?.let(onGoToArtist) }
                                            .padding(horizontal = 9.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "+" + (artistNames.size - 1),
                                            color = LocalAdaptiveSecondaryColor.current,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                        if (song != null) {
                            SongActionsMenuPopup(
                                song = song,
                                expanded = menuOpen,
                                onDismiss = { menuOpen = false },
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                                onAddToPlaylist = onAddToPlaylist,
                                onGoToAlbum = onGoToAlbum,
                                onGoToArtist = onGoToArtist,
                            )
                        }
                    }
                }
            }

            if (card) {
                // In the card these two belong to it, not to the screen.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PlayerColors.TextPrimary.copy(alpha = 0.7f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
                )
                if (showLyrics && lyrics?.hasRomanization() == true) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 12.dp)) {
                        RomanizationToggle(enabled = lyricsRomanization, onToggle = onToggleRomanization)
                    }
                }
                // The timecode along the card's bottom: elapsed time left, the full length right.
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = dialTime(positionMs), color = PlayerColors.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(text = dialTime(durationMs), color = LocalAdaptiveSecondaryColor.current, fontSize = 22.sp, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            if (card) panelLayer()
        }

        if (card) {
            DialCard(
                song = song,
                mode = backgroundMode,
                intensity = blurIntensity,
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = statusTop + 4.dp, bottom = aboveGrid)
                    .fillMaxSize(),
                content = screenPart,
            )
        } else {
            screenPart()
        }

        // The grid: in the app's own colors whenever it sits on the app's background.
        DialGridPalette(onTheme = card, appPalette = appPalette) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = sidePad, end = sidePad, bottom = groupBottom)
                    .fillMaxWidth()
                    .popIn(expand, active, order = 3),
                verticalArrangement = Arrangement.spacedBy(tileGap),
            ) {
                if (!card) {
                    // The timecode: elapsed time at the grid's left edge, the full length at its right.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(timeRow)
                            .padding(horizontal = 6.dp)
                            // Gives way to the queue panel as it opens, so no edge of the digits peeks out under it.
                            .graphicsLayer { alpha = 1f - queueShown.coerceIn(0f, 1f) },
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = dialTime(positionMs), color = PlayerColors.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(text = dialTime(durationMs), color = LocalAdaptiveSecondaryColor.current, fontSize = 22.sp, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(tileGap), verticalAlignment = Alignment.Bottom) {
                    // Where the sound goes (phone, headphones, speaker...): opens the system's switcher.
                    val output = rememberOutputDevice(active = active)
                    DialTile(filled = false, onClick = onDeviceClick, modifier = Modifier.size(tile)) {
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
                            tint = PlayerColors.TextPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    // Queue (tap) / lyrics (hold). Lit while either is showing.
                    DialTile(
                        filled = false,
                        lit = queueOpen || showLyrics,
                        onClick = { onQueueOpenChange(!queueOpen) },
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (!showLyrics) onQueueOpenChange(false)
                            onToggleLyrics()
                        },
                        modifier = Modifier.size(tile),
                    ) {
                        // The icon swaps with a little flip when the lyrics come or go.
                        AnimatedContent(
                            targetState = showLyrics,
                            transitionSpec = {
                                (fadeIn(tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)))
                                    .togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.3f, animationSpec = tween(120)))
                            },
                            label = "queueOrLyricsIcon",
                        ) { lyricsOn ->
                            Icon(
                                imageVector = if (lyricsOn) AppIcons.LyricsBubble else AppIcons.Queue,
                                contentDescription = stringResource(if (lyricsOn) R.string.lyrics else R.string.queue),
                                tint = PlayerColors.TextPrimary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    DialPlate(
                        isPlaying = shownPlaying,
                        active = active,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        onWinding = onWinding,
                        modifier = Modifier.size(tile),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(tileGap)) {
                    DialTile(filled = true, accent = onTheme, onClick = onTogglePlayPause, modifier = Modifier.size(tile)) {
                        PlayPauseIcon(isPlaying = shownPlaying, tint = if (onTheme) PlayerColors.OnAccent else PlayerColors.Background, modifier = Modifier.size(38.dp))
                    }
                    DialTile(filled = false, onClick = onSkipPrevious, modifier = Modifier.size(tile)) {
                        Icon(AppIcons.SkipPrevious, stringResource(R.string.cd_previous_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(34.dp))
                    }
                    DialTile(filled = false, onClick = onSkipNext, modifier = Modifier.size(tile)) {
                        Icon(AppIcons.SkipNext, stringResource(R.string.cd_next_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(34.dp))
                    }
                }
            }
        }

        // In the full-screen look the panel is drawn last, so it covers the timecode above the tiles too.
        if (!card) panelLayer()
    }
    }
}

/** The grid's colors: the app theme's when it sits on the app's own background (the card look), else the player's as they are. */
@Composable
private fun DialGridPalette(onTheme: Boolean, appPalette: com.artemiy.player.ui.theme.PlayerPalette, content: @Composable () -> Unit) {
    if (!onTheme) {
        content()
        return
    }
    com.artemiy.player.ui.theme.PaletteScope(appPalette) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAdaptiveSecondaryColor provides appPalette.textSecondary, content = content)
    }
}

/** The most the Dial lets text grow (system × app text size). */
private const val DIAL_MAX_FONT_SCALE = 1.3f

private val DIAL_CARD_CORNER = 34.dp

/** The queue panel's frame inside the card: the same on every side. */
private val DIAL_PANEL_FRAME = 10.dp

/** The "Диск" timecode: just digits, minutes and seconds with no colon — 27 s is "0027", 3:18 is "0318". */
private fun dialTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d%02d".format(total / 60, total % 60)
}

/** Room kept at the card's bottom for the timecode. */
private val CARD_TIME_ROOM = 76.dp

/**
 * The card of the "Диск" look: rounded, holding the background the player is set to — the blur —
 * or, with no blur, the album's own colors and the picture of its genre (as on the Library tiles).
 */
@Composable
private fun DialCard(
    song: Song?,
    mode: NowPlayingBackgroundMode,
    intensity: LiveBlurIntensity,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(DIAL_CARD_CORNER))) {
        if (mode != NowPlayingBackgroundMode.NONE) {
            NowPlayingSurface(song = song, mode = mode, intensity = intensity, matte = true, content = content)
        } else {
            com.artemiy.player.ui.theme.PaletteScope(com.artemiy.player.ui.theme.NowPlayingPalette) {
                androidx.compose.runtime.CompositionLocalProvider(LocalAdaptiveSecondaryColor provides com.artemiy.player.ui.theme.NowPlayingPalette.textSecondary) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // The picture (tone and genre drawing), in the same matte treatment as the blur card; when
                        // the song changes the colours ease over and the drawing cross-fades instead of cutting.
                        Box(modifier = Modifier.matchParentSize().matteColor()) {
                            AnimatedToneBackdrop(song)
                            val seed = (song?.id ?: 0L).toInt()
                            val genre = song?.genre?.takeIf { it.isNotBlank() } ?: song?.artist.orEmpty()
                            Crossfade(targetState = genre to seed, animationSpec = tween(900), label = "cardGenreArt") { (g, sd) ->
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    translate(size.width * 0.1f, size.height * 0.02f) {
                                        rotate(-8f) {
                                            scale(1.25f) {
                                                drawGenreMotif(g, sd, unknownAsTag = false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        content()
                    }
                }
            }
        }
    }
}

/** The surface under the whole player: the blurred background — or, in the "Диск" card look, just the app's own background. */
@Composable
private fun PlayerSurface(
    card: Boolean,
    song: Song?,
    mode: NowPlayingBackgroundMode,
    intensity: LiveBlurIntensity,
    openFrom: androidx.compose.ui.geometry.Rect?,
    openProgress: () -> Float,
    openTint: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    if (card) {
        Box(modifier = Modifier.fillMaxSize().background(PlayerColors.Background), content = content)
    } else {
        NowPlayingSurface(song = song, mode = mode, intensity = intensity, openFrom = openFrom, openProgress = openProgress, openTint = openTint, content = content)
    }
}

/** The queue as a panel for the "Диск" style: the app theme's colors, the list, the play modes under it. */
@Composable
private fun DialQueuePanel(
    accentModes: Boolean,
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
    modifier: Modifier = Modifier,
) {
    val appPalette = com.artemiy.player.ui.theme.LocalAppPalette.current
    QueueSheetPalette(appPalette) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(DIAL_CORNER))
                .background(appPalette.background.copy(alpha = 1f)),
        ) {
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
                bottomFadePx = { 0 },
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            )
            DialQueueModes(
                accented = accentModes,
                shuffleEnabled = shuffleEnabled,
                repeatEnabled = repeatEnabled,
                infinitePlayEnabled = infinitePlayEnabled,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onToggleInfinitePlay = onToggleInfinitePlay,
                modifier = Modifier.padding(start = DIAL_MODES_INSET, end = DIAL_MODES_INSET, bottom = DIAL_MODES_INSET),
            )
        }
    }
}

/** The gap between the queue panel's edge and its row of mode buttons. */
private val DIAL_MODES_INSET = 8.dp

/**
 * Shuffle / repeat / endless play for the "Диск" queue: three rounded rectangles set into the panel's
 * foot. Their corners at the panel's own corners follow its curve (the panel's radius less the gap),
 * the rest are tighter. Off: a faint plate; on: filled in the app's accent (or plain, see [accented]).
 */
@Composable
private fun DialQueueModes(
    accented: Boolean,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    infinitePlayEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lit: the app's accent (or, when plain, the text color itself, as the play tile is white).
    val accent = if (accented) PlayerColors.Accent else PlayerColors.TextPrimary
    val onAccent = if (accented) PlayerColors.OnAccent else PlayerColors.Background
    val offFill = PlayerColors.TextPrimary.copy(alpha = 0.10f)
    val follow = DIAL_CORNER - DIAL_MODES_INSET
    val inner = 12.dp
    Row(modifier = modifier.fillMaxWidth().height(54.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple(AppIcons.Shuffle to stringResource(R.string.shuffle), shuffleEnabled, onToggleShuffle),
            Triple(AppIcons.Repeat to stringResource(R.string.repeat), repeatEnabled, onToggleRepeat),
            Triple(AppIcons.Infinite to stringResource(R.string.endless_play), infinitePlayEnabled, onToggleInfinitePlay),
        ).forEachIndexed { index, (iconAndLabel, enabled, onClick) ->
            val interaction = remember { MutableInteractionSource() }
            val fill by animateColorAsState(if (enabled) accent else offFill, tween(220), label = "dialModeFill")
            val tint by animateColorAsState(if (enabled) onAccent else PlayerColors.TextPrimary, tween(220), label = "dialModeTint")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pressScale(interaction, pressedScale = 0.95f)
                    .clip(
                        RoundedCornerShape(
                            topStart = inner,
                            topEnd = inner,
                            bottomStart = if (index == 0) follow else inner,
                            bottomEnd = if (index == 2) follow else inner,
                        ),
                    )
                    .background(fill)
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconAndLabel.first, contentDescription = iconAndLabel.second, tint = tint, modifier = Modifier.size(22.dp))
                if (index == 1 && LocalRepeatOne.current) {
                    Text("1", color = tint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private val DIAL_CORNER = 26.dp

/**
 * A square, rounded button: solid in the text color when [filled] (the main one), a faint plate
 * otherwise, a little stronger while [lit]. A [onLongClick] adds a second job to the same button.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DialTile(
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lit: Boolean = false,
    /** The solid tile in the theme's accent color rather than the text color. */
    accent: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val plate by animateColorAsState(
        if (filled) (if (accent) PlayerColors.Accent else PlayerColors.TextPrimary) else PlayerColors.TextPrimary.copy(alpha = if (lit) 0.30f else 0.14f),
        tween(250),
        label = "dialTilePlate",
    )
    Box(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(RoundedCornerShape(DIAL_CORNER))
            .background(plate)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Degrees a second the plate turns at while playing: a turn in six seconds. */
private const val DIAL_SPIN = 60f

/** How much song one full turn of the plate winds, in seconds. */
private const val DIAL_SECONDS_PER_TURN = 30f

/**
 * A square plate with a disc on it, marked with a dot so its turning shows. The disc spins up when
 * the song plays and spins down on pause. Turn it with a finger and the song winds along with it
 * (clockwise forward), with a faint tick every few degrees.
 */
@Composable
private fun DialPlate(
    isPlaying: Boolean,
    active: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    /** Told when a finger starts winding the song (true) and when it lets go (false). */
    onWinding: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var angle by remember { mutableFloatStateOf(0f) }
    var spin by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val positionNow by rememberUpdatedState(positionMs)
    val durationNow by rememberUpdatedState(durationMs)
    val onSeekNow by rememberUpdatedState(onSeek)
    val onWindingNow by rememberUpdatedState(onWinding)
    val context = LocalContext.current

    // Frame by frame: the speed eases toward its goal (full when playing, none when paused), so the
    // disc takes a moment to get going and keeps turning a moment after the pause. Idle when still.
    LaunchedEffect(isPlaying, active) {
        if (!active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
            last = now
            if (!dragging) {
                val goal = if (isPlaying) DIAL_SPIN else 0f
                spin += (goal - spin) * (1f - kotlin.math.exp(-dt * 3f))
                angle = (angle + spin * dt) % 360f
                if (!isPlaying && kotlin.math.abs(spin) < 0.3f) {
                    spin = 0f
                    break
                }
            }
        }
    }

    val disc = PlayerColors.TextPrimary
    val mark = PlayerColors.Background
    val seekLabel = stringResource(R.string.cd_plate_seek)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DIAL_CORNER))
            .background(PlayerColors.TextPrimary.copy(alpha = 0.14f))
            // What a screen reader (TalkBack) says and does: it is a seek control, and adjusting it
            // (the volume keys, "increase / decrease") winds the song like turning it does.
            .semantics {
                contentDescription = seekLabel
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                    range = 0f..1f,
                )
                setProgress { fraction ->
                    if (durationNow > 0L) {
                        onSeekNow((fraction.coerceIn(0f, 1f) * durationNow).toLong())
                        true
                    } else false
                }
            }
            .pointerInput(Unit) {
                var lastTouch = 0f
                var target = 0f
                var tickCarry = 0f
                var lastSeekAt = 0L
                fun touchAngle(o: Offset) = Math.toDegrees(kotlin.math.atan2((o.y - size.height / 2f).toDouble(), (o.x - size.width / 2f).toDouble())).toFloat()
                // While the song's length is still unknown (a moment after a skip, a file that won't
                // open) the plate just keeps turning and leaves the song be.
                var blocked = false
                detectDragGestures(
                    onDragStart = { o ->
                        blocked = durationNow <= 0L
                        if (!blocked) {
                            dragging = true
                            onWindingNow(true)
                            lastTouch = touchAngle(o)
                            target = positionNow.toFloat()
                            tickCarry = 0f
                        }
                    },
                    onDragEnd = {
                        if (!blocked) {
                            onSeekNow(target.toLong())
                            dragging = false
                            onWindingNow(false)
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        if (!blocked) onWindingNow(false)
                    },
                ) { change, _ ->
                    change.consume()
                    if (blocked) return@detectDragGestures
                    val a = touchAngle(change.position)
                    var d = a - lastTouch
                    if (d > 180f) d -= 360f
                    if (d < -180f) d += 360f
                    lastTouch = a
                    angle = (angle + d) % 360f
                    target = (target + d / 360f * DIAL_SECONDS_PER_TURN * 1000f).coerceIn(0f, durationNow.toFloat().coerceAtLeast(0f))
                    // Winds the song as it goes (a few times a second), not only when let go.
                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastSeekAt > 120) {
                        lastSeekAt = nowMs
                        onSeekNow(target.toLong())
                    }
                    tickCarry += kotlin.math.abs(d)
                    if (tickCarry >= 12f) {
                        tickCarry = 0f
                        plateTick(context)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .graphicsLayer { rotationZ = angle },
        ) {
            val r = size.minDimension / 2f
            drawCircle(disc, radius = r)
            // Fine grooves, then the dot that shows the turning, and the spindle hole.
            drawCircle(mark.copy(alpha = 0.12f), radius = r * 0.62f, style = Stroke(1.dp.toPx()))
            drawCircle(mark.copy(alpha = 0.12f), radius = r * 0.44f, style = Stroke(1.dp.toPx()))
            drawCircle(mark, radius = r * 0.09f, center = Offset(center.x, center.y - r * 0.8f))
            drawCircle(mark, radius = r * 0.07f)
        }
    }
}


/**
 * Big text (36sp) that makes itself fit: it shrinks down to 28sp to get into [maxLines] lines, the
 * lines broken evenly (a heading's break: "MASS OF THE / FERMENTING DREGS", not "MASS OF THE FERME…").
 * If even that isn't enough it is cut off with an ellipsis — or, with [marqueeWhenTooLong], it stays
 * on one line and runs as a marquee.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DialFitText(
    text: String,
    color: Color,
    maxLines: Int,
    marqueeWhenTooLong: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** A "(…)" or "[…]" part starts a line of its own, so a bracket never ends a line with half its words. */
    bracketOnNewLine: Boolean = false,
) {
    val text = remember(text, bracketOnNewLine) {
        if (bracketOnNewLine) text.replace(Regex("\\s+(?=[(\\[])"), "\n") else text
    }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val baseStyle = androidx.compose.material3.LocalTextStyle.current
    val heading = androidx.compose.ui.text.TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading)
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth
        val density = LocalDensity.current
        val fitSp = remember(text, widthPx, maxLines, baseStyle, density.fontScale) {
            var size = 36f
            var found: Float? = null
            while (size >= 28f) {
                val result = measurer.measure(
                    text = text,
                    style = baseStyle.merge(heading).merge(androidx.compose.ui.text.TextStyle(fontSize = size.sp, lineHeight = (size * 1.11f).sp, fontWeight = FontWeight.ExtraBold)),
                    overflow = TextOverflow.Clip,
                    maxLines = maxLines,
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
                )
                if (!result.hasVisualOverflow) { found = size; break }
                size -= 1f
            }
            found
        }
        val runsAsMarquee = fitSp == null && marqueeWhenTooLong
        val sp = fitSp ?: if (marqueeWhenTooLong) 32f else 28f
        Text(
            text = text,
            color = color,
            style = baseStyle.merge(heading),
            fontSize = sp.sp,
            lineHeight = (sp * 1.11f).sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = if (runsAsMarquee) 1 else maxLines,
            softWrap = !runsAsMarquee,
            overflow = if (runsAsMarquee) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
                .then(if (runsAsMarquee) Modifier.marquee(LocalNowPlayingActive.current) else Modifier)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        )
    }
}


/**
 * One short tick from the vibration motor, for the plate. It goes to the motor directly: the usual
 * "haptic feedback" call doesn't buzz for a drag this fast on some phones. It still follows the
 * system's "touch feedback" switch.
 */
private fun plateTick(context: android.content.Context) {
    val touchFeedbackOn = android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
    if (!touchFeedbackOn) return
    val vibrator = context.getSystemService(android.os.Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    when {
        android.os.Build.VERSION.SDK_INT >= 29 -> vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
        android.os.Build.VERSION.SDK_INT >= 26 -> vibrator.vibrate(android.os.VibrationEffect.createOneShot(8, 90))
        else -> @Suppress("DEPRECATION") vibrator.vibrate(8)
    }
}
