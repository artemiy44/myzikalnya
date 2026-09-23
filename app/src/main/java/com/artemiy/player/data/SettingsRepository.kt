package com.artemiy.player.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        const val DEFAULT_FONT_SCALE = 1.08f
        const val MIN_FONT_SCALE = 0.9f
        const val MAX_FONT_SCALE = 1.35f
        val SCAN_FOLDERS_KEY = stringSetPreferencesKey("scan_folders")
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
}
