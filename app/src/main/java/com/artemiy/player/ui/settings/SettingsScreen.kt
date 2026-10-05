package com.artemiy.player.ui.settings

import com.artemiy.player.ui.theme.barsInset
import kotlin.math.roundToInt
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.data.label
import com.artemiy.player.ui.components.label
import com.artemiy.player.ui.theme.label
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.inAppFont
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.BackHandler
import com.artemiy.player.ui.components.AnimatedBackStack
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
import com.artemiy.player.ui.components.pressScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.InfinitePlayMode
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.Mood
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.PlayerStyle
import com.artemiy.player.data.SettingsRepository
import com.artemiy.player.ui.components.MinimalSlider
import com.artemiy.player.ui.components.AppTab
import com.artemiy.player.ui.icons.IconSet
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

/** Settings that came later, bundled so they travel through the pages as one. */
class ExtraSettings(
    val fadeMode: com.artemiy.player.data.FadeMode = com.artemiy.player.data.FadeMode.OFF,
    val onFadeModeChange: (com.artemiy.player.data.FadeMode) -> Unit = {},
    val qualityBadge: Boolean = false,
    val onQualityBadgeChange: (Boolean) -> Unit = {},
)

/** Settings pages. [parent] is where "back" goes from each one. */
private enum class SettingsRoute(val titleRes: Int, val parent: SettingsRoute?) {
    Main(R.string.settings, null),
    General(R.string.set_general, Main),
    Appearance(R.string.set_appearance, Main),
    Colors(R.string.set_colors, Appearance),
    Library(R.string.set_library, Main),
    Playback(R.string.set_playback, Main),
    Mood(R.string.tab_mood, Main),
    Player(R.string.onb_player, Main),
    NowPlayingBackground(R.string.set_player_background, Player),
    KeptArtists(R.string.set_kept_artists, General),
    LibraryTabs(R.string.set_library_tabs, General),
    Backup(R.string.set_backup, Main),
    Language(R.string.set_language, Main),
    About(R.string.set_about, Main),
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
    minDurationSec: Int,
    onMinDurationChange: (Int) -> Unit,
    notificationRepeatButton: Boolean,
    onNotificationRepeatButtonChange: (Boolean) -> Unit,
    infinitePlayMode: InfinitePlayMode,
    onInfinitePlayModeChange: (InfinitePlayMode) -> Unit,
    nowPlayingBackgroundMode: NowPlayingBackgroundMode,
    onNowPlayingBackgroundModeChange: (NowPlayingBackgroundMode) -> Unit,
    liveBlurIntensity: LiveBlurIntensity,
    onLiveBlurIntensityChange: (LiveBlurIntensity) -> Unit,
    lyricsTapPlays: Boolean,
    lrcGapDots: Boolean = true,
    onLrcGapDotsChange: (Boolean) -> Unit = {},
    onLyricsTapPlaysChange: (Boolean) -> Unit,
    playerStyle: PlayerStyle,
    onPlayerStyleChange: (PlayerStyle) -> Unit,
    heroStyle: com.artemiy.player.data.HeroStyle = com.artemiy.player.data.HeroStyle.VIVID,
    onHeroStyleChange: (com.artemiy.player.data.HeroStyle) -> Unit = {},
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    lightVariant: LightVariant,
    onLightVariantChange: (LightVariant) -> Unit,
    darkVariant: DarkVariant,
    onDarkVariantChange: (DarkVariant) -> Unit,
    accent: AccentChoice?,
    onAccentChange: (AccentChoice?) -> Unit,
    startTab: AppTab,
    onStartTabChange: (AppTab) -> Unit,
    iconSet: IconSet,
    onIconSetChange: (IconSet) -> Unit,
    appFont: com.artemiy.player.ui.theme.AppFont,
    onAppFontChange: (com.artemiy.player.ui.theme.AppFont) -> Unit,
    uiStyle: com.artemiy.player.ui.theme.UiStyle = com.artemiy.player.ui.theme.UiStyle.CLASSIC,
    onUiStyleChange: (com.artemiy.player.ui.theme.UiStyle) -> Unit = {},
    libraryTabs: List<com.artemiy.player.ui.library.TabSetting> = com.artemiy.player.ui.library.LibraryTab.DEFAULT,
    songs: List<com.artemiy.player.data.Song> = emptyList(),
    onDataRestored: () -> Unit = {},
    extras: ExtraSettings = ExtraSettings(),
    onLibraryTabsChange: (List<com.artemiy.player.ui.library.TabSetting>) -> Unit = {},
    onBack: () -> Unit,
    onShowOnboarding: () -> Unit = {},
    keptArtists: Set<String> = emptySet(),
    onAddKeptArtist: (String) -> Unit = {},
    onRemoveKeptArtist: (String) -> Unit = {},
    /** Artist lines from the library that currently count as several artists. */
    splitArtistLines: List<String> = emptyList(),
) {
    val context = LocalContext.current
    var route by remember { mutableStateOf(SettingsRoute.Main) }

    // The page and all its parents, top-most last — what the back gesture walks through.
    val stack = generateSequence(route) { it.parent }.toList().reversed()

    val reveal = rememberThemeReveal()
    androidx.compose.runtime.CompositionLocalProvider(LocalThemeReveal provides reveal) {
    Box(modifier = Modifier.fillMaxSize()) {
    AnimatedBackStack(stack = stack, onBack = { route = route.parent ?: SettingsRoute.Main }) { page ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.Background)
                .statusBarsPadding(),
        ) {
            val goBack = { page.parent?.let { route = it } ?: onBack(); Unit }
            // The big title folds into a slim bar as the page scrolls (see CollapsingHeader).
            com.artemiy.player.ui.components.CollapsingHeader(
                title = stringResource(page.titleRes),
                onBack = goBack,
                bigHeader = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = PlayerColors.TextPrimary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            page.parent?.let { route = it } ?: onBack()
                        },
                )
                Text(
                    text = stringResource(page.titleRes),
                    color = PlayerColors.TextPrimary,
                    style = com.artemiy.player.ui.theme.pageTitleStyle,
                )
            }
                },
            ) {

            when (page) {
                SettingsRoute.Main -> SettingsCategories(onOpen = { route = it })
                SettingsRoute.General -> GeneralContent(
                    startTab = startTab,
                    onStartTabChange = onStartTabChange,
                    onOpenKeptArtists = { route = SettingsRoute.KeptArtists },
                    onOpenLibraryTabs = { route = SettingsRoute.LibraryTabs },
                )
                SettingsRoute.Colors -> ColorsContent(
                    themeMode = themeMode,
                    lightVariant = lightVariant,
                    onLightVariantChange = onLightVariantChange,
                    darkVariant = darkVariant,
                    onDarkVariantChange = onDarkVariantChange,
                    accent = accent,
                    onAccentChange = onAccentChange,
                )
                SettingsRoute.Backup -> BackupContent(songs = songs, onChanged = onDataRestored)
                SettingsRoute.LibraryTabs -> LibraryTabsContent(tabs = libraryTabs, onChange = onLibraryTabsChange)
                SettingsRoute.KeptArtists -> KeptArtistsContent(
                    kept = keptArtists,
                    splitLines = splitArtistLines,
                    onAdd = onAddKeptArtist,
                    onRemove = onRemoveKeptArtist,
                )
                SettingsRoute.Appearance, SettingsRoute.Library, SettingsRoute.Playback,
                SettingsRoute.Mood, SettingsRoute.Player -> SettingsSectionContent(
                    section = page,
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
                    minDurationSec = minDurationSec,
                    onMinDurationChange = onMinDurationChange,
                    notificationRepeatButton = notificationRepeatButton,
                    onNotificationRepeatButtonChange = onNotificationRepeatButtonChange,
                    infinitePlayMode = infinitePlayMode,
                    onInfinitePlayModeChange = onInfinitePlayModeChange,
                    extras = extras,
                    context = context,
                    onOpenColors = { route = SettingsRoute.Colors },
                    onOpenNowPlayingBackground = { route = SettingsRoute.NowPlayingBackground },
                    lyricsTapPlays = lyricsTapPlays,
                    lrcGapDots = lrcGapDots,
                    onLrcGapDotsChange = onLrcGapDotsChange,
                    onLyricsTapPlaysChange = onLyricsTapPlaysChange,
                    playerStyle = playerStyle,
                    onPlayerStyleChange = onPlayerStyleChange,
                    heroStyle = heroStyle,
                    onHeroStyleChange = onHeroStyleChange,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    lightVariant = lightVariant,
                    onLightVariantChange = onLightVariantChange,
                    darkVariant = darkVariant,
                    onDarkVariantChange = onDarkVariantChange,
                    accent = accent,
                    onAccentChange = onAccentChange,
                    iconSet = iconSet,
                    onIconSetChange = onIconSetChange,
                    appFont = appFont,
                    onAppFontChange = onAppFontChange,
                    uiStyle = uiStyle,
                    onUiStyleChange = onUiStyleChange,
                )
                SettingsRoute.NowPlayingBackground -> NowPlayingBackgroundContent(
                    mode = nowPlayingBackgroundMode,
                    onModeChange = onNowPlayingBackgroundModeChange,
                    intensity = liveBlurIntensity,
                    onIntensityChange = onLiveBlurIntensityChange,
                )
                SettingsRoute.About -> AboutContent(onShowOnboarding = onShowOnboarding)
                SettingsRoute.Language -> LanguageContent()
            }
                    }
}
    }
    ThemeRevealOverlay(reveal)
    }
    }
}

