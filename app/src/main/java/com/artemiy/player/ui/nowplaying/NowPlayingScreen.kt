package com.artemiy.player.ui.nowplaying

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.palette.graphics.Palette
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.Song
import com.artemiy.player.lyrics.LyricLine
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.MinimalSlider
import com.artemiy.player.ui.components.rememberAlbumArtBitmap
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private enum class CenterMode { Art, Lyrics, Queue }

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
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    allSongs: List<Song>,
    onAddToQueue: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    nowPlayingBackgroundMode: NowPlayingBackgroundMode = NowPlayingBackgroundMode.LIVE_BLUR,
    liveBlurIntensity: LiveBlurIntensity = LiveBlurIntensity.NORMAL,
) {
    var centerMode by remember { mutableStateOf(CenterMode.Art) }
    var controlsVisible by remember { mutableStateOf(true) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background),
    ) {
        when (nowPlayingBackgroundMode) {
            // Passed as a String, not a Uri — android.net.Uri isn't a type Compose's compiler
            // can prove stable, so with a raw Uri param this composable (and its whole animated
            // blob subtree) would restart on every position tick from the progress slider (every
            // ~100-250ms), which is almost certainly the real cause of the blobs looking like
            // they "jump" instead of drifting smoothly — not the animation itself.
            NowPlayingBackgroundMode.LIVE_BLUR -> LiveBlurBackground(
                songUriString = song?.uri?.toString(),
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
                        .background(PlayerColors.TextSecondary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onClose() },
                )
            }

            var controlsHeightPx by remember { mutableStateOf(0) }
            var headerHeightPx by remember { mutableStateOf(0) }
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
                            contentPadding = PaddingValues(
                                top = with(density) { headerHeightPx.toDp() } + 8.dp,
                                bottom = with(density) { controlsHeightPx.toDp() } + 24.dp,
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

                    // The toggle row is fixed (not part of the scrolling list) — it sits right
                    // under the floating header and always stays put, only the song rows scroll.
                    CenterMode.Queue -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = with(density) { headerHeightPx.toDp() }),
                    ) {
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
                            onAddSongsClick = { showAddToQueuePicker = true },
                            state = queueListState,
                            modifier = Modifier
                                .weight(1f)
                                .nestedScroll(queueNestedScrollConnection),
                            contentPadding = PaddingValues(
                                bottom = with(density) { controlsHeightPx.toDp() } + 24.dp,
                            ),
                        )
                    }
                }

                // Floating mini-header — Lyrics/Queue only (Art shows title in the control panel
                // below instead). Overlaps the scrollable content instead of pushing it down. This
                // is a deliberately near-solid panel, not a half-blur — real backdrop blur (the
                // content actually visible-but-soft behind it) needs a library this app doesn't
                // pull in yet; a weak translucent gradient just looked like an unexplained smudge.
                if (centerMode != CenterMode.Art) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .onGloballyPositioned { headerHeightPx = it.size.height }
                            .background(
                                Brush.verticalGradient(
                                    0f to PlayerColors.Background.copy(alpha = 0.94f),
                                    0.82f to PlayerColors.Background.copy(alpha = 0.94f),
                                    1f to Color.Transparent,
                                ),
                            )
                            // Absorbs taps anywhere in this panel's bounds so they don't fall
                            // through to whatever's scrolling underneath (was letting stray taps
                            // near the header hit list rows behind it).
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .padding(top = 10.dp),
                    ) {
                        NowPlayingMiniHeader(song = song, onCollapse = { centerMode = CenterMode.Art }, onGoToAlbum = onGoToAlbum, onGoToArtist = onGoToArtist)
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
                        .then(
                            if (centerMode != CenterMode.Art) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.18f to PlayerColors.Background.copy(alpha = 0.94f),
                                        1f to PlayerColors.Background.copy(alpha = 0.94f),
                                    ),
                                )
                            } else Modifier,
                        )
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
                                    color = PlayerColors.TextSecondary,
                                    fontSize = 14.sp,
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
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Добавить в медиатеку",
                                tint = PlayerColors.TextPrimary,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(24.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {},
                            )
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
                        Text(text = formatMs(shownPosition.toLong()), color = PlayerColors.TextSecondary, fontSize = 11.sp)
                        Text(text = formatMs(durationMs), color = PlayerColors.TextSecondary, fontSize = 11.sp)
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

    if (showAddToQueuePicker) {
        AddToQueuePicker(
            songs = allSongs,
            onAdd = onAddToQueue,
            onDismiss = { showAddToQueuePicker = false },
        )
    }
}

