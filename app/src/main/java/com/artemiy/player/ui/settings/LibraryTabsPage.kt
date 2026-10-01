package com.artemiy.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.library.LibraryTab
import com.artemiy.player.ui.library.TabSetting
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.barsInset
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Settings → Общие → Вкладки медиатеки: which sections the Library page lists and in what order.
 * Drag a row by its handle to move it; the switch shows or hides it. Saved as soon as a drag ends.
 */
@Composable
internal fun LibraryTabsContent(tabs: List<TabSetting>, onChange: (List<TabSetting>) -> Unit) {
    var order by remember(tabs) { mutableStateOf(tabs) }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = order.indexOfFirst { it.tab.key == from.key }
        val toIndex = order.indexOfFirst { it.tab.key == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            order = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "hint") {
            Text(
                text = stringResource(R.string.library_tabs_hint),
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        items(order.size, key = { order[it].tab.key }) { index ->
            val setting = order[index]
            ReorderableItem(reorder, key = setting.tab.key) { isDragging ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(PlayerColors.Surface)
                        .padding(start = 6.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppIcons.DragHandle,
                        contentDescription = null,
                        tint = PlayerColors.TextTertiary,
                        modifier = Modifier
                            .draggableHandle(
                                onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDragStopped = { onChange(order) },
                            )
                            .padding(14.dp)
                            .size(22.dp),
                    )
                    Icon(
                        imageVector = tabIcon(setting.tab),
                        contentDescription = null,
                        tint = if (setting.shown) PlayerColors.TextPrimary else PlayerColors.TextTertiary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(setting.tab.labelRes),
                        color = if (setting.shown) PlayerColors.TextPrimary else PlayerColors.TextTertiary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    Switch(
                        checked = setting.shown,
                        onCheckedChange = { on ->
                            order = order.map { if (it.tab == setting.tab) it.copy(shown = on) else it }
                            onChange(order)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PlayerColors.OnAccent,
                            checkedTrackColor = PlayerColors.Accent,
                            uncheckedThumbColor = PlayerColors.TextSecondary,
                            uncheckedTrackColor = PlayerColors.SurfaceDim,
                            uncheckedBorderColor = PlayerColors.Border,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun tabIcon(tab: LibraryTab) = when (tab) {
    LibraryTab.PLAYLISTS -> AppIcons.Playlist
    LibraryTab.ARTISTS -> AppIcons.Artist
    LibraryTab.ALBUMS -> AppIcons.Album
    LibraryTab.TRACKS -> AppIcons.Songs
    LibraryTab.YEARS -> AppIcons.Years
    LibraryTab.GENRES -> AppIcons.Genres
    LibraryTab.FOLDERS -> AppIcons.Folder
}
