package com.artemiy.player.ui.nowplaying

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.withFrameNanos
import com.artemiy.player.ui.components.pressScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.Song
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.lyrics.hasRomanization
import com.artemiy.player.playback.OutputKind
import com.artemiy.player.playback.PlaySource
import androidx.compose.ui.text.rememberTextMeasurer
import com.artemiy.player.playback.rememberOutputDevice
import com.artemiy.player.ui.components.PlayPauseIcon
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.components.marquee
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.launch

// The "Диск" player (Now Playing style): the card, the grid of tiles, the plate. Split off from NowPlayingExpressive.kt.
// Shared with the Expressive player and so `internal`: PlayerSurface and DialContent/DialQueuePanel (used by
// ExpressiveNowPlaying, which picks the look), and over there popIn and QueueSheetPalette.

/**
 * The Dial's card as the player opens: it comes first, before what is on it — fading in, settling from a little smaller
 * and a little lower. (Closing runs it backwards.)
 */
private fun Modifier.cardPop(expand: () -> Float, active: Boolean): Modifier = graphicsLayer {
    if (!active) return@graphicsLayer
    val t = ((expand() - 0.1f) / 0.5f).coerceIn(0f, 1f)
    if (t >= 1f) return@graphicsLayer
    val eased = 1f - (1f - t) * (1f - t) * (1f - t)
    alpha = t
    val scale = 0.94f + 0.06f * eased
    scaleX = scale
    scaleY = scale
    translationY = (1f - eased) * 28.dp.toPx()
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
internal fun DialContent(
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
    // The text style as the rest of the app has it: the lyrics keep it whatever font the Disc is set in.
    val usualTextStyle = androidx.compose.material3.LocalTextStyle.current
    val dialDensity = remember(outerDensity.density, outerDensity.fontScale) {
        androidx.compose.ui.unit.Density(outerDensity.density, minOf(outerDensity.fontScale, DIAL_MAX_FONT_SCALE))
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides dialDensity,
        androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.let { base -> com.artemiy.player.ui.theme.LocalDiscFont.current?.let { base.copy(fontFamily = it) } ?: base },
    ) {
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
    val dialDots = LocalDialDots.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val appPalette = com.artemiy.player.ui.theme.LocalAppPalette.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePad = 16.dp
        val tileGap = 10.dp
        // The tiles fill the width the card has (a phone set to a small display size is a lot wider in dp
        // than 392: the grid then grows with it, rather than staying small and leaving the card wider
        // than it). Only a very short screen holds them back, and then the whole thing sits centred.
        val tile = ((maxWidth - sidePad * 2 - tileGap * 2) / 3).coerceAtMost(maxHeight * 0.19f)
        val gridWidth = tile * 3 + tileGap * 2
        val gridSide = ((maxWidth - gridWidth) / 2).coerceAtLeast(sidePad)
        // A phone with round screen corners: the card's top corners and the grid's two bottom outer ones
        // run parallel to them (the screen's radius less the gap to the edge), never rounder-than-usual
        // by less than before. Without the data the usual corners stay.
        val screenCorner = com.artemiy.player.ui.theme.screenCornerDp()
        val cardTopCorner = maxOf(DIAL_CARD_CORNER, screenCorner - (gridSide - 4.dp))
        val outerTileCorner = maxOf(DIAL_CORNER, screenCorner - gridSide)
        // The same air under the grid as at its sides (16 dp), on top of the system's own bottom inset.
        val groupBottom = barHeight + 8.dp
        // Without the card the timecode runs along the top of the grid (with it, along the card's bottom).
        val timeRow = if (card) 0.dp else if (dialDots) 78.dp else 52.dp
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
                            else Modifier.padding(start = gridSide, end = gridSide, top = statusTop + 14.dp, bottom = groupBottom + tile * 2 + tileGap + 12.dp),
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
                    // The lyrics run on behind the timecode, with no extra mask: the usual per-line fade, but it ends at the very
                    // bottom edge (card bottom / the tiles), so it plays out under the digits instead of stopping short above them.
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = if (card) 0.dp else aboveGrid - timeRow - tileGap)) {
                        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides usualTextStyle) {
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
                                bottomFadeSpan = FADE_SPAN * 1.6f,
                            )
                        }
                    }
                } else {
                    DialTitleBlock(
                        song = song,
                        playingFrom = playingFrom,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = contentTop + (if (card) 44.dp else 52.dp))
                            .popIn(expand, active, order = 1),
                    )
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
                        .padding(start = 24.dp, end = 24.dp, bottom = if (dialDots) 12.dp else 18.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DialTimecode(positionMs, durationMs, onSeek, onWinding, pixels = dialDots, small = false)
                }
            }

            if (card) panelLayer()
        }

        if (card) {
            DialCard(
                song = song,
                mode = backgroundMode,
                intensity = blurIntensity,
                topCorner = cardTopCorner,
                modifier = Modifier
                    .padding(start = gridSide - 4.dp, end = gridSide - 4.dp, top = statusTop + 4.dp, bottom = aboveGrid)
                    .fillMaxSize()
                    .cardPop(expand, active),
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
                    .padding(bottom = groupBottom)
                    .width(gridWidth),
                verticalArrangement = Arrangement.spacedBy(tileGap),
            ) {
                if (!card) {
                    // The timecode: elapsed time at the grid's left edge, the full length at its right.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .popIn(expand, active, order = 3)
                            .height(timeRow)
                            .padding(horizontal = if (dialDots) 0.dp else 6.dp)
                            // Gives way to the queue panel as it opens, so no edge of the digits peeks out under it.
                            .graphicsLayer { alpha = 1f - queueShown.coerceIn(0f, 1f) },
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        DialTimecode(positionMs, durationMs, onSeek, onWinding, pixels = dialDots, small = false)
                    }
                }
                // The two rows come in one after the other: the upper one first, the lower a beat later.
                Row(modifier = Modifier.popIn(expand, active, order = 3), horizontalArrangement = Arrangement.spacedBy(tileGap), verticalAlignment = Alignment.Bottom) {
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
                Row(modifier = Modifier.popIn(expand, active, order = 4), horizontalArrangement = Arrangement.spacedBy(tileGap)) {
                    DialTile(filled = true, accent = onTheme, onClick = onTogglePlayPause, modifier = Modifier.size(tile), shape = RoundedCornerShape(topStart = DIAL_CORNER, topEnd = DIAL_CORNER, bottomEnd = DIAL_CORNER, bottomStart = outerTileCorner)) {
                        PlayPauseIcon(isPlaying = shownPlaying, tint = if (onTheme) PlayerColors.OnAccent else PlayerColors.Background, modifier = Modifier.size(38.dp))
                    }
                    DialTile(filled = false, onClick = onSkipPrevious, modifier = Modifier.size(tile)) {
                        Icon(AppIcons.SkipPrevious, stringResource(R.string.cd_previous_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(34.dp))
                    }
                    DialTile(filled = false, onClick = onSkipNext, modifier = Modifier.size(tile), shape = RoundedCornerShape(topStart = DIAL_CORNER, topEnd = DIAL_CORNER, bottomStart = DIAL_CORNER, bottomEnd = outerTileCorner)) {
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

/**
 * The minimal "Диск": the title, a big disc, the time and three big buttons — nothing else. The disc turns under a finger
 * and winds the song, a tap on it opens the queue, a long press puts the lyrics in its place (and back).
 * With a blur behind it the disc is frosted, like the card; on the plain background it is the cover's colour.
 */
@Composable
internal fun DialMinimalContent(
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
    backgroundMode: NowPlayingBackgroundMode,
    onClose: () -> Unit,
    onToggleRomanization: () -> Unit,
    onToggleLyrics: () -> Unit,
    queueOpen: Boolean,
    onQueueOpenChange: (Boolean) -> Unit,
    queuePanel: @Composable (Modifier) -> Unit,
) {
    val outerDensity = LocalDensity.current
    // The text style as the rest of the app has it: the lyrics keep it whatever font the Disc is set in.
    val usualTextStyle = androidx.compose.material3.LocalTextStyle.current
    val dialDensity = remember(outerDensity.density, outerDensity.fontScale) {
        androidx.compose.ui.unit.Density(outerDensity.density, minOf(outerDensity.fontScale, DIAL_MAX_FONT_SCALE))
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides dialDensity,
        androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.let { base -> com.artemiy.player.ui.theme.LocalDiscFont.current?.let { base.copy(fontFamily = it) } ?: base },
    ) {
    val density = LocalDensity.current
    // Winding the song with the disc: the play button and the disc keep to what they showed when the finger went down.
    var windHold by remember { mutableStateOf(false) }
    var windHeldPlaying by remember { mutableStateOf(isPlaying) }
    var windingNow by remember { mutableStateOf(false) }
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
    val dialDots = LocalDialDots.current
    val blurred = backgroundMode != NowPlayingBackgroundMode.NONE
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val side = 16.dp
        val gap = 10.dp
        // Three flat keys in one strip, like a tape deck's.
        val keyHeight = 76.dp
        val buttonsWidth = maxWidth - side * 2
        val groupBottom = barHeight + 8.dp
        val timeRowHeight = if (dialDots) 74.dp else 36.dp
        // The disc is one size whatever the title does: what the width allows, or what is left of the height once the
        // buttons, the time and room for a long title (three lines and the artist's line) are taken out. It sits at the bottom
        // of its space, so a longer title only uses the room above it.
        val discSize = minOf(
            maxWidth - 48.dp,
            maxHeight - statusTop - 52.dp - timeRowHeight - gap - keyHeight - groupBottom - 12.dp - 160.dp,
        ).coerceAtLeast(150.dp)

        BackHandler(enabled = queueOpen && LocalNowPlayingActive.current) { onQueueOpenChange(false) }
        val queueShown by animateFloatAsState(
            if (queueOpen) 1f else 0f,
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            label = "dialMinimalQueue",
        )
        // The disc's colours live here, outside the disc/lyrics switch, so that coming back from the lyrics finds them as they were.
                            // Frosted (a veil of the text colour over the blur) — or, on the plain background, the cover's own bright colour.
                            val coverColor = com.artemiy.player.ui.components.rememberCoverTone(song?.uri)
                            // Three colours the cover itself has (or, for a cover of one colour, that colour paler and deeper), calmed a
                            // little, eased between songs rather than switching to the next song's at once; on the plain background
                            // they are laid round the disc as a sweep, and turn with it.
                            val swatches = com.artemiy.player.ui.components.rememberCoverSwatches(song?.uri)
                            fun calm(c: Color, valueCap: Float): Color {
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(c.toArgb(), hsv)
                                // The cover's own colours are often dark (a dark cover has dark colours): kept in hue, but lifted to a light floor so the disc is never dull.
                                return Color.hsv(hsv[0], (hsv[1] * 0.75f).coerceIn(0.30f, 0.62f), (0.62f + 0.32f * hsv[2]).coerceIn(0.70f, valueCap))
                            }
                            // A cover with no colour of its own (black and white): the app's accent, paler and deeper, as for a one-colour cover.
                            val accentHsv = FloatArray(3).also { android.graphics.Color.colorToHSV(PlayerColors.Accent.toArgb(), it) }
                            val accentSet = listOf(
                                PlayerColors.Accent,
                                Color.hsv((accentHsv[0] + 14f) % 360f, accentHsv[1] * 0.55f, minOf(1f, accentHsv[2] * 1.15f)),
                                Color.hsv((accentHsv[0] + 346f) % 360f, minOf(1f, accentHsv[1] * 1.05f), accentHsv[2] * 0.7f),
                            )
                            val usable = swatches ?: accentSet
                            val targetA = calm(usable[0], 0.88f)
                            val targetB = calm(usable[1], 0.84f)
                            val targetC = calm(usable[2], 0.90f)
                            val paintA by androidx.compose.animation.animateColorAsState(targetA, tween(700), label = "discA")
                            val paintB by androidx.compose.animation.animateColorAsState(targetB, tween(700), label = "discB")
                            val paintC by androidx.compose.animation.animateColorAsState(targetC, tween(700), label = "discC")
                            val discPaint by androidx.compose.animation.animateColorAsState(
                                if (blurred) PlayerColors.TextPrimary.copy(alpha = 0.18f) else paintA,
                                tween(700),
                                label = "discPaint",
                            )
                            val discSweep = androidx.compose.ui.graphics.Brush.sweepGradient(listOf(paintA, paintB, paintC, paintA))
                            val markPaint by androidx.compose.animation.animateColorAsState(
                                if (blurred) PlayerColors.TextPrimary.copy(alpha = 0.55f) else PlayerColors.Background.copy(alpha = 0.8f),
                                tween(700),
                                label = "markPaint",
                            )

        Column(modifier = Modifier.fillMaxSize()) {
            // (The close handle and the romanization switch are drawn over the whole player, as in the full-screen look.)
            Spacer(modifier = Modifier.height(statusTop + 52.dp))
            DialTitleBlock(
                song = song,
                playingFrom = playingFrom,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp)
                    .popIn(expand, active, order = 1),
                artistOnTop = true,
            )
            // The disc — or, held down, the lyrics in its place.
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).popIn(expand, active, order = 2, grow = true),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // The soft shadow under the disc lives out here, not inside the lyrics/disc fade: that fade is a layer, and a
                // layer cuts off whatever is drawn beyond its edge (the shadow's lower half went missing for a moment).
                val shadowAlpha by animateFloatAsState(if (showLyrics) 0f else 1f, tween(300), label = "discShadow")
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (dialDots) 26.dp else 12.dp)
                        .size(discSize)
                        .drawBehind {
                            val r = size.minDimension / 2f
                            val reach = r * 1.22f
                            drawCircle(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    0.55f to Color.Black.copy(alpha = 0.22f * shadowAlpha),
                                    0.80f to Color.Black.copy(alpha = 0.10f * shadowAlpha),
                                    1f to Color.Transparent,
                                    center = center + Offset(0f, 10.dp.toPx()),
                                    radius = reach,
                                ),
                                radius = reach,
                                center = center + Offset(0f, 10.dp.toPx()),
                            )
                        },
                )
                Crossfade(targetState = showLyrics, animationSpec = tween(300), label = "dialMinimalDiscOrLyrics") { lyricsShown ->
                    if (lyricsShown) {
                        if (LocalNowPlayingActive.current) com.artemiy.player.ui.components.KeepScreenOn()
                        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides usualTextStyle) {
                            LyricsView(
                                lyrics = lyrics,
                                positionMs = positionMs,
                                isPlaying = isPlaying,
                                topFadePx = { 0 },
                                bottomFadePx = { 0 },
                                anchorTopPx = 0,
                                anchorBottomPx = 0,
                                onLineClick = { timeMs ->
                                    onSeek(timeMs)
                                    if (lyricsTapPlays && !isPlaying) onTogglePlayPause()
                                },
                                onLineLongClick = onLineLongClick,
                                showRomanization = lyricsRomanization,
                                loading = lyricsLoading,
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = FADE_SPAN, bottom = FADE_SPAN),
                                bottomFadeSpan = FADE_SPAN,
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = if (dialDots) 26.dp else 12.dp), contentAlignment = Alignment.BottomCenter) {
                            DialPlate(
                                isPlaying = shownPlaying,
                                active = active,
                                positionMs = positionMs,
                                durationMs = durationMs,
                                onSeek = onSeek,
                                onWinding = onWinding,
                                modifier = Modifier.size(discSize),
                                bare = true,
                                discColor = discPaint,
                                markColor = markPaint,
                                discBrush = if (blurred) null else discSweep,
                                onTap = { onQueueOpenChange(!queueOpen) },
                                onLongPress = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (!showLyrics) onQueueOpenChange(false)
                                    onToggleLyrics()
                                },
                            )
                        }
                    }
                }
            }
            // The time, small, over where the dots will run.
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(buttonsWidth)
                    .height(timeRowHeight)
                    .padding(horizontal = if (dialDots) 0.dp else 6.dp)
                    .popIn(expand, active, order = 3)
                    .graphicsLayer { alpha = 1f - queueShown.coerceIn(0f, 1f) },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialTimecode(positionMs, durationMs, onSeek, onWinding, pixels = dialDots, small = true)
            }
            Spacer(modifier = Modifier.height(gap))
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = groupBottom)
                    .width(buttonsWidth)
                    .height(keyHeight)
                    .popIn(expand, active, order = 4),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DialKey(
                    onClick = onTogglePlayPause,
                    strong = true,
                    // Held down, play puts away whatever is open over the disc: the queue and the lyrics.
                    onLongClick = {
                        if (queueOpen || showLyrics) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (queueOpen) onQueueOpenChange(false)
                            if (showLyrics) onToggleLyrics()
                        }
                    },
                ) {
                    PlayPauseIcon(isPlaying = shownPlaying, tint = PlayerColors.TextPrimary, modifier = Modifier.size(36.dp))
                    // The little lamp of an old recorder: lit and breathing while the song plays, dim when it doesn't.
                    val lamp by androidx.compose.animation.core.rememberInfiniteTransition(label = "lamp").animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(900), androidx.compose.animation.core.RepeatMode.Reverse),
                        label = "lampBlink",
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(8.dp)
                            .graphicsLayer { alpha = if (shownPlaying) lamp else 0.25f }
                            .background(PlayerColors.Accent, androidx.compose.foundation.shape.CircleShape),
                    )
                }
                DialKey(onClick = onSkipPrevious) {
                    Icon(AppIcons.SkipPrevious, stringResource(R.string.cd_previous_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(34.dp))
                }
                DialKey(onClick = onSkipNext) {
                    Icon(AppIcons.SkipNext, stringResource(R.string.cd_next_track), tint = PlayerColors.TextPrimary, modifier = Modifier.size(34.dp))
                }
            }
        }

        // The queue panel: over the title and the disc, stopping above the buttons (held-down play, or back, closes it).
        if (queueShown > 0.001f) {
            queuePanel(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = side, end = side, top = statusTop + 14.dp, bottom = groupBottom + keyHeight + 2.dp)
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = queueShown.coerceIn(0f, 1f)
                        translationY = (1f - queueShown) * 48.dp.toPx()
                    },
            )
        }
    }
    }
}