@Composable
private fun SettingsCategories(onOpen: (SettingsRoute) -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(AppIcons.General, stringResource(R.string.set_general), stringResource(R.string.set_general_sub), { onOpen(SettingsRoute.General) }, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.TextSize, stringResource(R.string.set_appearance), stringResource(R.string.set_appearance_sub), { onOpen(SettingsRoute.Appearance) }, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.Library, stringResource(R.string.set_library), stringResource(R.string.set_library_sub), { onOpen(SettingsRoute.Library) }, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.Equalizer, stringResource(R.string.set_playback), stringResource(R.string.set_playback_sub), { onOpen(SettingsRoute.Playback) }, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.MoodSettings, stringResource(R.string.tab_mood), stringResource(R.string.set_mood_sub), { onOpen(SettingsRoute.Mood) }, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.Player, stringResource(R.string.onb_player), stringResource(R.string.set_player_sub), { onOpen(SettingsRoute.Player) }, showChevron = true)
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(AppIcons.Download, stringResource(R.string.set_backup), stringResource(R.string.set_backup_sub), { onOpen(SettingsRoute.Backup) }, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.Language, stringResource(R.string.set_language), stringResource(R.string.set_language_sub), { onOpen(SettingsRoute.Language) }, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.Info, stringResource(R.string.set_about), stringResource(R.string.set_about_sub), { onOpen(SettingsRoute.About) }, showChevron = true)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsContent(
    themeMode: ThemeMode,
    lightVariant: LightVariant,
    onLightVariantChange: (LightVariant) -> Unit,
    darkVariant: DarkVariant,
    onDarkVariantChange: (DarkVariant) -> Unit,
    accent: AccentChoice?,
    onAccentChange: (AccentChoice?) -> Unit,
) {
  Column(
    modifier = Modifier
        .verticalScroll(rememberScrollState()).barsInset()
        .padding(horizontal = 20.dp)
        .navigationBarsPadding(),
  ) {
    Spacer(modifier = Modifier.height(12.dp))
    SettingsCard {
        // Each background picker only matters for the theme it belongs to ("as in system" can be
        // either, so it shows both).
        if (themeMode != ThemeMode.DARK) {
            SettingsLabel(stringResource(R.string.set_light_bg), top = 18.dp)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {
                LightVariant.entries.forEach { variant ->
                    LabeledSwatch(variant.label, lightPalette(variant).background, variant == lightVariant) { onLightVariantChange(variant) }
                }
            }
        }
        if (themeMode != ThemeMode.LIGHT) {
            SettingsLabel(stringResource(R.string.set_dark_bg), top = 18.dp)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {
                DarkVariant.entries.forEach { variant ->
                    LabeledSwatch(variant.label, darkPalette(variant).background, variant == darkVariant) { onDarkVariantChange(variant) }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    SettingsCard {
        SettingsLabel(stringResource(R.string.set_accent))
        var family by remember { mutableStateOf(accent?.family ?: AccentFamily.STOCK) }
        FlowRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            AccentFamily.entries.filter { it != AccentFamily.WALLPAPER || com.artemiy.player.ui.theme.WallpaperAccents.available }.forEach { f ->
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
            text = stringResource(if (accent == null) R.string.set_accent_mono else R.string.set_accent_desc),
            color = PlayerColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
  }
}

@Composable
private fun LabeledSwatch(label: String, color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ColorSwatch(color = color, selected = selected, onClick = onClick, reveal = true)
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
private fun ColorSwatch(color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit, reveal: Boolean = false) {
    val theme = LocalThemeReveal.current
    val press = remember { MutableInteractionSource() }
    var center by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Box(
        modifier = Modifier
            .pressScale(press, 0.88f)
            .size(40.dp)
            .onGloballyPositioned { center = it.positionInWindow() + androidx.compose.ui.geometry.Offset(it.size.width / 2f, it.size.height / 2f) }
            .clip(CircleShape)
            .border(width = if (selected) 2.dp else 1.dp, color = if (selected) PlayerColors.TextPrimary else PlayerColors.Border, shape = CircleShape)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(interactionSource = press, indication = null) {
                if (reveal && !selected && theme != null) theme.play(center, color, onClick) else onClick()
            },
    )
}

@Composable
private fun GeneralContent(startTab: AppTab, onStartTabChange: (AppTab) -> Unit, onOpenKeptArtists: () -> Unit, onOpenLibraryTabs: () -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsLabel(stringResource(R.string.set_start_tab))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                listOf(AppTab.Home, AppTab.Library, AppTab.Mood).forEach { tab ->
                    InfinitePlayModeChip(tab.label, tab == startTab) { onStartTabChange(tab) }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(
                icon = AppIcons.Library,
                title = stringResource(R.string.set_library_tabs),
                subtitle = stringResource(R.string.set_library_tabs_sub),
                onClick = onOpenLibraryTabs,
                showChevron = true,
            )
            CategoryDivider()
            SettingsRow(
                icon = AppIcons.Artist,
                title = stringResource(R.string.set_kept_title),
                subtitle = stringResource(R.string.set_kept_sub),
                onClick = onOpenKeptArtists,
                showChevron = true,
            )
        }
    }
}

/**
 * The user's own "keep these whole" artist names. The app already splits "A & B, C" into three
 * artists and knows plenty of bands that only look like that ("Earth, Wind & Fire", "AC/DC");
 * this is for the ones it doesn't know. Lines from the library that are being split right now are
 * offered below, one tap to keep them whole.
 */
@Composable
private fun KeptArtistsContent(
    kept: Set<String>,
    splitLines: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.set_kept_explain),
            color = PlayerColors.TextSecondary,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlayerColors.SurfaceDim)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    com.artemiy.player.ui.components.HintTextField(
                        value = input,
                        onValueChange = { input = it },
                        hint = stringResource(R.string.set_artist_name),
                        fontSize = 14.sp,
                        cursorColor = PlayerColors.Accent,
                    )
                }
                Text(
                    text = stringResource(R.string.add),
                    color = if (input.isBlank()) PlayerColors.TextTertiary else PlayerColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (input.isNotBlank()) {
                                onAdd(input)
                                input = ""
                            }
                        },
                )
            }
            kept.sorted().forEach { name ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = name, color = PlayerColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.remove),
                        tint = PlayerColors.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRemove(name) },
                    )
                }
            }
        }
        val suggestions = splitLines.filter { line -> kept.none { it.equals(line, ignoreCase = true) } }
        if (suggestions.isNotEmpty()) {
            SectionTitle(stringResource(R.string.set_split_now))
            SettingsCard {
                suggestions.forEachIndexed { index, line ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = line, color = PlayerColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = AppIcons.Add,
                            contentDescription = stringResource(R.string.set_dont_split),
                            tint = PlayerColors.TextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAdd(line) },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
internal fun CategoryDivider() {
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
    minDurationSec: Int,
    onMinDurationChange: (Int) -> Unit,
    notificationRepeatButton: Boolean,
    onNotificationRepeatButtonChange: (Boolean) -> Unit,
    infinitePlayMode: InfinitePlayMode,
    onInfinitePlayModeChange: (InfinitePlayMode) -> Unit,
    extras: ExtraSettings,
    context: android.content.Context,
    onOpenColors: () -> Unit,
    onOpenNowPlayingBackground: () -> Unit,
    lyricsTapPlays: Boolean,
    lrcGapDots: Boolean = true,
    onLrcGapDotsChange: (Boolean) -> Unit = {},
    onLyricsTapPlaysChange: (Boolean) -> Unit,
    playerStyle: PlayerStyle,
    onPlayerStyleChange: (PlayerStyle) -> Unit,
    heroStyle: com.artemiy.player.data.HeroStyle = com.artemiy.player.data.HeroStyle.VIVID,
    onHeroStyleChange: (com.artemiy.player.data.HeroStyle) -> Unit = {},
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    lightVariant: LightVariant,
    onLightVariantChange: (LightVariant) -> Unit,
    darkVariant: DarkVariant,
    onDarkVariantChange: (DarkVariant) -> Unit,
    accent: AccentChoice?,
    onAccentChange: (AccentChoice?) -> Unit,
    iconSet: IconSet,
    onIconSetChange: (IconSet) -> Unit,
    appFont: com.artemiy.player.ui.theme.AppFont,
    onAppFontChange: (com.artemiy.player.ui.theme.AppFont) -> Unit,
    uiStyle: com.artemiy.player.ui.theme.UiStyle = com.artemiy.player.ui.theme.UiStyle.CLASSIC,
    onUiStyleChange: (com.artemiy.player.ui.theme.UiStyle) -> Unit = {},
) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()).barsInset()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {

            Spacer(modifier = Modifier.height(12.dp))
            if (section == SettingsRoute.Appearance) {
                SettingsCard {
                    SettingsLabel(stringResource(R.string.onb_theme))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        InfinitePlayModeChip(stringResource(R.string.theme_system), themeMode == ThemeMode.SYSTEM) { onThemeModeChange(ThemeMode.SYSTEM) }
                        InfinitePlayModeChip(stringResource(R.string.theme_dark), themeMode == ThemeMode.DARK) { onThemeModeChange(ThemeMode.DARK) }
                        InfinitePlayModeChip(stringResource(R.string.theme_light), themeMode == ThemeMode.LIGHT) { onThemeModeChange(ThemeMode.LIGHT) }
                    }
                    CategoryDivider()
                    SettingsRow(
                        icon = AppIcons.Palette,
                        title = stringResource(R.string.set_colors),
                        subtitle = stringResource(R.string.set_colors_sub),
                        onClick = onOpenColors,
                        showChevron = true,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    SettingsLabel(stringResource(R.string.set_ui_style))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        com.artemiy.player.ui.theme.UiStyle.entries.forEach { style ->
                            InfinitePlayModeChip(
                                stringResource(if (style == com.artemiy.player.ui.theme.UiStyle.CLASSIC) R.string.ui_style_classic else R.string.ui_style_expressive),
                                style == uiStyle,
                            ) { onUiStyleChange(style) }
                        }
                    }
                    Text(
                        text = stringResource(if (uiStyle == com.artemiy.player.ui.theme.UiStyle.CLASSIC) R.string.ui_style_classic_note else R.string.ui_style_expressive_note),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    SettingsLabel(stringResource(R.string.set_icons))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        IconSet.entries.forEach { set ->
                            InfinitePlayModeChip(set.label, set == iconSet) { onIconSetChange(set) }
                        }
                    }
                    Text(
                        text = stringResource(if (iconSet == IconSet.LUCIDE) R.string.set_icons_lucide else R.string.set_icons_tabler),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    SettingsLabel(stringResource(R.string.set_font))
                    FontChoices(selected = appFont, onSelect = onAppFontChange)
                    Text(
                        text = stringResource(R.string.font_note),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (section == SettingsRoute.Appearance) SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = AppIcons.TextSize, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.set_text_size),
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

            if (section == SettingsRoute.Appearance) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    SettingsLabel(stringResource(R.string.set_hero_style))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        com.artemiy.player.data.HeroStyle.entries.forEach { style ->
                            InfinitePlayModeChip(
                                stringResource(if (style == com.artemiy.player.data.HeroStyle.CLASSIC) R.string.hero_classic else R.string.hero_vivid),
                                style == heroStyle,
                            ) { onHeroStyleChange(style) }
                        }
                    }
                    Text(
                        text = stringResource(if (heroStyle == com.artemiy.player.data.HeroStyle.CLASSIC) R.string.set_hero_classic_desc else R.string.set_hero_vivid_desc),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            if (section == SettingsRoute.Library) SettingsCard {
                SettingsRow(
                    icon = AppIcons.Refresh,
                    title = stringResource(R.string.set_rescan),
                    subtitle = stringResource(R.string.set_tracks_found, songCount),
                    onClick = onRescanLibrary,
                )
                MinDurationSetting(minDurationSec, onMinDurationChange)
                Text(
                    text = stringResource(R.string.set_scan_folders),
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = if (scanFolders.isEmpty()) {
                        stringResource(R.string.set_scan_all)
                    } else {
                        stringResource(R.string.set_scan_picked)
                    },
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                if (availableScanFolders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.set_no_folders_yet),
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
                    icon = AppIcons.Equalizer,
                    title = stringResource(R.string.set_equalizer),
                    subtitle = stringResource(R.string.set_equalizer_sub),
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
                    text = stringResource(R.string.set_endless),
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = stringResource(R.string.set_endless_desc),
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    InfinitePlayModeChip(
                        label = stringResource(R.string.set_endless_random),
                        selected = infinitePlayMode == InfinitePlayMode.RANDOM,
                        onClick = { onInfinitePlayModeChange(InfinitePlayMode.RANDOM) },
                    )
                    InfinitePlayModeChip(
                        label = stringResource(R.string.set_endless_genre),
                        selected = infinitePlayMode == InfinitePlayMode.GENRE_RADIO,
                        onClick = { onInfinitePlayModeChange(InfinitePlayMode.GENRE_RADIO) },
                    )
                }
            }

            if (section == SettingsRoute.Playback) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    SettingsLabel(stringResource(R.string.set_fade))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        com.artemiy.player.data.FadeMode.entries.forEach { mode ->
                            InfinitePlayModeChip(
                                stringResource(when (mode) {
                                    com.artemiy.player.data.FadeMode.OFF -> R.string.fade_off
                                    com.artemiy.player.data.FadeMode.SHORT -> R.string.fade_short
                                    com.artemiy.player.data.FadeMode.MEDIUM -> R.string.fade_medium
                                    com.artemiy.player.data.FadeMode.LONG -> R.string.fade_long
                                }),
                                mode == extras.fadeMode,
                            ) { extras.onFadeModeChange(mode) }
                        }
                    }
                    Text(
                        text = stringResource(R.string.set_fade_desc),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCard {
                    SettingsLabel(stringResource(R.string.set_notif_button))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        InfinitePlayModeChip(stringResource(R.string.endless_play), !notificationRepeatButton) { onNotificationRepeatButtonChange(false) }
                        InfinitePlayModeChip(stringResource(R.string.repeat), notificationRepeatButton) { onNotificationRepeatButtonChange(true) }
                    }
                    Text(
                        text = stringResource(R.string.set_notif_button_desc),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            if (section == SettingsRoute.Mood) SettingsCard {
                Text(
                    text = stringResource(R.string.set_mood_explain),
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                )
                if (availableFolders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.set_folders_after_scan),
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

            if (section == SettingsRoute.Player) {
                SettingsCard {
                    SettingsLabel(stringResource(R.string.set_player_style))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        PlayerStyle.entries.forEach { style ->
                            InfinitePlayModeChip(
                                stringResource(if (style == PlayerStyle.CLASSIC) R.string.player_classic else R.string.player_expressive),
                                style == playerStyle,
                            ) { onPlayerStyleChange(style) }
                        }
                    }
                    Text(
                        text = stringResource(if (playerStyle == PlayerStyle.CLASSIC) R.string.set_player_classic_desc else R.string.set_player_expressive_desc),
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (section == SettingsRoute.Player) SettingsCard {
                SettingsRow(
                    icon = AppIcons.Background,
                    title = stringResource(R.string.set_player_background),
                    subtitle = stringResource(R.string.set_bg_sub),
                    onClick = onOpenNowPlayingBackground,
                    showChevron = true,
                )
                SettingsSwitchRow(
                    icon = AppIcons.Tap,
                    title = stringResource(R.string.set_tap_plays),
                    subtitle = stringResource(R.string.set_tap_plays_sub),
                    checked = lyricsTapPlays,
                    onCheckedChange = onLyricsTapPlaysChange,
                    modifier = Modifier.padding(top = 16.dp),
                )
                SettingsSwitchRow(
                    icon = AppIcons.Lyrics,
                    title = stringResource(R.string.set_lrc_dots),
                    subtitle = stringResource(R.string.set_lrc_dots_sub),
                    checked = lrcGapDots,
                    onCheckedChange = onLrcGapDotsChange,
                    modifier = Modifier.padding(top = 16.dp),
                )
                SettingsSwitchRow(
                    icon = AppIcons.Equalizer,
                    title = stringResource(R.string.set_quality_badge),
                    subtitle = stringResource(R.string.set_quality_badge_sub),
                    checked = extras.qualityBadge,
                    onCheckedChange = extras.onQualityBadgeChange,
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
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PlayerColors.Surface)
            .padding(16.dp),
        content = content,
    )
}

private val ROW_BLEED = 6.dp

/** A row as wide as its card's inner width plus [ROW_BLEED] each side, so the press patch reaches
 * past the text while the text itself stays where it was. */
private fun Modifier.rowBleed(): Modifier = this.layout { measurable, constraints ->
    val bleed = ROW_BLEED.roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + 2 * bleed, maxWidth = constraints.maxWidth + 2 * bleed))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
}

@Composable
internal fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = false,
) {
    Row(
        modifier = Modifier
            .rowBleed()
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onClick() }
            .padding(horizontal = ROW_BLEED, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(text = title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = PlayerColors.TextSecondary, fontSize = 12.sp)
        }
        if (showChevron) {
            Icon(
                imageVector = AppIcons.ChevronRight,
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
            .rowBleed()
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onCheckedChange(!checked) }
            .padding(horizontal = ROW_BLEED, vertical = 6.dp),
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
private enum class ComponentKind { FONT, ICONS, LIBRARY }

private class ThirdPartyComponent(val name: String, val authors: String, val license: String, val files: List<String>) {
    /** Fonts come with their OFL text, icon sets are named as such; the rest is code. */
    val kind: ComponentKind = when {
        files.any { it.endsWith("-ofl.txt") } -> ComponentKind.FONT
        "Icons" in name || name == "SVG Spinners" -> ComponentKind.ICONS
        else -> ComponentKind.LIBRARY
    }

    /** The license, short enough for the end of a row. */
    val shortLicense: String = when {
        license.startsWith("SIL") -> "OFL"
        license.startsWith("Apache") -> "Apache 2.0"
        else -> license
    }
}

private val THIRD_PARTY = listOf(
    ThirdPartyComponent(
        name = "ALAC decoder (Java)",
        authors = "Peter McQuillan; based on the ALAC decoder by David Hammerton",
        license = "BSD",
        files = listOf("alac-bsd.txt"),
    ),
    ThirdPartyComponent(
        name = "Kuromoji + mecab-ipadic",
        authors = "Atilika Inc. and contributors; Nara Institute of Science and Technology (NAIST)",
        license = "Apache License 2.0 + NAIST/ICOT notice",
        files = listOf("kuromoji-notice.txt", "apache-2.0.txt"),
    ),
    ThirdPartyComponent(
        name = "Lucide Icons",
        authors = "Lucide Icons and Contributors; portions from Feather by Cole Bemis",
        license = "ISC",
        files = listOf("lucide-isc.txt"),
    ),
    ThirdPartyComponent(
        name = "Tabler Icons",
        authors = "Paweł Kuna",
        license = "MIT",
        files = listOf("tabler-mit.txt"),
    ),
    ThirdPartyComponent(
        name = "Framework7 Icons",
        authors = "Vladimir Kharlampidi",
        license = "MIT",
        files = listOf("framework7-icons-mit.txt"),
    ),
    ThirdPartyComponent(
        name = "Phosphor Icons",
        authors = "Phosphor Icons",
        license = "MIT",
        files = listOf("phosphor-mit.txt"),
    ),
    ThirdPartyComponent(
        name = "SVG Spinners",
        authors = "Utkarsh Verma",
        license = "MIT",
        files = listOf("svg-spinners-mit.txt"),
    ),
    ThirdPartyComponent(
        name = "Inter",
        authors = "The Inter Project Authors (Rasmus Andersson)",
        license = "SIL Open Font License 1.1",
        files = listOf("inter-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Adwaita Sans",
        authors = "The Inter Project Authors; Renzhi Li; Jamie Gravendeel (GNOME)",
        license = "SIL Open Font License 1.1",
        files = listOf("adwaita-sans-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Geist",
        authors = "The Geist Project Authors (Vercel)",
        license = "SIL Open Font License 1.1",
        files = listOf("geist-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Manrope",
        authors = "The Manrope Project Authors (Mikhail Sharanda)",
        license = "SIL Open Font License 1.1",
        files = listOf("manrope-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Onest",
        authors = "The Onest Project Authors",
        license = "SIL Open Font License 1.1",
        files = listOf("onest-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Golos Text",
        authors = "The Golos Text Project Authors (Paratype)",
        license = "SIL Open Font License 1.1",
        files = listOf("golos-text-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Rubik",
        authors = "The Rubik Project Authors",
        license = "SIL Open Font License 1.1",
        files = listOf("rubik-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Nunito",
        authors = "The Nunito Project Authors",
        license = "SIL Open Font License 1.1",
        files = listOf("nunito-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Roboto",
        authors = "The Roboto Project Authors (Google)",
        license = "SIL Open Font License 1.1",
        files = listOf("roboto-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Unbounded",
        authors = "The Unbounded Project Authors",
        license = "SIL Open Font License 1.1",
        files = listOf("unbounded-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Playpen Sans",
        authors = "The Playpen Sans Project Authors (TypeTogether)",
        license = "SIL Open Font License 1.1",
        files = listOf("playpen-sans-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Climate Crisis",
        authors = "The Climate Crisis Project Authors (Daniel Coull, Eino Korkala)",
        license = "SIL Open Font License 1.1",
        files = listOf("climate-crisis-ofl.txt"),
    ),
    ThirdPartyComponent(
        name = "Caveat",
        authors = "The Caveat Project Authors (Impallari Type)",
        license = "SIL Open Font License 1.1",
        files = listOf("caveat-ofl.txt"),
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

/** This build's name, shown under the version in «О приложении». */
private const val RELEASE_NAME = "Akashi"
private const val RELEASE_NAME_JP = "灯"

@Composable
private fun AboutContent(onShowOnboarding: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    var openLicense by remember { mutableStateOf<ThirdPartyComponent?>(null) }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        // The app itself: its burst, name, version and what it is.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            com.artemiy.player.ui.mood.TalkingBurst(color = PlayerColors.AccentMark, speaking = false, modifier = Modifier.size(84.dp))
            Text(
                text = "Lumine",
                color = PlayerColors.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(text = stringResource(R.string.set_version, version), color = PlayerColors.TextSecondary, fontSize = 13.sp)
            // Each build's own name — "Akashi" (灯), an old Japanese word for a lamp's light: the
            // app finally seeing the light beyond one computer and one phone.
            Text(
                text = "$RELEASE_NAME · $RELEASE_NAME_JP",
                color = PlayerColors.AccentStandalone,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = stringResource(R.string.about_tagline),
                color = PlayerColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(AppIcons.Refresh, stringResource(R.string.set_show_welcome), stringResource(R.string.about_welcome_sub), onShowOnboarding, showChevron = true)
            CategoryDivider()
            SettingsRow(AppIcons.Share, stringResource(R.string.about_source), stringResource(R.string.about_source_sub), {
                runCatching {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(SOURCE_URL)))
                }
            }, showChevron = true)
        }
        // Everything bundled from others, in groups; a tap shows its license.
        listOf(
            R.string.about_fonts to THIRD_PARTY.filter { it.kind == ComponentKind.FONT },
            R.string.set_icons to THIRD_PARTY.filter { it.kind == ComponentKind.ICONS },
            R.string.about_libraries to THIRD_PARTY.filter { it.kind == ComponentKind.LIBRARY },
        ).forEach { (title, components) ->
            SectionTitle(stringResource(title))
            SettingsCard {
                components.forEachIndexed { index, component ->
                    if (index > 0) CategoryDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { openLicense = component }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = component.name, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = component.authors, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 2)
                        }
                        Text(
                            text = component.shortLicense,
                            color = PlayerColors.TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                        Icon(AppIcons.ChevronRight, contentDescription = null, tint = PlayerColors.TextTertiary, modifier = Modifier.padding(start = 4.dp).size(18.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
    openLicense?.let { component -> LicenseSheet(component, onDismiss = { openLicense = null }) }
}

/** A bundled component's full license text, in a sheet that slides up; the text can be copied. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LicenseSheet(component: ThirdPartyComponent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember(component.name) {
        component.files.joinToString("\n\n") { file ->
            runCatching { context.assets.open("licenses/$file").bufferedReader().use { it.readText() } }.getOrDefault("")
        }
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Never under the status bar, however tall the sheet gets.
        modifier = Modifier.statusBarsPadding(),
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlayerColors.Background,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PlayerColors.TextTertiary),
            )
        },
    ) {
        androidx.compose.foundation.text.selection.SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()).barsInset()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
            ) {
                Text(text = component.name, color = PlayerColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = component.authors, color = PlayerColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = stringResource(R.string.about_license) + ": " + component.license,
                    color = PlayerColors.AccentStandalone,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = text,
                    color = PlayerColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

private const val SOURCE_URL = "https://github.com/artemiy44/myzikalnya"

@Composable
private fun InfinitePlayModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Text(
        text = label,
        color = if (selected) PlayerColors.OnAccent else PlayerColors.TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .pressScale(press, 0.92f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PlayerColors.Accent else PlayerColors.SurfaceDim)
            .clickable(interactionSource = press, indication = null, onClick = onClick)
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
internal fun FolderChips(
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
            val press = remember { MutableInteractionSource() }
            Text(
                text = folder,
                color = if (selected) PlayerColors.OnAccent else PlayerColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .pressScale(press, 0.92f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) PlayerColors.Accent else PlayerColors.SurfaceDim)
                    .clickable(interactionSource = press, indication = null) { onToggleFolder(folder) }
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
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        SectionTitle(stringResource(R.string.set_bg_mode))
        SettingsCard {
            Text(
                text = stringResource(R.string.set_bg_mode_desc),
                color = PlayerColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            BackgroundModeOption(
                title = stringResource(R.string.bg_live),
                subtitle = stringResource(R.string.bg_live_sub),
                selected = mode == NowPlayingBackgroundMode.LIVE_BLUR,
                onClick = { onModeChange(NowPlayingBackgroundMode.LIVE_BLUR) },
            )
            BackgroundModeOption(
                title = stringResource(R.string.bg_static),
                subtitle = stringResource(R.string.bg_static_sub),
                selected = mode == NowPlayingBackgroundMode.STATIC_BLUR,
                onClick = { onModeChange(NowPlayingBackgroundMode.STATIC_BLUR) },
            )
            BackgroundModeOption(
                title = stringResource(R.string.bg_none),
                subtitle = stringResource(R.string.bg_none_sub),
                selected = mode == NowPlayingBackgroundMode.NONE,
                onClick = { onModeChange(NowPlayingBackgroundMode.NONE) },
            )
        }

        if (mode == NowPlayingBackgroundMode.LIVE_BLUR) {
            SectionTitle(stringResource(R.string.set_saturation))
            SettingsCard {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    InfinitePlayModeChip(
                        label = stringResource(R.string.sat_muted),
                        selected = intensity == LiveBlurIntensity.MUTED,
                        onClick = { onIntensityChange(LiveBlurIntensity.MUTED) },
                    )
                    InfinitePlayModeChip(
                        label = stringResource(R.string.sat_normal),
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

/** Every app font as a chip, each written in its own typeface; the chosen one filled in. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FontChoices(selected: com.artemiy.player.ui.theme.AppFont, onSelect: (com.artemiy.player.ui.theme.AppFont) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        com.artemiy.player.ui.theme.AppFont.entries.forEach { font ->
            val chosen = font == selected
            val press = remember { MutableInteractionSource() }
            Text(
                text = font.label,
                color = if (chosen) PlayerColors.OnAccent else PlayerColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = font.family,
                modifier = Modifier
                    .pressScale(press, 0.94f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(if (chosen) PlayerColors.Accent else PlayerColors.Surface)
                    .clickable(interactionSource = press, indication = null) { onSelect(font) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

/** The app's language and its tone; picking one restarts the screen in it. Languages are named
 * in themselves, so they're findable whatever the app is showing. */
@Composable
private fun LanguageContent() {
    val context = LocalContext.current
    val current = remember { com.artemiy.player.ui.i18n.LanguagePrefs.effective(context) }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            com.artemiy.player.ui.i18n.AppLanguage.entries.forEachIndexed { index, language ->
                if (index > 0) CategoryDivider()
                val chosen = language == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!chosen) {
                                com.artemiy.player.ui.i18n.LanguagePrefs.set(context, language)
                                (context as? android.app.Activity)?.recreate()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language.expressive) {
                                language.nativeName + " · " + stringResource(R.string.lang_expressive)
                            } else {
                                language.nativeName
                            },
                            color = PlayerColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (chosen) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        if (language.expressive) {
                            Text(text = stringResource(R.string.lang_expressive_hint), color = PlayerColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                    if (chosen) Icon(AppIcons.Check, contentDescription = null, tint = PlayerColors.AccentStandalone, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * "Skip files shorter than N s", 10 to 60 seconds in steps of 5. The library is only re-read
 * when the finger lets go, not on every step of the drag.
 */
@Composable
private fun MinDurationSetting(seconds: Int, onChange: (Int) -> Unit) {
    var dragged by remember(seconds) { mutableStateOf(seconds.toFloat()) }
    val shown = ((dragged / 5f).roundToInt() * 5).coerceIn(10, 60)
    Row(modifier = Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.set_min_length),
            color = PlayerColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(text = stringResource(R.string.unit_sec, shown), color = PlayerColors.TextSecondary, fontSize = 13.sp)
    }
    Text(
        text = stringResource(R.string.set_min_length_desc),
        color = PlayerColors.TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 2.dp),
    )
    MinimalSlider(
        value = dragged,
        onValueChange = { dragged = it },
        valueRange = 10f..60f,
        onValueChangeFinished = { if (shown != seconds) onChange(shown) },
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}
