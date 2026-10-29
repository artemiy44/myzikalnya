package com.artemiy.player.ui.nowplaying

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateColorAsState
import com.artemiy.player.ui.theme.inAppFont
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import kotlinx.coroutines.flow.first
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import com.artemiy.player.ui.components.horizontalSwipe
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.ui.components.pressScale
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// The Queue view: reorderable list, swipe to remove, play-mode toggles, add-songs picker.

/** One queue row plus a LazyColumn key that stays stable while rows are dragged around — the
 * same song can legitimately be queued twice, so the song id alone isn't unique. */
internal class QueueEntry(val key: String, val song: Song)

internal fun queueEntries(prefix: String, songs: List<Song>): List<QueueEntry> {
    val seen = HashMap<Long, Int>()
    return songs.map { song ->
        val occurrence = (seen[song.id] ?: 0) + 1
        seen[song.id] = occurrence
        QueueEntry("$prefix-${song.id}-$occurrence", song)
    }
}

internal fun List<QueueEntry>.moved(fromKey: Any, toKey: Any): List<QueueEntry> {
    val from = indexOfFirst { it.key == fromKey }
    val to = indexOfFirst { it.key == toKey }
    if (from < 0 || to < 0) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

internal val QueueRemoveRed = Color(0xFFE5383B)

internal enum class RowPhase { Settled, Entering, Leaving }

/** A queue row as shown: [Leaving] rows are already gone from the player's queue but stay a moment to fold away. */
internal class DisplayRow(val entry: QueueEntry, val phase: RowPhase)

private class RowsMemory {
    var shown: List<DisplayRow>? = null
    var droppedAt = 0
}

private const val ROW_FOLD_MS = 260
private const val ROW_GHOST_MS = 340L

/**
 * The rows to draw for [target], the player's list: rows that left it (a song picked or skipped to) fold up to nothing
 * and the ones below follow, rows that joined it (the song just played, after skipping back) unfold. A list that
 * changes completely (a new queue) just swaps. Nothing here slides rows around: their space really shrinks, so the
 * list's fade always knows where a row is.
 */
@Composable
private fun rememberAnimatedRows(target: List<QueueEntry>): List<DisplayRow> {
    val memory = remember { RowsMemory() }
    var generation by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val rows = remember(target, generation) {
        val before = memory.shown
        val dropLeaving = generation != memory.droppedAt
        memory.droppedAt = generation
        val merged = if (before == null) target.map { DisplayRow(it, RowPhase.Settled) } else {
            val targetKeys = target.mapTo(HashSet()) { it.key }
            val beforeKeys = before.mapTo(HashSet()) { it.entry.key }
            if (before.isNotEmpty() && target.isNotEmpty() && before.none { it.entry.key in targetKeys }) {
                target.map { DisplayRow(it, RowPhase.Settled) }
            } else {
                // Leaving rows stay where they were: after the row that was above them.
                val ghostsAfter = HashMap<String?, MutableList<DisplayRow>>()
                var above: String? = null
                for (row in before) {
                    if (row.entry.key in targetKeys) above = row.entry.key
                    else if (!dropLeaving) ghostsAfter.getOrPut(above) { mutableListOf() }.add(DisplayRow(row.entry, RowPhase.Leaving))
                }
                buildList {
                    ghostsAfter[null]?.let { addAll(it) }
                    for (entry in target) {
                        add(DisplayRow(entry, if (entry.key in beforeKeys) RowPhase.Settled else RowPhase.Entering))
                        ghostsAfter[entry.key]?.let { addAll(it) }
                    }
                }
            }
        }
        memory.shown = merged
        merged
    }
    val hasLeaving = rows.any { it.phase == RowPhase.Leaving }
    androidx.compose.runtime.LaunchedEffect(hasLeaving, rows) {
        if (hasLeaving) {
            kotlinx.coroutines.delay(ROW_GHOST_MS)
            generation++
        }
    }
    return rows
}

/** Folds a row's height (and fades it) to nothing when [phase] is Leaving, unfolds it from nothing when it starts as Entering. */
@Composable
private fun FoldingRow(phase: RowPhase, content: @Composable () -> Unit) {
    val startsFolded = remember { phase == RowPhase.Entering }
    val fold = remember { androidx.compose.animation.core.Animatable(if (startsFolded) 0f else 1f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (startsFolded) fold.animateTo(1f, tween(ROW_FOLD_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    val leaving = phase == RowPhase.Leaving
    androidx.compose.runtime.LaunchedEffect(leaving) {
        if (leaving) fold.animateTo(0f, tween(ROW_FOLD_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val height = (placeable.height * fold.value).roundToInt()
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .graphicsLayer {
                alpha = fold.value
                clip = true
            },
    ) { content() }
}

/** What the long-press song menu on a queue row can do — the same actions as everywhere else. */
internal class QueueSongActions(
    val onPlayNext: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onAddToPlaylist: (Song) -> Unit,
    val onGoToAlbum: (Song) -> Unit,
    val onGoToArtist: (Song) -> Unit,
)

@Composable
internal fun QueueList(
    manualQueue: List<Song>,
    continueQueue: List<Song>,
    onItemClick: (Song) -> Unit,
    onClearManualQueue: () -> Unit,
    onAddSongsClick: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (position: Int) -> Unit,
    onDragActiveChange: (Boolean) -> Unit,
    songActions: QueueSongActions,
    topFadePx: () -> Int,
    bottomFadePx: () -> Int,
    state: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** How long the rows take to fade out at the edges of the list. */
    fadeSpan: androidx.compose.ui.unit.Dp = FADE_SPAN,
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
    val manualDisplay = rememberAnimatedRows(manualRows)
    val continueDisplay = rememberAnimatedRows(continueRows)
    // The history sits above the queue, out of sight; pulling the list down at its very top lets it in: the last few
    // songs are brought down into view, and the older ones are further up. Once it has been scrolled out of sight again
    // it is taken away (nothing moves on screen: it is all above the viewport).
    val history = LocalQueueHistory.current
    var historyOpen by remember { mutableStateOf(false) }
    val historyEntries = remember(history.songs, historyOpen) { if (historyOpen) queueEntries("h", history.songs) else emptyList() }
    val historyOffset = if (historyEntries.isEmpty()) 0 else historyEntries.size + 1
    val historyOffsetNow by androidx.compose.runtime.rememberUpdatedState(historyOffset)
    val hasHistory by androidx.compose.runtime.rememberUpdatedState(history.songs.isNotEmpty())
    val historySongsNow by androidx.compose.runtime.rememberUpdatedState(history.songs)
    // The rows there were when the history was opened: they just stand; one that joins while it is open (a song
    // that has been played on) unfolds into place.
    var openingKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val pullThresholdPx = with(LocalDensity.current) { 150.dp.toPx() }
    val rowPx = with(LocalDensity.current) { 68.dp.toPx() }
    val pullConnection = remember(state) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && available.y > 0f && !state.canScrollBackward && hasHistory && !historyOpen) {
                    pulled += available.y
                    if (pulled > pullThresholdPx) {
                        pulled = 0f
                        openingKeys = queueEntries("h", historySongsNow).mapTo(HashSet()) { it.key }
                        historyOpen = true
                    }
                } else if (available.y <= 0f) pulled = 0f
                return Offset.Zero
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                pulled = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(historyOpen) {
        if (!historyOpen) return@LaunchedEffect
        // Let the rows above be laid out, then bring the latest ones down into view. A finger still on the list
        // (the pull that opened it) or any new scroll interrupts this: that is fine, it is only the glide.
        launch {
            androidx.compose.runtime.withFrameNanos { }
            androidx.compose.runtime.withFrameNanos { }
            val peek = (minOf(history.songs.size, 5) * rowPx) + rowPx * 0.6f
            try {
                state.animateScrollBy(-peek, tween(360, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            } catch (_: kotlinx.coroutines.CancellationException) {
            }
        }
        // Gone once the queue's own header is back at the top (all of the history is then above it) — but only after
        // the header has been seen further down: right at the opening it is still at the top.
        var seenBelow = false
        androidx.compose.runtime.snapshotFlow {
            // Not in view at all: above the list's start (the history is behind us) or far below it (up in the history).
            state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "header-queue" }?.offset
                ?: if (state.firstVisibleItemIndex >= historyOffsetNow) Int.MIN_VALUE else Int.MAX_VALUE
        }.first { offset ->
            if (offset > rowPx * 0.5f) seenBelow = true
            seenBelow && offset <= 0
        }
        historyOpen = false
    }

    val manualQueueStart = historyOffset + 2
    val continueQueueStart = manualQueueStart + manualDisplay.size + 1

    // A song picked from the list: once the rows before it have folded away, the list goes back to its top (the new
    // queue from its first row). The covers up there were out of sight and not loaded, so they are decoded while the
    // rows fold, ahead of the scroll.
    val leavingNow = manualDisplay.any { it.phase == RowPhase.Leaving } || continueDisplay.any { it.phase == RowPhase.Leaving }
    var hadLeaving by remember { mutableStateOf(false) }
    var returnAfterPick by remember { mutableStateOf(false) }
    val topSongs by androidx.compose.runtime.rememberUpdatedState((manualRows + continueRows).take(14).map { it.song })
    val artContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    androidx.compose.runtime.LaunchedEffect(leavingNow) {
        if (leavingNow) {
            hadLeaving = true
            if (returnAfterPick) topSongs.forEach { com.artemiy.player.ui.components.prefetchAlbumArt(artContext, it.uri) }
        } else if (hadLeaving) {
            hadLeaving = false
            if (returnAfterPick) {
                returnAfterPick = false
                if (state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0) {
                    state.animateScrollToItem(0)
                    if (state.firstVisibleItemIndex != 0 || state.firstVisibleItemScrollOffset != 0) state.scrollToItem(0)
                }
            }
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().nestedScroll(pullConnection),
        state = state,
        contentPadding = contentPadding,
    ) {
        if (historyEntries.isNotEmpty()) {
            item(key = "history-title") {
                Text(
                    text = stringResource(R.string.queue_history_title),
                    color = LocalAdaptiveSecondaryColor.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fadeInList(state, 0, topFadePx, bottomFadePx, fadeSpan, fadeSpan).padding(top = 12.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(historyEntries, key = { _, entry -> entry.key }, contentType = { _, _ -> "history" }) { i, entry ->
                FoldingRow(if (entry.key in openingKeys) RowPhase.Settled else RowPhase.Entering) {
                    HistoryRow(
                        song = entry.song,
                        onClick = { history.onPlay(entry.song) },
                        modifier = Modifier.fadeInList(state, 1 + i, topFadePx, bottomFadePx, fadeSpan, fadeSpan),
                    )
                }
            }
        }
        item(key = "header-queue") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeInList(state, historyOffset, topFadePx, bottomFadePx, fadeSpan, fadeSpan)
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.queue), color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (manualQueue.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.clear),
                        color = LocalAdaptiveSecondaryColor.current,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClearManualQueue() },
                    )
                }
            }
        }
        item(key = "header-add") {
            AddSongsToQueueRow(onClick = onAddSongsClick, modifier = Modifier.fadeInList(state, historyOffset + 1, topFadePx, bottomFadePx, fadeSpan, fadeSpan))
        }
        itemsIndexed(manualDisplay, key = { _, row -> row.entry.key }, contentType = { _, _ -> "song" }) { i, row ->
            val entry = row.entry
            ReorderableItem(reorderState, key = entry.key, animateItemModifier = if (reorderState.isAnyItemDragging) Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null) else Modifier) { isDragging ->
              FoldingRow(row.phase) {
                SwipeToRemove(onRemove = { onRemove(manualQueue.indexOfFirstEntry(entry.key, "m")) }) {
                    QueueRow(
                        item = entry.song,
                        isDragging = isDragging,
                        onClick = { returnAfterPick = true; onItemClick(entry.song) },
                        songActions = songActions,
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
                        modifier = Modifier.fadeInList(state, manualQueueStart + i, topFadePx, bottomFadePx, fadeSpan, fadeSpan),
                    )
                }
              }
            }
        }
        if (continueDisplay.isNotEmpty()) {
            item(key = "header-continue") {
                Text(
                    text = stringResource(R.string.up_next),
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fadeInList(state, manualQueueStart + manualDisplay.size, topFadePx, bottomFadePx, fadeSpan, fadeSpan)
                        .padding(top = 18.dp, bottom = 4.dp),
                )
            }
            itemsIndexed(continueDisplay, key = { _, row -> row.entry.key }, contentType = { _, _ -> "song" }) { i, row ->
                val entry = row.entry
                ReorderableItem(reorderState, key = entry.key, animateItemModifier = if (reorderState.isAnyItemDragging) Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null) else Modifier) { isDragging ->
                  FoldingRow(row.phase) {
                    SwipeToRemove(onRemove = {
                        onRemove(manualQueue.size + continueQueue.indexOfFirstEntry(entry.key, "c"))
                    }) {
                        QueueRow(
                            item = entry.song,
                            isDragging = isDragging,
                            onClick = { returnAfterPick = true; onItemClick(entry.song) },
                            songActions = songActions,
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
                            modifier = Modifier.fadeInList(state, continueQueueStart + i, topFadePx, bottomFadePx, fadeSpan, fadeSpan),
                        )
                    }
                  }
                }
            }
        }
    }
}

/** Position of the row with [key] in the player's own (not locally reordered) list. */
internal fun List<Song>.indexOfFirstEntry(key: String, prefix: String): Int =
    queueEntries(prefix, this).indexOfFirst { it.key == key }

/** Swipe left to remove: the row slides away over a red strip that only fills the part already
 * uncovered, so the row's own (transparent) content never sits on top of red. The label stays
 * put at the row's right edge and the strip just uncovers it, rather than sliding in with it.
 *
 * Hand-rolled rather than Material's SwipeToDismissBox: a queue can hold a whole library, and
 * that box made every row noticeably heavier to build while flinging through it. Until a finger
 * actually swipes, this is just one small drag listener. */
@Composable
internal fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    var dragPx by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    var removed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragState = rememberDraggableState { delta -> if (!removed) dragPx = (dragPx + delta).coerceIn(-widthPx.toFloat(), 0f) }
    Box(modifier = Modifier.onSizeChanged { widthPx = it.width }) {
        val revealedPx = -dragPx
        if (revealedPx > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentWidth(Alignment.End)
                    .fillMaxHeight()
                    .width(with(density) { revealedPx.toDp() })
                    .clip(RoundedCornerShape(10.dp))
                    .background(QueueRemoveRed),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.remove),
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
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(dragPx.toInt(), 0) }
                .horizontalSwipe(
                    enabled = !removed,
                    onDrag = { delta -> dragState.dispatchRawDelta(delta) },
                    onCancel = { scope.launch { androidx.compose.animation.core.animate(dragPx, 0f) { value, _ -> dragPx = value } } },
                    onEnd = { velocity ->
                        // Pulled far enough — or flicked fast, but only after a real pull: a finger
                        // drifting while the list is scrolled must never remove a song.
                        val threshold = with(density) { com.artemiy.player.ui.components.SWIPE_COMMIT.toPx() }
                        val flickMin = with(density) { com.artemiy.player.ui.components.SWIPE_FLICK_MIN.toPx() }
                        val flickSpeed = with(density) { com.artemiy.player.ui.components.SWIPE_FLICK_SPEED.toPx() }
                        val target = if (-dragPx > threshold || (-dragPx > flickMin && velocity < -flickSpeed)) -widthPx.toFloat() else 0f
                        scope.launch {
                            androidx.compose.animation.core.animate(dragPx, target) { value, _ -> dragPx = value }
                            if (target != 0f) {
                                removed = true
                                onRemove()
                            }
                        }
                    },
                ),
        ) {
            content()
        }
    }
}

