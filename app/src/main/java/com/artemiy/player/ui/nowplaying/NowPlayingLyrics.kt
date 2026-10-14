package com.artemiy.player.ui.nowplaying

import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.FastOutSlowInEasing
import com.artemiy.player.ui.theme.inAppFont
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.lyrics.LyricLine
import com.artemiy.player.lyrics.LyricVoice
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.lyrics.RubySegment
import com.artemiy.player.ui.components.LoadingMark
import com.artemiy.player.lyrics.withInstrumentalBreaks
import com.artemiy.player.lyrics.LyricWord
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import com.artemiy.player.ui.theme.PlayerColors

// The Lyrics view: synced/unsynced lines, word sweep, glow, romanization readings.

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun LyricsView(
    lyrics: ParsedLyrics?,
    positionMs: Long,
    isPlaying: Boolean,
    topFadePx: () -> Int,
    bottomFadePx: () -> Int,
    anchorTopPx: Int,
    anchorBottomPx: Int,
    onLineClick: (timeMs: Long) -> Unit,
    /** A line held down: its place in the lyrics' plain list of lines, for the share card. */
    onLineLongClick: (Int) -> Unit = {},
    showRomanization: Boolean,
    contentPadding: PaddingValues = PaddingValues(top = 16.dp, bottom = 220.dp),
    loading: Boolean = false,
) {
    // The controller only reports a fresh position every ~100ms, which is far too coarse for a
    // per-word karaoke sweep — it'd visibly step instead of glide. Interpolate every frame
    // between polls using elapsed wall-clock time, and resync to the real value on every poll
    // (or on seek/pause/play) so drift never exceeds one poll interval.
    var smoothPositionMs by remember { mutableStateOf(positionMs) }
    val active = LocalNowPlayingActive.current
    // Where the player last said it was, and when (the poll comes ~10 times a second).
    val lastPoll = remember { longArrayOf(positionMs, android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(positionMs) {
        lastPoll[0] = positionMs
        lastPoll[1] = android.os.SystemClock.elapsedRealtime()
        // Paused (e.g. seeking while paused): no clock running, just show where it is.
        if (!isPlaying || !active) smoothPositionMs = positionMs
    }
    LaunchedEffect(isPlaying, active) {
        // Starting (again, e.g. after a pause): the last poll is "now" — a paused player keeps
        // reporting the same position, so its timestamp would be from when it stopped, and the
        // clock would leap ahead by the whole pause for a moment.
        lastPoll[0] = positionMs
        lastPoll[1] = android.os.SystemClock.elapsedRealtime()
        smoothPositionMs = positionMs
        if (!isPlaying || !active) return@LaunchedEffect
        // One continuous clock for the whole time it plays (restarting it on every poll lost a
        // frame each time and made it lag behind). It never jumps and never runs backwards — that
        // made the sweep twitch back — it just runs a touch faster or slower than real time until
        // it matches where the player really is. Only a seek (a big difference) is taken at once.
        var smooth = smoothPositionMs.toDouble()
        var lastFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val dt = (frame - lastFrame) / 1_000_000.0
            lastFrame = frame
            val target = lastPoll[0] + (android.os.SystemClock.elapsedRealtime() - lastPoll[1])
            val error = target - smooth
            smooth = if (kotlin.math.abs(error) > SMOOTH_SNAP_MS) {
                target.toDouble()
            } else {
                smooth + dt * (1.0 + error / 250.0).coerceIn(0.5, 1.5)
            }
            smoothPositionMs = smooth.toLong()
        }
    }
    when (lyrics) {
        // Centered in the visible gap between the header and the controls, not in the whole area
        // (whose lower part sits under the controls).
        null -> if (loading && active) Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding()),
            contentAlignment = Alignment.Center,
        ) {
            LoadingMark(color = LocalAdaptiveSecondaryColor.current, modifier = Modifier.size(44.dp))
        } else Box(
            // No lyrics: said where they'd have begun — top left, in the lyrics' own weight —
            // rather than floating in the middle between the header and the controls.
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(top = 12.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            // As big as the lyrics would be, and broken into rows the same way they are — no lone
            // last word ("…just the / tune").
            val message = stringResource(R.string.lyrics_not_found)
            val color = LocalAdaptiveSecondaryColor.current
            val words = remember(message) { if (message.contains(' ')) WORD_UNIT.findAll(message).map { it.value }.toList() else null }
            if (words == null) {
                Text(
                    text = message,
                    color = color,
                    fontSize = NO_LYRICS_SIZE,
                    lineHeight = NO_LYRICS_SIZE * 1.25f,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading).inAppFont(),
                )
            } else {
                BalancedFlow(words = words, alignEnd = false, rowGap = 0.dp) {
                    words.forEachIndexed { i, word ->
                        Text(
                            text = if (i == words.lastIndex) word.trimEnd() else word,
                            color = color,
                            fontSize = NO_LYRICS_SIZE,
                            lineHeight = NO_LYRICS_SIZE * 1.25f,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            style = TextStyle().inAppFont(),
                        )
                    }
                }
            }
        }

        is ParsedLyrics.Unsynced -> {
            // One list item per line (not one giant text block) so fadeInList() can fade each
            // line out under the header/controls like synced lyrics do.
            val listState = rememberLazyListState()
            val textLines = remember(lyrics.text) { lyrics.text.lines() }
            val rubyLines = lyrics.rubyLines?.takeIf { showRomanization }
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                itemsIndexed(textLines) { index, textLine ->
                    val ruby = rubyLines?.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadeInList(listState, index, topFadePx, bottomFadePx)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = {
                                    if (textLine.isNotBlank()) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onLineLongClick(textLines.take(index).count { it.isNotBlank() })
                                    }
                                },
                            ),
                    ) {
                        if (ruby != null) UnsyncedRubyLine(ruby) else UnsyncedLine(textLine)
                    }
                }
            }
        }

        is ParsedLyrics.Synced -> {
            // -1 during an instrumental intro, before the first line starts — must stay -1, not
            // be clamped to 0, or line 0 lights up (big font, autoscroll, gray first word) early.
            // The lines plus instrumental breaks (three dots) where nothing is sung.
            val gapDots = LocalLrcGapDots.current
            val lines = remember(lyrics, gapDots) { withInstrumentalBreaks(lyrics.lines, lrcGapDots = gapDots) }
            // Judged by the smooth clock that moves the sweeps, not by the player's coarser poll: with
            // two clocks, the next line could start (and the last one stop, jumping its sweep to the
            // end) a moment before the sweep had actually got there.
            val activeIndexState = remember(lines) { derivedStateOf { lines.indexOfLast { it.timeMs <= smoothPositionMs } } }
            val activeIndex = activeIndexState.value
            // Lines still being finished while a later one has already started: the file gives
            // them an end time past the next line's start (overlapping vocals). They stay lit and
            // keep their sweep going until then, alongside the new one.
            val stillSinging by remember(lines) {
                derivedStateOf {
                    (0 until activeIndexState.value.coerceAtLeast(0)).filterTo(HashSet()) { i ->
                        val line = lines[i]
                        line.instrumentalUntilMs == null && line.singingEndMs() > smoothPositionMs
                    }
                }
            }
            val plainLines = remember(lyrics) { lyrics.lines.filter { it.text.isNotBlank() } }
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            val listState = rememberLazyListState()
            // How fast the lines are gliding right now — a new line coming before the last glide
            // has settled carries on from this speed instead of stopping and starting over, which
            // showed as a small jerk when lines came quickly one after another.
            val scrollVelocity = remember { floatArrayOf(0f) }
            var lastAnchorBottomPx by remember { mutableStateOf(anchorBottomPx) }
            // Scrolled through by hand: the lines are left where the finger put them, and a couple
            // of seconds after it lets go (and any fling has settled) they glide back to the one
            // being sung.
            val dragged by listState.interactionSource.collectIsDraggedAsState()
            var recenter by remember { mutableIntStateOf(0) }
            var browsed by remember { mutableStateOf(false) }
            LaunchedEffect(dragged) {
                if (dragged) {
                    browsed = true
                    return@LaunchedEffect
                }
                if (!browsed) return@LaunchedEffect
                delay(RECENTER_AFTER_MS)
                while (listState.isScrollInProgress) delay(100)
                browsed = false
                recenter++
            }
            LaunchedEffect(activeIndex, anchorTopPx, anchorBottomPx, recenter) {
                if (browsed) return@LaunchedEffect
                val panelToggled = anchorBottomPx != lastAnchorBottomPx
                lastAnchorBottomPx = anchorBottomPx
                // How far the line sits from its mark. The mark is within the gap that's actually
                // visible between the header and the control panel (or the screen bottom when the
                // panel is hidden): dead center of it while the panel is up, a bit above center
                // otherwise. No artificial padding backs this — near the start/end of a song the
                // scroll simply clamps instead of faking empty space to reach the mark.
                fun offFromMark(info: androidx.compose.foundation.lazy.LazyListLayoutInfo, item: androidx.compose.foundation.lazy.LazyListItemInfo): Float {
                    val viewportHeight = info.viewportSize.height
                    val gap = (viewportHeight - anchorTopPx - anchorBottomPx).coerceAtLeast(0)
                    val fraction = if (anchorBottomPx > 0) 0.5f else 0.4f
                    val anchor = anchorTopPx + gap * fraction
                    // offset is measured from the end of the top content padding, not from the
                    // top of the list (viewportStartOffset == -beforeContentPadding).
                    return item.offset + item.size / 2f - info.viewportStartOffset - anchor
                }
                val info = listState.layoutInfo
                val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == activeIndex }
                if (itemInfo != null) {
                    // Panel show/hide: a quick, eased glide to the new spot. Line changes: a soft,
                    // unhurried spring (the default one snapped over a little abruptly).
                    if (panelToggled) {
                        listState.animateScrollBy(offFromMark(info, itemInfo), tween(320, easing = FastOutSlowInEasing))
                    } else {
                        listState.glideBy(offFromMark(info, itemInfo), scrollVelocity)
                    }
                } else {
                    // The line is far from what's on screen — the lyrics were just opened in the
                    // middle of a song, the track changed, or a seek landed elsewhere. Jump to its
                    // neighbourhood at once, and as soon as it has been laid out put it on its mark
                    // (landing "roughly nearby" and stopping there left it a little low until the
                    // lyrics were scrolled by hand).
                    listState.scrollToItem((activeIndex - 2).coerceAtLeast(0))
                    val landed = kotlinx.coroutines.withTimeoutOrNull(600) {
                        snapshotFlow { listState.layoutInfo }.first { li -> li.visibleItemsInfo.any { it.index == activeIndex } }
                    }
                    landed?.visibleItemsInfo?.firstOrNull { it.index == activeIndex }?.let { item ->
                        listState.scrollBy(offFromMark(landed, item))
                    }
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
                itemsIndexed(lines) { index, line ->
                    val active = index == activeIndex || index in stillSinging
                    // Only a line being sung follows the smooth per-frame clock; the rest get the
                    // player's ~10-a-second position — reading the clock made every visible line
                    // rebuild itself 120 times a second.
                    // A line already behind the sung one is simply finished: it doesn't read the (coarser,
                    // slightly older) player position, which could draw its last word a hair short again.
                    val linePositionMs = when {
                        active -> smoothPositionMs
                        index < activeIndex && line.instrumentalUntilMs == null -> maxOf(positionMs, line.singingEndMs())
                        else -> positionMs
                    }
                    val alpha by animateFloatAsState(if (active) 1f else 0.35f, LINE_CHANGE, label = "lineAlpha")
                    // Every line is laid out at the sung size and inactive ones are only shrunk
                    // visually, so a line wraps the same way whether it's being sung or not —
                    // re-laying it out at a bigger font made words jump to the next row mid-song.
                    val scale by animateFloatAsState(if (active) 1f else INACTIVE_LYRIC_SCALE, LINE_CHANGE, label = "lineScale")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadeInList(listState, index, topFadePx, bottomFadePx)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onLineClick(line.timeMs.coerceAtLeast(0L)) },
                                onLongClick = {
                                    if (line.instrumentalUntilMs == null) {
                                        val place = plainLines.indexOfFirst { it.timeMs == line.timeMs }
                                        if (place >= 0) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onLineLongClick(place)
                                        }
                                    }
                                },
                            ),
                    ) {
                        val nextLineStartMs = lines.getOrNull(index + 1)?.timeMs
                        val breakUntil = line.instrumentalUntilMs
                        if (breakUntil != null) {
                            InstrumentalDots(
                                startMs = line.timeMs,
                                endMs = breakUntil,
                                positionMs = linePositionMs,
                                active = active,
                                alignEnd = line.voice == LyricVoice.V2,
                                playing = isPlaying,
                            )
                        } else SungLine(
                            line = line,
                            active = active,
                            alpha = alpha,
                            scale = scale,
                            positionMs = linePositionMs,
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
                                    positionMs = linePositionMs,
                                    nextLineStartMs = nextLineStartMs,
                                    showRomanization = showRomanization,
                                )
                            } else {
                                val alignEnd = line.voice == LyricVoice.V2
                                Box(modifier = Modifier.lyricScale(scale, alignEnd)) {
                                    SecondaryLyricLine(
                                        line = secondary,
                                        positionMs = linePositionMs,
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

/**
 * The font size for a line: [LYRIC_SIZE], unless the line is so long (a whole paragraph sung as
 * one line) that it would be taller than [MAX_LINE_SCREEN_SHARE] of the screen and reach under
 * the controls — then just small enough to fit. Ordinary lines never get here.
 */
@Composable
private fun fittedLyricSize(text: String, withReadings: Boolean): Float {
    if (text.length < FIT_CHECK_MIN_CHARS) return LYRIC_SIZE
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val family = com.artemiy.player.ui.theme.LocalAppFontFamily.current
    return remember(text, withReadings, configuration.screenWidthDp, configuration.screenHeightDp, family) {
        val widthPx = with(density) { (configuration.screenWidthDp - 44).dp.roundToPx() }.coerceAtLeast(1)
        val maxHeightPx = with(density) { (configuration.screenHeightDp * MAX_LINE_SCREEN_SHARE).dp.toPx() }
        var size = LYRIC_SIZE
        while (size > LYRIC_SIZE * 0.5f) {
            val rows = measurer.measure(
                text,
                TextStyle(fontSize = size.sp, fontWeight = FontWeight.ExtraBold, fontFamily = family),
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
            ).lineCount
            // Each row: the text, its reading above it (when shown) and the gap between rows.
            // Word-by-word layout wraps a bit worse than plain text, hence the extra 15%.
            val rowPx = with(density) {
                (size * 1.2f).sp.toPx() + (if (withReadings) (size * READING_SCALE * 1.2f).sp.toPx() else 0f) + 8.dp.toPx()
            }
            if (rows * rowPx * 1.15f <= maxHeightPx) break
            size *= 0.92f
        }
        size
    }
}

/** Lines shorter than this always fit; only longer ones are measured. */
private const val FIT_CHECK_MIN_CHARS = 40

/** The most of the screen's height one lyric line may take. */
private const val MAX_LINE_SCREEN_SHARE = 0.4f

/** When a line is really done: its own last word, or its background vocals / the other singer's
 * part if those go on longer — the line stays lit (and they keep sweeping) until then. */
private fun LyricLine.singingEndMs(): Long {
    fun LyricLine.ownEnd(): Long = endTimeMs ?: words?.last()?.timeMs?.plus(400L) ?: Long.MIN_VALUE
    val background = background?.ownEnd() ?: Long.MIN_VALUE
    val others = secondary.maxOfOrNull { it.ownEnd() } ?: Long.MIN_VALUE
    // Without its own end time a line only counts through its extras, as before.
    return maxOf(endTimeMs ?: Long.MIN_VALUE, background, others)
}

/** Font size every synced lyric line is laid out at (the size of the line being sung). */
internal const val LYRIC_SIZE = 33f

/** Lines not being sung are drawn at 30/33 of that, by scaling — see the item in [LyricsView]. */
internal const val INACTIVE_LYRIC_SCALE = 30f / 33f

/** Visual-only scale, anchored to the side the line is aligned to so it shrinks toward it. */
internal fun Modifier.lyricScale(scale: Float, alignEnd: Boolean): Modifier = graphicsLayer {
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
internal fun SungLine(
    line: LyricLine,
    active: Boolean,
    alpha: Float,
    scale: Float,
    positionMs: Long,
    nextLineStartMs: Long?,
    showRomanization: Boolean,
) = Column(modifier = Modifier.fillMaxWidth().lyricScale(scale, line.voice == LyricVoice.V2)) {
    // A line just becoming the sung one: its words start out fully lit (as an inactive line shows
    // them) and ease down to the "not sung yet" brightness together with the line brightening —
    // switching them at once made every line flicker darker for a moment.
    val unsungAlpha by animateFloatAsState(if (active) 0.55f else 1f, LINE_CHANGE, label = "unsungWords")
    val alignEnd = line.voice == LyricVoice.V2
    val background = line.background
    val bottomPadding = if (background != null) 2.dp else 8.dp
    val readings = line.wordReadings?.takeIf { showRomanization }
    val ruby = line.ruby?.takeIf { showRomanization }
    val fontSize = fittedLyricSize(line.text, withReadings = readings != null || ruby != null)
    // A light app theme with no blurred background: the text is dark, and a dark glow around dark text only
    // smudges it — so no glow there (the blurred backgrounds are always dark, and keep theirs).
    val glowOn = !com.artemiy.player.ui.theme.LocalPlayerPalette.current.isLight
    // The glow fades out after the line stops being the sung one (when nothing else lights up in
    // its place — dots, a pause — it was visibly cut off), and comes on at once when it becomes it.
    val glowFade by animateFloatAsState(
        if (active) 1f else 0f,
        if (active) androidx.compose.animation.core.snap() else tween(500, easing = LINE_EASE),
        label = "lineGlow",
    )
    val glowShown = glowOn && glowFade > 0.01f
    when {
        line.words != null -> {
            // Word-synced lines keep the same per-word layout whether sung or not (only the
            // sweep is off), so their words wrap identically in both states.
            // The glow pass is the exact same word-by-word composable, just recolored/blurred,
            // so it lines up pixel-for-pixel with the crisp text on top and sweeps forward in
            // lockstep instead of glowing ahead of what's been sung.
            Box {
                if (glowShown) WordSyncedLine(line, positionMs, alpha * glowFade, fontSize.sp, nextLineStartMs, alignEnd, 8.dp, bottomPadding, readings = readings, glow = true, unsungAlpha = unsungAlpha)
                WordSyncedLine(line, positionMs, alpha, fontSize.sp, nextLineStartMs, alignEnd, 8.dp, bottomPadding, readings = readings, sweep = active, unsungAlpha = unsungAlpha)
            }
        }
        ruby != null -> Box {
            if (glowShown) RubyLine(ruby, alpha * glowFade, fontSize, alignEnd, bottomPadding, glow = true)
            RubyLine(ruby, alpha, fontSize, alignEnd, bottomPadding)
        }
        // Plain LRC has no per-word timing, so the whole active line glows at once.
        else -> Box {
            if (glowShown) PlainLyricLine(line.text, alpha * glowFade, fontSize, alignEnd, 8.dp, bottomPadding, glow = true)
            PlainLyricLine(line.text, alpha, fontSize, alignEnd, 8.dp, bottomPadding)
        }
    }
    if (background != null) {
        val mainLineEndMs = line.endTimeMs ?: line.words?.last()?.timeMs?.plus(400L) ?: nextLineStartMs ?: line.timeMs
        BackgroundVocals(background, active, alpha, fontSize, positionMs, mainLineEndMs, alignEnd)
    }
}

/** Background vocals: a small, dimmer line under the main one that grows a little from the
 * moment it starts until both it and the main line above it are done, then settles back. */
@Composable
internal fun BackgroundVocals(
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
    // Like the main line: the not-yet-sung words ease back to full as the line ends, instead of
    // all lighting up in one frame (a flicker at the end of every line with backing vocals).
    // Backing vocals often go on after the main line is done: their words keep lighting up as
    // they're sung, rather than all at once the moment the main line ends (a flash of white).
    // Once they've begun, they stay swept — past the end every word is sung anyway.
    val swept = lineActive || positionMs >= startMs
    val unsungAlpha by animateFloatAsState(if (swept) 0.55f else 1f, LINE_CHANGE, label = "backgroundUnsung")
    Box(modifier = Modifier.lyricScale(scale, alignEnd)) {
        if (words != null) {
            // The same glow as the main line's, following the words as they're sung — only softer.
            if (lineActive && !com.artemiy.player.ui.theme.LocalPlayerPalette.current.isLight) {
                WordSyncedLine(background, positionMs, backgroundAlpha * BACKGROUND_GLOW, grownSize.sp, null, alignEnd, 6.dp, 8.dp, glow = true, unsungAlpha = unsungAlpha)
            }
            WordSyncedLine(background, positionMs, backgroundAlpha, grownSize.sp, null, alignEnd, 6.dp, 8.dp, sweep = swept, unsungAlpha = unsungAlpha)
        } else {
            PlainLyricLine(background.text, backgroundAlpha, grownSize, alignEnd, 6.dp, 8.dp)
        }
    }
}

@Composable
internal fun UnsyncedLine(text: String) {
    Text(
        text = com.artemiy.player.ui.theme.lyricAnnotated(text),
        color = PlayerColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun UnsyncedRubyLine(segments: List<RubySegment>) {
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

/**
 * With the hieroglyph fonts on, words in different fonts have different heights above their baseline (Jua's
 * letters are shorter than Inter's), and a reading sits on top of its word — so some readings crept down onto
 * their word. Every word gets the same room above its baseline (the tallest of the fonts), whatever font it is in.
 * Off with the phone's own font, where the lines have always been as they are.
 */
@Composable
internal fun Modifier.sameHeightAboveBaseline(fontSizeSp: Float): Modifier {
    if (com.artemiy.player.ui.theme.LocalLyricFontSetting.current.choice.isDefault) return this
    val room = with(androidx.compose.ui.platform.LocalDensity.current) { (fontSizeSp * 1.17f).sp.toDp() }
    return this.paddingFrom(androidx.compose.ui.layout.FirstBaseline, before = room)
}

/** Size of a reading printed above its word, relative to the lyric's own font size. */
internal const val READING_SCALE = 0.42f

@Composable
internal fun ReadingText(reading: String?, fontSize: Float, glow: Boolean) {
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
internal fun RubyLine(
    segments: List<RubySegment>,
    alpha: Float,
    fontSize: Float,
    alignEnd: Boolean,
    bottomPadding: Dp,
    glow: Boolean = false,
) {
    val color = if (glow) PlayerColors.TextPrimary.copy(alpha = LYRIC_GLOW_ALPHA) else PlayerColors.TextPrimary
    val rubyHint = remember(segments) { segments.joinToString("") { it.text } }
    BalancedFlow(
        words = remember(segments) { segments.map { it.text } },
        alignEnd = alignEnd,
        rowGap = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            // Faded without clipping: the first word swelling from its middle, and its glow,
            // reach a little past the line's left edge and were being cut off there.
            .graphicsLayer { this.alpha = alpha; compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha }
            .padding(top = 8.dp, bottom = bottomPadding)
            .then(if (glow) Modifier.blur(LYRIC_GLOW_BLUR, BlurredEdgeTreatment.Unbounded) else Modifier),
    ) {
        segments.forEachIndexed { index, segment ->
            val text = if (index == segments.lastIndex) segment.text.trimEnd() else segment.text
            Column(
                modifier = Modifier
                    .padding(end = if (segment.reading != null && !segment.text.last().isWhitespace()) 4.dp else 0.dp),
            ) {
                ReadingText(segment.reading, fontSize, glow)
                Text(text = com.artemiy.player.ui.theme.lyricAnnotated(text, rubyHint), color = color, fontSize = fontSize.sp, lineHeight = fontSize.sp * 1.2f, fontWeight = FontWeight.ExtraBold, modifier = Modifier.sameHeightAboveBaseline(fontSize))
            }
        }
    }
}

/** A translation line sharing its parent's timestamp — smaller and dimmer underneath, as a
 * comment on the main line rather than a competing one. */
@Composable
internal fun SecondaryLyricLine(
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
            text = com.artemiy.player.ui.theme.lyricAnnotated(line.text),
            color = LocalAdaptiveSecondaryColor.current,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                // Faded without clipping: the first word swelling from its middle, and its glow,
            // reach a little past the line's left edge and were being cut off there.
            .graphicsLayer { this.alpha = alpha; compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha }
                .padding(bottom = 6.dp),
        )
    }
}

/** How strong backing vocals glow, next to the main line's glow. */
private const val BACKGROUND_GLOW = 0.5f

/** Shared by the eLRC and plain-LRC glow passes so both look identical. */
internal val LYRIC_GLOW_BLUR = 15.dp

internal const val LYRIC_GLOW_ALPHA = 0.6f

@Composable
internal fun PlainLyricLine(
    text: String,
    alpha: Float,
    fontSize: Float,
    alignEnd: Boolean,
    topPadding: Dp,
    bottomPadding: Dp,
    glow: Boolean = false,
) {
    val color = if (glow) PlayerColors.TextPrimary.copy(alpha = LYRIC_GLOW_ALPHA) else PlayerColors.TextPrimary
    val modifier = Modifier
        .fillMaxWidth()
        // Faded without clipping: the first word swelling from its middle, and its glow,
        // reach a little past the line's left edge and were being cut off there.
        .graphicsLayer { this.alpha = alpha; compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha }
        .padding(top = topPadding, bottom = bottomPadding)
        .then(if (glow) Modifier.blur(LYRIC_GLOW_BLUR, BlurredEdgeTreatment.Unbounded) else Modifier)
    // Words set in rows by BalancedFlow (the phrasing decides the breaks). Text written without
    // spaces (Japanese, Chinese) keeps the system's phrase-aware breaking instead.
    val words = remember(text) { if (text.trim().contains(' ')) WORD_UNIT.findAll(text.trim()).map { it.value }.toList() else null }
    if (words == null) {
        Text(
            text = com.artemiy.player.ui.theme.lyricAnnotated(text),
            color = color,
            fontSize = fontSize.sp,
            lineHeight = fontSize.sp * 1.3f,
            fontWeight = FontWeight.ExtraBold,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            style = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading).inAppFont(),
            modifier = modifier,
        )
        return
    }
    BalancedFlow(words = words, alignEnd = alignEnd, rowGap = 0.dp, modifier = modifier) {
        words.forEachIndexed { i, word ->
            Text(
                // The last word's trailing space would leave a gap against the right edge.
                text = com.artemiy.player.ui.theme.lyricAnnotated(if (i == words.lastIndex) word.trimEnd() else word, text),
                color = color,
                fontSize = fontSize.sp,
                lineHeight = fontSize.sp * 1.3f,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                style = TextStyle().inAppFont(),
            )
        }
    }
}

private val NO_LYRICS_SIZE = 30.sp

/** A word and the space after it. */
private val WORD_UNIT = Regex("\\S+\\s*")

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
internal fun WordSyncedLine(
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
    /** How bright not-yet-sung words are — eased down as the line becomes the sung one. */
    unsungAlpha: Float = 0.55f,
) {
    val words = line.words ?: return
    val sungColor = if (glow) PlayerColors.TextPrimary.copy(alpha = LYRIC_GLOW_ALPHA) else PlayerColors.TextPrimary
    // Not-yet-sung words *within the currently active line* stay fairly bright (unlike fully
    // inactive lines, which fade via the line-level `alpha`) — otherwise the whole active line
    // reads as dim/gray for most of its duration since only one short word is ever fully white.
    // In the glow pass, "not yet sung" is fully transparent instead — the glow must only sit
    // behind text that has actually been reached, never ahead of the sweep.
    val unsungColor = if (glow) Color.Transparent else PlayerColors.TextPrimary.copy(alpha = unsungAlpha)
    val sungStyle = TextStyle(color = sungColor, fontSize = fontSize, fontWeight = FontWeight.ExtraBold).inAppFont()
    val unsungStyle = TextStyle(color = unsungColor, fontSize = fontSize, fontWeight = FontWeight.ExtraBold).inAppFont()
    // Exactly one word is ever "in progress" at a time — found the same way the active *line* is
    // found (last word whose tag time has passed). Every other word is a flat solid color. This
    // guarantees only a single word animates even when the line wraps onto two visual rows, and
    // keeps already-sung/not-yet-sung words from flickering due to their own window's edge cases.
    val currentIndex = if (sweep) words.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0) else words.size
    fun endOf(index: Int): Long = when {
        index + 1 < words.size -> words[index + 1].timeMs
        line.endTimeMs != null -> line.endTimeMs
        nextLineStartMs != null -> nextLineStartMs
        else -> words[index].timeMs + 400L
    }
    // Timed pieces with no space between them ("a" + "bout") are one word on screen — one unit
    // of the row layout, never split across rows.
    val groups = remember(words) { wordGroups(words) }
    val groupTexts = remember(groups) { groups.map { g -> g.joinToString("") { words[it].text } } }
    BalancedFlow(
        words = groupTexts,
        alignEnd = alignEnd,
        rowGap = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            // Faded without clipping: the first word swelling from its middle, and its glow,
            // reach a little past the line's left edge and were being cut off there.
            .graphicsLayer { this.alpha = alpha; compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha }
            .padding(top = topPadding, bottom = bottomPadding)
            .then(if (glow) Modifier.blur(LYRIC_GLOW_BLUR, BlurredEdgeTreatment.Unbounded) else Modifier),
    ) {
        groups.forEach { group ->
          // A word held for a long time ("fooor...") swells a little as a whole — every syllable
          // of it, not just the one being stretched — and glows brighter while it lasts, then
          // settles back. Mid-line words count from HELD_WORD_MS; a line's last word only from
          // HELD_LAST_WORD_MS, since files often just stretch it up to the next line.
          // "Held" is one piece of the word really being stretched, not several quick ones that
          // only add up to a long time together.
          val heldFor = if (currentIndex == words.lastIndex) HELD_LAST_WORD_MS else HELD_WORD_MS
          val inThisGroup = sweep && currentIndex in group && words[currentIndex].text.isNotBlank() && positionMs < endOf(currentIndex)
          // How much the word lingers: nothing for a quick one, a little for a medium one, all of it
          // by [heldFor] — it grows with the length rather than switching on at a threshold.
          val holdLevel = if (inThisGroup) {
              val length = endOf(currentIndex) - words[currentIndex].timeMs
              val t = ((length - HELD_START_MS).toFloat() / (heldFor - HELD_START_MS)).coerceIn(0f, 1f)
              t * t * (3f - 2f * t)
          } else 0f
          val swell by animateFloatAsState(holdLevel, tween(700, easing = FastOutSlowInEasing), label = "heldWord")
          Row(
              modifier = Modifier
                  .graphicsLayer {
                      val grow = 1f + HELD_WORD_GROWTH * swell
                      scaleX = grow
                      scaleY = grow
                      transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.7f)
                  },
          ) {
            group.forEach { index ->
            val word = words[index]
            // The last word's trailing space would leave a gap against the right edge of a v2 line.
            val text = if (index == words.lastIndex) word.text.trimEnd() else word.text
            val reading = readings?.getOrNull(index)
            val fontText = com.artemiy.player.ui.theme.lyricAnnotated(text, line.text)
            Column(
                modifier = Modifier
                    // Latin and Japanese glyphs sit at different heights in the same font; lining
                    // up the lyric text's baseline keeps "high" level with the kanji around it.
                    .then(if (readings != null) Modifier.alignBy(LastBaseline) else Modifier)
                    .padding(end = if (reading != null && text.lastOrNull()?.isWhitespace() == false) 4.dp else 0.dp),
            ) {
                if (readings != null) ReadingText(reading, fontSize.value, glow)
                // The extra glow of a lingering word follows the same easing as its lift: it stays
                // with the word after the singing has moved on and fades out together with the lift,
                // instead of being cut off the moment the next word starts.
                val lingerStyle = if (glow && swell > 0f) {
                    sungStyle.copy(color = PlayerColors.TextPrimary.copy(alpha = LYRIC_GLOW_ALPHA + (1f - LYRIC_GLOW_ALPHA) * swell))
                } else sungStyle
                when {
                    index < currentIndex -> Text(text = fontText, style = lingerStyle, modifier = if (readings != null) Modifier.sameHeightAboveBaseline(fontSize.value) else Modifier)
                    index > currentIndex -> Text(text = fontText, style = unsungStyle, modifier = if (readings != null) Modifier.sameHeightAboveBaseline(fontSize.value) else Modifier)
                    else -> {
                        val wordEndMs = endOf(index)
                        val progress = if (wordEndMs > word.timeMs) {
                            ((positionMs - word.timeMs).toFloat() / (wordEndMs - word.timeMs)).coerceIn(0f, 1f)
                        } else 1f
                        // A brush-based gradient here turned out to look gray throughout most of
                        // the word (its scaling wasn't behaving as a clean per-word wipe). Drawing
                        // the bright copy on top, hard-clipped to exactly `progress` of that word's
                        // own measured width, is a much more direct and reliably-correct reveal.
                        Box {
                            Text(text = fontText, style = unsungStyle, modifier = if (readings != null) Modifier.sameHeightAboveBaseline(fontSize.value) else Modifier)
                            // The sung copy, revealed up to `progress` with a soft edge rather than a
                            // hard cut; the reveal runs a little past the word so it ends fully lit.
                            Text(
                                text = fontText,
                                style = lingerStyle,
                                modifier = (if (readings != null) Modifier.sameHeightAboveBaseline(fontSize.value) else Modifier)
                                    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        val soft = SWEEP_EDGE.toPx()
                                        val edge = progress * (size.width + soft)
                                        drawRect(
                                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                listOf(Color.Black, Color.Transparent),
                                                startX = edge - soft,
                                                endX = edge,
                                            ),
                                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                                        )
                                    },
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

/**
 * An instrumental break: three dots that only exist while it's their turn — until then they take
 * no room at all. When the break comes they open a gap between the lines and appear, light up one
 * after another as the next line gets closer (each glowing like a sung line once lit), and just
 * before the singing starts again they fold away, letting the lines close back up.
 */
@Composable
internal fun InstrumentalDots(startMs: Long, endMs: Long, positionMs: Long, active: Boolean, alignEnd: Boolean, playing: Boolean = true) {
    val span = (endMs - startMs).coerceAtLeast(1L).toFloat()
    val progress = ((positionMs - startMs) / span).coerceIn(0f, 1f)
    val origin = androidx.compose.ui.graphics.TransformOrigin(if (alignEnd) 1f else 0f, 0.5f)
    androidx.compose.animation.AnimatedVisibility(
        visible = active && positionMs < endMs - DOTS_FOLD_EARLY_MS,
        // No clipping while opening/closing — it would cut the dots' glow off.
        enter = androidx.compose.animation.expandVertically(tween(380), clip = false) +
            androidx.compose.animation.fadeIn(tween(320, delayMillis = 80)) +
            androidx.compose.animation.scaleIn(tween(380), initialScale = 0.5f, transformOrigin = origin),
        exit = androidx.compose.animation.scaleOut(tween(260), targetScale = 0f, transformOrigin = origin) +
            androidx.compose.animation.fadeOut(tween(220)) +
            androidx.compose.animation.shrinkVertically(tween(320, delayMillis = 120), clip = false),
    ) {
        val color = PlayerColors.TextPrimary
        // A slow, small sway, forward and back, each dot a beat behind the one before it — like a
        // wave rolling along the three. Only while the dots are on screen.
        // Stands still while the song is paused: the phase only moves on frames drawn during playback.
        val swayPhase = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        LaunchedEffect(playing) {
            if (!playing) return@LaunchedEffect
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                swayPhase.floatValue = (swayPhase.floatValue + (now - last) / 1_000_000_000f * (2 * Math.PI).toFloat() / 2.6f) % (2 * Math.PI).toFloat()
                last = now
            }
        }
        val sway = swayPhase
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        ) {
            repeat(3) { i ->
                // Each dot fills over its own third of the break, and glows like a sung line
                // as it lights up.
                val lit = (progress * 3f - i).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .padding(end = if (i < 2) 12.dp else 0.dp)
                        .size(14.dp)
                        .graphicsLayer { translationX = kotlin.math.sin(sway.floatValue - i * 0.9f) * 3.dp.toPx() },
                    contentAlignment = Alignment.Center,
                ) {
                    // The halo: a bit bigger than the dot and blurred a bit less than text's glow —
                    // a dot is much smaller than a letter, and the same blur would dissolve it.
                    Box(
                        modifier = Modifier
                            .requiredSize(20.dp)
                            .graphicsLayer {
                                alpha = lit * LYRIC_GLOW_ALPHA
                                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                            }
                            .blur(12.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.3f + 0.7f * lit)),
                    )
                }
            }
        }
    }
}

/** The dots fold away this long before the next line starts, so it's already back in place. */
private const val DOTS_FOLD_EARLY_MS = 450L

/** A word held at least this long counts as a long, stretched note — in the middle of a line,
 * and as a line's last word (which files often stretch to the next line anyway, so more). */
/** A word shorter than this doesn't linger at all; from here the lift and glow grow with its length. */
private const val HELD_START_MS = 600L
private const val HELD_WORD_MS = 2_000L
private const val HELD_LAST_WORD_MS = 3_000L

/** How much bigger a held word gets while it lasts. */
private const val HELD_WORD_GROWTH = 0.08f

/** Width of the soft edge on the karaoke sweep. */
private val SWEEP_EDGE = 16.dp

/** Scrolls by [delta] on a soft spring, starting at [velocity]\[0] (and keeping it up to date). */
private suspend fun androidx.compose.foundation.lazy.LazyListState.glideBy(delta: Float, velocity: FloatArray) {
    scroll {
        var done = 0f
        androidx.compose.animation.core.animate(0f, delta, velocity[0], LINE_GLIDE) { value, speed ->
            scrollBy(value - done)
            done = value
            velocity[0] = speed
        }
        velocity[0] = 0f
    }
}

/** The line-change glide: settles in about half a second, with a small springy overshoot past its mark. */
private val LINE_GLIDE = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.68f, stiffness = 260f)

/** How long after scrolling the lyrics by hand they return to the line being sung. */
private const val RECENTER_AFTER_MS = 2_000L

/** Beyond this, the smoothed lyric clock takes the player's position as is (a seek, not drift). */
private const val SMOOTH_SNAP_MS = 500L

/** Moving on to the next line: a quick start easing out into place — decisive, but not a snap. */
private val LINE_EASE = com.artemiy.player.ui.components.SafeCubicBezier(0.25f, 0f, 0.1f, 1f)
private val LINE_CHANGE = tween<Float>(340, easing = LINE_EASE)


/**
 * [words] split into what can wrap as a unit: a timed piece that doesn't end in a space runs on
 * into the next one ("a" + "bout" is "about"). Not between Chinese/Japanese/Korean characters,
 * though — those lines have no spaces at all, and have to be able to wrap somewhere.
 */
private fun wordGroups(words: List<LyricWord>): List<List<Int>> {
    val groups = mutableListOf<MutableList<Int>>()
    words.forEachIndexed { index, word ->
        val previous = words.getOrNull(index - 1)
        val joins = previous != null && previous.text.isNotEmpty() && !previous.text.last().isWhitespace() &&
            word.text.isNotEmpty() && !word.text.first().isWhitespace() &&
            !previous.text.last().isCjk() && !word.text.first().isCjk() &&
            // "deserts/Go", "Pseudo-Sacrosanct": separate words, free to wrap between.
            previous.text.last() !in WORD_BREAKS && word.text.first() !in WORD_BREAKS
        if (joins) groups.last().add(index) else groups.add(mutableListOf(index))
    }
    return groups
}

private val WORD_BREAKS = setOf('/', '-', '\\', '|', '—', '–')

private fun Char.isCjk(): Boolean = when (Character.UnicodeScript.of(code)) {
    Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA, Character.UnicodeScript.HANGUL -> true
    else -> false
}

/** Settings → «Точки в паузах обычного LRC»: dots between plain LRC lines (see withInstrumentalBreaks). */
val LocalLrcGapDots = androidx.compose.runtime.staticCompositionLocalOf { true }
