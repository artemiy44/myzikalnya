package com.artemiy.player.ui.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.artemiy.player.ui.theme.AccentChoice
import com.artemiy.player.ui.theme.AccentFamily
import com.artemiy.player.ui.theme.LightVariant
import com.artemiy.player.ui.theme.LocalPlayerPalette
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.ThemeMode
import com.artemiy.player.ui.theme.accentColors
import com.artemiy.player.ui.theme.DarkVariant
import com.artemiy.player.ui.theme.darkPalette
import com.artemiy.player.ui.theme.lightPalette

/** Settings pages. [parent] is where "back" goes from each one. */
private enum class SettingsRoute(val title: String, val parent: SettingsRoute?) {
    Main("Настройки", null),
    Appearance("Внешний вид", Main),
    Library("Библиотека", Main),
    Playback("Воспроизведение", Main),
    Mood("Настроение", Main),
    Player("Плеер", Main),
    NowPlayingBackground("Фон плеера", Player),
    About("О приложении", Main),
}

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
    lyricsTapPlays: Boolean,
    onLyricsTapPlaysChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    lightVariant: LightVariant,
    onLightVariantChange: (LightVariant) -> Unit,
    darkVariant: DarkVariant,
    onDarkVariantChange: (DarkVariant) -> Unit,
    accent: AccentChoice?,
    onAccentChange: (AccentChoice?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var route by remember { mutableStateOf(SettingsRoute.Main) }

    BackHandler(enabled = route != SettingsRoute.Main) {
        route = route.parent ?: SettingsRoute.Main
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
                        route.parent?.let { route = it } ?: onBack()
                    }
                    .padding(end = 12.dp),
            )
            Text(
                text = route.title,
                color = PlayerColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        when (route) {
            SettingsRoute.Main -> SettingsCategories(onOpen = { route = it })
            SettingsRoute.Appearance, SettingsRoute.Library, SettingsRoute.Playback,
            SettingsRoute.Mood, SettingsRoute.Player -> SettingsSectionContent(
                section = route,
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
                lyricsTapPlays = lyricsTapPlays,
                onLyricsTapPlaysChange = onLyricsTapPlaysChange,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                lightVariant = lightVariant,
                onLightVariantChange = onLightVariantChange,
                darkVariant = darkVariant,
                onDarkVariantChange = onDarkVariantChange,
                accent = accent,
                onAccentChange = onAccentChange,
            )
            SettingsRoute.NowPlayingBackground -> NowPlayingBackgroundContent(
                mode = nowPlayingBackgroundMode,
                onModeChange = onNowPlayingBackgroundModeChange,
                intensity = liveBlurIntensity,
                onIntensityChange = onLiveBlurIntensityChange,
            )
            SettingsRoute.About -> AboutContent()
        }
    }
}

