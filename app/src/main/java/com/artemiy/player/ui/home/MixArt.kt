package com.artemiy.player.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/*
 * The drawings on the mix cards: plain white shapes at low opacity over the card's gradient, all
 * drawn in code (sharp at any size, nothing to download). Each kind of mix gets its own picture —
 * a big emblem running off the card's edge (a heart, a vinyl, the sun...) or a texture over the
 * whole card (bubbles, zigzags, flowing lines...). Genre mixes pick a picture by the genre's
 * name; genres without one of their own still get a steady, name-seeded one.
 */

private val Ink = Color.White

/** Draws [motif] (see [com.artemiy.player.data.Mix.motif]) over the whole draw area. */
fun DrawScope.drawMixMotif(motif: String, textMeasurer: TextMeasurer) {
    val kind = motif.substringBefore(':')
    val arg = motif.substringAfter(':', "")
    val seed = motif.hashCode()
    when (kind) {
        "favorites" -> drawHearts()
        "repeat" -> drawLoop()
        "artist" -> drawVinyl()
        "fresh" -> drawSprout()
        "forgotten" -> drawEchoRings()
        "deep" -> drawDepthWaves(seed)
        "decade" -> drawDecade(arg, textMeasurer)
        "daypart" -> when (arg) {
            "MORNING" -> drawSunrise()
            "AFTERNOON" -> drawHighSun()
            "EVENING" -> drawSunset()
            else -> drawMoon(seed)
        }
        "genre" -> drawGenre(arg, seed)
        else -> drawBubbles(seed)
    }
}

private val DrawScope.s get() = min(size.width, size.height)
private val DrawScope.w get() = size.width
private val DrawScope.h get() = size.height

private fun ink(alpha: Float) = Ink.copy(alpha = alpha)

// ---------- Mix kinds ----------

private fun heartPath(cx: Float, cy: Float, r: Float) = Path().apply {
    moveTo(cx, cy + r * 0.9f)
    cubicTo(cx - r * 1.7f, cy - r * 0.15f, cx - r * 0.95f, cy - r * 1.35f, cx, cy - r * 0.45f)
    cubicTo(cx + r * 0.95f, cy - r * 1.35f, cx + r * 1.7f, cy - r * 0.15f, cx, cy + r * 0.9f)
    close()
}

/** "Любимое": one big heart running off the right edge, a small outlined one beside it. */
private fun DrawScope.drawHearts() {
    rotate(-14f, Offset(w * 0.78f, h * 0.6f)) {
        drawPath(heartPath(w * 0.78f, h * 0.6f, s * 0.5f), ink(0.22f))
    }
    rotate(12f, Offset(w * 0.2f, h * 0.72f)) {
        drawPath(heartPath(w * 0.2f, h * 0.72f, s * 0.13f), ink(0.35f), style = Stroke(s * 0.025f, join = StrokeJoin.Round))
    }
}

/** "На повторе": a thick looping arrow, with a fainter smaller loop inside it. */
private fun DrawScope.drawLoop() {
    val c = Offset(w * 0.74f, h * 0.58f)
    fun loop(radius: Float, width: Float, alpha: Float) {
        val sweep = 290f
        val start = -60f
        drawArc(
            color = ink(alpha),
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(c.x - radius, c.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width, cap = StrokeCap.Round),
        )
        // Arrowhead at the arc's end, pointing along the direction of travel.
        val end = Math.toRadians((start + sweep).toDouble())
        val tip = Offset(c.x + radius * cos(end).toFloat(), c.y + radius * sin(end).toFloat())
        val tangent = Offset(-sin(end).toFloat(), cos(end).toFloat())
        val normal = Offset(cos(end).toFloat(), sin(end).toFloat())
        val head = width * 1.6f
        drawPath(Path().apply {
            moveTo(tip.x + tangent.x * head * 1.1f, tip.y + tangent.y * head * 1.1f)
            lineTo(tip.x + normal.x * head, tip.y + normal.y * head)
            lineTo(tip.x - normal.x * head, tip.y - normal.y * head)
            close()
        }, ink(alpha))
    }
    loop(s * 0.4f, s * 0.09f, 0.24f)
    loop(s * 0.2f, s * 0.05f, 0.14f)
}

