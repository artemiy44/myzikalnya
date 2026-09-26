package com.artemiy.player.ui.home

import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Mood
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.ui.components.songLongPressTrigger
import com.artemiy.player.ui.components.StatusBarFade
import com.artemiy.player.ui.theme.PlayerColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    quickPicks: List<Song>,
    keepListening: List<Song>,
    recentlyAdded: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onMoodClick: (Mood) -> Unit,
    onSettingsClick: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    // The header scrolls away with the page instead of being pinned under the status bar, and the
    // page runs edge-to-edge behind the status bar — only a soft fade keeps its icons readable.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 20.dp, 20.dp, 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Главная",
                    color = PlayerColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PlayerColors.Surface)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSettingsClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Настройки",
                        tint = PlayerColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            val menuActions = SongMenuActions(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
            MoodSection(onMoodClick = onMoodClick)
            SongGridSection(
                title = "Quick picks",
                songs = quickPicks,
                emptyHint = "Здесь появятся часто прослушиваемые треки",
                onSongClick = { song -> onSongClick(song, quickPicks) },
                menuActions = menuActions,
            )
            SongRowSection(
                title = "Keep listening",
                songs = keepListening,
                emptyHint = "Здесь появятся недавно прослушанные треки",
                onSongClick = { song -> onSongClick(song, keepListening) },
                menuActions = menuActions,
            )
            SongRowSection(
                title = "Recently added",
                songs = recentlyAdded,
                small = true,
                emptyHint = null,
                onSongClick = { song -> onSongClick(song, recentlyAdded) },
                menuActions = menuActions,
            )
        }
        StatusBarFade()
    }
}


/** Bundles the five "⋮" menu callbacks so they thread through Home's several song sections as
 * one param instead of five. */
private data class SongMenuActions(
    val onPlayNext: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onAddToPlaylist: (Song) -> Unit,
    val onGoToAlbum: (Song) -> Unit,
    val onGoToArtist: (Song) -> Unit,
)

@Composable
private fun SongGridSection(
    title: String,
    songs: List<Song>,
    emptyHint: String?,
    onSongClick: (Song) -> Unit,
    menuActions: SongMenuActions,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = title,
            color = PlayerColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        if (songs.isEmpty() && emptyHint != null) {
            Text(text = emptyHint, color = PlayerColors.TextSecondary, fontSize = 13.sp)
            return
        }
        val rows = (songs.size + 1) / 2
        val rowHeight = 62.dp
        val rowGap = 8.dp
        val gridHeight = rowHeight * rows + rowGap * (rows - 1).coerceAtLeast(0)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(rowGap),
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
        ) {
            items(songs) { song ->
                var menuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlayerColors.Surface)
                        .songLongPressTrigger(
                            onClick = { onSongClick(song) },
                            onLongPress = { menuExpanded = true },
                        )
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        uri = song.uri,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            text = song.title,
                            color = PlayerColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            color = PlayerColors.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    SongActionsMenuPopup(
                        song = song,
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlayNext = menuActions.onPlayNext,
                        onAddToQueue = menuActions.onAddToQueue,
                        onAddToPlaylist = menuActions.onAddToPlaylist,
                        onGoToAlbum = menuActions.onGoToAlbum,
                        onGoToArtist = menuActions.onGoToArtist,
                    )
                }
            }
        }
    }
}

