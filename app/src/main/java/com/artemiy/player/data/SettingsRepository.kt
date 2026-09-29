package com.artemiy.player.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class LibraryViewMode { LIST, GRID_2, GRID_3 }

enum class InfinitePlayMode { RANDOM, GENRE_RADIO }

enum class NowPlayingBackgroundMode { LIVE_BLUR, STATIC_BLUR, NONE }

enum class LiveBlurIntensity { MUTED, NORMAL, VIVID }

/** Now Playing's layout: ours, or one built from Material 3 Expressive pieces. */
enum class PlayerStyle { CLASSIC, EXPRESSIVE }

class SettingsRepository(private val context: Context) {

    companion object {
        val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        const val DEFAULT_FONT_SCALE = 1.08f
        const val MIN_FONT_SCALE = 0.9f
        const val MAX_FONT_SCALE = 1.35f
        val SCAN_FOLDERS_KEY = stringSetPreferencesKey("scan_folders")
        val MIN_DURATION_KEY = androidx.datastore.preferences.core.intPreferencesKey("min_duration_s")
        const val DEFAULT_MIN_DURATION_S = 60
        val KEPT_ARTISTS_KEY = stringSetPreferencesKey("kept_artists")
        val INFINITE_PLAY_MODE_KEY = stringPreferencesKey("infinite_play_mode")
        val NOTIF_REPEAT_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("notification_repeat_button")
        val NOW_PLAYING_BACKGROUND_MODE_KEY = stringPreferencesKey("now_playing_background_mode")
        val PLAYER_STYLE_KEY = stringPreferencesKey("player_style")
        val LIVE_BLUR_INTENSITY_KEY = stringPreferencesKey("live_blur_intensity")
        val LYRICS_ROMANIZATION_KEY = booleanPreferencesKey("lyrics_romanization")
        val LYRICS_TAP_PLAYS_KEY = booleanPreferencesKey("lyrics_tap_plays")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LIGHT_VARIANT_KEY = stringPreferencesKey("light_variant")
        val DARK_VARIANT_KEY = stringPreferencesKey("dark_variant")
        val ACCENT_KEY = stringPreferencesKey("accent")
        val START_TAB_KEY = stringPreferencesKey("start_tab")
        val ICON_SET_KEY = stringPreferencesKey("icon_set")
        val APP_FONT_KEY = stringPreferencesKey("app_font")
        val ONBOARDING_DONE_KEY = booleanPreferencesKey("onboarding_done")
        private fun viewModeKey(tab: String) = stringPreferencesKey("view_mode_$tab")
    }