@Composable
internal fun AddSongsToQueueRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = buttonTicked(onClick))
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
            Icon(imageVector = AppIcons.Add, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(22.dp))
        }
        Text(
            text = stringResource(R.string.add_tracks_to_queue),
            color = PlayerColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
internal fun QueueRow(
    item: Song,
    isDragging: Boolean,
    onClick: () -> Unit,
    songActions: QueueSongActions,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val liftFill = PlayerColors.Surface.copy(alpha = 0.55f)
    var menuExpanded by remember { mutableStateOf(false) }
    var menuOpenedOnce by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    // Holding still on the row long enough before dragging also counts as a long press — once
    // the row actually starts moving, that menu is clearly not what was meant.
    LaunchedEffect(isDragging) { if (isDragging) menuExpanded = false }
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
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {
                    if (!isDragging) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpenedOnce = true
                        menuExpanded = true
                    }
                },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AlbumArt(
                uri = item.uri,
                size = ART_SIZE_THUMB,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            // Only built once it's been opened: hundreds of rows each carrying a closed menu cost
            // real time while flinging through a long queue. Kept afterwards for its closing animation.
            if (menuOpenedOnce) SongActionsMenuPopup(
                song = item,
                expanded = menuExpanded && !isDragging,
                onDismiss = { menuExpanded = false },
                onPlayNext = songActions.onPlayNext,
                onAddToQueue = songActions.onAddToQueue,
                onAddToPlaylist = songActions.onAddToPlaylist,
                onGoToAlbum = songActions.onGoToAlbum,
                onGoToArtist = songActions.onGoToArtist,
            )
        }
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
            imageVector = AppIcons.DragHandle,
            contentDescription = stringResource(R.string.drag),
            tint = LocalAdaptiveSecondaryColor.current,
            modifier = dragHandleModifier
                .padding(start = 12.dp, end = 4.dp)
                .size(22.dp),
        )
    }
}

