package com.artemiy.player.ui.library

import com.artemiy.player.ui.theme.barsInset
import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import com.artemiy.player.ui.components.HeroBar
import com.artemiy.player.ui.components.groupedCard
import com.artemiy.player.ui.components.staggeredEntrance
import com.artemiy.player.ui.components.rememberEntrance
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.inAppFont
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.artemiy.player.ui.components.expandAnchor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Song
import androidx.compose.ui.window.Dialog
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.HeroOverArt
import com.artemiy.player.ui.components.heroPanel
import com.artemiy.player.ui.components.drawHeroDrain
import com.artemiy.player.ui.components.rememberAlbumArtBitmap
import com.artemiy.player.ui.components.HeroTextShadow
import com.artemiy.player.ui.components.rememberArrowTint
import com.artemiy.player.ui.components.CircleIconButton
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.artemiy.player.ui.components.BlurredCollageArt
import com.artemiy.player.ui.components.COLLAGE_HERO_HEIGHT
import com.artemiy.player.ui.components.rememberBlurredCollage
import com.artemiy.player.ui.components.PlayPillButton
import com.artemiy.player.ui.components.rememberCheckFlash
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.ui.components.songLongPressTrigger
import com.artemiy.player.ui.theme.PlayerColors

@Composable
fun ArtistDetailScreen(
    artist: String,
    songs: List<Song>,
    albums: List<Pair<String, List<Song>>>,
    /** What a long-pressed album offers (see AlbumActionsMenuPopup). */
    albumMenu: @Composable (List<Song>, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
    /** The page can be a year's or a genre's too: its name in the header ([artist] still seeds
     * the header picture), a huge [bigMark] behind it ("2024"), what each song row says under
     * its title, and how "add all to the queue" is worded. */
    title: String = artist,
    bigMark: String? = null,
    songSubtitle: (Song) -> String = { it.album.ifBlank { artist } },
    queueMessageRes: Int = R.string.add_artist_to_queue_msg,
    /** Which genres' pictures the header shows; null = worked out from [songs]. */
    genrePictures: List<String>? = null,
    /** Kept by the caller so the page is where it was left when an album it opened is closed. */
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onBack: () -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddAllToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
) {
    val entrance = rememberEntrance()
    val queuedFlash = rememberCheckFlash()
    val context = androidx.compose.ui.platform.LocalContext.current
    // How often each song was played: for "Often played" and the sort.
    val playCounts by androidx.compose.runtime.produceState<Map<Long, Int>>(emptyMap(), songs) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.artemiy.player.data.AppDatabase.get(context).playHistoryDao().playCounts().associate { it.songId to it.playCount }
        }
    }
    var sortName by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(TrackSort.DEFAULT.name) }
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var searching by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val sort = TrackSort.valueOf(sortName)
    val shown = remember(songs, sort, query, playCounts) {
        val q = query.trim().lowercase()
        val found = if (q.isEmpty()) songs else songs.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) }
        when (sort) {
            TrackSort.DEFAULT -> found
            TrackSort.TITLE -> found.sortedBy { it.title.lowercase() }
            TrackSort.MOST_PLAYED -> found.sortedWith(compareByDescending<Song> { playCounts[it.id] ?: 0 }.thenBy { it.title.lowercase() })
            TrackSort.NEWEST -> found.sortedByDescending { it.dateAddedMs }
            TrackSort.LONGEST -> found.sortedByDescending { it.durationMs }
        }
    }
    // A short page has nothing to "often play": the block is for an artist with several songs, two or more of them heard twice or more.
    val topPlayed = remember(songs, playCounts) {
        if (songs.size <= 3) emptyList() else {
            val heard = songs.filter { (playCounts[it.id] ?: 0) >= 2 }
            if (heard.size >= 2) heard.sortedByDescending { playCounts[it.id] ?: 0 }.take(5) else emptyList()
        }
    }
    val totalMs = remember(songs) { songs.sumOf { it.durationMs } }
    var showAddToQueueDialog by remember { mutableStateOf(false) }
    // A lazy list: only the rows on screen are built (and their covers loaded) — a page of hundreds
    // of songs ("no genre") built all at once heated the phone up.
    val albumRows = remember(albums) { albums.chunked(2) }
    val barShown = com.artemiy.player.ui.components.rememberHeroBarShown(listState)
    val washOut = remember { com.artemiy.player.ui.components.HeroWashState() }
    // No stretching of the list's edge on this page: the picture behind it (the wash, the panel) is
    // drawn from the scroll position and doesn't stretch along, so pulling past the end tore a light
    // strip between the picture and the panel.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.LocalOverscrollFactory provides null) {
    Box(modifier = Modifier.fillMaxSize().drawHeroDrain(washOut, listState)) {
    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.artemiy.player.ui.theme.LocalBarsInset.current),
    ) {
      item(key = "hero") {
        HeroOverArt(
            // The drawn picture is always deep and dark: a white arrow reads on it.
            topTint = androidx.compose.ui.graphics.Color.White,
            onBack = onBack,
            washKey = "artist:$title",
            washOut = washOut,
            height = COLLAGE_HERO_HEIGHT,
            barShown = barShown,
            roundArt = true,
            discStats = listOf(pluralStringResource(R.plurals.songs_count, songs.size, songs.size), pluralStringResource(R.plurals.albums_count, albums.size, albums.size), totalTimeLabel(totalMs)),
            art = {
                ArtistGenreArt(artist, songs, genrePictures)
                if (bigMark != null) {
                    Text(
                        text = bigMark,
                        color = Color.White.copy(alpha = 0.16f),
                        fontSize = 150.sp,
                        fontFamily = com.artemiy.player.ui.mood.Unbounded,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.align(Alignment.Center).padding(bottom = 90.dp),
                    )
                }
            },
        ) {
            Text(
                text = com.artemiy.player.ui.components.heroTitleText(title),
                color = com.artemiy.player.ui.components.heroText,
                fontSize = com.artemiy.player.ui.components.heroTitleSize(30.sp),
                style = com.artemiy.player.ui.components.heroTitleStyle(TextStyle(shadow = com.artemiy.player.ui.components.heroShadow).inAppFont()),
                fontWeight = FontWeight.ExtraBold,
                textAlign = com.artemiy.player.ui.components.heroTextAlign,
                maxLines = com.artemiy.player.ui.components.heroTitleLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (!com.artemiy.player.ui.theme.discUi) Text(
                text = pluralStringResource(R.plurals.songs_count, songs.size, songs.size) + " · " + pluralStringResource(R.plurals.albums_count, albums.size, albums.size) + " · " + totalTimeLabel(totalMs),
                color = com.artemiy.player.ui.components.heroSubText,
                fontSize = 13.sp,
                style = TextStyle(shadow = com.artemiy.player.ui.components.heroShadow).inAppFont(),
                modifier = Modifier.padding(top = 4.dp),
            )
            Box(modifier = Modifier.padding(top = 18.dp)) {
                com.artemiy.player.ui.components.HeroButtons(
                    onShuffle = { onShuffleAll(songs) },
                    onPlay = { onPlayAll(songs) },
                    trailingIcon = AppIcons.AddToQueue,
                    trailingDescription = stringResource(R.string.add_to_play_queue),
                    trailingCheck = queuedFlash.visible,
                    onTrailing = { showAddToQueueDialog = true },
                )
            }
        }

        if (showAddToQueueDialog) {
            AddArtistToQueueDialog(
                songCount = songs.size,
                messageRes = queueMessageRes,
                onDismiss = { showAddToQueueDialog = false },
                onConfirm = {
                    showAddToQueueDialog = false
                    onAddAllToQueue(songs)
                    queuedFlash.flash()
                },
            )
        }

      }

        if (topPlayed.isNotEmpty()) {
            item(key = "topTitle") {
                Row(
                    modifier = Modifier.fillMaxWidth().heroPanel(roundTop = true).padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(text = stringResource(R.string.often_played), color = PlayerColors.TextPrimary, style = com.artemiy.player.ui.theme.sectionTitleStyle)
                    val totalPlays = topPlayed.sumOf { playCounts[it.id] ?: 0 }
                    Text(
                        text = pluralStringResource(R.plurals.plays_count, totalPlays, totalPlays),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 10.dp, bottom = 3.dp),
                    )
                }
            }
            item(key = "topRow") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heroPanel()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    topPlayed.forEach { song ->
                        Column(
                            modifier = Modifier
                                .width(124.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onSongClick(song, topPlayed) },
                        ) {
                            AlbumArt(uri = song.uri, modifier = Modifier.size(124.dp).clip(RoundedCornerShape(if (com.artemiy.player.ui.theme.expressiveUi) 22.dp else 10.dp)))
                            Text(text = song.title, color = PlayerColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            val count = playCounts[song.id] ?: 0
                            Text(text = pluralStringResource(R.plurals.plays_count, count, count), color = PlayerColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        if (albums.isNotEmpty()) {
            item(key = "albumsTitle") {
                Text(text = stringResource(R.string.albums), color = PlayerColors.TextPrimary, style = com.artemiy.player.ui.theme.sectionTitleStyle, modifier = Modifier.fillMaxWidth().heroPanel(roundTop = topPlayed.isEmpty()).padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 10.dp))
            }
            items(albumRows.size, key = { "albums$it" }) { rowIndex ->
                val rowAlbums = albumRows[rowIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth().heroPanel().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowAlbums.forEach { (album, albumSongs) ->
                            var albumMenuOpen by remember { mutableStateOf(false) }
                            com.artemiy.player.ui.components.WarmHeroWash(albumSongs.firstOrNull()?.uri, "album:${albumSongs.firstOrNull()?.uri}")
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .songLongPressTrigger(onClick = { onAlbumClick(album) }, onLongPress = { albumMenuOpen = true }),
                            ) {
                                albumMenu(albumSongs, albumMenuOpen) { albumMenuOpen = false }
                                AlbumArt(
                                    uri = albumSongs.firstOrNull()?.uri,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .expandAnchor(
                                            com.artemiy.player.ui.components.LibraryAnchors,
                                            "album:${albumKey(album, albumSongs.firstOrNull()?.artist.orEmpty())}",
                                            if (com.artemiy.player.ui.theme.expressiveUi) 22.dp else 10.dp,
                                            washKey = "album:${albumSongs.firstOrNull()?.uri}",
                                        )
                                        .clip(RoundedCornerShape(if (com.artemiy.player.ui.theme.expressiveUi) 22.dp else 10.dp)),
                                )
                                Text(
                                    text = album.ifBlank { stringResource(R.string.no_album) },
                                    color = PlayerColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        if (rowAlbums.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
            }
        }

        item(key = "tracksTitle") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heroPanel(roundTop = albums.isEmpty() && topPlayed.isEmpty())
                    .padding(horizontal = com.artemiy.player.ui.components.pageGutter)
                    .padding(top = 20.dp, bottom = 10.dp, start = if (com.artemiy.player.ui.theme.expressiveUi) 8.dp else 0.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tracks),
                        color = PlayerColors.TextPrimary,
                        style = com.artemiy.player.ui.theme.sectionTitleStyle,
                        modifier = Modifier.weight(1f),
                    )
                    // Only worth having once there is something to search through.
                    if (songs.size > 4) {
                        Icon(
                            imageVector = AppIcons.Search,
                            contentDescription = stringResource(R.string.search_tracks_hint),
                            tint = if (searching) PlayerColors.TextPrimary else PlayerColors.TextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) {
                                    searching = !searching
                                    if (!searching) query = ""
                                },
                        )
                        Box(modifier = Modifier.padding(start = 14.dp)) {
                            Icon(
                                imageVector = AppIcons.Sort,
                                contentDescription = null,
                                tint = if (sort != TrackSort.DEFAULT) PlayerColors.TextPrimary else PlayerColors.TextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { sortMenuOpen = true },
                            )
                            com.artemiy.player.ui.components.AppDropdownMenu(expanded = sortMenuOpen, onDismiss = { sortMenuOpen = false }) {
                                TrackSort.entries.forEach { option ->
                                    com.artemiy.player.ui.components.AppMenuItem(
                                        text = stringResource(option.labelRes),
                                        selected = option == sort,
                                        onClick = { sortName = option.name; sortMenuOpen = false },
                                    )
                                }
                            }
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(visible = searching) {
                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(if (com.artemiy.player.ui.theme.discUi) 18.dp else 10.dp))
                            .background(if (com.artemiy.player.ui.theme.discUi) com.artemiy.player.ui.theme.Disc.tileFill else PlayerColors.Surface)
                            .padding(horizontal = if (com.artemiy.player.ui.theme.discUi) 16.dp else 12.dp, vertical = if (com.artemiy.player.ui.theme.discUi) 15.dp else 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = AppIcons.Search, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(16.dp))
                        com.artemiy.player.ui.components.HintTextField(
                            value = query,
                            onValueChange = { query = it },
                            hint = stringResource(R.string.search_tracks_hint),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                    }
                }
            }
        }
        if (shown.isEmpty() && query.isNotBlank()) {
            item(key = "noTracks") {
                Text(
                    text = stringResource(R.string.no_tracks_found),
                    color = PlayerColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().heroPanel().padding(horizontal = com.artemiy.player.ui.components.pageGutter).padding(vertical = 14.dp),
                )
            }
        }
        items(shown.size, key = { shown[it].id }) { index ->
                val song = shown[index]
                var menuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().heroPanel().padding(horizontal = com.artemiy.player.ui.components.pageGutter)) {
                com.artemiy.player.ui.components.SwipeSongRow(song) {
                    Row(
                        modifier = Modifier
                            .staggeredEntrance(index, entrance)
                        .groupedCard(index, shown.size)
                            .fillMaxWidth()
                            .songLongPressTrigger(
                                onClick = { onSongClick(song, shown) },
                                onLongPress = { menuExpanded = true },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArt(uri = song.uri, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(text = song.title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = songSubtitle(song), color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SongActionsMenuPopup(
                            song = song,
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = onAddToPlaylist,
                            onGoToAlbum = onGoToAlbum,
                        )
                    }
                }
                }
        }
    }
    HeroBar(shown = barShown, title = title, onBack = onBack)
    }
    }
}

@Composable
private fun AddArtistToQueueDialog(songCount: Int, messageRes: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    com.artemiy.player.ui.components.AppDialog(onDismiss = onDismiss) {
        com.artemiy.player.ui.components.DialogTitle(stringResource(R.string.add_to_play_queue_q))
        com.artemiy.player.ui.components.DialogMessage(
            stringResource(messageRes, songCount),
        )
        com.artemiy.player.ui.components.DialogButtons(dismissLabel = stringResource(R.string.no), onDismiss = onDismiss, confirmLabel = stringResource(R.string.add), onConfirm = onConfirm)
    }
}


/** How the tracks of an artist (or year, genre) page can be ordered. */
private enum class TrackSort(val labelRes: Int) {
    DEFAULT(R.string.sort_default),
    TITLE(R.string.sort_by_title),
    MOST_PLAYED(R.string.sort_most_played),
    NEWEST(R.string.sort_recent),
    LONGEST(R.string.sort_longest),
}

/** The page's total playing time: "47 min", or "3 h 12 min". */
@Composable
private fun totalTimeLabel(ms: Long): String {
    val minutes = (ms / 60_000).toInt()
    return if (minutes < 60) stringResource(R.string.unit_min, minutes) else stringResource(R.string.unit_hours_min, minutes / 60, minutes % 60)
}