/** Artist mixes: a vinyl record half off the right edge — grooves, label, spindle hole. */
private fun DrawScope.drawVinyl() {
    val c = Offset(w * 0.9f, h * 0.6f)
    val r = s * 0.6f
    drawCircle(ink(0.16f), r, c)
    var groove = r * 0.42f
    while (groove < r * 0.96f) {
        drawCircle(ink(0.1f), groove, c, style = Stroke(s * 0.006f))
        groove += r * 0.07f
    }
    drawCircle(ink(0.3f), r * 0.32f, c)
    drawCircle(Color.Black.copy(alpha = 0.22f), r * 0.05f, c)
}

private fun leafPath(base: Offset, length: Float, angleDeg: Float): Path {
    val a = Math.toRadians(angleDeg.toDouble())
    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
    val side = Offset(-dir.y, dir.x)
    val tip = base + dir * length
    val mid = base + dir * (length * 0.5f)
    return Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(mid.x + side.x * length * 0.45f, mid.y + side.y * length * 0.45f, tip.x, tip.y)
        quadraticTo(mid.x - side.x * length * 0.45f, mid.y - side.y * length * 0.45f, base.x, base.y)
        close()
    }
}

/** "Новое, не опробованное": a sprout coming up from the bottom edge. */
private fun DrawScope.drawSprout() {
    val bottom = Offset(w * 0.7f, h * 1.02f)
    val top = Offset(w * 0.66f, h * 0.5f)
    drawPath(Path().apply {
        moveTo(bottom.x, bottom.y)
        quadraticTo(w * 0.76f, h * 0.72f, top.x, top.y)
    }, ink(0.28f), style = Stroke(s * 0.045f, cap = StrokeCap.Round))
    drawPath(leafPath(top, s * 0.42f, -150f), ink(0.26f))
    drawPath(leafPath(top, s * 0.5f, -35f), ink(0.2f))
    drawPath(leafPath(Offset(w * 0.72f, h * 0.76f), s * 0.28f, 10f), ink(0.16f))
}

/** "Давно не слушал": rings spreading from a corner and fading out, like an echo. */
private fun DrawScope.drawEchoRings() {
    val c = Offset(w * 1.02f, h * 1.02f)
    for (i in 0 until 7) {
        val alpha = 0.34f - i * 0.045f
        drawCircle(ink(alpha), s * (0.22f + i * 0.2f), c, style = Stroke(s * 0.035f))
    }
}

/** "Глубокие треки": wave bands stacking up toward the bottom — deeper = denser. */
private fun DrawScope.drawDepthWaves(seed: Int) {
    val random = Random(seed)
    for (i in 0 until 5) {
        val top = h * (0.42f + i * 0.12f)
        val amp = s * (0.035f + random.nextFloat() * 0.03f)
        val phase = random.nextFloat() * 2f * PI.toFloat()
        val period = w * (0.7f + random.nextFloat() * 0.5f)
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w + 4f) {
                lineTo(x, top + amp * sin(2f * PI.toFloat() * x / period + phase))
                x += 4f
            }
            lineTo(w, h)
            close()
        }
        drawPath(path, ink(0.09f))
    }
}

/** Decade mixes: the decade itself, huge, running off the bottom-right. */
private fun DrawScope.drawDecade(year: String, textMeasurer: TextMeasurer) {
    val label = year.toIntOrNull()?.let { "%02ds".format(it % 100) } ?: year
    val layout = textMeasurer.measure(
        label,
        TextStyle(fontSize = (h * 0.42f / density).sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp),
    )
    rotate(-8f, Offset(w * 0.5f, h * 0.6f)) {
        drawText(layout, color = ink(0.2f), topLeft = Offset(w - layout.size.width * 0.82f, h * 0.36f))
    }
}

// ---------- Time of day ----------

