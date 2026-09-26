package com.artemiy.player.lyrics

import android.os.Build
import com.atilika.kuromoji.ipadic.Tokenizer
import java.lang.Character.UnicodeScript

/**
 * Adds a Latin-script reading to lyric lines written in non-Latin scripts: Japanese (Kuromoji
 * morphological analysis → katakana reading → Hepburn), Korean (own Revised Romanization), and
 * everything else — Chinese, Hindi, Greek, Arabic... — through Android's built-in ICU
 * transliterator. Latin and Cyrillic text is deliberately never touched. Fully offline.
 */
object LyricsRomanizer {

    private val SKIPPED_SCRIPTS = setOf(UnicodeScript.LATIN, UnicodeScript.CYRILLIC, UnicodeScript.COMMON, UnicodeScript.INHERITED)

    /** Loads a ~12MB dictionary, so only built the first time a Japanese song actually shows up. */
    private val tokenizer by lazy { Tokenizer() }

    fun romanize(lyrics: ParsedLyrics): ParsedLyrics = when (lyrics) {
        is ParsedLyrics.Synced -> {
            // Kanji-only lines are ambiguous (Chinese or Japanese?) — decided per song: any kana
            // anywhere in the lyrics means the whole song is Japanese.
            val japaneseSong = lyrics.lines.any { hasKana(it.text) }
            if (lyrics.lines.none { needsRomanization(it.text) || it.secondary.any { s -> needsRomanization(s.text) } }) {
                lyrics
            } else {
                ParsedLyrics.Synced(
                    lyrics.lines.map { line ->
                        romanizeLine(line, japaneseSong).copy(
                            // Only the other singer's simultaneous part — translations under a
                            // line stay as they are.
                            secondary = line.secondary.map { s -> if (s.voice != null) romanizeLine(s, japaneseSong) else s },
                        )
                    },
                )
            }
        }
        is ParsedLyrics.Unsynced -> {
            val japaneseSong = hasKana(lyrics.text)
            val ruby = lyrics.text.lines().map { rubyFor(it, japaneseSong) }
            if (ruby.all { it == null }) lyrics else lyrics.copy(rubyLines = ruby)
        }
    }

    private enum class Language { JAPANESE, KOREAN, CHINESE, OTHER }

    private fun languageOf(text: String, japaneseSong: Boolean): Language = when {
        hasKana(text) || (hasScript(text, UnicodeScript.HAN) && japaneseSong) -> Language.JAPANESE
        hasScript(text, UnicodeScript.HANGUL) -> Language.KOREAN
        hasScript(text, UnicodeScript.HAN) -> Language.CHINESE
        else -> Language.OTHER
    }

    /** Word-synced lines get one reading per timed word (so it can sit right above that word);
     * other lines get cut into word-sized pieces here. */
    private fun romanizeLine(line: LyricLine, japaneseSong: Boolean): LyricLine {
        if (!needsRomanization(line.text)) return line
        val words = line.words
        return if (words != null) {
            val language = languageOf(line.text, japaneseSong)
            val readings = words.map { word -> reading(word.text, runCatching { readingOf(word.text.trim(), language) }.getOrNull()) }
            if (readings.all { it == null }) line else line.copy(wordReadings = readings)
        } else {
            line.copy(ruby = rubyFor(line.text, japaneseSong))
        }
    }

    /** Reading for one timed word. Latin/Cyrillic words glued into the same timed chunk (e.g.
     * "클락션 Don't even") are left out — they're already readable right below. */
    private fun readingOf(text: String, language: Language): String? {
        if (language == Language.JAPANESE) {
            return japaneseSegments(text).mapNotNull { segment -> segment.reading?.takeIf { needsRomanization(segment.text) } }.joinToString("")
        }
        val foreignPart = WORD.findAll(text).map { it.value.trim() }.filter { needsRomanization(it) }.joinToString(" ")
        return when (language) {
            Language.KOREAN -> romanizeKorean(foreignPart)
            else -> transliterate(foreignPart)
        }
    }