/** The songs heard before (the latest last) and what tapping one does — provided by the app, so every look of the queue shows them. */
class QueueHistoryState(val songs: List<Song>, val onPlay: (Song) -> Unit)

val LocalQueueHistory = androidx.compose.runtime.compositionLocalOf { QueueHistoryState(emptyList()) {} }

/** A song from the history: like a queue row, a little quieter, and without the handle. */
@Composable
private fun HistoryRow(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(uri = song.uri, size = ART_SIZE_THUMB, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = song.title, color = PlayerColors.TextPrimary.copy(alpha = 0.78f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = song.artist, color = LocalAdaptiveSecondaryColor.current, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Provided by the app: repeat is on and it's one song that repeats (the button gets a small "1"). */
val LocalRepeatOne = androidx.compose.runtime.compositionLocalOf { false }

@Composable
internal fun QueueToggleRow(
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
            icon = AppIcons.Shuffle,
            description = stringResource(R.string.shuffle),
            active = shuffleEnabled,
            onClick = onToggleShuffle,
            modifier = Modifier.weight(1f),
        )
        QueueToggleButton(
            icon = AppIcons.Repeat,
            description = stringResource(R.string.repeat),
            active = repeatEnabled,
            onClick = onToggleRepeat,
            modifier = Modifier.weight(1f),
            badge = if (LocalRepeatOne.current) "1" else null,
        )
        QueueToggleButton(
            icon = AppIcons.Infinite,
            description = stringResource(R.string.endless_play),
            active = infinitePlayEnabled,
            onClick = onToggleInfinitePlay,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun QueueToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = (if (active) PlayerColors.Accent else PlayerColors.Surface).copy(alpha = 0.4f),
        animationSpec = tween(180),
        label = "queueToggleBg",
    )
    Box(
        modifier = modifier
            .height(52.dp)
            .pressScale(interaction, pressedScale = 0.92f)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = buttonTicked(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) PlayerColors.OnAccent else PlayerColors.TextPrimary,
            modifier = Modifier.size(22.dp),
        )
        if (badge != null) {
            Text(
                text = badge,
                color = if (active) PlayerColors.OnAccent else PlayerColors.TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center).offset(x = 0.dp, y = 0.dp),
            )
        }
    }
}

