package com.artemiy.player.ui.home

import com.artemiy.player.ui.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Mix
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.CircleIconButton
import com.artemiy.player.ui.components.PlayPillButton
import com.artemiy.player.ui.components.rememberCheckFlash
import com.artemiy.player.ui.components.HeroOverArt
import com.artemiy.player.ui.components.HeroTextShadow
import com.artemiy.player.ui.components.SongActionsMenu
import com.artemiy.player.ui.theme.AccentFamily
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.accentColors

/** Each mix gets its own color from GNOME's accent set, deepening toward one corner. */
fun mixBrush(colorIndex: Int): Brush {
    val colors = accentColors(AccentFamily.STOCK)
    val base = colors[colorIndex % colors.size]
    return Brush.linearGradient(listOf(lerp(base, Color.White, 0.12f), lerp(base, Color.Black, 0.35f)))
}

/** The Home card for one mix: tall, colored, title up top, its artists at the bottom. */
@Composable
fun MixCard(mix: Mix, onClick: () -> Unit) {
    val textMeasurer = rememberTextMeasurer()
    Column(
        modifier = Modifier
            .width(200.dp)
            .height(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(mixBrush(mix.colorIndex))
            .drawBehind { drawMixMotif(mix.motif, textMeasurer) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Text(text = "Микс", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        BoxWithConstraints(modifier = Modifier.padding(top = 2.dp)) {
            // Shrinks from 27sp only as far as needed for the longest word to fit on one line, so
            // "опробованное" isn't chopped up mid-word.
            val density = LocalDensity.current
            val baseStyle = LocalTextStyle.current
            val widthPx = constraints.maxWidth
            val fontSize = remember(mix.title, widthPx) {
                val longest = mix.title.split(' ').maxByOrNull { it.length }.orEmpty()
                var size = 27f
                while (size > 16f) {
                    // Measured with the same style the Text below ends up drawing with (the theme's
                    // default text style carries its own letter spacing).
                    val width = textMeasurer.measure(
                        longest,
                        baseStyle.merge(TextStyle(fontSize = size.sp, fontWeight = FontWeight.ExtraBold)),
                        density = density,
                    ).size.width
                    if (width <= widthPx) break
                    size -= 1f
                }
                size
            }
            Text(
                text = mix.title,
                color = Color.White,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.11f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(modifier = Modifier.weight(1f))
        Text(
            text = mix.subtitle,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 17.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A mix's own page — laid out like an album/artist page, with the mix color as its "cover". */
@Composable
fun MixScreen(
    mix: Mix,
    onBack: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onSaveAsPlaylist: (Mix) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    val savedFlash = rememberCheckFlash()
    Column(modifier = Modifier.fillMaxSize().background(PlayerColors.Background).verticalScroll(rememberScrollState())) {
        HeroOverArt(
            topTint = Color.White,
            onBack = onBack,
            art = {
                val textMeasurer = rememberTextMeasurer()
                // The card's picture, big, drifting slowly.
                val drift = rememberInfiniteTransition(label = "mixDrift")
                val phase by drift.animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * Math.PI).toFloat(),
                    animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
                    label = "mixDriftPhase",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(mixBrush(mix.colorIndex))
                        .drawBehind { drawMixMotifDrifting(mix.motif, textMeasurer, phase) },
                )
            },
        ) {
            Text(
                text = mix.title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = HeroTextShadow),
            )
            Text(
                text = "${mix.songs.size} песен · обновляется каждый день",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
                style = TextStyle(shadow = HeroTextShadow),
            )
            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = AppIcons.Shuffle, description = "Перемешать") {
                    mix.songs.shuffled().let { onPlay(it.first(), it) }
                }
                PlayPillButton(onClick = { onPlay(mix.songs.first(), mix.songs) }, modifier = Modifier.padding(horizontal = 14.dp))
                CircleIconButton(icon = AppIcons.Add, description = "Сохранить в мои плейлисты", showCheck = savedFlash.visible) {
                    onSaveAsPlaylist(mix)
                    savedFlash.flash()
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            mix.songs.forEach { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPlay(song, mix.songs) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(uri = song.uri, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = song.title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = song.artist, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