    private fun rubyFor(text: String, japaneseSong: Boolean): List<RubySegment>? {
        if (!needsRomanization(text)) return null
        val segments = runCatching {
            when (languageOf(text, japaneseSong)) {
                Language.JAPANESE -> japaneseSegments(text)
                Language.KOREAN -> WORD.findAll(text).map { RubySegment(it.value, romanizeKorean(it.value.trim())) }.toList()
                // One reading per character — each hanzi is its own syllable.
                Language.CHINESE -> chineseSegments(text)
                Language.OTHER -> WORD.findAll(text).map { RubySegment(it.value, transliterate(it.value.trim())) }.toList()
            }
        }.getOrNull() ?: return null
        val cleaned = segments.map { it.copy(reading = reading(it.text, it.reading)) }
        return cleaned.takeIf { list -> list.any { it.reading != null } }
    }

    /** A word plus the space after it — how Korean, Hindi etc. are cut into ruby pieces. */
    private val WORD = Regex("\\S+\\s*")

    /** Null when there's nothing worth showing: no reading, or a piece that isn't in a
     * romanizable script at all (an English word inside a Japanese line). */
    private fun reading(text: String, reading: String?): String? {
        val normalized = reading?.replace(Regex("\\s+"), " ")?.trim() ?: return null
        if (normalized.isEmpty() || !needsRomanization(text) || normalized.equals(text.trim(), ignoreCase = true)) return null
        return normalized
    }

    private fun needsRomanization(text: String): Boolean =
        text.any { it.isLetter() && UnicodeScript.of(it.code) !in SKIPPED_SCRIPTS }

    private fun hasScript(text: String, script: UnicodeScript): Boolean = text.any { UnicodeScript.of(it.code) == script }

    private fun hasKana(text: String): Boolean =
        hasScript(text, UnicodeScript.HIRAGANA) || hasScript(text, UnicodeScript.KATAKANA)

    private fun transliterate(text: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return android.icu.text.Transliterator.getInstance("Any-Latin").transliterate(text)
    }

    private fun chineseSegments(text: String): List<RubySegment> {
        val segments = mutableListOf<RubySegment>()
        val other = StringBuilder()
        for (c in text) {
            if (UnicodeScript.of(c.code) == UnicodeScript.HAN) {
                if (other.isNotEmpty()) segments += RubySegment(other.toString(), null).also { other.clear() }
                segments += RubySegment(c.toString(), transliterate(c.toString()))
            } else {
                other.append(c)
            }
        }
        if (other.isNotEmpty()) segments += RubySegment(other.toString(), null)
        return segments
    }

    // ---- Japanese ----

    /** Kuromoji's tokens, with endings/particles glued onto the word they belong to so each
     * piece is a readable word ("yonde", "tabeta"), each with its Hepburn reading. */
    private fun japaneseSegments(text: String): List<RubySegment> {
        val segments = mutableListOf<RubySegment>()
        val tokens = tokenizer.tokenize(text)
        tokens.forEachIndexed { i, token ->
            val surface = token.surface
            if (surface.isBlank()) {
                // Keep the original spacing — it belongs to the piece before it.
                if (segments.isNotEmpty()) segments[segments.lastIndex] = segments.last().let { it.copy(text = it.text + surface) }
                else segments += RubySegment(surface, null)
                return@forEachIndexed
            }
            val pos1 = token.partOfSpeechLevel1
            val pos2 = token.partOfSpeechLevel2
            val piece = when {
                pos1 == "記号" -> JAPANESE_PUNCTUATION[surface] ?: surface
                pos1 == "助詞" && surface == "は" -> "wa"
                pos1 == "助詞" && surface == "へ" -> "e"
                else -> {
                    val reading = token.reading?.takeIf { it.isNotEmpty() && it != "*" } ?: surface
                    val next = tokens.getOrNull(i + 1)?.reading?.takeIf { it != "*" }
                    kanaToRomaji(reading, next)
                }
            }
            // Verb/adjective endings, suffixes and punctuation read as part of the word before
            // them ("tabeta", "yonde", "kimitachi", "yo,"); a copula after a noun stays its own
            // word ("gakusei desu", not "gakuseidesu").
            val previousPos1 = tokens.getOrNull(i - 1)?.partOfSpeechLevel1
            val attach = pos1 == "記号" || pos2 == "接尾" || (pos1 == "助詞" && pos2 == "接続助詞") ||
                (pos1 == "助動詞" && previousPos1 in setOf("動詞", "形容詞", "助動詞"))
            if (attach && segments.isNotEmpty()) {
                val last = segments.last()
                segments[segments.lastIndex] = RubySegment(last.text + surface, (last.reading ?: "") + piece)
            } else {
                segments += RubySegment(surface, piece)
            }
        }
        return segments
    }

