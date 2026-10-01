package com.artemiy.player.ui.library

import androidx.annotation.StringRes
import com.artemiy.player.R

/**
 * The sections the Library page lists, in the order the user chose (Settings → Общие → Вкладки
 * медиатеки). [key] is what's stored; add a new tab here and it shows up at the end of everyone's
 * list on its own.
 */
enum class LibraryTab(val key: String, @StringRes val labelRes: Int) {
    PLAYLISTS("playlists", R.string.playlists),
    ARTISTS("artists", R.string.artists),
    ALBUMS("albums", R.string.albums),
    TRACKS("tracks", R.string.tracks),
    YEARS("years", R.string.years),
    GENRES("genres", R.string.genres),
    FOLDERS("folders", R.string.folders);

    companion object {
        /** The order and visibility when nothing has been chosen yet: everything, in this order. */
        val DEFAULT: List<TabSetting> = entries.map { TabSetting(it, true) }

        /**
         * Stored as "playlists,artists,-albums,…": a leading "-" = hidden. Tabs missing from the
         * text (added in a later version) come after the ones that are there, shown; unknown
         * names are ignored.
         */
        fun parse(text: String?): List<TabSetting> {
            if (text.isNullOrBlank()) return DEFAULT
            val seen = LinkedHashMap<LibraryTab, Boolean>()
            for (token in text.split(',')) {
                val hidden = token.startsWith("-")
                val tab = entries.firstOrNull { it.key == token.removePrefix("-") } ?: continue
                seen.putIfAbsent(tab, !hidden)
            }
            for (tab in entries) seen.putIfAbsent(tab, true)
            return seen.map { (tab, shown) -> TabSetting(tab, shown) }
        }

        fun serialize(settings: List<TabSetting>): String =
            settings.joinToString(",") { (if (it.shown) "" else "-") + it.tab.key }
    }
}

/** One tab as the user set it up: [shown], and its place in the list. */
data class TabSetting(val tab: LibraryTab, val shown: Boolean)
