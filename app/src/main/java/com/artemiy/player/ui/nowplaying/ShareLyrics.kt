package com.artemiy.player.ui.nowplaying

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.em
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.artemiy.player.R
import com.artemiy.player.data.Song
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.rememberAlbumArtBitmap
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.library.ToneBackdrop
import com.artemiy.player.ui.theme.LocalAppPalette
import com.artemiy.player.ui.theme.PaletteScope
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.inAppFont
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

/** A line of lyrics as the card can use it: its text, and the translation sung/printed beside it, if any. */
internal class ShareLine(val text: String, val secondary: String?)

/** The lyrics as a plain list of lines to pick from. */
internal fun shareLines(lyrics: ParsedLyrics): List<ShareLine> = when (lyrics) {
    is ParsedLyrics.Synced -> lyrics.lines
        .filter { it.text.isNotBlank() }
        .map { line ->
            ShareLine(
                line.text.trim(),
                line.secondary.firstOrNull { it.voice == null || it.voice == line.voice }?.text?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    is ParsedLyrics.Unsynced -> lyrics.text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { ShareLine(it, null) }
}

private enum class CardStyle(val labelRes: Int) {
    TONE(R.string.card_tone), PAPER(R.string.card_paper), BLUR(R.string.card_blur), ACCENT(R.string.card_accent),
}

/** The picture's proportions, height over width. */
private enum class CardShape(val label: String, val ratio: Float) {
    PORTRAIT("4:5", 1.25f), SQUARE("1:1", 1f), STORY("9:16", 16f / 9f),
}

private const val MAX_PICKED = 6
private const val CARD_PX = 1080

/**
 * The share card, as a page of the player itself (not a separate window): the card big on top, its
 * look / proportions / translation underneath, and the lines to put on it chosen in a tall panel
 * that slides up — then save it to Pictures/Lumine or hand it to any app. The picture is the very
 * card shown here, drawn once at full size (1080 px wide) however small it appears on screen.
 */
@Composable
internal fun ShareLyricsDialog(song: Song, lyrics: ParsedLyrics, startIndex: Int, onDismiss: () -> Unit) {
    val all = remember(lyrics) { shareLines(lyrics) }
    if (all.isEmpty()) {
        onDismiss()
        return
    }
    var picked by remember { mutableStateOf(setOf(startIndex.coerceIn(0, all.lastIndex))) }
    var style by remember { mutableStateOf(CardStyle.TONE) }
    var shape by remember { mutableStateOf(CardShape.PORTRAIT) }
    var withTranslation by remember { mutableStateOf(true) }
    var panelOpen by remember { mutableStateOf(false) }
    val hasTranslation = remember(all) { all.any { it.secondary != null } }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val cornerPx = com.artemiy.player.ui.components.rememberScreenCornerRadiusPx()

    // Slides up when it opens; the close button plays that backwards, the back gesture pulls the
    // page away from the middle instead (shrinking, rounded like the screen, melting away).
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    val back = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    val close: () -> Unit = {
        scope.launch {
            appear.animateTo(0f, androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing))
            onDismiss()
        }
    }
    androidx.activity.compose.PredictiveBackHandler(enabled = !panelOpen) { progress ->
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
    // Registered after the one above, so it gets the back press first while the panel is up.
    androidx.activity.compose.BackHandler(enabled = panelOpen) { panelOpen = false }

    // In the player's own palette (dark, like the queue picker), not the app's.
    run {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val b = back.value
                    val sc = 1f - 0.12f * b
                    scaleX = sc
                    scaleY = sc
                    alpha = appear.value * (1f - ((b - 0.4f) / 0.6f).coerceIn(0f, 1f))
                    translationY = (1f - appear.value) * 90.dp.toPx()
                    val radius = cornerPx * (b / 0.2f).coerceIn(0f, 1f)
                    if (radius > 0.5f) {
                        this.shape = RoundedCornerShape(radius)
                        clip = true
                    }
                }
                .background(PlayerColors.Background)
                // The page is on top of the player: nothing touched here reaches what's under it
                // (the player's own drag-to-close included).
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                    }
                },
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        color = PlayerColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = PlayerColors.TextPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { close() }
                            .padding(10.dp)
                            .size(22.dp),
                    )
                }

                // The preview: the card at full size, shown scaled down to fit the space.
                val density = LocalDensity.current
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val cardW = CARD_PX / density.density
                    val cardH = cardW * shape.ratio
                    val scale = min(maxWidth.value / cardW, maxHeight.value / cardH).coerceAtMost(1f)
                    Box(modifier = Modifier.size((cardW * scale).dp, (cardH * scale).dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .requiredSize(cardW.dp, cardH.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin.Center
                                }
                                .drawWithContent {
                                    layer.record { this@drawWithContent.drawContent() }
                                    drawLayer(layer)
                                },
                        ) {
                            LyricsShareCard(
                                song = song,
                                lines = picked.sorted().map { all[it] },
                                style = style,
                                withTranslation = withTranslation && hasTranslation,
                                widthDp = cardW,
                                heightDp = cardH,
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    // The look: a strip you can swipe.
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CardStyle.entries.size) { i ->
                            val st = CardStyle.entries[i]
                            LookChip(st, selected = st == style, accent = PlayerColors.Accent) { style = st }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CardShape.entries.forEach { sh ->
                            Chip(sh.label, sh == shape) { shape = sh }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    if (hasTranslation) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.card_translation), color = PlayerColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = withTranslation,
                                onCheckedChange = { withTranslation = it },
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
                    // The lines on the card: one button opening the panel to choose them.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PlayerColors.SurfaceDim)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { panelOpen = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.card_lines), color = PlayerColors.TextSecondary, fontSize = 12.sp)
                            Text(
                                text = all[picked.min()].text,
                                color = PlayerColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text("${picked.size} / $MAX_PICKED  ›", color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (Build.VERSION.SDK_INT >= 29) {
                        ActionButton(stringResource(R.string.card_save), filled = false, modifier = Modifier.weight(1f)) {
                            scope.launch {
                                val bmp = runCatching { layer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                                val ok = bmp != null && saveToPictures(context, bmp, fileName(song))
                                Toast.makeText(context, if (ok) R.string.card_saved else R.string.card_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ActionButton(stringResource(R.string.card_share), filled = true, modifier = Modifier.weight(1f)) {
                        scope.launch {
                            val bmp = runCatching { layer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                            if (bmp == null || !shareImage(context, bmp, fileName(song))) {
                                Toast.makeText(context, R.string.card_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            // The panel for choosing lines: over a dimmed page, sliding up from the bottom.
            androidx.compose.animation.AnimatedVisibility(
                visible = panelOpen,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { panelOpen = false },
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = panelOpen,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it },
                exit = androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { it },
            ) {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                androidx.compose.runtime.LaunchedEffect(Unit) { listState.scrollToItem((picked.min() - 1).coerceAtLeast(0)) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.68f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(PlayerColors.Background)
                        .navigationBarsPadding(),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.share_lyrics_hint),
                            color = PlayerColors.TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.card_done),
                            color = PlayerColors.Accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { panelOpen = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        itemsIndexed(all) { index, line ->
                            val chosen = index in picked
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (chosen) PlayerColors.Accent.copy(alpha = 0.16f) else Color.Transparent)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) {
                                        picked = when {
                                            chosen && picked.size > 1 -> picked - index
                                            !chosen && picked.size < MAX_PICKED -> picked + index
                                            else -> picked
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (chosen) PlayerColors.Accent else PlayerColors.SurfaceDim),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (chosen) Icon(AppIcons.Check, null, tint = PlayerColors.OnAccent, modifier = Modifier.size(13.dp))
                                }
                                Text(
                                    text = line.text,
                                    color = PlayerColors.TextPrimary,
                                    fontSize = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A look in the strip: a coloured dot showing it, and its name. */
@Composable
private fun LookChip(style: CardStyle, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val dot = when (style) {
        CardStyle.TONE -> Brush.linearGradient(listOf(Color(0xFF8C6B6B), Color(0xFFB59A9A)))
        CardStyle.PAPER -> Brush.linearGradient(listOf(Color(0xFFF3EFE6), Color(0xFFE4DDCD)))
        CardStyle.BLUR -> Brush.linearGradient(listOf(Color(0xFF2A2A2D), Color(0xFF5A4A50)))
        CardStyle.ACCENT -> Brush.linearGradient(listOf(accent, accent))
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PlayerColors.Accent else PlayerColors.SurfaceDim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress, onClick = onClick)
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(dot).border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape))
        Text(
            text = stringResource(style.labelRes),
            color = if (selected) PlayerColors.OnAccent else PlayerColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) PlayerColors.OnAccent else PlayerColors.TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PlayerColors.Accent else PlayerColors.SurfaceDim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun ActionButton(label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (filled) PlayerColors.Accent else PlayerColors.SurfaceDim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (filled) PlayerColors.OnAccent else PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** The card itself, sized [widthDp]×[heightDp]; everything in it is proportional to its width. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LyricsShareCard(song: Song, lines: List<ShareLine>, style: CardStyle, withTranslation: Boolean, widthDp: Float, heightDp: Float) {
    // Whatever the user's text size is, the picture comes out the same.
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        val u = widthDp / 360f
        val foreground = when (style) {
            CardStyle.TONE, CardStyle.BLUR -> Color.White
            CardStyle.PAPER -> Color(0xFF211F1C)
            CardStyle.ACCENT -> PlayerColors.OnAccent
        }
        Box(modifier = Modifier.size(widthDp.dp, heightDp.dp)) {
            when (style) {
                CardStyle.TONE -> ToneBackdrop(listOf(song))
                CardStyle.PAPER -> Box(Modifier.fillMaxSize().background(Color(0xFFF3EFE6)))
                CardStyle.BLUR -> {
                    // This song's own cover, blurred — not the collage of covers the page headers use.
                    val soft = rememberSoftCover(song)
                    if (soft != null) {
                        androidx.compose.foundation.Image(
                            bitmap = soft.asImageBitmap(),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFF2A2A2D)))
                    }
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
                }
                CardStyle.ACCENT -> Box(Modifier.fillMaxSize().background(PlayerColors.Accent))
            }
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = (32 * u).dp, vertical = (36 * u).dp)) {
                Text("“", color = foreground.copy(alpha = 0.55f), fontSize = (72 * u).sp, lineHeight = (60 * u).sp, fontWeight = FontWeight.Black)
                // All the picked lines as one text that is made as big as fits the room — never so big
                // that it runs onto the cover and the title below.
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = (10 * u).dp).clipToBounds(), contentAlignment = Alignment.CenterStart) {
                    val text = androidx.compose.runtime.remember(lines, withTranslation, foreground) {
                        androidx.compose.ui.text.buildAnnotatedString {
                            lines.forEachIndexed { i, line ->
                                if (i > 0) append("\n")
                                append(line.text)
                                if (withTranslation && line.secondary != null) {
                                    append("\n")
                                    pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 0.6.em, fontWeight = FontWeight.Medium, color = foreground.copy(alpha = 0.72f)))
                                    append(line.secondary)
                                    pop()
                                }
                            }
                        }
                    }
                    BasicText(
                        text = text,
                        style = TextStyle(color = foreground, fontWeight = FontWeight.ExtraBold, lineHeight = 1.28.em, lineBreak = LineBreak.Paragraph).inAppFont(),
                        autoSize = TextAutoSize.StepBased(minFontSize = (9 * u).sp, maxFontSize = (36 * u).sp, stepSize = 0.5.sp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(
                        uri = song.uri,
                        size = ART_SIZE_THUMB,
                        modifier = Modifier.size((52 * u).dp).clip(RoundedCornerShape((10 * u).dp)),
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = (12 * u).dp)) {
                        Text(song.title, color = foreground, fontSize = (15 * u).sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle().inAppFont())
                        Text(song.artist, color = foreground.copy(alpha = 0.75f), fontSize = (13 * u).sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle().inAppFont())
                    }
                    Text("Lumine", color = foreground.copy(alpha = 0.45f), fontSize = (11 * u).sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = (12 * u).dp))
                }
            }
        }
    }
}

private fun fileName(song: Song): String =
    "Lumine - ${song.title}".replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60)

private fun saveToPictures(context: Context, bitmap: Bitmap, name: String): Boolean = runCatching {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lumine")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
    resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    if (Build.VERSION.SDK_INT >= 29) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    true
}.getOrDefault(false)

private fun shareImage(context: Context, bitmap: Bitmap, name: String): Boolean = runCatching {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, "$name.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
    true
}.getOrDefault(false)


/** This song's cover blurred, as a bitmap: the thumbnail is shrunk to a handful of pixels and smoothed a few times. */
@Composable
private fun rememberSoftCover(song: Song): Bitmap? {
    val thumb = rememberAlbumArtBitmap(song.uri, ART_SIZE_THUMB)
    return androidx.compose.runtime.remember(thumb) { thumb?.let(::softBlur) }
}

private fun softBlur(src: Bitmap): Bitmap {
    val n = 48
    val small = Bitmap.createScaledBitmap(src.copy(Bitmap.Config.ARGB_8888, false), n, n, true)
    val px = IntArray(n * n)
    small.getPixels(px, 0, n, 0, 0, n, n)
    var pixels = px
    repeat(4) { pixels = boxBlur(pixels, n, 3) }
    val out = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, n, 0, 0, n, n)
    return out
}

/** One pass of a box blur of radius [r] over an n×n picture (edges clamp). */
private fun boxBlur(src: IntArray, n: Int, r: Int): IntArray {
    val tmp = IntArray(src.size)
    val out = IntArray(src.size)
    fun pass(from: IntArray, to: IntArray, horizontal: Boolean) {
        for (y in 0 until n) for (x in 0 until n) {
            var a = 0; var red = 0; var g = 0; var b = 0
            for (k in -r..r) {
                val xx = if (horizontal) (x + k).coerceIn(0, n - 1) else x
                val yy = if (horizontal) y else (y + k).coerceIn(0, n - 1)
                val c = from[yy * n + xx]
                a += c ushr 24; red += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            val d = 2 * r + 1
            to[y * n + x] = ((a / d) shl 24) or ((red / d) shl 16) or ((g / d) shl 8) or (b / d)
        }
    }
    pass(src, tmp, true)
    pass(tmp, out, false)
    return out
}
