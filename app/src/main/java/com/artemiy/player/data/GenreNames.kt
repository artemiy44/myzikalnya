package com.artemiy.player.data

import java.text.Normalizer

/**
 * One genre however its tags spell it — the way [ArtistNames] does for artists. "Alternative",
 * "Alt" and the French "Alternatif et Indé" are one genre, "Drum & Bass" / "DnB" / "Drum'n'Bass"
 * another; the tag in a song's own file is never touched (Song Info still shows it as written).
 *
 * Genres the dictionary below doesn't know still merge when they differ only in case, spacing,
 * punctuation or accents ("J-Pop", "jpop", "J Pop"), and they're shown in the spelling the
 * library uses most. Only clear variants of the *same* genre are listed here: a subgenre with a
 * character of its own (Deep House, Hard Rock, Trance) stays a genre of its own.
 *
 * To teach it more, add an [Entry]: the name to show, then every other way it's written.
 */
object GenreNames {
    private class Entry(val main: String, vararg val aliases: String)

    private val ENTRIES = listOf(
        Entry("Alternative", "Alt", "Alternatif", "Alternatif et Indé", "Alternative & Indie", "Alternative and Indie", "Alternative Rock", "Alt Rock", "Alt-Rock", "Альтернатива", "Альтернативный рок", "Альтернативный"),
        Entry("Electronic", "Electronica", "Electronique", "Électronique", "Elektronik", "Elektronische Musik", "Electronic Music", "Электроника", "Электронная музыка", "Электронный", "Электричка"),
        Entry("Hip-Hop", "Hip Hop", "Hiphop", "Hip-Hop/Rap", "Rap", "Хип-хоп", "Хипхоп", "Рэп", "Рэп и хип-хоп"),
        Entry("R&B", "RnB", "R and B", "R'n'B", "Rhythm and Blues", "Rhythm & Blues"),
        Entry("Pop", "Pop Music", "Поп", "Поп-музыка", "Популярная музыка"),
        Entry("J-Pop", "JPop", "J Pop", "Japanese Pop", "Джей-поп", "Джей поп"),
        Entry("K-Pop", "KPop", "K Pop", "Korean Pop", "Кей-поп"),
        Entry("Rock", "Rock Music", "Рок", "Рок-музыка"),
        Entry("Metal", "Heavy Metal", "Металл", "Хэви-метал", "Хеви-метал"),
        Entry("Punk", "Punk Rock", "Панк", "Панк-рок"),
        Entry("Drum & Bass", "Drum and Bass", "Drum'n'Bass", "Drum N Bass", "DnB", "D&B", "D'n'B", "Drum n Bass", "Драм-н-бейс"),
        Entry("Hardstyle", "Hard Style"),
        Entry("Indie", "Инди"),
        Entry("Classical", "Classic", "Classical Music", "Классика", "Классическая музыка", "Классическая"),
        Entry("Jazz", "Джаз"),
        Entry("Blues", "Блюз"),
        Entry("Folk", "Folk Music", "Фолк", "Фолк-музыка", "Народная"),
        Entry("Country", "Country Music", "Кантри"),
        Entry("Soundtrack", "OST", "Original Soundtrack", "Саундтрек", "Саундтреки", "Soundtracks"),
        Entry("Anime", "Anison", "Аниме", "アニメ"),
        Entry("Vocaloid", "Vocaloud", "Vocalo", "Вокалоид", "ボーカロイド"),
        Entry("Ambient", "Эмбиент"),
        Entry("Lo-Fi", "Lofi", "Lo Fi", "Лоу-фай"),
        Entry("Reggae", "Регги"),
    )

    /** key of any known spelling → key of the genre's main name. */
    private val aliasToMain: Map<String, String> = buildMap {
        for (e in ENTRIES) {
            val mainKey = key(e.main)
            put(mainKey, mainKey)
            for (alias in e.aliases) put(key(alias), mainKey)
        }
    }

    /** key of the genre's main name → how it's shown. */
    private val mainNames: Map<String, String> = ENTRIES.associate { key(it.main) to it.main }

    /** A spelling with case, spacing, punctuation and (Latin) accents stripped — equal for variants of one thing. */
    fun key(raw: String): String {
        val sb = StringBuilder()
        for (ch in Normalizer.normalize(raw, Normalizer.Form.NFKC).lowercase()) {
            // "é" → "e", but Cyrillic "й" must stay "й".
            val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD).first()
            sb.append(if (Character.UnicodeScript.of(base.code) == Character.UnicodeScript.LATIN) base else ch)
        }
        return sb.filter { it.isLetterOrDigit() }.toString()
    }

    /** The genre [raw] belongs to: the key its spellings share (empty = nothing to go by). */
    fun canonicalKey(raw: String): String = key(raw).let { aliasToMain[it] ?: it }

    /** The genre of [song] (its first tag's), or null without one. */
    fun keyOf(song: Song): String? = primaryGenre(song)?.let(::canonicalKey)?.takeIf { it.isNotEmpty() }

    /** How a genre is shown: its main name when the dictionary has one, else the spelling [spellings] use most. */
    fun displayName(canonicalKey: String, spellings: List<String>): String =
        mainNames[canonicalKey]
            ?: spellings.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: canonicalKey
}