@Composable
private fun SongRowSection(
    title: String,
    songs: List<Song>,
    emptyHint: String?,
    onSongClick: (Song) -> Unit,
    menuActions: SongMenuActions,
    small: Boolean = false,
) {
    val artSize = if (small) 88.dp else 120.dp
    Column {
        Text(
            text = title,
            color = PlayerColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
        )
        if (songs.isEmpty() && emptyHint != null) {
            Text(
                text = emptyHint,
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            return
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(if (small) 10.dp else 12.dp),
        ) {
            songs.forEach { song ->
                var menuExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .width(artSize)
                        .songLongPressTrigger(
                            onClick = { onSongClick(song) },
                            onLongPress = { menuExpanded = true },
                        ),
                ) {
                    Box {
                        AlbumArt(
                            uri = song.uri,
                            modifier = Modifier
                                .size(artSize)
                                .clip(RoundedCornerShape(if (small) 10.dp else 12.dp)),
                        )
                        SongActionsMenuPopup(
                            song = song,
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            onPlayNext = menuActions.onPlayNext,
                            onAddToQueue = menuActions.onAddToQueue,
                            onAddToPlaylist = menuActions.onAddToPlaylist,
                            onGoToAlbum = menuActions.onGoToAlbum,
                            onGoToArtist = menuActions.onGoToArtist,
                        )
                    }
                    Text(
                        text = song.title,
                        color = PlayerColors.TextPrimary,
                        fontSize = if (small) 12.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    if (!small) {
                        Text(
                            text = song.artist,
                            color = PlayerColors.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}


private val MOOD_COLORS = mapOf(
    Mood.NORMAL to Color(0xFF8E8E93),
    Mood.HAPPY to Color(0xFFFFB020),
    Mood.LOUD to Color(0xFFFF4D6D),
    Mood.SAD to Color(0xFF4B6CB7),
    Mood.CRY to Color(0xFF5B3FAE),
)

// Display order matches the approved mockup, not the enum's declaration order.
private val MOOD_DISPLAY_ORDER = listOf(Mood.HAPPY, Mood.LOUD, Mood.NORMAL, Mood.SAD, Mood.CRY)

@Composable
private fun MoodSection(onMoodClick: (Mood) -> Unit) {
    Column {
        Text(
            text = "Настроение",
            color = PlayerColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            text = "Подберём музыку по жанру и настрою",
            color = PlayerColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MOOD_DISPLAY_ORDER.forEach { mood ->
                MoodCard(mood = mood, onClick = { onMoodClick(mood) })
            }
        }
    }
}

@Composable
private fun MoodCard(mood: Mood, onClick: () -> Unit) {
    val color = MOOD_COLORS.getValue(mood)
    val isDefault = mood == Mood.NORMAL
    Box(
        modifier = Modifier
            .width(326.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(PlayerColors.Surface)
            .then(
                if (isDefault) {
                    Modifier.border(1.5.dp, PlayerColors.TextTertiary, RoundedCornerShape(22.dp))
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        // A radial gradient fades to transparent by construction — no hard edge to clip,
        // unlike a blurred solid circle (which kept showing up as a visible disc regardless of
        // blur radius/edge treatment: the blur wasn't dissipating the way it does on plain text).
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-80).dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(22.dp),
        ) {
            MoodIcon(mood = mood, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = mood.label,
                color = PlayerColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = mood.subtitle, color = PlayerColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

/** Small hand-drawn glyphs per mood — kept simple on purpose, matches the rest of this pass
 * (mechanics first, visual polish is a separate backlog item). */
@Composable
private fun MoodIcon(mood: Mood, modifier: Modifier = Modifier) {
    val glyph = PlayerColors.TextPrimary
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.09f
        when (mood) {
            Mood.NORMAL -> {
                val heights = listOf(0.35f, 0.7f, 1f, 0.55f, 0.85f)
                val barWidth = size.width / (heights.size * 1.8f)
                val gap = barWidth * 0.8f
                var x = barWidth / 2
                heights.forEach { hFrac ->
                    drawLine(
                        color = glyph,
                        start = Offset(x, size.height),
                        end = Offset(x, size.height * (1f - hFrac)),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    x += barWidth + gap
                }
            }

            Mood.HAPPY -> {
                val center = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension * 0.22f
                drawCircle(color = glyph, radius = r, center = center, style = Stroke(width = strokeWidth))
                val rayLen = size.minDimension * 0.2f
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble()).toFloat()
                    val innerR = r + strokeWidth * 1.6f
                    val outerR = innerR + rayLen
                    drawLine(
                        color = glyph,
                        start = Offset(center.x + innerR * cos(angle), center.y + innerR * sin(angle)),
                        end = Offset(center.x + outerR * cos(angle), center.y + outerR * sin(angle)),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            Mood.LOUD -> {
                val w = size.width
                val h = size.height
                val bolt = Path().apply {
                    moveTo(w * 0.60f, 0f)
                    lineTo(w * 0.16f, h * 0.58f)
                    lineTo(w * 0.46f, h * 0.58f)
                    lineTo(w * 0.38f, h)
                    lineTo(w * 0.88f, h * 0.38f)
                    lineTo(w * 0.56f, h * 0.38f)
                    close()
                }
                drawPath(bolt, color = glyph)
            }

            Mood.SAD -> {
                val r = size.minDimension * 0.34f
                val center = Offset(size.width * 0.52f, size.height * 0.5f)
                val moon = Path().apply {
                    addOval(Rect(Offset(center.x - r, center.y - r), Size(r * 2, r * 2)))
                    val cutCenter = Offset(center.x + r * 0.62f, center.y - r * 0.3f)
                    val cutR = r * 0.92f
                    addOval(Rect(Offset(cutCenter.x - cutR, cutCenter.y - cutR), Size(cutR * 2, cutR * 2)))
                    fillType = PathFillType.EvenOdd
                }
                drawPath(moon, color = glyph)
            }

            Mood.CRY -> {
                val w = size.width
                val h = size.height
                val drop = Path().apply {
                    moveTo(w * 0.5f, h * 0.04f)
                    cubicTo(w * 0.5f, h * 0.04f, w * 0.14f, h * 0.56f, w * 0.14f, h * 0.74f)
                    cubicTo(w * 0.14f, h * 0.95f, w * 0.31f, h, w * 0.5f, h)
                    cubicTo(w * 0.69f, h, w * 0.86f, h * 0.95f, w * 0.86f, h * 0.74f)
                    cubicTo(w * 0.86f, h * 0.56f, w * 0.5f, h * 0.04f, w * 0.5f, h * 0.04f)
                    close()
                }
                drawPath(drop, color = glyph)
            }
        }
    }
}
