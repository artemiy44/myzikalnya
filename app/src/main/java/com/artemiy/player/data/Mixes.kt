package com.artemiy.player.data

import java.util.Calendar
import kotlin.random.Random

/** An automatic playlist on Home, rebuilt daily from listening stats. Not a user playlist. */
data class Mix(
    val id: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    /** Position in the card color palette. */
    val colorIndex: Int,
    /** What the card's drawing shows: the mix kind, plus its genre / time of day / decade where
     * it has one — e.g. "favorites", "genre:Rock", "daypart:NIGHT", "decade:1990". */
    val motif: String = id,
)

const val MIX_MIN_STAT_DAYS = 3
private const val MIX_SIZE = 20
private const val MIX_MIN_SONGS = 8
private const val MAX_MIXES = 10
private const val DAY_MS = 24L * 60 * 60 * 1000

/** How many different days have at least one play — mixes wait for [MIX_MIN_STAT_DAYS]. */
fun statDays(plays: List<SongEvent>): Int = plays.map { dayKey(it.at) }.distinct().size

private fun dayKey(at: Long): Int = Calendar.getInstance().run {
    timeInMillis = at
    get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
}

private fun hourOf(at: Long): Int = Calendar.getInstance().run {
    timeInMillis = at
    get(Calendar.HOUR_OF_DAY)
}

enum class DayPart(val mixTitle: String, val label: String) {
    MORNING("Утренний микс", "утром"),
    AFTERNOON("Дневной микс", "днём"),
    EVENING("Вечерний микс", "вечером"),
    NIGHT("Ночной микс", "ночью"),
}

fun dayPartOf(hour: Int): DayPart = when (hour) {
    in 5..11 -> DayPart.MORNING
    in 12..16 -> DayPart.AFTERNOON
    in 17..22 -> DayPart.EVENING
    else -> DayPart.NIGHT
}

