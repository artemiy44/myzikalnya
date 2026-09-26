package com.artemiy.player.lyrics

data class LyricWord(val timeMs: Long, val text: String)

/** Which singer a duet line belongs to (eLRC `v1:`/`v2:` prefix). v2 renders right-aligned. */
enum class LyricVoice { V1, V2 }

/** A word-sized piece of a lyric line and its Latin reading, shown above it (null = no reading,
 * e.g. an English word inside a Japanese line). */
data class RubySegment(val text: String, val reading: String?)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord>? = null,
    /** End time of the last word, when the source tagged one (e.g. eLRC's trailing `<mm:ss.xx>`). */
    val endTimeMs: Long? = null,
    /** Other line(s) tagged at this exact same timestamp — a translation line, or the other
     * singer's part when both sing at once. A translation renders as smaller "comment" text under
     * this line; a different-voice line renders full-size on its own side. */
    val secondary: List<LyricLine> = emptyList(),
    /** Null when the file doesn't mark voices at all (renders like v1, on the left). */
    val voice: LyricVoice? = null,
    /** Background vocals sung around this line (eLRC `[bg: ...]` right after it). */
    val background: LyricLine? = null,
    /** Romanization for a line without word timing: the text cut into word-sized pieces, each
     * with its reading. Filled in after parsing by [LyricsRomanizer]; null = nothing to romanize. */
    val ruby: List<RubySegment>? = null,
    /** Romanization for a word-synced line: one reading per entry of [words], same order. */
    val wordReadings: List<String?>? = null,
) {
    val hasRomanization: Boolean get() = ruby != null || wordReadings != null
}

sealed class ParsedLyrics {
    data class Synced(val lines: List<LyricLine>) : ParsedLyrics()

    /** [rubyLines] lines up 1:1 with `text.lines()` when any of them needed romanizing. */
    data class Unsynced(val text: String, val rubyLines: List<List<RubySegment>?>? = null) : ParsedLyrics()
}

fun ParsedLyrics.hasRomanization(): Boolean = when (this) {
    is ParsedLyrics.Synced -> lines.any { line -> line.hasRomanization || line.secondary.any { it.hasRomanization } }
    is ParsedLyrics.Unsynced -> rubyLines != null
}
