package com.artemiy.player.ui.search

import com.artemiy.player.ui.components.fadesWithHeader
import com.artemiy.player.ui.theme.discRowTile
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.inAppFont
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Song
import com.artemiy.player.data.artists
import com.artemiy.player.ui.library.albumKey
import com.artemiy.player.data.normalizeForSearch
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.SongActionsMenu
import com.artemiy.player.ui.components.LoadingMark
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
    onGoToArtistNamed: (String) -> Unit,
    searchLyrics: suspend (String) -> List<LyricsHit>,
    lyricsIndexProgress: Pair<Int, Int>?,
) {
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    // Waits for a pause in typing before hitting the lyrics database.
    val lyricHits by produceState(emptyList<LyricsHit>(), query, lyricsIndexProgress == null) {
        val q = query.trim()
        value = if (!com.artemiy.player.data.lyricsQueryLongEnough(q)) {
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

    // Artists and albums by name, each with how well the typed text fits them.
    val artistGroups = remember(songs) { songs.flatMap { s -> s.artists().map { it to s } }.groupBy({ it.first }, { it.second }).toList() }
    val albumGroups = remember(songs) { songs.filter { it.album.isNotBlank() }.groupBy { albumKey(it.album, it.artist) }.toList() }
    val artistHits = remember(query, artistGroups) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else artistGroups.map { it to nameFit(it.first, q) }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Pair<String, List<Song>>, Int>> { it.second }.thenByDescending { it.first.second.size })
            .take(SECTION_MAX).map { it.first }
    }
    val albumHits = remember(query, albumGroups) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else albumGroups.map { it to nameFit(it.second.first().album, q) }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Pair<String, List<Song>>, Int>> { it.second }.thenByDescending { it.first.second.size })
            .take(SECTION_MAX).map { it.first }
    }
    val order = remember(query, artistHits, albumHits, results, lyricHits) {
        val q = query.trim()
        val fit = mapOf(
            SearchSection.Artists to (artistHits.maxOfOrNull { nameFit(it.first, q) } ?: 0).toFloat(),
            SearchSection.Albums to (albumHits.maxOfOrNull { nameFit(it.second.first().album, q) } ?: 0).toFloat(),
            // Songs found only by their artist or album still count as a weak match.
            SearchSection.Songs to if (results.isEmpty()) 0f else maxOf(1f, results.maxOf { nameFit(it.title, q) }.toFloat()),
            // A line of several words is most likely a lyric being remembered; one word is more likely a name.
            SearchSection.Lyrics to if (lyricHits.isEmpty()) 0f else if (q.split(Regex("\\s+")).size >= 3) 2.5f else 0.5f,
        )
        SearchSection.entries.filter { (fit[it] ?: 0f) > 0f }.sortedByDescending { fit[it] }.ifEmpty { listOf(SearchSection.Songs) }
    }
    val showHeaders = order.size > 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
      // The big title folds into a slim bar as the results scroll; the search field stays put.
      com.artemiy.player.ui.components.CollapsingHeader(
        title = stringResource(R.string.tab_search),
        bigHeader = {
            if (com.artemiy.player.ui.theme.discUi) com.artemiy.player.ui.components.DiscPageTitle(
                stringResource(R.string.tab_search),
                modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 12.dp),
            ) else Text(
                text = stringResource(R.string.tab_search),
                color = PlayerColors.TextPrimary,
                style = com.artemiy.player.ui.theme.pageTitleStyle,
                modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 12.dp),
            )
        },
      ) {

        Row(
            modifier = Modifier
                .fadesWithHeader()
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(if (com.artemiy.player.ui.theme.expressiveUi) RoundedCornerShape(percent = 50) else if (com.artemiy.player.ui.theme.discUi) RoundedCornerShape(20.dp) else RoundedCornerShape(12.dp))
                .background(if (com.artemiy.player.ui.theme.discUi) com.artemiy.player.ui.theme.Disc.tileFill else PlayerColors.Surface)
                .then(if (com.artemiy.player.ui.theme.discUi) Modifier.heightIn(min = 56.dp) else Modifier)
                .padding(horizontal = if (com.artemiy.player.ui.theme.expressiveUi || com.artemiy.player.ui.theme.discUi) 18.dp else 14.dp, vertical = if (com.artemiy.player.ui.theme.expressiveUi) 14.dp else if (com.artemiy.player.ui.theme.discUi) 8.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = null,
                tint = PlayerColors.TextSecondary,
                modifier = Modifier.size(if (com.artemiy.player.ui.theme.discUi) 22.dp else 18.dp),
            )
            com.artemiy.player.ui.components.HintTextField(
                value = query,
                onValueChange = { query = it },
                hint = stringResource(R.string.search_hint),
                fontSize = if (com.artemiy.player.ui.theme.discUi) 17.sp else 15.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.clear),
                    tint = PlayerColors.TextSecondary,
                    modifier = (if (com.artemiy.player.ui.theme.discUi) Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(com.artemiy.player.ui.theme.Disc.tileFill)
                        .padding(11.dp)
                    else Modifier.size(18.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) {
                            query = ""
                        },
                )
            }
        }

        if (query.isNotBlank() && lyricsIndexProgress != null) {
            val (done, total) = lyricsIndexProgress
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoadingMark(color = PlayerColors.TextTertiary, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.lyrics_indexing, done, total),
                    color = PlayerColors.TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

        when {
            query.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.search_start_typing),
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }

            results.isEmpty() && lyricHits.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.nothing_found), color = PlayerColors.TextSecondary, fontSize = 13.sp)
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.artemiy.player.ui.theme.LocalBarsInset.current)) {
                    // What was typed decides the order: names (an artist, an album, a title) or, for a few
                    // words, the lyrics — whichever matches most closely goes first.
                    for (section in order) {
                    when (section) {
                    SearchSection.Artists -> {
                        if (showHeaders) item(key = "artists-header") { SectionTitle(stringResource(R.string.artists), first = order.first() == SearchSection.Artists) }
                        items(artistHits, key = { "artist-${it.first}" }) { (name, artistSongs) ->
                            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .discRowTile()
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onGoToArtistNamed(name) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AlbumArt(uri = artistSongs.first().uri, modifier = Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape))
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(text = name, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = androidx.compose.ui.res.pluralStringResource(R.plurals.songs_count, artistSongs.size, artistSongs.size),
                                            color = PlayerColors.TextSecondary,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SearchSection.Albums -> {
                        if (showHeaders) item(key = "albums-header") { SectionTitle(stringResource(R.string.albums), first = order.first() == SearchSection.Albums) }
                        items(albumHits, key = { "album-${it.first}" }) { (_, albumSongs) ->
                            val first = albumSongs.first()
                            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .discRowTile()
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onGoToAlbum(first) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AlbumArt(uri = first.uri, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(text = first.album, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = first.artist, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    SearchSection.Songs -> {
                        if (showHeaders) item(key = "songs-header") { SectionTitle(stringResource(R.string.tracks), first = order.first() == SearchSection.Songs) }
                    items(results, key = { "title-${it.id}" }) { song ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        com.artemiy.player.ui.components.SwipeSongRow(song) {
                            Row(
                                modifier = Modifier
                                    .discRowTile()
                                    .fillMaxWidth()
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) {
                                        onSongClick(song, results)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
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
                        }
                    }
                    }
                    SearchSection.Lyrics -> {
                    if (lyricHits.isNotEmpty()) {
                        item(key = "lyrics-header") {
                            Text(
                                text = stringResource(R.string.in_lyrics),
                                color = PlayerColors.TextPrimary,
                                fontSize = if (com.artemiy.player.ui.theme.discUi) 24.sp else 15.sp,
                                fontWeight = if (com.artemiy.player.ui.theme.discUi) FontWeight.ExtraBold else FontWeight.Bold,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = if (order.first() == SearchSection.Lyrics) 0.dp else 18.dp, bottom = 4.dp),
                            )
                        }
                        items(lyricHits, key = { "lyrics-${it.song.id}" }) { hit ->
Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                            com.artemiy.player.ui.components.SwipeSongRow(hit.song) {
                                Row(
                                    modifier = Modifier
                                        .discRowTile()
                                        .fillMaxWidth()
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) {
                                            onSongClick(hit.song, lyricHits.map { it.song })
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
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
                                            text = highlightMatch(hit.line, query.trim(), PlayerColors.TextPrimary, hit.marks),
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
                }
            }
        }
          }
}
}

/** The lyric line with the searched-for part in bright bold. */
private fun highlightMatch(line: String, query: String, highlight: Color, soundMarks: List<IntRange> = emptyList()): AnnotatedString = buildAnnotatedString {
    append(line)
    val style = SpanStyle(color = highlight, fontWeight = FontWeight.SemiBold)
    val start = normalizeForSearch(line).indexOf(normalizeForSearch(query))
    if (start >= 0 && start + query.length <= line.length) {
        addStyle(style, start, start + query.length)
    } else {
        // Found by how it sounds: the characters that read like the query.
        soundMarks.forEach { range ->
            val from = range.first.coerceIn(0, line.length)
            val to = (range.last + 1).coerceIn(from, line.length)
            if (to > from) addStyle(style, from, to)
        }
    }
}

/** The parts of the results; the order they come in depends on what was typed. */
private enum class SearchSection { Artists, Albums, Songs, Lyrics }

/** How many artists / albums are shown. */
private const val SECTION_MAX = 5

/** How well [query] fits [name]: 3 the same, 2 the start of it or of one of its words, 1 somewhere inside, 0 not at all. */
private fun nameFit(name: String, query: String): Int {
    val n = normalizeForSearch(name)
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return 0
    return when {
        n == q -> 3
        n.startsWith(q) || n.split(' ', '-', '(', '.').any { it.startsWith(q) } -> 2
        n.contains(q) -> 1
        else -> 0
    }
}

@androidx.compose.runtime.Composable
private fun SectionTitle(text: String, first: Boolean) {
    Text(
        text = text,
        color = PlayerColors.TextPrimary,
        fontSize = if (com.artemiy.player.ui.theme.discUi) 24.sp else 15.sp,
        fontWeight = if (com.artemiy.player.ui.theme.discUi) FontWeight.ExtraBold else FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = if (first) 0.dp else if (com.artemiy.player.ui.theme.discUi) 22.dp else 18.dp, bottom = if (com.artemiy.player.ui.theme.discUi) 8.dp else 4.dp),
    )
}