/**
 * "Add tracks to the queue": the Tracks tab — search, sort, list or grid — except that a tap on a
 * song queues it (instead of playing it), and the page stays open so several can be queued before
 * it's closed. It slides up when it opens and slides away when it closes.
 */
@Composable
internal fun AddToQueuePicker(songs: List<Song>, actions: QueueSongActions, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    // The chosen look (list / grid) is remembered between visits, like the Tracks tab's.
    val settingsVm: com.artemiy.player.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val viewMode = com.artemiy.player.ui.library.ViewMode.valueOf(settingsVm.viewMode("queue_picker").name)
    val sort = settingsVm.sortName("queue_picker")
        ?.let { name -> com.artemiy.player.ui.library.SongSort.entries.firstOrNull { it.name == name } }
        ?: com.artemiy.player.ui.library.SongSort.RECENT
    val filtered = remember(songs, query, sort) {
        val q = query.trim()
        songs
            .filter { q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) || it.album.contains(q, ignoreCase = true) }
            .let { list ->
                when (sort) {
                    com.artemiy.player.ui.library.SongSort.TITLE -> list.sortedBy { it.title.lowercase() }
                    com.artemiy.player.ui.library.SongSort.ARTIST -> list.sortedBy { it.artist.lowercase() }
                    com.artemiy.player.ui.library.SongSort.RECENT -> list.sortedByDescending { it.dateAddedMs }
                    com.artemiy.player.ui.library.SongSort.RELEASE_DATE -> list.sortedByDescending { it.year ?: -1 }
                }
            }
    }
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        appear.animateTo(1f, androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    val close: () -> Unit = {
        scope.launch {
            appear.animateTo(0f, androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing))
            onDismiss()
        }
    }
    // The back gesture pulls the page away with the finger (like every other page); let go to
    // close it, or it settles back.
    val back = remember { androidx.compose.animation.core.Animatable(0f) }
    val cornerPx = com.artemiy.player.ui.components.rememberScreenCornerRadiusPx()
    androidx.activity.compose.PredictiveBackHandler { progress ->
        try {
            progress.collect { event -> back.snapTo(event.progress) }
            back.animateTo(1f, androidx.compose.animation.core.tween(240))
            onDismiss()
        } catch (e: kotlinx.coroutines.CancellationException) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                back.animateTo(0f, androidx.compose.animation.core.tween(200))
            }
        }
    }
    // A tap queues the song (at the end of the queue) and says so.
    val add: (Song) -> Unit = { song ->
        actions.onAddToQueue(song)
        android.widget.Toast.makeText(context, "✓ ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Opening slides up; the back gesture pulls the page away from the middle instead
                // — shrinking, rounded like the screen, melting away at the end.
                val b = back.value
                val s = 1f - 0.12f * b
                scaleX = s
                scaleY = s
                alpha = appear.value * (1f - ((b - 0.4f) / 0.6f).coerceIn(0f, 1f))
                translationY = (1f - appear.value) * 90.dp.toPx()
                val radius = cornerPx * (b / 0.2f).coerceIn(0f, 1f)
                if (radius > 0.5f) {
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
                    clip = true
                }
            }
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.add_to_queue), color = PlayerColors.TextPrimary, style = com.artemiy.player.ui.theme.pageTitleStyle)
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = PlayerColors.TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
                )
            }
            com.artemiy.player.ui.library.ListToolbar(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.search_tracks),
                sortOptions = com.artemiy.player.ui.library.SongSort.entries,
                sortOptionLabel = { stringResource(it.labelRes) },
                currentSort = stringResource(sort.labelRes),
                onSortSelect = { settingsVm.setSortName("queue_picker", it.name) },
                viewMode = viewMode,
                onViewModeCycle = { settingsVm.setViewMode("queue_picker", com.artemiy.player.data.LibraryViewMode.valueOf(viewMode.next().name)) },
            )
            Box(modifier = Modifier.weight(1f)) {
                when (viewMode) {
                    com.artemiy.player.ui.library.ViewMode.LIST -> com.artemiy.player.ui.library.SongList(
                        songs = filtered,
                        onSongClick = add,
                        onPlayNext = actions.onPlayNext,
                        onAddToQueue = actions.onAddToQueue,
                        onAddToPlaylist = actions.onAddToPlaylist,
                        onGoToAlbum = actions.onGoToAlbum,
                        onGoToArtist = actions.onGoToArtist,
                        swipe = false,
                    )
                    else -> com.artemiy.player.ui.library.SongsGrid(
                        songs = filtered,
                        columns = if (viewMode == com.artemiy.player.ui.library.ViewMode.GRID_2) 2 else 3,
                        state = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
                        onSongClick = add,
                        onPlayNext = actions.onPlayNext,
                        onAddToQueue = actions.onAddToQueue,
                        onAddToPlaylist = actions.onAddToPlaylist,
                        onGoToAlbum = actions.onGoToAlbum,
                        onGoToArtist = actions.onGoToArtist,
                    )
                }
            }
        }
    }
}