    private val JAPANESE_PUNCTUATION = mapOf(
        "、" to ",", "。" to ".", "！" to "!", "？" to "?", "「" to "\"", "」" to "\"",
        "『" to "\"", "』" to "\"", "…" to "...", "・" to " ", "〜" to "~", "～" to "~",
    )

    private val KANA_DIGRAPHS = mapOf(
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo", "シャ" to "sha", "シュ" to "shu", "シェ" to "she", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チェ" to "che", "チョ" to "cho", "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo", "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo", "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジェ" to "je", "ジョ" to "jo", "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo", "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo", "ティ" to "ti", "ディ" to "di", "トゥ" to "tu",
        "ドゥ" to "du", "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo", "ヴァ" to "va", "ヴィ" to "vi", "ヴェ" to "ve",
        "ヴォ" to "vo", "ツァ" to "tsa", "ツェ" to "tse", "ツォ" to "tso", "イェ" to "ye",
    )

    private val KANA = mapOf(
        'ア' to "a", 'イ' to "i", 'ウ' to "u", 'エ' to "e", 'オ' to "o",
        'カ' to "ka", 'キ' to "ki", 'ク' to "ku", 'ケ' to "ke", 'コ' to "ko",
        'サ' to "sa", 'シ' to "shi", 'ス' to "su", 'セ' to "se", 'ソ' to "so",
        'タ' to "ta", 'チ' to "chi", 'ツ' to "tsu", 'テ' to "te", 'ト' to "to",
        'ナ' to "na", 'ニ' to "ni", 'ヌ' to "nu", 'ネ' to "ne", 'ノ' to "no",
        'ハ' to "ha", 'ヒ' to "hi", 'フ' to "fu", 'ヘ' to "he", 'ホ' to "ho",
        'マ' to "ma", 'ミ' to "mi", 'ム' to "mu", 'メ' to "me", 'モ' to "mo",
        'ヤ' to "ya", 'ユ' to "yu", 'ヨ' to "yo",
        'ラ' to "ra", 'リ' to "ri", 'ル' to "ru", 'レ' to "re", 'ロ' to "ro",
        'ワ' to "wa", 'ヰ' to "i", 'ヱ' to "e", 'ヲ' to "o", 'ン' to "n",
        'ガ' to "ga", 'ギ' to "gi", 'グ' to "gu", 'ゲ' to "ge", 'ゴ' to "go",
        'ザ' to "za", 'ジ' to "ji", 'ズ' to "zu", 'ゼ' to "ze", 'ゾ' to "zo",
        'ダ' to "da", 'ヂ' to "ji", 'ヅ' to "zu", 'デ' to "de", 'ド' to "do",
        'バ' to "ba", 'ビ' to "bi", 'ブ' to "bu", 'ベ' to "be", 'ボ' to "bo",
        'パ' to "pa", 'ピ' to "pi", 'プ' to "pu", 'ペ' to "pe", 'ポ' to "po",
        'ヴ' to "vu", 'ァ' to "a", 'ィ' to "i", 'ゥ' to "u", 'ェ' to "e", 'ォ' to "o",
        'ャ' to "ya", 'ュ' to "yu", 'ョ' to "yo", 'ヮ' to "wa", 'ヵ' to "ka", 'ヶ' to "ke",
    )