@Composable
private fun SettingsCategories(onOpen: (SettingsRoute) -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(Icons.Filled.TextFields, "Внешний вид", "Тема, акцентный цвет, размер текста", { onOpen(SettingsRoute.Appearance) }, showChevron = true)
            CategoryDivider()
            SettingsRow(Icons.Filled.LibraryMusic, "Библиотека", "Сканирование и папки с музыкой", { onOpen(SettingsRoute.Library) }, showChevron = true)
            CategoryDivider()
            SettingsRow(Icons.Filled.Equalizer, "Воспроизведение", "Эквалайзер, «бесконечное» воспроизведение", { onOpen(SettingsRoute.Playback) }, showChevron = true)
            CategoryDivider()
            SettingsRow(Icons.Filled.EmojiEmotions, "Настроение", "Какие папки считать каким настроением", { onOpen(SettingsRoute.Mood) }, showChevron = true)
            CategoryDivider()
            SettingsRow(Icons.Filled.PlayCircle, "Плеер", "Фон плеера, текст песни", { onOpen(SettingsRoute.Player) }, showChevron = true)
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(Icons.Filled.Info, "О приложении", "Версия и лицензии сторонних компонентов", { onOpen(SettingsRoute.About) }, showChevron = true)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSettings(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    lightVariant: LightVariant,
    onLightVariantChange: (LightVariant) -> Unit,
    darkVariant: DarkVariant,
    onDarkVariantChange: (DarkVariant) -> Unit,
    accent: AccentChoice?,
    onAccentChange: (AccentChoice?) -> Unit,
) {
    SettingsCard {
        SettingsLabel("Тема")
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            InfinitePlayModeChip("Как в системе", themeMode == ThemeMode.SYSTEM) { onThemeModeChange(ThemeMode.SYSTEM) }
            InfinitePlayModeChip("Тёмная", themeMode == ThemeMode.DARK) { onThemeModeChange(ThemeMode.DARK) }
            InfinitePlayModeChip("Светлая", themeMode == ThemeMode.LIGHT) { onThemeModeChange(ThemeMode.LIGHT) }
        }

        // Each background picker only matters for the theme it belongs to ("as in system" can be
        // either, so it shows both).
        if (themeMode != ThemeMode.DARK) {
            SettingsLabel("Фон светлой темы", top = 18.dp)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {
                LightVariant.entries.forEach { variant ->
                    LabeledSwatch(variant.label, lightPalette(variant).background, variant == lightVariant) { onLightVariantChange(variant) }
                }
            }
        }
        if (themeMode != ThemeMode.LIGHT) {
            SettingsLabel("Фон тёмной темы", top = 18.dp)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {
                DarkVariant.entries.forEach { variant ->
                    LabeledSwatch(variant.label, darkPalette(variant).background, variant == darkVariant) { onDarkVariantChange(variant) }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    SettingsCard {
        SettingsLabel("Акцентный цвет")
        var family by remember { mutableStateOf(accent?.family ?: AccentFamily.STOCK) }
        FlowRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            AccentFamily.entries.forEach { f ->
                InfinitePlayModeChip(f.label, f == family) { family = f }
            }
        }
        FlowRow(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            // Monochrome: the app's own text color, no hue at all (the default).
            ColorSwatch(
                color = LocalPlayerPalette.current.textPrimary,
                selected = accent == null,
                onClick = { onAccentChange(null) },
            )
            accentColors(family).forEachIndexed { index, color ->
                ColorSwatch(
                    color = color,
                    selected = accent == AccentChoice(family, index),
                    onClick = { onAccentChange(AccentChoice(family, index)) },
                )
            }
        }
        Text(
            text = if (accent == null) "Монохром — без цветного акцента" else "Кнопки, ползунки, переключатели и выбранная вкладка",
            color = PlayerColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun LabeledSwatch(label: String, color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ColorSwatch(color = color, selected = selected, onClick = onClick)
        Text(text = label, color = PlayerColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SettingsLabel(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        color = PlayerColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = top, bottom = 10.dp),
    )
}

/** A round color sample; the selected one gets a ring in the text color around it. */
@Composable
private fun ColorSwatch(color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(width = if (selected) 2.dp else 1.dp, color = if (selected) PlayerColors.TextPrimary else PlayerColors.Border, shape = CircleShape)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    )
}

@Composable
private fun CategoryDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(PlayerColors.Border),
    )
}

@Composable
private fun SettingsSectionContent(
    section: SettingsRoute,
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
    lyricsTapPlays: Boolean,
    onLyricsTapPlaysChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    lightVariant: LightVariant,
    onLightVariantChange: (LightVariant) -> Unit,
    darkVariant: DarkVariant,
    onDarkVariantChange: (DarkVariant) -> Unit,
    accent: AccentChoice?,
    onAccentChange: (AccentChoice?) -> Unit,
) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {

            Spacer(modifier = Modifier.height(12.dp))
            if (section == SettingsRoute.Appearance) {
                ThemeSettings(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    lightVariant = lightVariant,
                    onLightVariantChange = onLightVariantChange,
                    darkVariant = darkVariant,
                    onDarkVariantChange = onDarkVariantChange,
                    accent = accent,
                    onAccentChange = onAccentChange,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (section == SettingsRoute.Appearance) SettingsCard {
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

            if (section == SettingsRoute.Library) SettingsCard {
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

            if (section == SettingsRoute.Playback) SettingsCard {
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

            if (section == SettingsRoute.Mood) SettingsCard {
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

            if (section == SettingsRoute.Player) SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.BlurOn,
                    title = "Фон плеера",
                    subtitle = "Живой блюр, статичный блюр или без блюра",
                    onClick = onOpenNowPlayingBackground,
                    showChevron = true,
                )
                SettingsSwitchRow(
                    icon = Icons.Filled.TouchApp,
                    title = "Нажатие на строку текста включает музыку",
                    subtitle = "Если песня на паузе: перемотать к строке и сразу продолжить воспроизведение",
                    checked = lyricsTapPlays,
                    onCheckedChange = onLyricsTapPlaysChange,
                    modifier = Modifier.padding(top = 16.dp),
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
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 10.dp, end = 12.dp).weight(1f)) {
            Text(text = title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = PlayerColors.TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

/** Third-party components bundled in the app, each with the license text it requires us to
 * ship (kept as plain files in assets/licenses). Placeholder layout — to be redesigned. */
private class ThirdPartyComponent(val name: String, val authors: String, val license: String, val files: List<String>)

private val THIRD_PARTY = listOf(
    ThirdPartyComponent(
        name = "ALAC decoder (Java)",
        authors = "Peter McQuillan; based on the ALAC decoder by David Hammerton",
        license = "BSD",
        files = listOf("alac-bsd.txt"),
    ),
    ThirdPartyComponent(
        name = "Kuromoji + словарь mecab-ipadic",
        authors = "Atilika Inc. and contributors; Nara Institute of Science and Technology (NAIST)",
        license = "Apache License 2.0 + уведомление NAIST/ICOT",
        files = listOf("kuromoji-notice.txt", "apache-2.0.txt"),
    ),
    ThirdPartyComponent(
        name = "Reorderable",
        authors = "Calvin Liang",
        license = "Apache License 2.0",
        files = listOf("apache-2.0.txt"),
    ),
    ThirdPartyComponent(
        name = "Android Jetpack (Compose, Media3, Room, DataStore)",
        authors = "The Android Open Source Project",
        license = "Apache License 2.0",
        files = listOf("apache-2.0.txt"),
    ),
)

@Composable
private fun AboutContent() {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    var expanded by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        SectionTitle("Приложение")
        SettingsCard {
            Text(text = "Плеер", color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "Версия $version", color = PlayerColors.TextSecondary, fontSize = 12.sp)
        }
        SectionTitle("Сторонние компоненты")
        THIRD_PARTY.forEach { component ->
            val isOpen = expanded == component.name
            Column(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlayerColors.Surface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        expanded = if (isOpen) null else component.name
                    }
                    .padding(16.dp),
            ) {
                Text(text = component.name, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = component.authors, color = PlayerColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Text(
                    text = if (isOpen) component.license else "${component.license} · нажми, чтобы показать текст",
                    color = PlayerColors.TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (isOpen) {
                    val text = remember(component.name) {
                        component.files.joinToString("\n\n") { file ->
                            runCatching { context.assets.open("licenses/$file").bufferedReader().use { it.readText() } }.getOrDefault("")
                        }
                    }
                    Text(
                        text = text,
                        color = PlayerColors.TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun InfinitePlayModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) PlayerColors.OnAccent else PlayerColors.TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PlayerColors.Accent else PlayerColors.SurfaceDim)
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
                color = if (selected) PlayerColors.OnAccent else PlayerColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) PlayerColors.Accent else PlayerColors.SurfaceDim)
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
                .background(if (selected) PlayerColors.Accent else PlayerColors.SurfaceDim),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PlayerColors.OnAccent)
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
