package com.artemiy.player.ui.nowplaying

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
) {
    var showLyrics by memory::expressiveLyrics
    var showAddToQueuePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    NowPlayingSurface(song = song, mode = backgroundMode, intensity = blurIntensity) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // A little extra lift, so the bar doesn't sit right on the screen's bottom edge.
            val barHeight = 78.dp + navBottom

            Column(modifier = Modifier.fillMaxSize()) {
                // Everything above the title: the cover sitting at the bottom of this space, or the
                // lyrics filling all of it.
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Crossfade(targetState = showLyrics, animationSpec = tween(300), label = "coverOrLyrics") { lyricsShown ->
                        if (lyricsShown) {
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
                    ExpressiveTitleRow(
                        song = song,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                    )
                    WavySeekBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        onSeek = onSeek,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    ExpressiveTransport(
                        isPlaying = isPlaying,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
                // Some air between the buttons and the bottom bar.
                Spacer(modifier = Modifier.height(barHeight + 30.dp))
            }

            // Close handle and (with lyrics open) the romanization toggle, at the top of the screen.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + 10.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.7f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
            )
            if (showLyrics && lyrics?.hasRomanization() == true) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = statusTop + 4.dp, end = 16.dp)) {
                    RomanizationToggle(enabled = lyricsRomanization, onToggle = onToggleLyricsRomanization)
                }
            }

            QueueSheet(
                fullHeight = maxHeight - statusTop - 12.dp,
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
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
                text = song?.title ?: "Ничего не играет",
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
private fun WavySeekBar(positionMs: Long, durationMs: Long, isPlaying: Boolean, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
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
                .height(28.dp)
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
            val wavelength = 26.dp.toPx()

            val waveEnd = thumbX - gap
            if (waveEnd > 0f) {
                val path = Path()
                var x = 0f
                path.moveTo(0f, mid + amp * sin(phase))
                while (x < waveEnd) {
                    x = (x + 2f).coerceAtMost(waveEnd)
                    path.lineTo(x, mid + amp * sin(2f * PI.toFloat() * x / wavelength + phase))
                }
                drawPath(path, played, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val restStart = thumbX + gap
            if (restStart < size.width) {
                drawLine(rest, Offset(restStart, mid), Offset(size.width - stroke / 2, mid), strokeWidth = stroke, cap = StrokeCap.Round)
                drawCircle(played, radius = stroke * 0.6f, center = Offset(size.width - stroke / 2, mid))
            }
            drawLine(played, Offset(thumbX, mid - 11.dp.toPx()), Offset(thumbX, mid + 11.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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

    Row(modifier = modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpressiveButton(
            weight = prevWeight,
            corner = if (prevPressed) 16.dp else 28.dp,
            fill = sideFill,
            interaction = prevInteraction,
            onClick = onSkipPrevious,
        ) {
            Icon(AppIcons.SkipBack, contentDescription = "Предыдущий трек", tint = PlayerColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
        ExpressiveButton(
            weight = playWeight,
            corner = when {
                playPressed -> 18.dp
                isPlaying -> 28.dp
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
            corner = if (nextPressed) 16.dp else 28.dp,
            fill = sideFill,
            interaction = nextInteraction,
            onClick = onSkipNext,
        ) {
            Icon(AppIcons.SkipForward, contentDescription = "Следующий трек", tint = PlayerColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ExpressiveButton(
    weight: Float,
    corner: androidx.compose.ui.unit.Dp,
    fill: Color,
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val animatedCorner by animateDpAsState(corner, spring(stiffness = Spring.StiffnessMediumLow), label = "buttonCorner")
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxSize()
            .clip(RoundedCornerShape(animatedCorner))
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
        scope.launch { openness.animateTo(if (open) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) }
    }
    BackHandler(enabled = openness.targetValue > 0.5f && LocalNowPlayingActive.current) { settle(false) }

    val dragState = rememberDraggableState { delta ->
        scope.launch { openness.snapTo((openness.value - delta / travelPx).coerceIn(0f, 1f)) }
    }
    val sheetFill = lerp(Color.Black.copy(alpha = 0.28f), PlayerColors.Background.copy(alpha = 0.97f), openness.value)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(fullHeight)
            .offset { IntOffset(0, ((1f - openness.value) * travelPx).roundToInt()) }
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(sheetFill),
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
                Text(
                    text = buildAnnotatedString {
                        append("Играет из ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PlayerColors.TextPrimary)) {
                            append(playingFrom?.name ?: "очереди")
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
                    contentDescription = "Текст песни",
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
                        OutputKind.SPEAKER -> AppIcons.DeviceSpeaker
                        OutputKind.BLUETOOTH -> AppIcons.DeviceBluetooth
                        OutputKind.USB -> AppIcons.DeviceUsb
                        OutputKind.TV -> AppIcons.DeviceTv
                    },
                    contentDescription = output.name ?: "Это устройство",
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
                QueueToggleRow(
                    shuffleEnabled = shuffleEnabled,
                    repeatEnabled = repeatEnabled,
                    infinitePlayEnabled = infinitePlayEnabled,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleInfinitePlay = onToggleInfinitePlay,
                )
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
                    contentPadding = PaddingValues(top = 8.dp, bottom = navBottom + 24.dp),
                )
            }
        }
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
