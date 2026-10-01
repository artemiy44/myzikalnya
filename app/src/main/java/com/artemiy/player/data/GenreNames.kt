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
        // — main families —
        Entry("Alternative", "Alt", "Alt Rock", "Alternative Rock", "Alternatif", "Alternatif et Indé", "Alternative & Indie", "Alternative and Indie", "Alternativ", "Alternativ und Indie", "Alternativ & Indie", "Alternativo", "Alternativa", "Adult Alternative", "Альтернатива", "Альтернативный рок", "Альтернативный", "オルタナティブ", "얼터너티브"),
        Entry("Electronic", "Electronica", "Electro", "Electronique", "Électronique", "Elektronik", "Elektronische Musik", "Elektro", "Electrónica", "Electrónico", "Elettronica", "Eletrônica", "Electronic Music", "Электроника", "Электронная музыка", "Электронный", "Электро", "Электричка", "エレクトロニカ", "電子音楽", "전자음악"),
        Entry("Hip-Hop", "Hip Hop", "Hiphop", "Hip-Hop/Rap", "Hip Hop & Rap", "Rap", "Korean Hip-Hop", "K-Hip Hop", "Japanese Hip-Hop", "French Hip-Hop", "Rap Français", "Deutschrap", "Deutscher Hip-Hop", "Russian Hip-Hop", "Russian Rap", "Русский рэп", "Русский хип-хоп", "Хип-хоп", "Хипхоп", "Рэп", "Рэп и хип-хоп", "ヒップホップ", "힙합"),
        Entry("R&B", "RnB", "R and B", "R'n'B", "Rhythm and Blues", "Rhythm & Blues", "リズム&ブルース", "알앤비"),
        Entry("Soul", "Соул", "ソウル"),
        Entry("Pop", "Pop Music", "Popmusik", "Música Pop", "Поп", "Поп-музыка", "Популярная музыка", "ポップス", "팝"),
        Entry("J-Pop", "JPop", "J Pop", "Japanese Pop", "Джей-поп", "Джей поп", "Jポップ"),
        Entry("K-Pop", "KPop", "K Pop", "Korean Pop", "Кей-поп", "케이팝"),
        Entry("Rock", "Rock Music", "Rock in Russian", "Russian Rock", "Русский рок", "Рок", "Рок-музыка", "General Rock", "General Mainstream Rock", "Mainstream Rock", "Rock Russe", "Rock Français", "Deutschrock", "Rock en Español", "Rock Nacional", "Rock Brasileiro", "ロック", "록"),
        Entry("Hard Rock", "Hardrock", "Хард-рок", "Хард рок"),
        Entry("Metal", "Heavy Metal", "Canadian Metal", "Metal Music", "Métal", "Металл", "Хэви-метал", "Хеви-метал", "メタル", "메탈"),
        Entry("Punk", "Punk Rock", "Панк", "Панк-рок", "パンク", "펑크"),
        Entry("Pop Punk", "Поп-панк"),
        Entry("Indie", "Indie Music", "Инди", "インディー"),
        Entry("Drum & Bass", "Drum and Bass", "Drum'n'Bass", "Drum N Bass", "DnB", "D&B", "D'n'B", "Atmospheric Drum & Bass", "Atmospheric Drum and Bass", "Atmospheric DnB", "Liquid Drum & Bass", "Liquid Drum and Bass", "Liquid DnB", "Liquid Funk", "Neurofunk", "Драм-н-бейс", "Драм-энд-бейс", "ドラムンベース"),
        Entry("Hardstyle", "Hard Style", "Hardstyle Techno", "Хардстайл"),
        Entry("Dance", "Eurodance", "Euro Dance", "Dance Music", "Танцевальная", "Танцевальная музыка", "ダンス"),
        Entry("House", "House Music", "Хаус", "ハウス"),
        Entry("Techno", "Техно", "テクノ"),
        Entry("Trance", "Транс", "トランス"),
        Entry("Dubstep", "Dub Step", "Дабстеп"),
        Entry("Synthpop", "Synth Pop", "Synth-Pop", "Синтипоп"),
        // — everything else people write in different ways —
        Entry("Classical", "Classic", "Classical Music", "Klassik", "Klassische Musik", "Classique", "Musique Classique", "Música Clásica", "Clásica", "Música Clássica", "Classica", "Musica Classica", "Классика", "Классическая музыка", "Классическая", "クラシック", "클래식"),
        Entry("Jazz", "Jazz Music", "Джаз", "ジャズ", "재즈"),
        Entry("Blues", "Блюз", "ブルース"),
        Entry("Folk", "Folk Music", "Folklore", "Фолк", "Фолк-музыка", "Народная", "フォーク"),
        Entry("Country", "Country Music", "Кантри", "カントリー"),
        Entry("Reggae", "Регги", "レゲエ"),
        Entry("Latin", "Latin Music", "Latino", "Música Latina", "Latina", "Латино", "Латинская"),
        Entry("Ambient", "Эмбиент", "アンビエント"),
        Entry("Lo-Fi", "Lofi", "Lo Fi", "Лоу-фай"),
        Entry("Soundtrack", "OST", "Original Soundtrack", "Original Motion Picture Soundtrack", "Motion Picture Soundtrack", "Film Score", "Film Music", "Filmmusik", "Bande Originale", "Bande Originale de Film", "Bandes originales de films", "Banda Sonora", "Banda Sonora Original", "Colonna Sonora", "Trilha Sonora", "Саундтрек", "Саундтреки", "Музыка из фильмов", "サウンドトラック", "사운드트랙", "Soundtracks", "Score"),
        Entry("Video Game", "Game", "Games", "Game Music", "Game Soundtrack", "Video Game Music", "Videogame", "Video Games", "VGM", "Videospiel", "Jeux Vidéo", "Видеоигры", "Игры", "ゲーム", "ゲーム音楽", "게임"),
        Entry("Anime", "Anison", "Anime Music", "Аниме", "アニメ", "アニソン"),
        Entry("Vocaloid", "Vocaloud", "Vocalo", "Вокалоид", "ボーカロイド", "ボカロ"),
        Entry("Asian Music", "Musique Asiatique", "Música Asiática", "Asiatische Musik", "Азиатская музыка"),
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

    /** The genre [raw] belongs to: the key its spellings share. A name with no letters at all ("<")
     * is still its own genre, as written. */
    fun canonicalKey(raw: String): String = key(raw).let { aliasToMain[it] ?: it }.ifEmpty { raw.trim().lowercase() }

    /**
     * Every genre of [song], each once, as (key, the tag's own spelling) — a song tagged with
     * several ("Rock/Pop", "Alternative~Indie Pop~Pop") belongs to each of them, the way a song
     * with several artists is listed under each.
     */
    fun entriesOf(song: Song): List<Pair<String, String>> =
        genresOf(song).map { canonicalKey(it) to it }.filter { it.first.isNotEmpty() }.distinctBy { it.first }

    /** The first genre of [song], or null without one. */
    fun keyOf(song: Song): String? = entriesOf(song).firstOrNull()?.first

    /** How a genre is shown: its main name when the dictionary has one, else the spelling [spellings] use most. */
    fun displayName(canonicalKey: String, spellings: List<String>): String =
        mainNames[canonicalKey]
            ?: spellings.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: canonicalKey
}
