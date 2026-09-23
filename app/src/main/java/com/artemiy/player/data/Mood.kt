package com.artemiy.player.data

enum class Mood(val label: String, val subtitle: String) {
    NORMAL("Обычное", "На основе жанров в твоей медиатеке"),
    HAPPY("Весёлое", "Бодрые и яркие жанры"),
    LOUD("Громкое", "Энергичное и драйвовое"),
    SAD("Грустное", "Тише и лиричнее"),
    CRY("Поплакать", "Медленное и пронзительное"),
}

// Plain substring matching on the free-text genre tag — approximate on purpose, since MediaStore
// genres are whatever the file's own tags say, with no fixed taxonomy. A genre can legitimately
// match more than one mood (e.g. "blues rock" is both sad-ish and loud-ish); that's fine, moods
// aren't meant to be mutually exclusive buckets.
private val CRY_KEYWORDS = listOf(
    "piano", "classical", "классик", "ambient", "orchestral", "soundtrack", "ost",
    "instrumental", "инструментал", "elegy", "requiem", "neoclassical",
)
private val SAD_KEYWORDS = listOf(
    "ballad", "баллада", "blues", "блюз", "acoustic", "акустик", "singer-songwriter",
    "folk", "фолк", "emo", "lo-fi", "lofi", "sad", "грустн", "melanch", "меланхол",
)
private val LOUD_KEYWORDS = listOf(
    "rock", "рок", "metal", "метал", "punk", "панк", "hardcore", "dubstep",
    "drum and bass", "dnb", "electronic", "электрон", "edm", "techno", "техно",
    "trance", "транс", "hardstyle", "rap", "рэп", "hip-hop", "hip hop", "хип-хоп", "industrial",
    "alternative", "альтернатив",
)
private val HAPPY_KEYWORDS = listOf(
    "pop", "поп", "dance", "disco", "диско", "funk", "фанк", "reggae", "регги",
    "ska", "ска", "k-pop", "j-pop", "city pop", "house", "tropical", "latin", "summer",
    "electro", "top 40",
)

fun matchesMood(genre: String?, mood: Mood): Boolean {
    if (mood == Mood.NORMAL) return true
    if (genre.isNullOrBlank()) return false
    val g = genre.lowercase()
    val keywords = when (mood) {
        Mood.NORMAL -> return true
        Mood.CRY -> CRY_KEYWORDS
        Mood.SAD -> SAD_KEYWORDS
        Mood.LOUD -> LOUD_KEYWORDS
        Mood.HAPPY -> HAPPY_KEYWORDS
    }
    return keywords.any { g.contains(it) }
}

/** Songs matching the mood's genre keywords OR sitting in one of the user's hand-picked mood
 * folders, shuffled. Falls back to the whole library shuffled when nothing matches at all
 * (missing/unrecognized genre tags and no folders picked), so a tap always plays something. */
fun songsForMood(allSongs: List<Song>, mood: Mood, folderWhitelist: Set<String> = emptySet()): List<Song> {
    val matched = allSongs.filter { song ->
        matchesMood(song.genre, mood) || (song.folder != null && song.folder in folderWhitelist)
    }
    return matched.ifEmpty { allSongs }.shuffled()
}