/**
 * Extracts a handful of dominant colors from the current track's art via
 * [androidx.palette.graphics.Palette], for the live-blur "color blob" background. Runs off the
 * main thread since Palette's analysis isn't free; recomputes only when the bitmap changes.
 */
@Composable
private fun rememberAlbumPalette(uri: android.net.Uri?): List<Color> {
    val bitmap = rememberAlbumArtBitmap(uri, ART_SIZE_THUMB)
    var colors by remember { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(bitmap) {
        val bmp = bitmap
        colors = if (bmp == null) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) {
                runCatching {
                    val palette = Palette.from(bmp).maximumColorCount(12).generate()
                    listOfNotNull(
                        palette.vibrantSwatch,
                        palette.lightVibrantSwatch,
                        palette.darkVibrantSwatch,
                        palette.mutedSwatch,
                        palette.darkMutedSwatch,
                        palette.lightMutedSwatch,
                    ).ifEmpty { palette.swatches }
                        .sortedByDescending { it.population }
                        .take(4)
                        .map { Color(it.rgb) }
                }.getOrElse { emptyList() }
            }
        }
    }
    return colors
}

/**
 * The "живой блюр" background: a handful of the album art's own dominant colors, rendered as
 * large soft glowing blobs that slowly drift and blend into each other — the Apple Music style of
 * "living" background, rather than the photo itself panning/scaling (which showed visible
 * banding at the blur radii needed to fully obscure the art).
 *
 * This deliberately does NOT use `Modifier.blur()` — a solid-color shape blurred with it rendered
 * as a plain hard-edged circle with zero visible softness on this device (same dead end this
 * project already hit once before with the mood-card glow). Instead each blob is its own radial
 * gradient fading to transparent, which can't produce a hard edge by construction.
 */
@Composable
private fun LiveBlurBackground(songUriString: String?, intensity: LiveBlurIntensity) {
    val songUri = remember(songUriString) { songUriString?.let { android.net.Uri.parse(it) } }
    val palette = rememberAlbumPalette(songUri)
    val base = palette.ifEmpty { listOf(PlayerColors.Surface, PlayerColors.SurfaceDim) }
    // Always render 4 blobs regardless of how many distinct colors Palette actually found —
    // repeating colors still gives more overlap/coverage than 1-2 sparse blobs did.
    val colors = List(4) { base[it % base.size] }

    val blobAlpha = when (intensity) {
        LiveBlurIntensity.MUTED -> 0.42f
        LiveBlurIntensity.NORMAL -> 0.60f
        LiveBlurIntensity.VIVID -> 0.80f
    }
    // How far the gradient holds full color before it starts fading to transparent — a bigger
    // "core" reads as more saturated/present (vivid), a smaller one as a softer wash (muted).
    val coreStop = when (intensity) {
        LiveBlurIntensity.MUTED -> 0.10f
        LiveBlurIntensity.NORMAL -> 0.25f
        LiveBlurIntensity.VIVID -> 0.4f
    }
    val liveScrimAlpha = when (intensity) {
        LiveBlurIntensity.MUTED -> 0.45f
        LiveBlurIntensity.NORMAL -> 0.34f
        LiveBlurIntensity.VIVID -> 0.22f
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(PlayerColors.Background)) {
        val density = LocalDensity.current
        val areaWidthPx = with(density) { maxWidth.toPx() }
        val areaHeightPx = with(density) { maxHeight.toPx() }
        colors.forEachIndexed { index, color ->
            LiveBlurBlob(
                color = color,
                index = index,
                alpha = blobAlpha,
                coreStop = coreStop,
                areaWidthPx = areaWidthPx,
                areaHeightPx = areaHeightPx,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.Background.copy(alpha = liveScrimAlpha)),
        )
    }
}

/** One drifting color blob — its own independent period (seeded from [index]) so a handful of
 * these together never look like they're moving in lockstep, each anchored to a different
 * quadrant of the screen so different album colors don't just stack on top of each other.
 *
 * The position is read inside the `offset { }` lambda (deferred to the layout phase) rather than
 * directly in the composable body — reading the animated value directly would recompose this
 * whole composable on every single animation frame, which was the actual cause of the motion
 * looking like it was "jumping" instead of drifting smoothly.
 */
