package com.artemiy.player.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artemiy.player.data.InfinitePlayMode
import com.artemiy.player.data.LibraryViewMode
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.Mood
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.SettingsRepository
import com.artemiy.player.data.discoverAllAudioFolders
import com.artemiy.player.ui.components.AppTab
import com.artemiy.player.ui.theme.AccentChoice
import com.artemiy.player.ui.theme.DarkVariant
import com.artemiy.player.ui.theme.LightVariant
import com.artemiy.player.ui.theme.ThemeMode
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

    var infinitePlayMode by mutableStateOf(InfinitePlayMode.RANDOM)
        private set

    var nowPlayingBackgroundMode by mutableStateOf(NowPlayingBackgroundMode.LIVE_BLUR)
        private set

    var liveBlurIntensity by mutableStateOf(LiveBlurIntensity.NORMAL)
        private set

    var lyricsRomanization by mutableStateOf(true)
        private set

    var lyricsTapPlays by mutableStateOf(false)
        private set

    var themeMode by mutableStateOf(ThemeMode.DARK)
        private set

    var lightVariant by mutableStateOf(LightVariant.WHITE)
        private set

    var darkVariant by mutableStateOf(DarkVariant.GNOME)
        private set

    /** Null = monochrome. */
    var accent by mutableStateOf<AccentChoice?>(null)
        private set

    var startTab by mutableStateOf(AppTab.Home)
        private set

    /** False until the saved choice has been read — the app waits for it before picking a tab. */
    var startTabLoaded by mutableStateOf(false)
        private set

    private var artistViewMode by mutableStateOf(LibraryViewMode.LIST)
    private var albumViewMode by mutableStateOf(LibraryViewMode.GRID_2)
    private var songViewMode by mutableStateOf(LibraryViewMode.GRID_2)
    private var playlistViewMode by mutableStateOf(LibraryViewMode.LIST)

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
        viewModelScope.launch {
            repository.infinitePlayMode.collect { infinitePlayMode = it }
        }
        viewModelScope.launch {
            repository.nowPlayingBackgroundMode.collect { nowPlayingBackgroundMode = it }
        }
        viewModelScope.launch {
            repository.liveBlurIntensity.collect { liveBlurIntensity = it }
        }
        viewModelScope.launch {
            repository.lyricsRomanization.collect { lyricsRomanization = it }
        }
        viewModelScope.launch {
            repository.lyricsTapPlays.collect { lyricsTapPlays = it }
        }
        viewModelScope.launch {
            repository.themeMode.collect { value ->
                themeMode = value?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK
            }
        }
        viewModelScope.launch {
            repository.lightVariant.collect { value ->
                lightVariant = value?.let { runCatching { LightVariant.valueOf(it) }.getOrNull() } ?: LightVariant.WHITE
            }
        }
        viewModelScope.launch {
            repository.darkVariant.collect { value ->
                darkVariant = value?.let { runCatching { DarkVariant.valueOf(it) }.getOrNull() } ?: DarkVariant.GNOME
            }
        }
        viewModelScope.launch {
            repository.accent.collect { accent = AccentChoice.fromKey(it) }
        }
        viewModelScope.launch {
            repository.startTab.collect { value ->
                startTab = value?.let { runCatching { AppTab.valueOf(it) }.getOrNull() }?.takeIf { it != AppTab.Search } ?: AppTab.Home
                startTabLoaded = true
            }
        }
        viewModelScope.launch {
            repository.viewMode("artists", LibraryViewMode.LIST).collect { artistViewMode = it }
        }
        viewModelScope.launch {
            repository.viewMode("albums", LibraryViewMode.GRID_2).collect { albumViewMode = it }
        }
        viewModelScope.launch {
            repository.viewMode("songs", LibraryViewMode.GRID_2).collect { songViewMode = it }
        }
        viewModelScope.launch {
            repository.viewMode("playlist", LibraryViewMode.LIST).collect { playlistViewMode = it }
        }
        loadAvailableScanFolders()
    }

    fun viewMode(tab: String): LibraryViewMode = when (tab) {
        "artists" -> artistViewMode
        "albums" -> albumViewMode
        "playlist" -> playlistViewMode
        else -> songViewMode
    }

    fun setViewMode(tab: String, mode: LibraryViewMode) {
        when (tab) {
            "artists" -> artistViewMode = mode
            "albums" -> albumViewMode = mode
            "playlist" -> playlistViewMode = mode
            else -> songViewMode = mode
        }
        viewModelScope.launch { repository.setViewMode(tab, mode) }
    }

    fun updateInfinitePlayMode(mode: InfinitePlayMode) {
        infinitePlayMode = mode
        viewModelScope.launch { repository.setInfinitePlayMode(mode) }
    }

    fun updateNowPlayingBackgroundMode(mode: NowPlayingBackgroundMode) {
        nowPlayingBackgroundMode = mode
        viewModelScope.launch { repository.setNowPlayingBackgroundMode(mode) }
    }

    fun updateLiveBlurIntensity(intensity: LiveBlurIntensity) {
        liveBlurIntensity = intensity
        viewModelScope.launch { repository.setLiveBlurIntensity(intensity) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        viewModelScope.launch { repository.setThemeMode(mode.name) }
    }

    fun updateLightVariant(variant: LightVariant) {
        lightVariant = variant
        viewModelScope.launch { repository.setLightVariant(variant.name) }
    }

    fun updateDarkVariant(variant: DarkVariant) {
        darkVariant = variant
        viewModelScope.launch { repository.setDarkVariant(variant.name) }
    }

    fun updateStartTab(tab: AppTab) {
        startTab = tab
        viewModelScope.launch { repository.setStartTab(tab.name) }
    }

    fun updateAccent(choice: AccentChoice?) {
        accent = choice
        viewModelScope.launch { repository.setAccent(choice?.toKey().orEmpty()) }
    }

    fun updateLyricsTapPlays(enabled: Boolean) {
        lyricsTapPlays = enabled
        viewModelScope.launch { repository.setLyricsTapPlays(enabled) }
    }

    fun toggleLyricsRomanization() {
        lyricsRomanization = !lyricsRomanization
        viewModelScope.launch { repository.setLyricsRomanization(lyricsRomanization) }
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
