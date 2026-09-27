package com.artemiy.player.ui.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
        itemsIndexed(manualRows, key = { _, entry -> entry.key }, contentType = { _, _ -> "song" }) { i, entry ->
            ReorderableItem(reorderState, key = entry.key) { isDragging ->
                SwipeToRemove(onRemove = { onRemove(manualQueue.indexOfFirstEntry(entry.key, "m")) }) {
                    QueueRow(
                        item = entry.song,
                        isDragging = isDragging,
                        onClick = { onItemClick(entry.song) },
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
            itemsIndexed(continueRows, key = { _, entry -> entry.key }, contentType = { _, _ -> "song" }) { i, entry ->
                ReorderableItem(reorderState, key = entry.key) { isDragging ->
                    SwipeToRemove(onRemove = {
                        onRemove(manualQueue.size + continueQueue.indexOfFirstEntry(entry.key, "c"))
                    }) {
                        QueueRow(
                            item = entry.song,
                            isDragging = isDragging,
                            onClick = { onItemClick(entry.song) },
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
                            modifier = Modifier.fadeInList(state, continueQueueStart + i, topFadePx, bottomFadePx),
                        )
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
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(dragPx.toInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = !removed,
                    onDragStopped = { velocity ->
                        val threshold = with(density) { SWIPE_REMOVE_THRESHOLD.toPx() }
                        val flung = velocity < -with(density) { SWIPE_REMOVE_VELOCITY.toPx() }
                        val target = if (-dragPx > threshold || flung) -widthPx.toFloat() else 0f
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

/** How far a row has to be pulled, or how fast flung, to be removed on letting go. */
private val SWIPE_REMOVE_THRESHOLD = 56.dp
private val SWIPE_REMOVE_VELOCITY = 125.dp

@Composable
internal fun AddSongsToQueueRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            Icon(imageVector = AppIcons.Add, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(22.dp))
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
            contentDescription = "Перетащить",
            tint = LocalAdaptiveSecondaryColor.current,
            modifier = dragHandleModifier
                .padding(start = 12.dp, end = 4.dp)
                .size(22.dp),
        )
    }
}

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
            description = "Перемешать",
            active = shuffleEnabled,
            onClick = onToggleShuffle,
            modifier = Modifier.weight(1f),
        )
        QueueToggleButton(
            icon = AppIcons.Repeat,
            description = "Повтор",
            active = repeatEnabled,
            onClick = onToggleRepeat,
            modifier = Modifier.weight(1f),
        )
        QueueToggleButton(
            icon = AppIcons.Infinite,
            description = "Бесконечное воспроизведение",
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
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) PlayerColors.OnAccent else PlayerColors.TextPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Full-screen search overlay for the Queue's "Добавить треки в очередь" row — same idea as the
 * Search tab, but tapping a result adds it to the end of the queue instead of playing it, and
 * the sheet stays open so you can queue up several before dismissing. */
@Composable
internal fun AddToQueuePicker(songs: List<Song>, onAdd: (Song) -> Unit, onDismiss: () -> Unit) {
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
                    imageVector = AppIcons.Close,
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
                Icon(imageVector = AppIcons.Search, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(18.dp))
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
                            imageVector = if (added) AppIcons.Check else AppIcons.Add,
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
