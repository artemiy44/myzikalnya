package com.artemiy.player.data

import androidx.annotation.StringRes
import com.artemiy.player.R
import java.util.Calendar

/**
 * Playlists that fill themselves from how the library is listened to: the rules are fixed (these
 * below), the songs are worked out each time they're looked at. Their ids are negative, so
 * they can't clash with the ids of the user's own playlists and the playlist page can show them.
 */
enum class SmartKind(val id: Long, @StringRes val titleRes: Int) {
    ADDED_MONTH(-1, R.string.sp_added_month),
    PLAYED_WEEK(-2, R.string.sp_played_week),
    TOP_MONTH(-3, R.string.sp_top_month),
    TOP_YEAR(-4, R.string.sp_top_year),
    MOST_EVER(-5, R.string.sp_most_ever),
    NOT_HEARD(-6, R.string.sp_not_heard),
    OLD_FAVORITES(-7, R.string.sp_old_favorites),
    NEVER_PLAYED(-8, R.string.sp_never_played),
    SKIPPED(-9, R.string.sp_skipped),
    NIGHT(-10, R.string.sp_night),
    THIS_YEAR(-11, R.string.sp_this_year),
    LONG(-12, R.string.sp_long),
    SHORT(-13, R.string.sp_short),
    ;

    companion object {
        fun of(id: Long): SmartKind? = entries.firstOrNull { it.id == id }
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

/** How many songs the "top" lists hold at most. */
private const val TOP_MAX = 100

/** Songs this long (or short) go in the "long" ("short") list. */
private const val LONG_MS = 6 * 60_000L
private const val SHORT_MS = 2 * 60_000L

/** Every smart playlist's songs, in the order they're shown; one with no songs is simply empty. */
fun buildSmartPlaylists(songs: List<Song>, plays: List<SongEvent>, skips: List<SongEvent>, now: Long = System.currentTimeMillis()): Map<SmartKind, List<Song>> {
    val byId = songs.associateBy { it.id }
    val calendar = Calendar.getInstance()

    fun counted(events: List<SongEvent>, min: Int) =
        events.groupingBy { it.songId }.eachCount().filterValues { it >= min }
            .entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }

    val allCounts = plays.groupingBy { it.songId }.eachCount()
    val lastPlayed = plays.groupBy { it.songId }.mapValues { (_, events) -> events.maxOf { it.at } }
    val yearNow = calendar.apply { timeInMillis = now }.get(Calendar.YEAR)

    return SmartKind.entries.associateWith { kind ->
        when (kind) {
            SmartKind.ADDED_MONTH -> songs.filter { it.dateAddedMs > now - 30 * DAY_MS }.sortedByDescending { it.dateAddedMs }
            // Most recently heard first, each song once.
            SmartKind.PLAYED_WEEK -> lastPlayed.filterValues { it > now - 7 * DAY_MS }
                .entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }
            SmartKind.TOP_MONTH -> counted(plays.filter { it.at > now - 30 * DAY_MS }, 2).take(TOP_MAX)
            SmartKind.TOP_YEAR -> counted(plays.filter { it.at > now - 365 * DAY_MS }, 3).take(TOP_MAX)
            SmartKind.MOST_EVER -> counted(plays, 5).take(TOP_MAX)
            // Heard before, but not for a month — the most played of those first.
            SmartKind.NOT_HEARD -> songs.filter { (allCounts[it.id] ?: 0) > 0 && (lastPlayed[it.id] ?: now) < now - 30 * DAY_MS }
                .sortedByDescending { allCounts[it.id] ?: 0 }
            SmartKind.OLD_FAVORITES -> songs.filter { (allCounts[it.id] ?: 0) >= 5 && (lastPlayed[it.id] ?: now) < now - 90 * DAY_MS }
                .sortedByDescending { allCounts[it.id] ?: 0 }
            SmartKind.NEVER_PLAYED -> songs.filter { it.id !in allCounts }.sortedByDescending { it.dateAddedMs }
            SmartKind.SKIPPED -> counted(skips, 3).take(TOP_MAX)
            // What's played between midnight and five in the morning.
            SmartKind.NIGHT -> counted(plays.filter { calendar.apply { timeInMillis = it.at }.get(Calendar.HOUR_OF_DAY) in 0..4 }, 2).take(TOP_MAX)
            SmartKind.THIS_YEAR -> songs.filter { it.year == yearNow }.sortedByDescending { it.dateAddedMs }
            SmartKind.LONG -> songs.filter { it.durationMs >= LONG_MS }.sortedByDescending { it.durationMs }
            SmartKind.SHORT -> songs.filter { it.durationMs in 1 until SHORT_MS }.sortedBy { it.durationMs }
        }
    }
}
