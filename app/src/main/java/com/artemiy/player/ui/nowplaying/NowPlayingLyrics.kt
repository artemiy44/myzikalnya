package com.artemiy.player.ui.nowplaying

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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
import com.artemiy.player.ui.theme.PlayerColors

// The Lyrics view: synced/unsynced lines, word sweep, glow, romanization readings.

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
internal const val LYRIC_SIZE = 31f

/** Lines not being sung are drawn at 28/31 of that, by scaling — see the item in [LyricsView]. */
internal const val INACTIVE_LYRIC_SCALE = 28f / 31f

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
    Box(modifier = Modifier.lyricScale(scale, alignEnd)) {
        if (words != null) {
            WordSyncedLine(background, positionMs, backgroundAlpha, grownSize.sp, null, alignEnd, 6.dp, 8.dp, sweep = lineActive)
        } else {
            PlainLyricLine(background.text, backgroundAlpha, grownSize, alignEnd, 6.dp, 8.dp)
        }
    }
}

@Composable
internal fun UnsyncedLine(text: String) {
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
internal val LYRIC_GLOW_BLUR = 18.dp

internal const val LYRIC_GLOW_ALPHA = 0.75f

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
