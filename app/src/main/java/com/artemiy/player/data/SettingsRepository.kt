package com.artemiy.player.data

import android.content.Context
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

class SettingsRepository(private val context: Context) {

    companion object {
        val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        const val DEFAULT_FONT_SCALE = 1.08f
        const val MIN_FONT_SCALE = 0.9f
        const val MAX_FONT_SCALE = 1.35f
        val SCAN_FOLDERS_KEY = stringSetPreferencesKey("scan_folders")
        val INFINITE_PLAY_MODE_KEY = stringPreferencesKey("infinite_play_mode")
        val NOW_PLAYING_BACKGROUND_MODE_KEY = stringPreferencesKey("now_playing_background_mode")
        val LIVE_BLUR_INTENSITY_KEY = stringPreferencesKey("live_blur_intensity")
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
}
