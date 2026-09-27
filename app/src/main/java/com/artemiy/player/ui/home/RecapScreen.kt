package com.artemiy.player.ui.home

import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Recap
import com.artemiy.player.data.Song
import com.artemiy.player.data.nextRecapAt
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.library.LibraryHeader
import com.artemiy.player.ui.theme.PlayerColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayMonth get() = SimpleDateFormat("d MMMM", Locale.getDefault())

/** "14 – 20 сентября" for a Monday-to-Monday week. */
fun weekLabel(recap: Recap): String {
    val lastDay = Date(recap.weekEnd - 1)
    val first = Date(recap.weekStart)
    val sameMonth = SimpleDateFormat("M", Locale.getDefault()).let { it.format(first) == it.format(lastDay) }
    val start = if (sameMonth) SimpleDateFormat("d", Locale.getDefault()).format(first) else dayMonth.format(first)
    return "$start – ${dayMonth.format(lastDay)}"
}

@Composable
private fun hoursLabel(minutes: Int): String =
    if (minutes < 60) stringResource(R.string.unit_min, minutes) else stringResource(R.string.unit_hours_approx, minutes / 60f)

/** Home's last section: the week's recap, or when the first one will show up. */
@Composable
fun RecapCard(recap: Recap?, onOpen: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(text = stringResource(R.string.weekly_recap), color = PlayerColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        if (recap == null) {
            Text(
                text = stringResource(R.string.recap_first_at, dayMonth.format(Date(nextRecapAt(System.currentTimeMillis())))),
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
            )
            return
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PlayerColors.Surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(uri = recap.topSongs.firstOrNull()?.first?.uri, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(text = weekLabel(recap), color = PlayerColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = pluralStringResource(R.plurals.plays_count, recap.playCount, recap.playCount) + " · " + hoursLabel(recap.minutes),
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                recap.topArtists.firstOrNull()?.let { (artist, _) ->
                    Text(
                        text = stringResource(R.string.recap_top_artist, artist),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun RecapScreen(recap: Recap, onBack: () -> Unit, onPlay: (Song, List<Song>) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
        LibraryHeader(title = stringResource(R.string.weekly_recap), showBack = true, onBack = onBack)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(text = weekLabel(recap), color = PlayerColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 14.dp))

            RecapBlock {
                Row {
                    RecapStat(value = "${recap.playCount}", label = pluralStringResource(R.plurals.plays_word, recap.playCount), modifier = Modifier.weight(1f))
                    RecapStat(value = hoursLabel(recap.minutes), label = stringResource(R.string.recap_of_music), modifier = Modifier.weight(1f))
                }
                recap.topSongs.firstOrNull()?.let { (song, count) ->
                    Text(
                        text = stringResource(R.string.recap_most_played, song.title, pluralStringResource(R.plurals.times_count, count, count)),
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            RecapTitle(stringResource(R.string.recap_top_songs))
            val topSongs = recap.topSongs.map { it.first }
            recap.topSongs.forEachIndexed { index, (song, count) ->
                RecapSongRow(place = index + 1, song = song, note = pluralStringResource(R.plurals.times_count, count, count), onClick = { onPlay(song, topSongs) })
            }

            RecapTitle(stringResource(R.string.recap_top_artists))
            RecapBlock {
                recap.topArtists.forEachIndexed { index, (artist, count) ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = "${index + 1}", color = PlayerColors.TextTertiary, fontSize = 14.sp, modifier = Modifier.padding(end = 12.dp))
                        Text(text = artist, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "$count", color = PlayerColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            if (recap.topGenre != null || recap.busiestDay != null || recap.busiestPart != null) {
                RecapTitle(stringResource(R.string.recap_how))
                RecapBlock {
                    recap.topGenre?.let { RecapFact(stringResource(R.string.recap_top_genre), it) }
                    recap.busiestDay?.let { RecapFact(stringResource(R.string.recap_busiest_day), it) }
                    recap.busiestPart?.let { RecapFact(stringResource(R.string.recap_busiest_part), stringResource(it.labelRes)) }
                }
            }

            if (recap.newByArtist.isNotEmpty()) {
                RecapTitle(stringResource(R.string.recap_new_in_library))
                RecapBlock {
                    recap.newByArtist.forEach { (artist, songs) ->
                        RecapFact(artist, "+${songs.size}")
                    }
                }
            }

            if (recap.forgotten.isNotEmpty()) {
                RecapTitle(stringResource(R.string.recap_forgotten))
                recap.forgotten.forEach { song ->
                    RecapSongRow(place = null, song = song, note = null, onClick = { onPlay(song, recap.forgotten) })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecapTitle(text: String) {
    Text(
        text = text,
        color = PlayerColors.TextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun RecapBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PlayerColors.Surface)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun RecapStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, color = PlayerColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = label, color = PlayerColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun RecapFact(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, color = PlayerColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = PlayerColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecapSongRow(place: Int?, song: Song, note: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (place != null) {
            Text(text = "$place", color = PlayerColors.TextTertiary, fontSize = 14.sp, modifier = Modifier.padding(end = 12.dp))
        }
        AlbumArt(uri = song.uri, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = song.title, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = song.artist, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (note != null) Text(text = note, color = PlayerColors.TextSecondary, fontSize = 12.sp)
    }
}
