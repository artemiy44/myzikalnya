package com.artemiy.player.ui.nowplaying

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.artemiy.player.data.Song
import com.artemiy.player.lyrics.LyricLine
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.MinimalSlider
import com.artemiy.player.ui.theme.PlayerColors
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
) {
    var centerMode by remember { mutableStateOf(CenterMode.Art) }
    var controlsVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Reset to visible whenever the center content changes mode.
    LaunchedEffect(centerMode) {
        controlsVisible = true
    }

    // Lyrics: auto-hide after a few seconds of inactivity, tapping the bottom brings it back.
    LaunchedEffect(centerMode, controlsVisible) {
        if (centerMode == CenterMode.Lyrics && controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
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

    val scrimAlpha by animateFloatAsState(
        targetValue = if (centerMode == CenterMode.Lyrics) 0.32f else 0.62f,
        animationSpec = tween(350),
        label = "scrim",
    )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp),
            ) {
                when (centerMode) {
                    CenterMode.Art -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

                    CenterMode.Lyrics -> Column(modifier = Modifier.fillMaxSize()) {
                        NowPlayingMiniHeader(song = song, onCollapse = { centerMode = CenterMode.Art })
                        Box(modifier = Modifier.fillMaxSize()) {
                            LyricsView(lyrics = lyrics, positionMs = positionMs, isPlaying = isPlaying)
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
                    }

                    CenterMode.Queue -> Column(modifier = Modifier.fillMaxSize()) {
                        NowPlayingMiniHeader(song = song, onCollapse = { centerMode = CenterMode.Art })
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
                            state = queueListState,
                            modifier = Modifier.nestedScroll(queueNestedScrollConnection),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(250)),
            ) {
                Column {
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
                                )
                                Text(
                                    text = song?.artist ?: "",
                                    color = PlayerColors.TextSecondary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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

@Composable
private fun LyricsView(lyrics: ParsedLyrics?, positionMs: Long, isPlaying: Boolean) {
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

        is ParsedLyrics.Unsynced -> LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                contentPadding = PaddingValues(top = 16.dp, bottom = 220.dp),
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
private fun NowPlayingMiniHeader(song: Song?, onCollapse: () -> Unit) {
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
            )
            Text(
                text = song?.artist ?: "",
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    state: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    if (manualQueue.isEmpty() && continueQueue.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Очередь пуста", color = PlayerColors.TextSecondary, fontSize = 13.sp)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), state = state) {
        if (manualQueue.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Очередь", color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Очистить",
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClearManualQueue() },
                    )
                }
            }
            itemsIndexed(manualQueue) { _, item -> QueueRow(item = item, onClick = { onItemClick(item) }) }
        }
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
            .background(if (active) PlayerColors.AccentOnDark else PlayerColors.Surface)
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