    private fun toKatakana(text: String): String = buildString {
        for (c in text) append(if (c in 'ぁ'..'ゖ') (c + 0x60) else c)
    }

    /** Hepburn. `ッ` doubles the next consonant (looking into [nextReading] when it ends the
     * token), `ー` repeats the previous vowel. Anything that isn't kana passes through as-is. */
    private fun kanaToRomaji(reading: String, nextReading: String?): String {
        val kana = toKatakana(reading)
        val out = StringBuilder()
        var i = 0
        var geminate = false
        while (i < kana.length) {
            val c = kana[i]
            if (c == 'ッ') {
                geminate = true
                i++
                continue
            }
            if (c == 'ー') {
                out.lastOrNull { it in "aeiou" }?.let { out.append(it) }
                i++
                continue
            }
            val pair = if (i + 1 < kana.length) KANA_DIGRAPHS[kana.substring(i, i + 2)] else null
            val syllable = pair ?: KANA[c] ?: c.toString()
            if (geminate) {
                out.append(if (syllable.startsWith("ch")) "t" else syllable.first().takeIf { it !in "aeiou" }?.toString() ?: "")
                geminate = false
            }
            out.append(syllable)
            i += if (pair != null) 2 else 1
        }
        if (geminate && nextReading != null) {
            val next = kanaToRomaji(nextReading, null)
            next.firstOrNull()?.takeIf { it !in "aeiou" }?.let { out.append(if (next.startsWith("ch")) 't' else it) }
        }
        return out.toString()
    }

    // ---- Korean (Revised Romanization, with the common sound-change rules) ----

    private val INITIALS = arrayOf("g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h")
    private val MEDIALS = arrayOf("a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i")

    /** Final consonant as it sounds at the end of a syllable. */
    private val FINALS = arrayOf("", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l", "p", "l", "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t")

    /** Final split for liaison into a following silent ㅇ: what stays + what moves over. */
    private val LIAISON = arrayOf(
        "" to "", "" to "g", "" to "kk", "k" to "s", "" to "n", "n" to "j", "n" to "", "" to "d", "" to "r",
        "l" to "g", "l" to "m", "l" to "b", "l" to "s", "l" to "t", "l" to "p", "l" to "", "" to "m", "" to "b",
        "p" to "s", "" to "s", "" to "ss", "ng" to "", "" to "j", "" to "ch", "" to "k", "" to "t", "" to "p", "" to "",
    )

    private fun romanizeKorean(text: String): String {
        val out = StringBuilder()
        // Set when ㄹ+ㄹ was already written as "ll" — the next syllable must not add its own "r".
        var skipNextInitial = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c !in '가'..'힣') {
                out.append(c)
                i++
                continue
            }
            val index = c - '가'
            val initial = index / (21 * 28)
            val medial = (index % (21 * 28)) / 28
            val final = index % 28
            if (!skipNextInitial) out.append(INITIALS[initial])
            skipNextInitial = false
            out.append(MEDIALS[medial])

            val next = text.getOrNull(i + 1)?.takeIf { it in '가'..'힣' }
            val nextInitial = next?.let { (it - '가') / (21 * 28) }
            when {
                final == 0 -> {}
                // Next syllable starts with a silent ㅇ: the final carries over into it.
                nextInitial == 11 && final != 21 -> {
                    val (stay, move) = LIAISON[final]
                    out.append(stay).append(move)
                }
                nextInitial == 5 && FINALS[final] == "l" -> {
                    out.append("ll")
                    skipNextInitial = true
                }
                nextInitial == 2 || nextInitial == 6 -> out.append(
                    when (FINALS[final]) {
                        "k" -> "ng"
                        "t" -> "n"
                        "p" -> "m"
                        else -> FINALS[final]
                    },
                )
                else -> out.append(FINALS[final])
            }
            i++
        }
        return out.toString()
    }
}
