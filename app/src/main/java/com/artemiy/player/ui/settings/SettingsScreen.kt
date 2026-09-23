package com.artemiy.player.ui.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.InfinitePlayMode
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.Mood
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.SettingsRepository
import com.artemiy.player.ui.components.MinimalSlider
import com.artemiy.player.ui.theme.PlayerColors

private enum class SettingsRoute { Main, NowPlayingBackground }

@Composable
fun SettingsScreen(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    songCount: Int,
    onRescanLibrary: () -> Unit,
    availableFolders: List<String>,
    moodFolders: Map<Mood, Set<String>>,
    onToggleMoodFolder: (Mood, String) -> Unit,
    availableScanFolders: List<String>,
    scanFolders: Set<String>,
    onToggleScanFolder: (String) -> Unit,
    infinitePlayMode: InfinitePlayMode,
    onInfinitePlayModeChange: (InfinitePlayMode) -> Unit,
    nowPlayingBackgroundMode: NowPlayingBackgroundMode,
    onNowPlayingBackgroundModeChange: (NowPlayingBackgroundMode) -> Unit,
    liveBlurIntensity: LiveBlurIntensity,
    onLiveBlurIntensityChange: (LiveBlurIntensity) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var route by remember { mutableStateOf(SettingsRoute.Main) }

    BackHandler(enabled = route != SettingsRoute.Main) {
        route = SettingsRoute.Main
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = PlayerColors.TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (route == SettingsRoute.Main) onBack() else route = SettingsRoute.Main
                    }
                    .padding(end = 12.dp),
            )
            Text(
                text = when (route) {
                    SettingsRoute.Main -> "Настройки"
                    SettingsRoute.NowPlayingBackground -> "Фон плеера"
                },
                color = PlayerColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        when (route) {
            SettingsRoute.Main -> SettingsMainContent(
                fontScale = fontScale,
                onFontScaleChange = onFontScaleChange,
                songCount = songCount,
                onRescanLibrary = onRescanLibrary,
                availableFolders = availableFolders,
                moodFolders = moodFolders,
                onToggleMoodFolder = onToggleMoodFolder,
                availableScanFolders = availableScanFolders,
                scanFolders = scanFolders,
                onToggleScanFolder = onToggleScanFolder,
                infinitePlayMode = infinitePlayMode,
                onInfinitePlayModeChange = onInfinitePlayModeChange,
                context = context,
                onOpenNowPlayingBackground = { route = SettingsRoute.NowPlayingBackground },
            )
            SettingsRoute.NowPlayingBackground -> NowPlayingBackgroundContent(
                mode = nowPlayingBackgroundMode,
                onModeChange = onNowPlayingBackgroundModeChange,
                intensity = liveBlurIntensity,
                onIntensityChange = onLiveBlurIntensityChange,
            )
        }
    }
}