@Composable
private fun LiveBlurBlob(
    color: Color,
    index: Int,
    alpha: Float,
    coreStop: Float,
    areaWidthPx: Float,
    areaHeightPx: Float,
) {
    val periodMs = 19_000 + index * 7_000
    val transition = rememberInfiniteTransition(label = "liveBlurBlob$index")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val anchorX = when (index % 4) { 0 -> 0.28f; 1 -> 0.75f; 2 -> 0.7f; else -> 0.25f }
    val anchorY = when (index % 4) { 0 -> 0.3f; 1 -> 0.34f; 2 -> 0.72f; else -> 0.76f }
    // Big and overlapping on purpose — the gradient fade (not a size-vs-blur-radius trick) is
    // what keeps the edges soft, so there's no downside to going large like there was with blur.
    val blobDiameterPx = minOf(areaWidthPx, areaHeightPx) * 1.15f
    val brush = remember(color, alpha, coreStop) {
        Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            coreStop to color.copy(alpha = alpha),
            1f to color.copy(alpha = 0f),
        )
    }

    Box(
        modifier = Modifier
            .size(with(LocalDensity.current) { blobDiameterPx.toDp() })
            .offset {
                val p = phase.value
                val driftX = 0.14f * kotlin.math.sin(p + index)
                val driftY = 0.12f * kotlin.math.cos(p * 0.8f + index)
                androidx.compose.ui.unit.IntOffset(
                    (areaWidthPx * (anchorX + driftX) - blobDiameterPx * 0.5f).toInt(),
                    (areaHeightPx * (anchorY + driftY) - blobDiameterPx * 0.5f).toInt(),
                )
            }
            .background(brush),
    )
}