/** "Rock" of "Rock/Pop" — the first genre of a tag, trimmed. */
fun primaryGenre(song: Song): String? =
    song.genre?.split('/', ';', ',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Builds up to [MAX_MIXES] mixes, each describing one thing about how you listen. Stable for a
 * whole day (the same day + time of day always gives the same picks), new the next day. Mixes
 * without enough material to fill [MIX_MIN_SONGS] are simply left out.
 */
fun buildMixes(songs: List<Song>, plays: List<SongEvent>, skips: List<SongEvent>, now: Long): List<Mix> {
    if (songs.isEmpty()) return emptyList()
    val dayPart = dayPartOf(hourOf(now))
    val random = Random(dayKey(now) * 10 + dayPart.ordinal)
    val byId = songs.associateBy { it.id }

    val allCounts = plays.groupingBy { it.songId }.eachCount()
    val recentCounts = plays.filter { it.at > now - 14 * DAY_MS }.groupingBy { it.songId }.eachCount()
    val monthCounts = plays.filter { it.at > now - 30 * DAY_MS }.groupingBy { it.songId }.eachCount()
    val lastPlayed = plays.groupBy { it.songId }.mapValues { (_, events) -> events.maxOf { it.at } }
    // Skipped twice or more lately: probably not wanted in a "discover" pick right now.
    val skippedLately = skips.filter { it.at > now - 30 * DAY_MS }.groupingBy { it.songId }.eachCount()
        .filterValues { it >= 2 }.keys

    fun ranked(counts: Map<Long, Int>, minPlays: Int) =
        counts.filterValues { it >= minPlays }.entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }

    val candidates = mutableListOf<Pair<String, () -> Pair<String, List<Song>>?>>()

    candidates += "repeat" to { "На повторе" to ranked(recentCounts, 2).take(MIX_SIZE) }
    candidates += "favorites" to { "Любимое" to ranked(allCounts, 3).take(MIX_SIZE) }

    val topArtists = monthCounts.entries
        .mapNotNull { (id, count) -> byId[id]?.let { it.artist to count } }
        .groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        .filterValues { it >= 3 }.entries.sortedByDescending { it.value }.map { it.key }
    val topGenres = monthCounts.entries
        .mapNotNull { (id, count) -> byId[id]?.let(::primaryGenre)?.let { it to count } }
        .groupBy({ it.first.lowercase() }, { it }).mapValues { (_, list) -> list.first().first to list.sumOf { it.second } }
        .values.sortedByDescending { it.second }.map { it.first }

    fun artistMix(artist: String): Pair<String, List<Song>>? {
        val own = songs.filter { it.artist == artist }.sortedByDescending { allCounts[it.id] ?: 0 }
        val genre = own.mapNotNull(::primaryGenre).groupingBy { it.lowercase() }.eachCount().maxByOrNull { it.value }?.key
        val similar = if (genre == null) emptyList() else songs.filter {
            it.artist != artist && primaryGenre(it)?.lowercase() == genre && it.id !in skippedLately
        }.shuffled(random)
        return "Микс: $artist" to (own.take(12) + similar).take(MIX_SIZE).shuffled(random)
    }

    fun genreMix(genre: String): Pair<String, List<Song>> {
        val inGenre = songs.filter { primaryGenre(it)?.equals(genre, ignoreCase = true) == true && it.id !in skippedLately }
        val (played, unplayed) = inGenre.partition { (allCounts[it.id] ?: 0) > 0 }
        val picks = played.sortedByDescending { allCounts[it.id] ?: 0 }.take(MIX_SIZE / 2) + unplayed.shuffled(random)
        return "$genre-микс" to picks.take(MIX_SIZE).shuffled(random)
    }

    topArtists.getOrNull(0)?.let { artist -> candidates += "artist-0" to { artistMix(artist) } }
    topGenres.getOrNull(0)?.let { genre -> candidates += "genre-0" to { genreMix(genre) } }

    candidates += "daypart" to {
        val counts = plays.filter { dayPartOf(hourOf(it.at)) == dayPart }.groupingBy { it.songId }.eachCount()
        dayPart.mixTitle to ranked(counts, 1).take(MIX_SIZE)
    }

    candidates += "fresh" to {
        fun untried(days: Int) = songs.filter {
            it.dateAddedMs > now - days * DAY_MS && (allCounts[it.id] ?: 0) <= 1 && it.id !in skippedLately
        }
        val pool = untried(60).takeIf { it.size >= MIX_MIN_SONGS } ?: untried(180)
        "Новое, не опробованное" to pool.shuffled(random).take(MIX_SIZE)
    }

    candidates += "forgotten" to {
        val forgotten = songs.filter { (allCounts[it.id] ?: 0) >= 3 && (lastPlayed[it.id] ?: now) < now - 30 * DAY_MS }
        "Давно не слушал" to forgotten.sortedByDescending { allCounts[it.id] ?: 0 }.take(MIX_SIZE)
    }

    topArtists.getOrNull(1)?.let { artist -> candidates += "artist-1" to { artistMix(artist) } }
    topGenres.getOrNull(1)?.let { genre -> candidates += "genre-1" to { genreMix(genre) } }

    candidates += "decade" to {
        val decadeCounts = allCounts.entries.mapNotNull { (id, count) -> byId[id]?.year?.let { (it / 10) * 10 to count } }
            .groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        val decade = decadeCounts.maxByOrNull { it.value }?.key
        if (decade == null) {
            "" to emptyList()
        } else {
            val inDecade = songs.filter { it.year != null && it.year / 10 * 10 == decade }
            val (played, unplayed) = inDecade.partition { (allCounts[it.id] ?: 0) > 0 }
            "$decade-е" to (played.shuffled(random).take(MIX_SIZE / 2) + unplayed.shuffled(random)).take(MIX_SIZE).shuffled(random)
        }
    }

    candidates += "deep" to {
        "Глубокие треки" to songs.filter { it.id !in allCounts && it.id !in skippedLately }.shuffled(random).take(MIX_SIZE)
    }

    val mixes = mutableListOf<Mix>()
    for ((id, build) in candidates) {
        if (mixes.size == MAX_MIXES) break
        val (title, picks) = build() ?: continue
        val distinct = picks.distinctBy { it.id }
        if (distinct.size < MIX_MIN_SONGS) continue
        val motif = when {
            id.startsWith("genre-") -> "genre:" + topGenres[id.removePrefix("genre-").toInt()]
            id.startsWith("artist-") -> "artist"
            id == "daypart" -> "daypart:" + dayPart.name
            id == "decade" -> "decade:" + title.removeSuffix("-е")
            else -> id
        }
        mixes += Mix(id = id, title = title, subtitle = artistsLine(distinct), songs = distinct, colorIndex = mixes.size, motif = motif)
    }
    return mixes
}

/** "A, B, C, D и другие" — the mix's most frequent artists first. */
fun artistsLine(songs: List<Song>, shown: Int = 4): String {
    val artists = songs.groupingBy { it.artist }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    val head = artists.take(shown).joinToString(", ")
    return if (artists.size > shown) "$head и другие" else head
}