@Composable
private fun SettingsMainContent(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    songCount: Int,
    onRescanLibrary: () -> Unit,
    availableFolders: List<String>,
    moodFolders: Map<Mood, Set<String>>,
    onToggleMoodFolder: (Mood, String) -> Unit,
    availableScanFolders: List<String>,
    scanFolders: Set<String>,
    onToggleScanFolder: (String) -> Unit,
    infinitePlayMode: InfinitePlayMode,
    onInfinitePlayModeChange: (InfinitePlayMode) -> Unit,
    context: android.content.Context,
    onOpenNowPlayingBackground: () -> Unit,
) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {

            SectionTitle("Внешний вид")
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.TextFields, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Размер текста",
                        color = PlayerColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 10.dp).weight(1f),
                    )
                    Text(
                        text = "${(fontScale * 100).toInt()}%",
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
                MinimalSlider(
                    value = fontScale,
                    onValueChange = onFontScaleChange,
                    valueRange = SettingsRepository.MIN_FONT_SCALE..SettingsRepository.MAX_FONT_SCALE,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            SectionTitle("Библиотека")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Refresh,
                    title = "Пересканировать медиатеку",
                    subtitle = "Треков найдено: $songCount",
                    onClick = onRescanLibrary,
                )
                Text(
                    text = "Какие папки сканировать",
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = if (scanFolders.isEmpty()) {
                        "Ничего не выбрано — сканируется всё, что система считает музыкой " +
                            "(иногда цепляет рингтоны/уведомления). Отметь свои папки, чтобы сканировать только их."
                    } else {
                        "Сканируются только выбранные папки и всё, что лежит в них (включая подпапки)."
                    },
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                if (availableScanFolders.isEmpty()) {
                    Text(
                        text = "Папки ещё не найдены — пересканируй медиатеку.",
                        color = PlayerColors.TextTertiary,
                        fontSize = 12.sp,
                    )
                } else {
                    FolderChips(
                        availableFolders = availableScanFolders,
                        selectedFolders = scanFolders,
                        onToggleFolder = onToggleScanFolder,
                    )
                }
            }

            SectionTitle("Воспроизведение")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Equalizer,
                    title = "Эквалайзер",
                    subtitle = "Открыть системный или установленный",
                    onClick = {
                        runCatching {
                            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            }
                            context.startActivity(intent)
                        }
                    },
                )
                Text(
                    text = "«Бесконечное» воспроизведение",
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "Когда очередь подходит к концу, а в плеере включена кнопка \"∞\" — чем её подмешивать.",
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    InfinitePlayModeChip(
                        label = "Случайно",
                        selected = infinitePlayMode == InfinitePlayMode.RANDOM,
                        onClick = { onInfinitePlayModeChange(InfinitePlayMode.RANDOM) },
                    )
                    InfinitePlayModeChip(
                        label = "По жанру (радио)",
                        selected = infinitePlayMode == InfinitePlayMode.GENRE_RADIO,
                        onClick = { onInfinitePlayModeChange(InfinitePlayMode.GENRE_RADIO) },
                    )
                }
            }

            SectionTitle("Настроение")
            SettingsCard {
                Text(
                    text = "Подбор по жанру из тегов файла — не всегда точный, теги бывают неполными " +
                        "или вообще не отражают настроение. Отметь ниже, какие свои папки библиотеки " +
                        "считать тем или иным настроением — это добавится поверх жанра, а не заменит его.",
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                )
                if (availableFolders.isEmpty()) {
                    Text(
                        text = "Папки появятся здесь после сканирования медиатеки.",
                        color = PlayerColors.TextTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                } else {
                    Mood.entries.filter { it != Mood.NORMAL }.forEach { mood ->
                        MoodFolderPicker(
                            mood = mood,
                            availableFolders = availableFolders,
                            selectedFolders = moodFolders[mood] ?: emptySet(),
                            onToggleFolder = { folder -> onToggleMoodFolder(mood, folder) },
                        )
                    }
                }
            }

            SectionTitle("Плеер")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.BlurOn,
                    title = "Фон плеера",
                    subtitle = "Живой блюр, статичный блюр или без блюра",
                    onClick = onOpenNowPlayingBackground,
                    showChevron = true,
                )
            }

            SectionTitle("Скоро")
            SettingsCard {
                Text(
                    text = "Цветовая тема (светлая/тёмная), акцентный цвет, поиск по тексту песен — появятся здесь по мере готовности.",
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = PlayerColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
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
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(text = title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = PlayerColors.TextSecondary, fontSize = 12.sp)
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = PlayerColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun InfinitePlayModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) PlayerColors.AccentText else PlayerColors.TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PlayerColors.AccentOnDark else PlayerColors.SurfaceDim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun MoodFolderPicker(
    mood: Mood,
    availableFolders: List<String>,
    selectedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Text(text = mood.label, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FolderChips(
            availableFolders = availableFolders,
            selectedFolders = selectedFolders,
            onToggleFolder = onToggleFolder,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderChips(
    availableFolders: List<String>,
    selectedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        availableFolders.forEach { folder ->
            val selected = folder in selectedFolders
            Text(
                text = folder,
                color = if (selected) PlayerColors.AccentText else PlayerColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) PlayerColors.AccentOnDark else PlayerColors.SurfaceDim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onToggleFolder(folder) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingBackgroundContent(
    mode: NowPlayingBackgroundMode,
    onModeChange: (NowPlayingBackgroundMode) -> Unit,
    intensity: LiveBlurIntensity,
    onIntensityChange: (LiveBlurIntensity) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        SectionTitle("Режим фона")
        SettingsCard {
            Text(
                text = "Как выглядит фон в режиме воспроизведения — одинаково на вкладках обложки, текста и очереди.",
                color = PlayerColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            BackgroundModeOption(
                title = "Живой блюр",
                subtitle = "Размытая обложка мягко «дышит» — плавное движение и масштаб.",
                selected = mode == NowPlayingBackgroundMode.LIVE_BLUR,
                onClick = { onModeChange(NowPlayingBackgroundMode.LIVE_BLUR) },
            )
            BackgroundModeOption(
                title = "Статичный блюр",
                subtitle = "Обложка размыта, но неподвижна — как раньше.",
                selected = mode == NowPlayingBackgroundMode.STATIC_BLUR,
                onClick = { onModeChange(NowPlayingBackgroundMode.STATIC_BLUR) },
            )
            BackgroundModeOption(
                title = "Без блюра",
                subtitle = "Обычный фон приложения, без обложки.",
                selected = mode == NowPlayingBackgroundMode.NONE,
                onClick = { onModeChange(NowPlayingBackgroundMode.NONE) },
            )
        }

        if (mode == NowPlayingBackgroundMode.LIVE_BLUR) {
            SectionTitle("Насыщенность")
            SettingsCard {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    InfinitePlayModeChip(
                        label = "Приглушённый",
                        selected = intensity == LiveBlurIntensity.MUTED,
                        onClick = { onIntensityChange(LiveBlurIntensity.MUTED) },
                    )
                    InfinitePlayModeChip(
                        label = "Обычный",
                        selected = intensity == LiveBlurIntensity.NORMAL,
                        onClick = { onIntensityChange(LiveBlurIntensity.NORMAL) },
                    )
                    InfinitePlayModeChip(
                        label = "Vivid",
                        selected = intensity == LiveBlurIntensity.VIVID,
                        onClick = { onIntensityChange(LiveBlurIntensity.VIVID) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun BackgroundModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) PlayerColors.AccentOnDark else PlayerColors.SurfaceDim),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PlayerColors.AccentText)
                        .size(8.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = PlayerColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
