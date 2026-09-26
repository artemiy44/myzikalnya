package com.artemiy.player.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Song
import com.artemiy.player.data.normalizeForSearch
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.SongActionsMenu
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    searchLyrics: suspend (String) -> List<LyricsHit>,
    lyricsIndexProgress: Pair<Int, Int>?,
) {
    var query by remember { mutableStateOf("") }

    // Waits for a pause in typing before hitting the lyrics database.
    val lyricHits by produceState(emptyList<LyricsHit>(), query, lyricsIndexProgress == null) {
        val q = query.trim()
        value = if (q.length < 3) {
            emptyList()
        } else {
            delay(300)
            searchLyrics(q)
        }
    }

    val results = remember(query, songs) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else songs.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
        Text(
            text = "Поиск",
            color = PlayerColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 12.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PlayerColors.Surface)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = PlayerColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Название, исполнитель, строчка из песни",
                        color = PlayerColors.TextTertiary,
                        fontSize = 15.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = PlayerColors.TextPrimary, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(PlayerColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Очистить",
                    tint = PlayerColors.TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            query = ""
                        },
                )
            }
        }

        if (query.isNotBlank() && lyricsIndexProgress != null) {
            val (done, total) = lyricsIndexProgress
            Text(
                text = "Тексты песен ещё собираются ($done из $total) — по тексту найдётся пока не всё",
                color = PlayerColors.TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        when {
            query.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Начни вводить, чтобы найти трек, исполнителя, альбом или строчку из песни",
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }

            results.isEmpty() && lyricHits.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Ничего не найдено", color = PlayerColors.TextSecondary, fontSize = 13.sp)
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    items(results, key = { "title-${it.id}" }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onSongClick(song, results)
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AlbumArt(uri = song.uri, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    text = song.title,
                                    color = PlayerColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = song.artist,
                                    color = PlayerColors.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            SongActionsMenu(
                                song = song,
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                                onAddToPlaylist = onAddToPlaylist,
                                onGoToAlbum = onGoToAlbum,
                                onGoToArtist = onGoToArtist,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    if (lyricHits.isNotEmpty()) {
                        item(key = "lyrics-header") {
                            Text(
                                text = "В тексте песен",
                                color = PlayerColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = if (results.isEmpty()) 0.dp else 18.dp, bottom = 4.dp),
                            )
                        }
                        items(lyricHits, key = { "lyrics-${it.song.id}" }) { hit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        onSongClick(hit.song, lyricHits.map { it.song })
                                    }
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AlbumArt(uri = hit.song.uri, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(
                                        text = hit.song.title,
                                        color = PlayerColors.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = hit.song.artist,
                                        color = PlayerColors.TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = highlightMatch(hit.line, query.trim()),
                                        color = PlayerColors.TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                SongActionsMenu(
                                    song = hit.song,
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onAddToPlaylist = onAddToPlaylist,
                                    onGoToAlbum = onGoToAlbum,
                                    onGoToArtist = onGoToArtist,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The lyric line with the searched-for part in bright bold. */
private fun highlightMatch(line: String, query: String): AnnotatedString = buildAnnotatedString {
    append(line)
    val start = normalizeForSearch(line).indexOf(normalizeForSearch(query))
    if (start >= 0 && start + query.length <= line.length) {
        addStyle(SpanStyle(color = PlayerColors.TextPrimary, fontWeight = FontWeight.SemiBold), start, start + query.length)
    }
}
