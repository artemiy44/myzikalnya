package com.artemiy.player.lyrics

data class LyricWord(val timeMs: Long, val text: String)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord>? = null,
    /** End time of the last word, when the source tagged one (e.g. eLRC's trailing `<mm:ss.xx>`). */
    val endTimeMs: Long? = null,
    /** Other line(s) tagged at this exact same timestamp — a translation line, or a second
     * singer's part (v1/v2). Rendered as smaller "comment" text under this line instead of
     * competing with it for which one is "active". */
    val secondary: List<LyricLine> = emptyList(),
)

sealed class ParsedLyrics {
    data class Synced(val lines: List<LyricLine>) : ParsedLyrics()
    data class Unsynced(val text: String) : ParsedLyrics()
}
