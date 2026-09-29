package com.artemiy.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.EqualizerLoader
import com.artemiy.player.ui.components.MiniPlayer
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.barsInset

/*
 * Debug builds only (Settings → О приложении): every "nothing here yet" and "still loading" state
 * of the app on one page, each with a caption, so they can be looked at and talked about without
 * hunting for a song that has no cover or no lyrics.
 */

/** Whether this is a debug build — the showcase is never offered in the ones for friends. */
@Composable
internal fun isDebugBuild(): Boolean =
    (androidx.compose.ui.platform.LocalContext.current.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

@Composable
internal fun ShowcaseContent() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Все заглушки и состояния «пока пусто / ещё грузится» на одной странице. Номера — чтобы было проще ссылаться.",
            color = PlayerColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        Item("1. Нет обложки — в списках", "Строки песен, альбомов, очереди, поиска.") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AlbumArt(uri = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                Column {
                    Text("Песня без обложки", color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Артист", color = PlayerColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Item("2. Нет обложки — плитка", "Сетки альбомов, «Недавно добавленные», полки на главной.") {
            AlbumArt(uri = null, modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)))
        }
        Item("3. Нет обложки — большой плеер", "Обложка в полноэкранном плеере (оба стиля).") {
            AlbumArt(uri = null, modifier = Modifier.size(260.dp).clip(RoundedCornerShape(16.dp)))
        }
        Item("4. Мини-плеер: ничего не играет", "Пока не выбрана ни одна песня (например, первый запуск).") {
            MiniPlayer(
                title = stringResource(R.string.nothing_playing),
                artist = stringResource(R.string.no_track_chosen),
                albumArtUri = null,
                isPlaying = false,
                onOpen = {},
                onTogglePlayPause = {},
                onSkipNext = {},
            )
        }
        Item("5. Текст песни ещё грузится", "Прыгающие столбики в просмотрщике текста (и пока строится индекс поиска по текстам).") {
            PlayerLikeBox { EqualizerLoader(color = PlayerColors.TextSecondary, modifier = Modifier.size(44.dp)) }
        }
        Item("6. Текста у песни нет", "Вместо текста в просмотрщике.") {
            PlayerLikeBox {
                Text(stringResource(R.string.lyrics_not_found), color = PlayerColors.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
        Item("7. Поиск: ничего не найдено", "Во вкладке «Поиск», когда нет ни песен, ни строк текста.") {
            Hint(stringResource(R.string.nothing_found))
        }
        Item("8. Плейлистов нет", "Медиатека → Плейлисты.") { Hint(stringResource(R.string.no_playlists)) }
        Item("9. Плейлист пуст", "Открытый плейлист без песен.") { Hint(stringResource(R.string.playlist_empty)) }
        Item("10. Главная: миксов пока нет", "Пока статистики прослушиваний мало.") { Hint(stringResource(R.string.mixes_nothing_yet)) }
        Item("11. Главная: быстрый выбор пуст", "Пока нет часто слушаемых песен.") { Hint(stringResource(R.string.quick_picks_empty)) }
        Item("12. Медиатека: треков нет", "Ничего не нашлось в выбранных папках (или по фильтру).") { Hint(stringResource(R.string.no_tracks_found)) }
        Item("13. Альбом без названия", "Подпись вместо пустого тега альбома.") { Hint(stringResource(R.string.no_album)) }
        Box(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Item(title: String, where: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlayerColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(where, color = PlayerColors.TextSecondary, fontSize = 12.sp)
        }
        content()
    }
}

/** Roughly the lyrics viewer's ground, so its states look the way they do in the player. */
@Composable
private fun PlayerLikeBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PlayerColors.Background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Hint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PlayerColors.Background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = PlayerColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