/** "Playing from …", the title and the artist of the Dial's screen (and of the minimal look): a tap goes to the album / artist, a long press opens the song's menu. */
@Composable
private fun DialTitleBlock(
    song: Song?,
    playingFrom: PlaySource?,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier,
    /** The minimal look: the artist is the small line above the title (where "Playing from" stood) and has no big lines of its own. */
    artistOnTop: Boolean = false,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
                        Column {
                            // "Играет из …": small and quiet, above the title.
                            val playingFromLabel = stringResource(R.string.playing_from)
                            val queueLabel = stringResource(R.string.playing_from_queue)
                            if (artistOnTop) {
                                val allNames = remember(song?.artist) { song?.artists().orEmpty().joinToString(", ").ifBlank { song?.artist.orEmpty() } }
                                Text(
                                    text = if (com.artemiy.player.ui.theme.LocalDiscFont.current != null) androidx.compose.ui.text.AnnotatedString(allNames) else com.artemiy.player.ui.theme.lyricAnnotated(allNames),
                                    color = LocalAdaptiveSecondaryColor.current,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .padding(bottom = 10.dp)
                                        .marquee(LocalNowPlayingActive.current)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { song?.let(onGoToArtist) },
                                )
                            } else Text(
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
                            if (!artistOnTop) Row(verticalAlignment = Alignment.CenterVertically) {
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
    topCorner: androidx.compose.ui.unit.Dp = DIAL_CARD_CORNER,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(topStart = topCorner, topEnd = topCorner, bottomStart = DIAL_CARD_CORNER, bottomEnd = DIAL_CARD_CORNER))) {
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
internal fun PlayerSurface(
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
internal fun DialQueuePanel(
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
    /** The minimal look: a square panel and flat keys for the modes, like the strip below it. */
    flat: Boolean = false,
) {
    val appPalette = com.artemiy.player.ui.theme.LocalAppPalette.current
    QueueSheetPalette(appPalette) {
        Column(
            modifier = modifier
                .then(if (flat) Modifier else Modifier.clip(RoundedCornerShape(DIAL_CORNER)))
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
                modifier = if (flat) Modifier else Modifier.padding(start = DIAL_MODES_INSET, end = DIAL_MODES_INSET, bottom = DIAL_MODES_INSET),
                flat = flat,
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
    flat: Boolean = false,
) {
    // Lit: the app's accent (or, when plain, the text color itself, as the play tile is white).
    val accent = if (accented) PlayerColors.Accent else PlayerColors.TextPrimary
    val onAccent = if (accented) PlayerColors.OnAccent else PlayerColors.Background
    val offFill = PlayerColors.TextPrimary.copy(alpha = 0.10f)
    val follow = DIAL_CORNER - DIAL_MODES_INSET
    val inner = 12.dp
    Row(modifier = modifier.fillMaxWidth().height(if (flat) 56.dp else 54.dp), horizontalArrangement = Arrangement.spacedBy(if (flat) 2.dp else 6.dp)) {
        listOf(
            Triple(AppIcons.Shuffle to stringResource(R.string.shuffle), shuffleEnabled, onToggleShuffle),
            Triple(AppIcons.Repeat to stringResource(R.string.repeat), repeatEnabled, onToggleRepeat),
            Triple(AppIcons.Infinite to stringResource(R.string.endless_play), infinitePlayEnabled, onToggleInfinitePlay),
        ).forEachIndexed { index, (iconAndLabel, enabled, onClick) ->
            val interaction = remember { MutableInteractionSource() }
            val fill by animateColorAsState(
                if (flat) PlayerColors.TextPrimary.copy(alpha = if (enabled) 0.30f else 0.10f) else if (enabled) accent else offFill,
                tween(220),
                label = "dialModeFill",
            )
            val tint by animateColorAsState(if (flat) PlayerColors.TextPrimary else if (enabled) onAccent else PlayerColors.TextPrimary, tween(220), label = "dialModeTint")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (flat) Modifier else Modifier.pressScale(interaction, pressedScale = 0.95f))
                    .then(
                        if (flat) Modifier else Modifier.clip(
                            RoundedCornerShape(
                                topStart = inner,
                                topEnd = inner,
                                bottomStart = if (index == 0) follow else inner,
                                bottomEnd = if (index == 2) follow else inner,
                            ),
                        ),
                    )
                    .background(fill)
                    .clickable(interactionSource = interaction, indication = null, onClick = buttonTicked(onClick)),
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
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(DIAL_CORNER),
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
            .background(plate, shape)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = buttonTicked(onClick), onLongClick = onLongClick),
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
    /** The minimal look: just the disc, no square tile under it, drawn in [discColor] / [markColor] when given. */
    bare: Boolean = false,
    discColor: Color? = null,
    markColor: Color? = null,
    /** A tap and a long press on the disc (it still turns under a finger that moves). */
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    /** The minimal look: a very soft shadow behind the disc, and the disc painted with [discBrush] (it turns with the disc). */
    softShadow: Boolean = false,
    discBrush: androidx.compose.ui.graphics.Brush? = null,
) {
    val onTapNow by rememberUpdatedState(onTap)
    val onLongPressNow by rememberUpdatedState(onLongPress)
    // The turning is shared with the mini player's small plate, so the two always match.
    var angle by com.artemiy.player.ui.components.PlateRotation::angle
    var dragging by com.artemiy.player.ui.components.PlateRotation::dragging
    val positionNow by rememberUpdatedState(positionMs)
    val durationNow by rememberUpdatedState(durationMs)
    val onSeekNow by rememberUpdatedState(onSeek)
    val onWindingNow by rememberUpdatedState(onWinding)
    val context = LocalContext.current
    val scrubScope = androidx.compose.runtime.rememberCoroutineScope()
    // Never leave the song sped up or slowed if the plate goes away mid-wind.
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.artemiy.player.playback.ScrubSound.set?.invoke(1f) } }

    // Frame by frame: the speed eases toward its goal (full when playing, none when paused), so the
    // disc takes a moment to get going and keeps turning a moment after the pause. Idle when still.
    LaunchedEffect(isPlaying, active) {
        if (!active) return@LaunchedEffect
        while (true) {
            val now = withFrameNanos { it }
            if (!com.artemiy.player.ui.components.PlateRotation.tick(now, isPlaying)) break
        }
    }

    val disc = discColor ?: PlayerColors.TextPrimary
    val mark = markColor ?: PlayerColors.Background
    val seekLabel = stringResource(R.string.cd_plate_seek)
    Box(
        modifier = modifier
            .then(if (bare) Modifier else Modifier.clip(RoundedCornerShape(DIAL_CORNER)).background(PlayerColors.TextPrimary.copy(alpha = 0.14f)))
            .then(
                if (onTap != null || onLongPress != null) Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { onTapNow?.invoke() }, onLongPress = { onLongPressNow?.invoke() })
                } else Modifier,
            )
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
                var lastMoveAt = 0L
                var velocity = 0f
                var rate = 1f
                // Eases the pitch back to normal once the finger lets go.
                fun easeBack() {
                    scrubScope.launch {
                        var r = rate
                        while (kotlin.math.abs(r - 1f) > 0.02f) {
                            r += (1f - r) * 0.25f
                            com.artemiy.player.playback.ScrubSound.set?.invoke(r)
                            kotlinx.coroutines.delay(30)
                        }
                        com.artemiy.player.playback.ScrubSound.set?.invoke(1f)
                    }
                }
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
                            lastMoveAt = System.currentTimeMillis()
                            velocity = 0f
                            rate = 1f
                        }
                    },
                    onDragEnd = {
                        if (!blocked) {
                            easeBack()
                            onSeekNow(target.toLong())
                            dragging = false
                            onWindingNow(false)
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        if (!blocked) { easeBack(); onWindingNow(false) }
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
                    // How fast the plate is being turned (deg/s, signed), smoothed; the sound follows it.
                    val moveAt = System.currentTimeMillis()
                    val dtMs = (moveAt - lastMoveAt).coerceAtLeast(1L)
                    lastMoveAt = moveAt
                    velocity += (d / dtMs * 1000f - velocity) * 0.3f
                    rate = (1f + velocity / 360f).coerceIn(0.75f, 1.5f)
                    com.artemiy.player.playback.ScrubSound.set?.invoke(rate)
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
        if (softShadow) Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = 10.dp.toPx() }) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    0.55f to Color.Black.copy(alpha = 0.22f),
                    0.80f to Color.Black.copy(alpha = 0.10f),
                    1f to Color.Transparent,
                    center = center,
                    radius = r * 1.22f,
                ),
                radius = r * 1.22f,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (bare) 0.dp else 14.dp)
                .graphicsLayer { rotationZ = angle },
        ) {
            val r = size.minDimension / 2f
            if (discBrush != null) drawCircle(discBrush, radius = r) else drawCircle(disc, radius = r)
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
    // The hieroglyphs in the font picked for the lyrics (the phone's own may have no bold ones).
    // In the pixel font the kana and kanji are its own (it has them all); otherwise they are set in the lyrics' fonts.
    val shown = if (com.artemiy.player.ui.theme.LocalDiscFont.current != null) androidx.compose.ui.text.AnnotatedString(text) else com.artemiy.player.ui.theme.lyricAnnotated(text)
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth
        val density = LocalDensity.current
        val fitSp = remember(shown, widthPx, maxLines, baseStyle, density.fontScale) {
            var size = 36f
            var found: Float? = null
            while (size >= 28f) {
                val result = measurer.measure(
                    text = shown,
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
            text = shown,
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
private fun plateTick(context: android.content.Context) = buttonTick(context)

/**
 * The Dial's time: the big digits (elapsed on the left, the full length on the right) — or, with "Time as pixels", the
 * small digits over [DialDots].
 */
@Composable
private fun DialTimecode(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, onWinding: (Boolean) -> Unit, pixels: Boolean, small: Boolean, modifier: Modifier = Modifier) {
    if (!pixels) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(text = dialTime(positionMs), color = PlayerColors.TextPrimary, fontSize = if (small) 22.sp else 40.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                text = dialTime(durationMs),
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = if (small) 16.sp else 22.sp,
                maxLines = 1,
                modifier = if (small) Modifier else Modifier.padding(bottom = 4.dp),
            )
        }
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        // The row always runs the whole width (from the left edge of the first button to the right edge of the last).
        // The song's length sets how many squares that is: a short one has fewer, bigger ones (up to 10 dp), a long one
        // more, smaller ones (down to 5 dp) — so they always read as pixels, never as a bar.
        val gap = 4.dp
        val fullWidth = maxWidth
        // While the next song's length is not known yet (it reads 0) the row keeps the last one's look rather than jumping.
        val lastLength = remember { LongArray(1) }
        if (durationMs > 0) lastLength[0] = durationMs
        val shapedBy = if (durationMs > 0) durationMs else lastLength[0]
        val minColumns = kotlin.math.ceil((maxWidth + gap) / (DOT_MAX + gap)).toInt()
        val maxColumns = ((maxWidth + gap) / (DOT_MIN + gap)).toInt().coerceAtLeast(minColumns)
        val longness = ((shapedBy / 1000f - 60f) / 360f).coerceIn(0f, 1f)
        val columns = (minColumns + (maxColumns - minColumns) * longness).toInt().coerceIn(minColumns, maxColumns)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = dialTime(positionMs), color = PlayerColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(text = dialTime(durationMs), color = LocalAdaptiveSecondaryColor.current, fontSize = 14.sp, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // A different count of squares (the next song is a different length) fades over rather than jumping.
            androidx.compose.animation.Crossfade(targetState = columns, animationSpec = tween(260), label = "dialDotsLayout") { cols ->
                DialDots(positionMs, durationMs, cols, (fullWidth - gap * (cols - 1)) / cols, gap, onSeek, onWinding)
            }
        }
    }
}

/** The squares of the pixel time are at least this big... */
private val DOT_MIN = 5.dp
/** ...and at most this (the row's height is kept for the biggest). */
private val DOT_MAX = 10.dp

/**
 * Rows of small squares, three high, filled column by column (top to bottom, then the next column): the lit ones are the
 * time that has gone, the one in progress warms up as it goes. A tap or a drag along them sets the time.
 */
@Composable
private fun DialDots(
    positionMs: Long,
    durationMs: Long,
    columns: Int,
    dot: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    onSeek: (Long) -> Unit,
    onWinding: (Boolean) -> Unit,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val onWindingNow by rememberUpdatedState(onWinding)
    val durationNow by rememberUpdatedState(durationMs)
    val onSeekNow by rememberUpdatedState(onSeek)
    val played = PlayerColors.TextPrimary
    val rest = PlayerColors.TextPrimary.copy(alpha = 0.2f)
    val total = columns * 3
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(DOT_MAX * 3 + gap * 2)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (durationNow > 0) onSeekNow(((offset.x / size.width).coerceIn(0f, 1f) * durationNow).toLong())
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        // The disc is wound along with the squares: held still by the finger's hand, it turns with it.
                        com.artemiy.player.ui.components.PlateRotation.dragging = true
                        onWindingNow(true)
                    },
                    onDragEnd = {
                        dragFraction?.let { if (durationNow > 0) onSeekNow((it * durationNow).toLong()) }
                        dragFraction = null
                        com.artemiy.player.ui.components.PlateRotation.dragging = false
                        onWindingNow(false)
                    },
                    onDragCancel = {
                        dragFraction = null
                        com.artemiy.player.ui.components.PlateRotation.dragging = false
                        onWindingNow(false)
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val before = dragFraction ?: 0f
                    val now = (change.position.x / size.width).coerceIn(0f, 1f)
                    dragFraction = now
                    // Turned as the plate itself is: DIAL_SECONDS_PER_TURN of the song to a turn, clockwise forward.
                    val seconds = (now - before) * durationNow / 1000f
                    com.artemiy.player.ui.components.PlateRotation.angle =
                        (com.artemiy.player.ui.components.PlateRotation.angle + seconds / DIAL_SECONDS_PER_TURN * 360f) % 360f
                }
            },
    ) {
        val fraction = dragFraction ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        val exact = fraction * total
        val lit = exact.toInt()
        val partial = exact - lit
        val side = dot.toPx()
        val pitch = side + gap.toPx()
        val corner = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        val top = (size.height - (side * 3 + gap.toPx() * 2)) / 2f
        for (i in 0 until total) {
            val color = when {
                i < lit -> played
                i == lit -> androidx.compose.ui.graphics.lerp(rest, played, partial)
                else -> rest
            }
            drawRoundRect(
                color = color,
                topLeft = Offset((i / 3) * pitch, top + (i % 3) * pitch),
                size = androidx.compose.ui.geometry.Size(side, side),
                cornerRadius = corner,
            )
        }
    }
}

/**
 * One flat key of the minimal look's strip (no rounded corners, like the keys of a tape deck): a faint plate, a notch lighter
 * when [strong]; pressed, it sinks a little. [onLongClick] gives it a second job.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun androidx.compose.foundation.layout.RowScope.DialKey(
    onClick: () -> Unit,
    strong: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by androidx.compose.animation.core.animateFloatAsState(if (pressed) 1f else 0f, tween(70), label = "keySink")
    val plate = PlayerColors.TextPrimary.copy(alpha = (if (strong) 0.24f else 0.13f) + 0.07f * sink)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer { translationY = 3.dp.toPx() * sink }
            .background(plate)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = buttonTicked(onClick), onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
