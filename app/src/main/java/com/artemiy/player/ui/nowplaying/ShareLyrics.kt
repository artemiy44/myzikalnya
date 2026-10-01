package com.artemiy.player.ui.nowplaying

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.artemiy.player.ui.components.BlurredCollageArt
import com.artemiy.player.ui.components.rememberBlurredCollage
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
 * The share card: pick up to six lines of the lyrics, a look for the picture, its proportions —
 * then save it to Pictures/Lumine or hand it to any app. The picture is the very card shown here,
 * drawn once at full size (1080 px wide) however small it appears on screen.
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
    val hasTranslation = remember(all) { all.any { it.secondary != null } }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val app = LocalAppPalette.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        PaletteScope(app) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlayerColors.Background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
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
                        contentDescription = null,
                        tint = PlayerColors.TextPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDismiss() }
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
                                withTranslation = withTranslation,
                                widthDp = cardW,
                                heightDp = cardH,
                            )
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)) {
                    item {
                        OptionsRows(style, { style = it }, shape, { shape = it }, withTranslation && hasTranslation, hasTranslation) { withTranslation = it }
                        Text(
                            text = stringResource(R.string.share_lyrics_hint),
                            color = PlayerColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                    }
                    itemsIndexed(all) { index, line ->
                        val chosen = index in picked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (chosen) PlayerColors.Accent.copy(alpha = 0.16f) else Color.Transparent)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) {
                                    picked = when {
                                        chosen && picked.size > 1 -> picked - index
                                        !chosen && picked.size < MAX_PICKED -> picked + index
                                        else -> picked
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (chosen) PlayerColors.Accent else PlayerColors.SurfaceDim),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (chosen) Icon(AppIcons.Check, null, tint = PlayerColors.OnAccent, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = line.text,
                                color = PlayerColors.TextPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
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
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsRows(
    style: CardStyle,
    onStyle: (CardStyle) -> Unit,
    shape: CardShape,
    onShape: (CardShape) -> Unit,
    translation: Boolean,
    translationAvailable: Boolean,
    onTranslation: (Boolean) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        CardStyle.entries.forEach { Chip(stringResource(it.labelRes), it == style) { onStyle(it) } }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        CardShape.entries.forEach { Chip(it.label, it == shape) { onShape(it) } }
    }
    if (translationAvailable) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.card_translation), color = PlayerColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = translation,
                onCheckedChange = onTranslation,
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
                    BlurredCollageArt(rememberBlurredCollage(listOf(song)))
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
                }
                CardStyle.ACCENT -> Box(Modifier.fillMaxSize().background(PlayerColors.Accent))
            }
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = (32 * u).dp, vertical = (36 * u).dp)) {
                Text("“", color = foreground.copy(alpha = 0.55f), fontSize = (72 * u).sp, lineHeight = (60 * u).sp, fontWeight = FontWeight.Black)
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    val chars = lines.sumOf { it.text.length + (if (withTranslation) it.secondary?.length ?: 0 else 0) }
                    val size = 30f * when {
                        chars < 50 -> 1.1f
                        chars < 100 -> 0.95f
                        chars < 170 -> 0.8f
                        chars < 260 -> 0.66f
                        else -> 0.55f
                    }
                    Column(verticalArrangement = Arrangement.spacedBy((10 * u).dp)) {
                        lines.forEach { line ->
                            Column {
                                Text(
                                    text = line.text,
                                    color = foreground,
                                    fontSize = (size * u).sp,
                                    lineHeight = (size * 1.25f * u).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = TextStyle(lineBreak = LineBreak.Paragraph).inAppFont(),
                                )
                                if (withTranslation && line.secondary != null) {
                                    Text(
                                        text = line.secondary,
                                        color = foreground.copy(alpha = 0.7f),
                                        fontSize = (size * 0.58f * u).sp,
                                        lineHeight = (size * 0.75f * u).sp,
                                        style = TextStyle(lineBreak = LineBreak.Paragraph).inAppFont(),
                                    )
                                }
                            }
                        }
                    }
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
