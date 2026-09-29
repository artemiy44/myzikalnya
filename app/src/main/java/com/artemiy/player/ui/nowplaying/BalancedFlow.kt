package com.artemiy.player.ui.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * Lays a lyric line's words out in rows the way a person would set them: as few rows as the line
 * needs, broken where it reads best — rows of about the same length, a break after a comma rather
 * than after "the", never a single word left alone on the last row if it can be helped.
 *
 * The words ([content], one child per word, [words] their texts in the same order) are measured
 * as they really are — readings above kanji included — so nothing is estimated. Each row lines
 * its words up on their text's baseline; [alignEnd] sets rows against the right edge.
 */
@Composable
internal fun BalancedFlow(
    words: List<String>,
    alignEnd: Boolean,
    rowGap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val placeables = measurables.map { it.measure(Constraints(maxWidth = maxWidth)) }
        val rows = if (constraints.hasBoundedWidth) breakRows(placeables.map { it.width }, words, maxWidth) else listOf(placeables.indices)
        val gap = rowGap.roundToPx()

        // Per row: how far its baseline sits from the row's top, and the row's height.
        class Row(val range: IntRange, val ascent: Int, val height: Int, val width: Int)
        val laid = rows.map { range ->
            var ascent = 0
            var descent = 0
            var width = 0
            for (i in range) {
                val p = placeables[i]
                val base = p[LastBaseline].takeIf { it != AlignmentLine.Unspecified } ?: p.height
                ascent = maxOf(ascent, base)
                descent = maxOf(descent, p.height - base)
                width += p.width
            }
            Row(range, ascent, ascent + descent, width)
        }
        val width = if (constraints.hasBoundedWidth) maxWidth else laid.maxOfOrNull { it.width } ?: 0
        val height = laid.sumOf { it.height } + gap * (laid.size - 1).coerceAtLeast(0)
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            for (row in laid) {
                var x = if (alignEnd) width - row.width else 0
                for (i in row.range) {
                    val p = placeables[i]
                    val base = p[LastBaseline].takeIf { it != AlignmentLine.Unspecified } ?: p.height
                    p.place(x, y + row.ascent - base)
                    x += p.width
                }
                y += row.height + gap
            }
        }
    }
}

/**
 * Where to break a line of words [widths] wide (in that order) into rows no wider than [maxWidth]:
 * the fewest rows it can take, then of all the ways to split it into that many, the one scoring
 * best (see [rowCost] and [breakCost]).
 */
internal fun breakRows(widths: List<Int>, words: List<String>, maxWidth: Int): List<IntRange> {
    val n = widths.size
    if (n == 0) return emptyList()
    if (widths.sum() <= maxWidth) return listOf(0 until n)

    val rowCount = greedyRows(widths, maxWidth).size
    val target = widths.sum().toFloat() / rowCount
    // best[k][i]: the cheapest way to set the first i words in k rows; from[k][i]: where its last row starts.
    val inf = Float.MAX_VALUE
    val best = Array(rowCount + 1) { FloatArray(n + 1) { inf } }
    val from = Array(rowCount + 1) { IntArray(n + 1) }
    best[0][0] = 0f
    for (k in 1..rowCount) {
        for (i in 1..n) {
            var rowWidth = 0
            for (start in i - 1 downTo 0) {
                rowWidth += widths[start]
                // A word too wide for any row still gets a row of its own.
                if (rowWidth > maxWidth && start < i - 1) break
                val before = best[k - 1][start]
                if (before == inf) continue
                val isLast = i == n
                var cost = before + rowCost(rowWidth, target, maxWidth, i - start, n, rowCount, isLast)
                if (!isLast) cost += breakCost(words.getOrNull(i - 1))
                if (cost < best[k][i]) {
                    best[k][i] = cost
                    from[k][i] = start
                }
            }
        }
    }
    if (best[rowCount][n] == inf) return greedyRows(widths, maxWidth)
    val rows = ArrayList<IntRange>(rowCount)
    var end = n
    for (k in rowCount downTo 1) {
        val start = from[k][end]
        rows.add(0, start until end)
        end = start
    }
    return rows
}

private fun greedyRows(widths: List<Int>, maxWidth: Int): List<IntRange> {
    val rows = ArrayList<IntRange>()
    var start = 0
    var width = 0
    for (i in widths.indices) {
        if (i > start && width + widths[i] > maxWidth) {
            rows.add(start until i)
            start = i
            width = 0
        }
        width += widths[i]
    }
    rows.add(start until widths.size)
    return rows
}

/** Rows of about equal length read best; a word left alone on a row — worst of all the last
 * one — looks stranded, when there are enough words to avoid it. */
private fun rowCost(width: Int, target: Float, maxWidth: Int, words: Int, total: Int, rowCount: Int, isLast: Boolean): Float {
    val off = (width - target) / maxWidth
    var cost = off * off * 10f
    if (words == 1 && total >= rowCount + 2) cost += if (isLast) LONELY_LAST_WORD else LONELY_WORD
    return cost
}

/** Breaking right after a comma or a full stop follows the phrasing; after "the" or "и" it cuts a
 * phrase in half. */
private fun breakCost(word: String?): Float {
    val w = word?.trim() ?: return 0f
    if (w.isEmpty()) return 0f
    if (w.last() in PHRASE_END) return -PHRASE_BREAK_BONUS
    return if (w.lowercase() in CLINGING_WORDS) CLINGING_WORD_COST else 0f
}

private const val LONELY_WORD = 0.8f
private const val LONELY_LAST_WORD = 1.2f
private const val PHRASE_BREAK_BONUS = 0.6f
private const val CLINGING_WORD_COST = 0.5f

private const val PHRASE_END = ",.;:!?…、。，！？」』)）"

/** Short words that belong with the word after them, so a row shouldn't end on one. */
private val CLINGING_WORDS = setOf(
    "a", "an", "the", "of", "to", "and", "or", "but", "in", "on", "at", "for", "with", "from", "by",
    "my", "your", "his", "her", "our", "their", "its", "i", "i'm", "i've", "i'll", "i'd", "is", "am",
    "are", "was", "be", "that", "this", "as", "so", "if", "no", "not",
    "и", "а", "но", "в", "во", "на", "с", "со", "к", "ко", "по", "из", "у", "о", "об", "за", "от",
    "до", "не", "ни", "я", "ты", "мы", "что", "как", "же",
)