private fun DrawScope.drawRays(c: Offset, from: Float, to: Float, count: Int, width: Float, alpha: Float) {
    for (i in 0 until count) {
        val a = 2 * PI * i / count
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        drawLine(ink(alpha), c + dir * from, c + dir * to, strokeWidth = width, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawSunrise() {
    val c = Offset(w * 0.68f, h * 0.86f)
    val r = s * 0.27f
    drawCircle(ink(0.28f), r, c)
    drawRays(c, r * 1.3f, r * 1.75f, 14, s * 0.035f, 0.24f)
}

private fun DrawScope.drawHighSun() {
    val c = Offset(w * 0.74f, h * 0.52f)
    val r = s * 0.24f
    drawCircle(ink(0.06f), r * 2.3f, c)
    drawCircle(ink(0.1f), r * 1.6f, c)
    drawCircle(ink(0.3f), r, c)
}

private fun DrawScope.drawSunset() {
    val horizon = h * 0.74f
    val c = Offset(w * 0.64f, horizon)
    val r = s * 0.36f
    clipRect(bottom = horizon) { drawCircle(ink(0.28f), r, c) }
    // The sun's reflection: shorter and shorter strokes below the horizon.
    for (i in 0 until 5) {
        val y = horizon + s * 0.05f * (i + 1)
        val half = r * (0.9f - i * 0.17f)
        drawLine(ink(0.2f - i * 0.03f), Offset(c.x - half, y), Offset(c.x + half, y), strokeWidth = s * 0.022f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawMoon(seed: Int) {
    val c = Offset(w * 0.7f, h * 0.55f)
    val r = s * 0.3f
    val crescent = Path.combine(
        PathOperation.Difference,
        Path().apply { addOval(Rect(c, r)) },
        Path().apply { addOval(Rect(c + Offset(r * 0.45f, -r * 0.3f), r * 0.85f)) },
    )
    drawPath(crescent, ink(0.3f))
    val random = Random(seed)
    repeat(16) {
        val p = Offset(random.nextFloat() * w, h * 0.3f + random.nextFloat() * h * 0.6f)
        drawCircle(ink(0.15f + random.nextFloat() * 0.3f), s * (0.006f + random.nextFloat() * 0.01f), p)
    }
}

// ---------- Genres ----------

private enum class GenreArt { BOLT, ZIGZAG, BUBBLES, SQUARE_WAVE, BARS, FLOW, PIANO, HILLS, STRINGS, TRIANGLES, FILM, DOT_RINGS }

/** Keyword → picture. Checked in order, so more specific words come first ("synthpop" is
 * electronic, "k-pop" is pop). */
private val GENRE_ART = listOf(
    listOf("metal", "металл", "hardcore", "grind", "djent") to GenreArt.ZIGZAG,
    listOf("hip", "rap", "рэп", "хип", "trap", "drill", "grime") to GenreArt.BARS,
    listOf("electr", "электр", "techno", "house", "trance", "edm", "dance", "dubstep", "drum", "dnb", "synth", "idm", "garage", "breakbeat", "phonk") to GenreArt.SQUARE_WAVE,
    listOf("punk", "rock", "рок", "grunge", "garage rock") to GenreArt.BOLT,
    listOf("pop", "поп", "idol", "city") to GenreArt.BUBBLES,
    listOf("jazz", "джаз", "blues", "блюз", "soul", "соул", "r&b", "rnb", "funk", "фанк", "swing", "gospel") to GenreArt.FLOW,
    listOf("classic", "класси", "piano", "фортеп", "orchestr", "opera", "опер", "baroque", "chamber", "symphon") to GenreArt.PIANO,
    listOf("ambient", "эмбиент", "chill", "lo-fi", "lofi", "new age", "drone", "downtempo", "relax", "sleep") to GenreArt.HILLS,
    listOf("folk", "фолк", "acoustic", "акуст", "country", "кантри", "singer", "bard", "бард", "шансон") to GenreArt.STRINGS,
    listOf("indie", "инди", "alternative", "альтерн", "shoegaze", "post", "пост", "math", "emo", "dream") to GenreArt.TRIANGLES,
    listOf("soundtrack", "саундтрек", "score", "ost", "game", "anime", "аниме", "film", "movie", "кино", "musical") to GenreArt.FILM,
    listOf("reggae", "регги", "latin", "латин", "world", "afro", "ska", "ска", "samba", "bossa", "salsa", "cumbia", "k-") to GenreArt.DOT_RINGS,
)

/** Pictures that don't say anything specific — for genres with no picture of their own. */
private val FALLBACK_ART = listOf(GenreArt.BUBBLES, GenreArt.TRIANGLES, GenreArt.DOT_RINGS, GenreArt.FLOW, GenreArt.ZIGZAG, GenreArt.HILLS)

private fun genreArtFor(genre: String): GenreArt {
    val lower = genre.lowercase()
    GENRE_ART.firstOrNull { (words, _) -> words.any { it in lower } }?.let { return it.second }
    return FALLBACK_ART[(lower.hashCode() and Int.MAX_VALUE) % FALLBACK_ART.size]
}

private fun DrawScope.drawGenre(genre: String, seed: Int) {
    when (genreArtFor(genre)) {
        GenreArt.BOLT -> drawBolt()
        GenreArt.ZIGZAG -> drawZigzags()
        GenreArt.BUBBLES -> drawBubbles(seed)
        GenreArt.SQUARE_WAVE -> drawSquareWaves()
        GenreArt.BARS -> drawBars(seed)
        GenreArt.FLOW -> drawFlow(seed)
        GenreArt.PIANO -> drawPiano()
        GenreArt.HILLS -> drawHills()
        GenreArt.STRINGS -> drawStrings()
        GenreArt.TRIANGLES -> drawTriangles(seed)
        GenreArt.FILM -> drawFilmStrip()
        GenreArt.DOT_RINGS -> drawDotRings()
    }
}

private fun DrawScope.drawBolt() {
    val x = w * 0.46f
    val y = h * 0.3f
    val u = s * 0.12f
    val bolt = Path().apply {
        moveTo(x + u * 2.6f, y)
        lineTo(x + u * 0.6f, y + u * 3.4f)
        lineTo(x + u * 2.2f, y + u * 3.4f)
        lineTo(x + u * 0.9f, y + u * 6.6f)
        lineTo(x + u * 4.2f, y + u * 2.4f)
        lineTo(x + u * 2.5f, y + u * 2.4f)
        lineTo(x + u * 3.9f, y)
        close()
    }
    drawPath(bolt, ink(0.25f))
}

private fun DrawScope.drawZigzags() {
    val step = s * 0.12f
    for (row in 0 until 6) {
        val y = h * 0.34f + row * step * 0.9f
        val path = Path().apply {
            moveTo(-step, y)
            var x = -step
            var up = true
            while (x < w + step) {
                x += step * 0.6f
                lineTo(x, if (up) y - step * 0.4f else y)
                up = !up
            }
        }
        drawPath(path, ink(0.2f - row * 0.02f), style = Stroke(s * 0.022f, join = StrokeJoin.Miter))
    }
}

private fun DrawScope.drawBubbles(seed: Int) {
    val random = Random(seed)
    repeat(10) {
        val r = s * (0.05f + random.nextFloat() * 0.17f)
        val c = Offset(w * (0.3f + random.nextFloat() * 0.8f), h * (0.3f + random.nextFloat() * 0.65f))
        if (random.nextBoolean()) drawCircle(ink(0.1f + random.nextFloat() * 0.14f), r, c)
        else drawCircle(ink(0.22f), r, c, style = Stroke(s * 0.018f))
    }
}

private fun DrawScope.drawSquareWaves() {
    fun wave(y: Float, amp: Float, period: Float, width: Float, alpha: Float, shift: Float) {
        val path = Path().apply {
            var x = -shift
            var high = true
            moveTo(x, y + amp)
            while (x < w + period) {
                lineTo(x, if (high) y - amp else y + amp)
                x += period / 2
                lineTo(x, if (high) y - amp else y + amp)
                high = !high
            }
        }
        drawPath(path, ink(alpha), style = Stroke(width, join = StrokeJoin.Miter))
    }
    wave(h * 0.55f, s * 0.12f, s * 0.36f, s * 0.04f, 0.28f, 0f)
    wave(h * 0.78f, s * 0.06f, s * 0.2f, s * 0.02f, 0.16f, s * 0.07f)
}

private fun DrawScope.drawBars(seed: Int) {
    val random = Random(seed)
    val count = 7
    val barW = w / (count * 1.6f)
    for (i in 0 until count) {
        val x = barW * 0.4f + i * barW * 1.6f
        val barH = h * (0.2f + random.nextFloat() * 0.45f)
        drawRoundRect(
            ink(0.14f + (i % 3) * 0.05f),
            topLeft = Offset(x, h - barH),
            size = Size(barW, barH + barW),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2),
        )
    }
}

private fun DrawScope.drawFlow(seed: Int) {
    val random = Random(seed)
    for (i in 0 until 6) {
        val y0 = h * (0.3f + i * 0.12f)
        val path = Path().apply {
            moveTo(-w * 0.1f, y0)
            cubicTo(
                w * 0.3f, y0 - h * (0.1f + random.nextFloat() * 0.2f),
                w * 0.6f, y0 + h * (0.1f + random.nextFloat() * 0.2f),
                w * 1.1f, y0 - h * 0.05f,
            )
        }
        drawPath(path, ink(0.12f + random.nextFloat() * 0.16f), style = Stroke(s * (0.012f + random.nextFloat() * 0.04f), cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawPiano() {
    val keyW = s * 0.16f
    val top = h * 0.56f
    rotate(-12f, Offset(w / 2, h * 0.75f)) {
        var x = -keyW * 2
        var i = 0
        while (x < w + keyW * 2) {
            drawRect(ink(0.2f), Offset(x, top), Size(keyW, h), style = Stroke(s * 0.012f))
            // Black keys sit between white ones, skipping the gaps (E–F, B–C).
            if (i % 7 != 2 && i % 7 != 6) {
                drawRect(ink(0.26f), Offset(x + keyW * 0.68f, top), Size(keyW * 0.64f, h * 0.2f))
            }
            x += keyW
            i++
        }
    }
}

private fun DrawScope.drawHills() {
    drawCircle(ink(0.22f), s * 0.1f, Offset(w * 0.74f, h * 0.4f))
    for (i in 0 until 3) {
        val base = h * (0.62f + i * 0.1f)
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, base)
            cubicTo(w * (0.2f + i * 0.1f), base - h * 0.16f, w * (0.5f - i * 0.05f), base + h * 0.08f, w * 0.75f, base - h * 0.06f)
            quadraticTo(w * 0.9f, base - h * 0.12f, w, base - h * 0.02f)
            lineTo(w, h)
            close()
        }
        drawPath(path, ink(0.1f + i * 0.04f))
    }
}

private fun DrawScope.drawStrings() {
    val c = Offset(w * 0.68f, h * 0.62f)
    val r = s * 0.28f
    drawCircle(ink(0.12f), r, c)
    drawCircle(ink(0.26f), r * 1.18f, c, style = Stroke(s * 0.035f))
    for (i in 0 until 6) {
        val x = c.x - r * 0.75f + i * r * 0.3f
        drawLine(ink(0.3f), Offset(x, 0f), Offset(x, h), strokeWidth = s * (0.006f + i * 0.002f))
    }
}

private fun DrawScope.drawTriangles(seed: Int) {
    val random = Random(seed)
    val side = s * 0.2f
    val rowH = side * 0.866f
    var row = 0
    var y = h * 0.3f
    while (y < h) {
        var x = if (row % 2 == 0) 0f else -side / 2
        while (x < w) {
            val path = Path().apply {
                moveTo(x, y + rowH)
                lineTo(x + side / 2, y)
                lineTo(x + side, y + rowH)
                close()
            }
            if (random.nextFloat() < 0.3f) drawPath(path, ink(0.16f + random.nextFloat() * 0.12f))
            else drawPath(path, ink(0.12f), style = Stroke(s * 0.008f))
            x += side
        }
        y += rowH
        row++
    }
}

private fun DrawScope.drawFilmStrip() {
    rotate(-18f, Offset(w * 0.6f, h * 0.62f)) {
        val bandH = s * 0.34f
        val top = h * 0.62f - bandH / 2
        drawRect(ink(0.16f), Offset(-w, top), Size(w * 3, bandH))
        val hole = s * 0.045f
        var x = -w
        while (x < w * 2) {
            drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(x, top + hole * 0.5f), Size(hole, hole), androidx.compose.ui.geometry.CornerRadius(hole * 0.25f))
            drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(x, top + bandH - hole * 1.5f), Size(hole, hole), androidx.compose.ui.geometry.CornerRadius(hole * 0.25f))
            x += hole * 2
        }
        // Frame dividers.
        var f = -w
        while (f < w * 2) {
            drawLine(Color.Black.copy(alpha = 0.12f), Offset(f, top + hole * 2f), Offset(f, top + bandH - hole * 2f), strokeWidth = s * 0.01f)
            f += bandH * 1.2f
        }
    }
}

private fun DrawScope.drawDotRings() {
    val c = Offset(w * 0.78f, h * 0.66f)
    for (ring in 1..6) {
        val r = s * 0.1f * ring
        val count = 8 * ring
        for (i in 0 until count) {
            val a = 2 * PI * i / count + ring * 0.3
            drawCircle(ink(0.34f - ring * 0.04f), s * 0.014f, c + Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat()))
        }
    }
}

/** The whole picture drifting slowly on the mix page's header — just moving what's drawn. */
fun DrawScope.drawMixMotifDrifting(motif: String, textMeasurer: TextMeasurer, phase: Float) {
    val dx = s * 0.03f * cos(phase)
    val dy = s * 0.02f * sin(phase * 2f)
    translate(dx, dy) { drawMixMotif(motif, textMeasurer) }
}