    val fontScale: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[FONT_SCALE_KEY] ?: DEFAULT_FONT_SCALE
    }

    suspend fun setFontScale(value: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[FONT_SCALE_KEY] = value.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        }
    }

    private fun moodFoldersKey(mood: Mood) = stringSetPreferencesKey("mood_folders_${mood.name}")

    /** Folder names the user hand-picked as belonging to each mood, on top of the genre guess. */
    val moodFolders: Flow<Map<Mood, Set<String>>> = context.settingsDataStore.data.map { prefs ->
        Mood.entries.associateWith { mood -> prefs[moodFoldersKey(mood)] ?: emptySet() }
    }

    suspend fun setMoodFolders(mood: Mood, folders: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[moodFoldersKey(mood)] = folders
        }
    }

    /** Folder whitelist for the library scanner itself (empty = scan everything IS_MUSIC flags
     * as music, the old behavior; non-empty = only files under these folders, see querySongs). */
    /** Files shorter than this many seconds are left out of the library. */
    val minDurationSec: Flow<Int> = context.settingsDataStore.data.map { it[MIN_DURATION_KEY] ?: DEFAULT_MIN_DURATION_S }

    suspend fun setMinDurationSec(value: Int) {
        context.settingsDataStore.edit { it[MIN_DURATION_KEY] = value }
    }

    val scanFolders: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[SCAN_FOLDERS_KEY] ?: emptySet()
    }

    suspend fun setScanFolders(folders: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[SCAN_FOLDERS_KEY] = folders
        }
    }

    /** Per-tab list/grid choice (Artists/Albums/Songs — `tab` is just a stable key string, not
     * shared with any UI enum, so this stays independent of Compose). */
    fun viewMode(tab: String, default: LibraryViewMode): Flow<LibraryViewMode> =
        context.settingsDataStore.data.map { prefs ->
            prefs[viewModeKey(tab)]?.let { runCatching { LibraryViewMode.valueOf(it) }.getOrNull() } ?: default
        }

    suspend fun setViewMode(tab: String, mode: LibraryViewMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[viewModeKey(tab)] = mode.name
        }
    }

    /** The notification's second button: repeat instead of endless play. */
    val notificationRepeatButton: Flow<Boolean> = context.settingsDataStore.data.map { it[NOTIF_REPEAT_KEY] ?: false }

    suspend fun setNotificationRepeatButton(value: Boolean) {
        context.settingsDataStore.edit { it[NOTIF_REPEAT_KEY] = value }
    }

    val infinitePlayMode: Flow<InfinitePlayMode> = context.settingsDataStore.data.map { prefs ->
        prefs[INFINITE_PLAY_MODE_KEY]?.let { runCatching { InfinitePlayMode.valueOf(it) }.getOrNull() } ?: InfinitePlayMode.RANDOM
    }

    suspend fun setInfinitePlayMode(mode: InfinitePlayMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[INFINITE_PLAY_MODE_KEY] = mode.name
        }
    }

    val nowPlayingBackgroundMode: Flow<NowPlayingBackgroundMode> = context.settingsDataStore.data.map { prefs ->
        prefs[NOW_PLAYING_BACKGROUND_MODE_KEY]?.let { runCatching { NowPlayingBackgroundMode.valueOf(it) }.getOrNull() }
            ?: NowPlayingBackgroundMode.LIVE_BLUR
    }

    val playerStyle: Flow<PlayerStyle> = context.settingsDataStore.data.map { prefs ->
        prefs[PLAYER_STYLE_KEY]?.let { runCatching { PlayerStyle.valueOf(it) }.getOrNull() } ?: PlayerStyle.CLASSIC
    }

    suspend fun setPlayerStyle(style: PlayerStyle) {
        context.settingsDataStore.edit { prefs -> prefs[PLAYER_STYLE_KEY] = style.name }
    }

    suspend fun setNowPlayingBackgroundMode(mode: NowPlayingBackgroundMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[NOW_PLAYING_BACKGROUND_MODE_KEY] = mode.name
        }
    }

    val liveBlurIntensity: Flow<LiveBlurIntensity> = context.settingsDataStore.data.map { prefs ->
        prefs[LIVE_BLUR_INTENSITY_KEY]?.let { runCatching { LiveBlurIntensity.valueOf(it) }.getOrNull() } ?: LiveBlurIntensity.NORMAL
    }

    suspend fun setLiveBlurIntensity(intensity: LiveBlurIntensity) {
        context.settingsDataStore.edit { prefs ->
            prefs[LIVE_BLUR_INTENSITY_KEY] = intensity.name
        }
    }

    val lyricsRomanization: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[LYRICS_ROMANIZATION_KEY] ?: true
    }

    suspend fun setLyricsRomanization(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[LYRICS_ROMANIZATION_KEY] = enabled
        }
    }

    /** Whether tapping a lyric line while paused also starts playback (not just seeks). */
    val lyricsTapPlays: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[LYRICS_TAP_PLAYS_KEY] ?: false
    }

    suspend fun setLyricsTapPlays(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[LYRICS_TAP_PLAYS_KEY] = enabled
        }
    }

    /** Theme choices are stored as plain names/keys; parsing them is the UI layer's business. */
    val themeMode: Flow<String?> = context.settingsDataStore.data.map { it[THEME_MODE_KEY] }
    val lightVariant: Flow<String?> = context.settingsDataStore.data.map { it[LIGHT_VARIANT_KEY] }
    val darkVariant: Flow<String?> = context.settingsDataStore.data.map { it[DARK_VARIANT_KEY] }

    /** Empty/absent = monochrome. */
    val accent: Flow<String?> = context.settingsDataStore.data.map { it[ACCENT_KEY] }

    suspend fun setThemeMode(value: String) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = value }
    }

    suspend fun setLightVariant(value: String) {
        context.settingsDataStore.edit { it[LIGHT_VARIANT_KEY] = value }
    }

    suspend fun setDarkVariant(value: String) {
        context.settingsDataStore.edit { it[DARK_VARIANT_KEY] = value }
    }

    /** Which bottom tab the app opens on; absent = Home. */
    val startTab: Flow<String?> = context.settingsDataStore.data.map { it[START_TAB_KEY] }

    val iconSet: Flow<String?> = context.settingsDataStore.data.map { it[ICON_SET_KEY] }

    suspend fun setIconSet(value: String) {
        context.settingsDataStore.edit { it[ICON_SET_KEY] = value }
    }

    /** Whether the welcome screens have been gone through (absent = not yet). */
    val onboardingDone: Flow<Boolean> = context.settingsDataStore.data.map { it[ONBOARDING_DONE_KEY] ?: false }

    suspend fun setOnboardingDone(value: Boolean) {
        context.settingsDataStore.edit { it[ONBOARDING_DONE_KEY] = value }
    }

    /** The app's typeface (an AppFont name); absent = the default one. */
    val appFont: Flow<String?> = context.settingsDataStore.data.map { it[APP_FONT_KEY] }

    suspend fun setAppFont(value: String) {
        context.settingsDataStore.edit { it[APP_FONT_KEY] = value }
    }

    /** Artist names the user wants kept whole, never split into several artists. */
    val keptArtists: Flow<Set<String>> = context.settingsDataStore.data.map { it[KEPT_ARTISTS_KEY] ?: emptySet() }

    suspend fun setKeptArtists(value: Set<String>) {
        context.settingsDataStore.edit { it[KEPT_ARTISTS_KEY] = value }
    }

    suspend fun setStartTab(value: String) {
        context.settingsDataStore.edit { it[START_TAB_KEY] = value }
    }

    suspend fun setAccent(value: String) {
        context.settingsDataStore.edit { it[ACCENT_KEY] = value }
    }
}
