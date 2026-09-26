package com.artemiy.player.data

import java.util.Calendar
import java.util.Locale

/** A calendar week (Monday to Monday) of listening, summed up. */
data class Recap(
    val weekStart: Long,
    val weekEnd: Long,
    val playCount: Int,
    /** Approximate: each play counted as the song's full length. */
    val minutes: Int,
    val topSongs: List<Pair<Song, Int>>,
    val topArtists: List<Pair<String, Int>>,
    /** Songs that joined the library this week, grouped by artist. */
    val newByArtist: List<Pair<String, List<Song>>>,
    /** Songs you used to play, untouched for 2+ months. */
    val forgotten: List<Song>,
    val topGenre: String?,
    val busiestDay: String?,
    val busiestPart: DayPart?,
)

private const val DAY_MS = 24L * 60 * 60 * 1000

/** Midnight at the start of the Monday of [at]'s week. */
private fun mondayOf(at: Long): Long = Calendar.getInstance(Locale("ru")).run {
    firstDayOfWeek = Calendar.MONDAY
    timeInMillis = at
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    timeInMillis
}

/** The last complete week's [start, end) — the Monday-to-Monday before the current one. */
fun lastCompleteWeek(now: Long): Pair<Long, Long> {
    val thisMonday = mondayOf(now)
    return mondayOf(thisMonday - DAY_MS) to thisMonday
}

/** When the next recap becomes available (the coming Monday). */
fun nextRecapAt(now: Long): Long = mondayOf(mondayOf(now) + 8 * DAY_MS)

private val DAY_NAMES = mapOf(
    Calendar.MONDAY to "понедельник", Calendar.TUESDAY to "вторник", Calendar.WEDNESDAY to "среда",
    Calendar.THURSDAY to "четверг", Calendar.FRIDAY to "пятница", Calendar.SATURDAY to "суббота", Calendar.SUNDAY to "воскресенье",
)

/** Null when nothing was played during the last complete week. */
fun buildRecap(songs: List<Song>, plays: List<SongEvent>, now: Long): Recap? {
    val (start, end) = lastCompleteWeek(now)
    val byId = songs.associateBy { it.id }
    val week = plays.filter { it.at in start until end && it.songId in byId }
    if (week.isEmpty()) return null

    val songCounts = week.groupingBy { it.songId }.eachCount()
    val topSongs = songCounts.entries.sortedByDescending { it.value }.take(5).map { byId.getValue(it.key) to it.value }
    val artistCounts = week.groupBy { byId.getValue(it.songId).artist }.mapValues { it.value.size }
    val topArtists = artistCounts.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
    val minutes = (week.sumOf { byId.getValue(it.songId).durationMs } / 60_000L).toInt()

    val newByArtist = songs.filter { it.dateAddedMs in start until end }
        .groupBy { it.artist }.entries.sortedByDescending { it.value.size }.take(5).map { it.key to it.value }

    val allCounts = plays.groupingBy { it.songId }.eachCount()
    val lastPlayed = plays.groupBy { it.songId }.mapValues { (_, events) -> events.maxOf { it.at } }
    val forgotten = songs.filter { (allCounts[it.id] ?: 0) >= 3 && (lastPlayed[it.id] ?: end) < end - 60 * DAY_MS }
        .sortedByDescending { allCounts[it.id] ?: 0 }.take(5)

    val topGenre = week.mapNotNull { primaryGenre(byId.getValue(it.songId)) }
        .groupBy { it.lowercase() }.values.maxByOrNull { it.size }?.first()

    val calendar = Calendar.getInstance()
    val busiestDay = week.groupingBy { calendar.apply { timeInMillis = it.at }.get(Calendar.DAY_OF_WEEK) }.eachCount()
        .maxByOrNull { it.value }?.key?.let { DAY_NAMES[it] }
    val busiestPart = week.groupingBy { dayPartOf(calendar.apply { timeInMillis = it.at }.get(Calendar.HOUR_OF_DAY)) }.eachCount()
        .maxByOrNull { it.value }?.key

    return Recap(
        weekStart = start,
        weekEnd = end,
        playCount = week.size,
        minutes = minutes,
        topSongs = topSongs,
        topArtists = topArtists,
        newByArtist = newByArtist,
        forgotten = forgotten,
        topGenre = topGenre,
        busiestDay = busiestDay,
        busiestPart = busiestPart,
    )
}
