package com.artemiy.player.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artemiy.player.data.Mood
import com.artemiy.player.data.SettingsRepository
import com.artemiy.player.data.discoverAllAudioFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SettingsRepository(app)

    var fontScale by mutableFloatStateOf(SettingsRepository.DEFAULT_FONT_SCALE)
        private set

    var moodFolders by mutableStateOf<Map<Mood, Set<String>>>(emptyMap())
        private set

    var scanFolders by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Every folder name found anywhere in the device's audio index — ringtones, notifications,
     * downloads, the user's real music, all of it — so they can pick which ones are real. */
    var availableScanFolders by mutableStateOf<List<String>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            repository.fontScale.collect { fontScale = it }
        }
        viewModelScope.launch {
            repository.moodFolders.collect { moodFolders = it }
        }
        viewModelScope.launch {
            repository.scanFolders.collect { scanFolders = it }
        }
        loadAvailableScanFolders()
    }

    fun loadAvailableScanFolders() {
        viewModelScope.launch {
            availableScanFolders = withContext(Dispatchers.IO) { discoverAllAudioFolders(getApplication<Application>()) }
        }
    }

    fun updateFontScale(value: Float) {
        fontScale = value
        viewModelScope.launch { repository.setFontScale(value) }
    }

    fun toggleMoodFolder(mood: Mood, folder: String) {
        val current = moodFolders[mood] ?: emptySet()
        val updated = if (folder in current) current - folder else current + folder
        moodFolders = moodFolders + (mood to updated)
        viewModelScope.launch { repository.setMoodFolders(mood, updated) }
    }

    fun toggleScanFolder(folder: String) {
        val updated = if (folder in scanFolders) scanFolders - folder else scanFolders + folder
        scanFolders = updated
        viewModelScope.launch { repository.setScanFolders(updated) }
    }
}