@Composable
private fun LyricsView(
    lyrics: ParsedLyrics?,
    positionMs: Long,
    isPlaying: Boolean,
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
                color = PlayerColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        is ParsedLyrics.Unsynced -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item {
                Text(
                    text = lyrics.text,
                    color = PlayerColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                )
            }
        }

        is ParsedLyrics.Synced -> {
            val activeIndex = remember(lyrics, positionMs) {
                lyrics.lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
            }
            val listState = rememberLazyListState()
            LaunchedEffect(activeIndex) {
                val info = listState.layoutInfo
                val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == activeIndex }
                if (itemInfo != null) {
                    // Anchor a bit above dead center — reads more natural than exact middle.
                    // No artificial top padding backs this: while there isn't enough real
                    // content above yet (start of the song, or a short first line), the scroll
                    // simply clamps at the true top instead of faking empty space to center it.
                    val anchor = (info.viewportSize.height * 0.42f).toInt()
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    listState.animateScrollBy((itemCenter - anchor).toFloat())
                } else {
                    // Big jump (e.g. track just changed) — land roughly nearby first.
                    listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val active = index == activeIndex
                    val alpha by animateFloatAsState(if (active) 1f else 0.35f, label = "lineAlpha")
                    val fontSize by animateFloatAsState(if (active) 28f else 22f, label = "lineSize")
                    Column {
                        if (line.words != null && active) {
                            // Glow only applies to the active eLRC (word-synced) line — a plain
                            // LRC line has no per-word timing to justify the effect, and a
                            // not-yet-reached line never hits this branch since `active` is only
                            // ever true for the current line. The glow pass is the exact same
                            // word-by-word composable, just recolored/blurred, so it always lines
                            // up pixel-for-pixel with the crisp text on top and sweeps forward in
                            // lockstep with it instead of glowing ahead of what's been sung.
                            Box {
                                WordSyncedLine(
                                    line = line,
                                    positionMs = smoothPositionMs,
                                    alpha = alpha,
                                    fontSize = fontSize.sp,
                                    nextLineStartMs = lyrics.lines.getOrNull(index + 1)?.timeMs,
                                    glow = true,
                                )
                                WordSyncedLine(
                                    line = line,
                                    positionMs = smoothPositionMs,
                                    alpha = alpha,
                                    fontSize = fontSize.sp,
                                    nextLineStartMs = lyrics.lines.getOrNull(index + 1)?.timeMs,
                                )
                            }
                        } else {
                            Text(
                                text = line.text,
                                color = PlayerColors.TextPrimary,
                                fontSize = fontSize.sp,
                                lineHeight = fontSize.sp * 1.3f,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .alpha(alpha)
                                    .padding(vertical = 8.dp),
                            )
                        }
                        line.secondary.forEach { secondary ->
                            SecondaryLyricLine(
                                line = secondary,
                                positionMs = smoothPositionMs,
                                alpha = alpha,
                                fontSize = (fontSize * 0.62f).sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A translation or second-singer (v1/v2) line sharing its parent's timestamp — rendered smaller
 * and dimmer underneath, as a comment on the main line rather than a competing one.
 */
@Composable
private fun SecondaryLyricLine(line: LyricLine, positionMs: Long, alpha: Float, fontSize: androidx.compose.ui.unit.TextUnit) {
    if (line.words != null) {
        WordSyncedLine(line = line, positionMs = positionMs, alpha = alpha, fontSize = fontSize, nextLineStartMs = null)
    } else {
        Text(
            text = line.text,
            color = PlayerColors.TextSecondary,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .alpha(alpha)
                .padding(bottom = 6.dp),
        )
    }
}

/**
 * Renders a word-synced line the way Apple Music / Gramophone do it: rather than flipping each
 * word from dim to bright the instant its timestamp hits, the *currently singing* word sweeps
 * from bright to dim left-to-right as playback moves through its time window, so the highlight
 * looks like it travels through the word instead of jumping whole words at a time.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WordSyncedLine(
    line: LyricLine,
    positionMs: Long,
    alpha: Float,
    fontSize: androidx.compose.ui.unit.TextUnit,
    nextLineStartMs: Long?,
    glow: Boolean = false,
) {
    val words = line.words ?: return
    val sungColor = PlayerColors.TextPrimary
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
    val currentIndex = words.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    FlowRow(
        modifier = Modifier
            .alpha(alpha)
            .padding(vertical = 8.dp)
            .then(if (glow) Modifier.blur(18.dp, BlurredEdgeTreatment.Unbounded) else Modifier),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        words.forEachIndexed { index, word ->
            when {
                index < currentIndex -> Text(text = word.text, style = sungStyle)
                index > currentIndex -> Text(text = word.text, style = unsungStyle)
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
                    // A brush-based gradient here turned out to look gray throughout most of the
                    // word (its scaling wasn't behaving as a clean per-word wipe). Drawing the
                    // bright copy on top, hard-clipped to exactly `progress` of that word's own
                    // measured width, is a much more direct and reliably-correct reveal.
                    Box {
                        Text(text = word.text, style = unsungStyle)
                        Text(
                            text = word.text,
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

@Composable
private fun NowPlayingMiniHeader(
    song: Song?,
    onCollapse: () -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
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
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (song != null) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGoToArtist(song) }
                    } else Modifier,
                ),
            )
        }
    }
}

@Composable
private fun QueueList(
    manualQueue: List<Song>,
    continueQueue: List<Song>,
    onItemClick: (Song) -> Unit,
    onClearManualQueue: () -> Unit,
    onAddSongsClick: () -> Unit,
    state: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(modifier = modifier.fillMaxSize(), state = state, contentPadding = contentPadding) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Очередь", color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (manualQueue.isNotEmpty()) {
                    Text(
                        text = "Очистить",
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClearManualQueue() },
                    )
                }
            }
        }
        item { AddSongsToQueueRow(onClick = onAddSongsClick) }
        itemsIndexed(manualQueue) { _, item -> QueueRow(item = item, onClick = { onItemClick(item) }) }
        if (continueQueue.isNotEmpty()) {
            item {
                Text(
                    text = "Далее по очереди",
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(continueQueue) { _, item -> QueueRow(item = item, onClick = { onItemClick(item) }) }
        }
    }
}

@Composable
private fun AddSongsToQueueRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PlayerColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(18.dp))
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
private fun QueueRow(item: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
        Icon(imageVector = Icons.Filled.VolumeDown, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(18.dp))
        MinimalSlider(
            value = volume,
            onValueChange = {
                volume = it
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
            },
            valueRange = 0f..maxVolume.toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        Icon(imageVector = Icons.Filled.VolumeUp, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(20.dp))
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
            tint = if (lyricsActive) PlayerColors.TextPrimary else PlayerColors.TextSecondary,
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
                tint = PlayerColors.TextSecondary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Это устройство",
                color = PlayerColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.QueueMusic,
            contentDescription = "Очередь",
            tint = if (queueActive) PlayerColors.TextPrimary else PlayerColors.TextSecondary,
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
