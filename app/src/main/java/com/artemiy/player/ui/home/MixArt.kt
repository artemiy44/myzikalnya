package com.artemiy.player.ui.home

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
        // Reaching well past every edge: on the mix page the picture drifts around.
        val path = Path().apply {
            moveTo(-w * 0.15f, h * 1.15f)
            var x = -w * 0.15f
            while (x <= w * 1.15f) {
                lineTo(x, top + amp * sin(2f * PI.toFloat() * x / period + phase))
                x += 4f
            }
            lineTo(w * 1.15f, h * 1.15f)
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

/**
 * A crescent: the circle ([center], [radius]) minus a second circle shifted by [shift] with
 * [cutRadius]. Built from the two arcs between the circles' crossing points rather than with a
 * path boolean operation — that one cut the curve off with a straight line on the big mix header.
 */
private fun crescentPath(center: Offset, radius: Float, shift: Offset, cutRadius: Float): Path {
    val d = shift.getDistance()
    val u = shift / d
    val phi = Math.toDegrees(kotlin.math.atan2(u.y, u.x).toDouble()).toFloat()
    // Distance along the shift from the first center to the chord through both crossing points.
    val a = (radius * radius - cutRadius * cutRadius + d * d) / (2 * d)
    val chordHalf = kotlin.math.sqrt((radius * radius - a * a).coerceAtLeast(0f))
    val alpha = Math.toDegrees(kotlin.math.atan2(chordHalf, a).toDouble()).toFloat()
    val beta = Math.toDegrees(kotlin.math.atan2(chordHalf, a - d).toDouble()).toFloat()
    val cutCenter = center + shift
    return Path().apply {
        // The lit outer edge: the long way round the first circle, away from the cut...
        arcTo(Rect(center, radius), phi + alpha, 360f - 2 * alpha, forceMoveTo = true)
        // ...then back along the cut circle's edge, the part that lies inside the first one.
        arcTo(Rect(cutCenter, cutRadius), phi - beta, -(360f - 2 * beta), forceMoveTo = false)
        close()
    }
}

private fun DrawScope.drawMoon(seed: Int) {
    val c = Offset(w * 0.7f, h * 0.55f)
    val r = s * 0.3f
    drawPath(crescentPath(c, r, Offset(r * 0.45f, -r * 0.3f), r * 0.85f), ink(0.3f))
    val random = Random(seed)
    repeat(16) {
        val p = Offset(random.nextFloat() * w, h * 0.3f + random.nextFloat() * h * 0.6f)
        drawCircle(ink(0.15f + random.nextFloat() * 0.3f), s * (0.006f + random.nextFloat() * 0.01f), p)
    }
}

// ---------- Genres ----------

private enum class GenreArt {
    BOLT, ZIGZAG, BUBBLES, SQUARE_WAVE, BARS, FLOW, PIANO, HILLS, STRINGS, TRIANGLES, FILM, DOT_RINGS,
    SEIGAIHA, SPARKLES, CROSSES, HAZE, CRESCENDO, BASS_RINGS, ARCHES,
    AMPS, DISCO_BALL, SPIKES, BROKEN_BEAT, RAYS, FOUR_ON_FLOOR, STEP_GRID, SHARDS, GEAR, VINYL, PIXELS, POP_PUNK,
    THRASH, DOOM, PROG_STAIRS,
    /** "Not really a genre, but a tag" — for names no picture is known for. */
    TAG,
}

/** Keyword → picture. Checked in order, so more specific words come first ("synthpop" is
 * electronic, "k-pop" is pop). */
private val GENRE_ART = listOf(
    listOf("post-rock", "post rock", "postrock", "пост-рок", "построк", "post-metal", "postmetal") to GenreArt.CRESCENDO,
    listOf("j-rock", "jrock", "j rock", "visual kei", "visual-kei", "вижуал", "джей-рок") to GenreArt.SEIGAIHA,
    listOf("shoegaze", "шугейз", "dream pop", "dream-pop", "dreampop", "дрим-поп", "psychedel", "психодел", "trip hop", "trip-hop", "triphop") to GenreArt.HAZE,
    listOf("video game", "videogame", "vgm", "chiptune", "8-bit", "8bit", "game", "nintendo", "jeux", "игр", "ゲーム", "게임") to GenreArt.PIXELS,
    listOf("vocaloid", "vocalo", "вокалоид", "anime", "аниме", "anison", "j-pop", "jpop", "j pop", "джей-поп", "japanese", "asian", "asiatique", "shibuya", "nightcore") to GenreArt.SPARKLES,
    listOf("pop punk", "pop-punk", "poppunk", "поп-панк") to GenreArt.POP_PUNK,
    listOf("metalcore", "post-hardcore", "posthardcore", "nu metal", "nu-metal", "numetal", "deathcore", "mathcore", "screamo") to GenreArt.SHARDS,
    listOf("thrash", "speed metal", "crossover") to GenreArt.THRASH,
    listOf("doom", "sludge", "funeral") to GenreArt.DOOM,
    listOf("progressive metal", "prog metal", "progressive rock", "prog rock", "art rock") to GenreArt.PROG_STAIRS,
    listOf("hardstyle", "hard style", "hardtek", "gabber", "speedcore", "hardbass", "frenchcore", "uptempo", "happy hardcore", "hardcore techno") to GenreArt.SPIKES,
    listOf("drum & bass", "drum and bass", "drum'n'bass", "drum n bass", "dnb", "d&b", "jungle", "breakbeat", "breakcore", "neurofunk", "drumstep", "footwork", "juke") to GenreArt.BROKEN_BEAT,
    listOf("trance", "psytrance", "uplifting", "rave", "транс") to GenreArt.RAYS,
    listOf("house", "хаус") to GenreArt.FOUR_ON_FLOOR,
    listOf("techno", "техно") to GenreArt.STEP_GRID,
    listOf("disco", "dance", "eurodance", "eurobeat", "italo", "synthpop", "synth-pop", "synth pop", "танцев") to GenreArt.DISCO_BALL,
    listOf("industrial", "индастриал") to GenreArt.GEAR,
    listOf("soul", "r&b", "rnb", "funk", "motown", "соул", "фанк") to GenreArt.VINYL,
    listOf("hard rock", "hardrock", "хард-рок", "хард рок", "glam rock", "arena rock", "stoner", "southern rock") to GenreArt.AMPS,
    listOf("punk", "панк") to GenreArt.CROSSES,
    listOf("phonk", "фонк") to GenreArt.BASS_RINGS,
    listOf("goth", "гот", "darkwave", "dark wave", "deathrock", "coldwave") to GenreArt.ARCHES,
    listOf("metal", "металл", "hardcore", "grind", "djent", "noise") to GenreArt.ZIGZAG,
    listOf("hip", "rap", "рэп", "хип", "trap", "drill", "grime") to GenreArt.BARS,
    listOf("electr", "электр", "techno", "house", "trance", "edm", "dance", "dubstep", "drum", "dnb", "synth", "idm", "garage", "breakbeat", "ebm", "électr", "elektr", "hardstyle", "hard style", "hardtekk", "hardbass", "gabber", "speedcore", "breakcore", "eurobeat", "disco") to GenreArt.SQUARE_WAVE,
    listOf("rock", "рок", "grunge", "garage rock") to GenreArt.BOLT,
    listOf("pop", "поп", "idol", "city") to GenreArt.BUBBLES,
    listOf("jazz", "джаз", "blues", "блюз", "soul", "соул", "r&b", "rnb", "funk", "фанк", "swing", "gospel") to GenreArt.FLOW,
    listOf("classic", "класси", "piano", "фортеп", "orchestr", "opera", "опер", "baroque", "chamber", "symphon") to GenreArt.PIANO,
    listOf("ambient", "эмбиент", "chill", "lo-fi", "lofi", "new age", "drone", "downtempo", "relax", "sleep") to GenreArt.HILLS,
    listOf("folk", "фолк", "americana", "bluegrass", "acoustic", "акуст", "country", "кантри", "singer", "bard", "бард", "шансон") to GenreArt.STRINGS,
    listOf("indie", "инди", "alternative", "альтерн", "shoegaze", "post", "пост", "math", "emo", "alternatif", "indé") to GenreArt.TRIANGLES,
    listOf("soundtrack", "саундтрек", "score", "ost", "game", "anime", "аниме", "film", "movie", "кино", "musical", "ゲーム") to GenreArt.FILM,
    listOf("reggae", "регги", "latin", "латин", "world", "afro", "ska", "ска", "samba", "bossa", "salsa", "cumbia", "k-", "telugu", "hindi", "bollywood", "brésil", "brazil", "brasil") to GenreArt.DOT_RINGS,
)

/** Pictures that don't say anything specific — for genres with no picture of their own. */
private val FALLBACK_ART = listOf(GenreArt.BUBBLES, GenreArt.TRIANGLES, GenreArt.DOT_RINGS, GenreArt.FLOW, GenreArt.ZIGZAG, GenreArt.HILLS)

/** The picture for [genre]'s name. A name nothing is known about gets the plain "tag" label — or,
 * with [unknownAsTag] off (an artist with no genres, where the name itself seeds a picture), a
 * stable pick from [FALLBACK_ART]. */
private fun genreArtFor(genre: String, unknownAsTag: Boolean = true): GenreArt {
    val lower = genre.lowercase()
    GENRE_ART.firstOrNull { (words, _) -> words.any { it in lower } }?.let { return it.second }
    return if (unknownAsTag) GenreArt.TAG else FALLBACK_ART[(lower.hashCode() and Int.MAX_VALUE) % FALLBACK_ART.size]
}

/** A genre's picture on its own (for the artist page's header art). */
internal fun DrawScope.drawGenreMotif(genre: String, seed: Int, unknownAsTag: Boolean = true) = drawGenre(genre, seed, unknownAsTag)

/** Which picture a genre gets — two genres with the same picture count as one ("rock", "punk"). */
internal fun genrePictureOf(genre: String): String = genreArtFor(genre).name

private fun DrawScope.drawGenre(genre: String, seed: Int, unknownAsTag: Boolean = true) {
    when (genreArtFor(genre, unknownAsTag)) {
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
        GenreArt.SEIGAIHA -> drawSeigaiha()
        GenreArt.SPARKLES -> drawSparkles(seed)
        GenreArt.CROSSES -> drawCrosses(seed)
        GenreArt.HAZE -> drawHaze(seed)
        GenreArt.CRESCENDO -> drawCrescendo()
        GenreArt.BASS_RINGS -> drawBassRings(seed)
        GenreArt.ARCHES -> drawArches()
        GenreArt.AMPS -> drawAmps()
        GenreArt.DISCO_BALL -> drawDiscoBall()
        GenreArt.SPIKES -> drawSpikes()
        GenreArt.BROKEN_BEAT -> drawBrokenBeat(seed)
        GenreArt.RAYS -> drawRays()
        GenreArt.FOUR_ON_FLOOR -> drawFourOnFloor()
        GenreArt.STEP_GRID -> drawStepGrid(seed)
        GenreArt.SHARDS -> drawShards(seed)
        GenreArt.GEAR -> drawGears()
        GenreArt.VINYL -> drawRecordGrooves()
        GenreArt.PIXELS -> drawPixels(seed)
        GenreArt.POP_PUNK -> { drawBubbles(seed); drawCrosses(seed + 1) }
        GenreArt.THRASH -> drawThrash()
        GenreArt.DOOM -> drawDoom()
        GenreArt.PROG_STAIRS -> drawProgStairs()
        GenreArt.TAG -> drawTag()
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
            var x = -shift - period
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
            moveTo(-w * 0.15f, h * 1.15f)
            lineTo(-w * 0.15f, base + h * 0.03f)
            cubicTo(w * (0.2f + i * 0.1f), base - h * 0.16f, w * (0.5f - i * 0.05f), base + h * 0.08f, w * 0.75f, base - h * 0.06f)
            quadraticTo(w * 0.9f, base - h * 0.12f, w * 1.15f, base)
            lineTo(w * 1.15f, h * 1.15f)
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
        var x = if (row % 2 == 0) -side else -side * 1.5f
        while (x < w + side) {
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

/** J-rock, visual kei: seigaiha, the Japanese "waves of the sea" — rows of fanned half-rings,
 * each row tucked under the one in front. */
private fun DrawScope.drawSeigaiha() {
    val r = s * 0.13f
    // Drawn on a layer of its own, so each scale can wipe out what's behind it.
    drawContext.canvas.saveLayer(Rect(0f, 0f, w, h), androidx.compose.ui.graphics.Paint())
    var row = 0
    var y = h * 0.32f
    while (y < h + r) {
        var x = if (row % 2 == 0) 0f else r
        while (x < w + r) {
            val c = Offset(x, y)
            drawCircle(Color.Black, r, c, blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
            for (k in 1..4) drawCircle(ink(0.22f - k * 0.03f), r * k / 4f, c, style = Stroke(s * 0.009f))
            x += r * 2
        }
        y += r * 0.55f
        row++
    }
    drawContext.canvas.restore()
}

/** Vocaloid, anime, J-pop: four-pointed sparkles, a couple big and a scatter of small ones. */
private fun DrawScope.drawSparkles(seed: Int) {
    val random = Random(seed)
    fun sparkle(c: Offset, r: Float, alpha: Float) {
        val inner = r * 0.18f
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticTo(c.x + inner, c.y - inner, c.x + r, c.y)
            quadraticTo(c.x + inner, c.y + inner, c.x, c.y + r)
            quadraticTo(c.x - inner, c.y + inner, c.x - r, c.y)
            quadraticTo(c.x - inner, c.y - inner, c.x, c.y - r)
            close()
        }
        drawPath(path, ink(alpha))
    }
    sparkle(Offset(w * 0.7f, h * 0.48f), s * 0.24f, 0.28f)
    sparkle(Offset(w * 0.34f, h * 0.72f), s * 0.14f, 0.22f)
    repeat(9) {
        sparkle(
            Offset(w * (0.1f + random.nextFloat() * 0.85f), h * (0.25f + random.nextFloat() * 0.7f)),
            s * (0.025f + random.nextFloat() * 0.05f),
            0.14f + random.nextFloat() * 0.18f,
        )
    }
}

/** Punk: rough crosses scrawled at odd angles, like marker on a wall. */
private fun DrawScope.drawCrosses(seed: Int) {
    val random = Random(seed)
    val spots = listOf(Offset(0.7f, 0.5f) to 0.2f, Offset(0.3f, 0.75f) to 0.13f, Offset(0.88f, 0.85f) to 0.1f, Offset(0.48f, 0.34f) to 0.08f, Offset(0.15f, 0.45f) to 0.07f)
    for ((spot, size) in spots) {
        val c = Offset(w * spot.x, h * spot.y)
        val r = s * size
        rotate(-20f + random.nextFloat() * 40f, c) {
            // Each stroke a little off — scrawled by hand, not ruled.
            fun jitter() = (random.nextFloat() - 0.5f) * r * 0.25f
            val width = s * (0.02f + size * 0.12f)
            val alpha = 0.18f + random.nextFloat() * 0.14f
            drawLine(ink(alpha), c + Offset(-r + jitter(), -r + jitter()), c + Offset(r + jitter(), r + jitter()), width, StrokeCap.Square)
            drawLine(ink(alpha), c + Offset(r + jitter(), -r + jitter()), c + Offset(-r + jitter(), r + jitter()), width, StrokeCap.Square)
        }
    }
}

/** Shoegaze, dream pop: wide, soft waves washing over each other — everything blurs into one. */
private fun DrawScope.drawHaze(seed: Int) {
    val random = Random(seed)
    for (i in 0 until 5) {
        val y0 = h * (0.38f + i * 0.13f)
        val path = Path().apply {
            moveTo(-w * 0.2f, y0)
            cubicTo(
                w * 0.25f, y0 - h * (0.12f + random.nextFloat() * 0.14f),
                w * 0.7f, y0 + h * (0.12f + random.nextFloat() * 0.14f),
                w * 1.2f, y0 - h * 0.04f,
            )
        }
        drawPath(path, ink(0.07f + random.nextFloat() * 0.06f), style = Stroke(s * (0.1f + random.nextFloat() * 0.08f), cap = StrokeCap.Round))
    }
}

/** Post-rock: lines rising slowly from nothing to a towering crescendo. */
private fun DrawScope.drawCrescendo() {
    val count = 26
    val base = h * 0.94f
    for (i in 0 until count) {
        val t = i / (count - 1f)
        val x = w * (0.04f + t * 0.92f)
        val height = h * (0.03f + 0.62f * t * t * t)
        drawLine(ink(0.1f + t * 0.2f), Offset(x, base), Offset(x, base - height), s * 0.012f, StrokeCap.Round)
    }
}

/** Phonk: a speaker, and the shock of its bass going out in thick broken rings. */
private fun DrawScope.drawBassRings(seed: Int) {
    val random = Random(seed)
    val c = Offset(w * 0.68f, h * 0.62f)
    drawCircle(ink(0.28f), s * 0.07f, c)
    drawCircle(ink(0.2f), s * 0.12f, c, style = Stroke(s * 0.018f))
    for (ring in 1..4) {
        val r = s * (0.12f + ring * 0.12f)
        var angle = random.nextFloat() * 360f
        var drawn = 0f
        // Each ring torn into a few pieces with gaps between.
        while (drawn < 330f) {
            val piece = 40f + random.nextFloat() * 70f
            val gap = 10f + random.nextFloat() * 25f
            drawArc(
                ink(0.26f - ring * 0.045f), angle, piece, useCenter = false,
                topLeft = c - Offset(r, r), size = Size(r * 2, r * 2),
                style = Stroke(s * (0.05f - ring * 0.008f), cap = StrokeCap.Butt),
            )
            angle += piece + gap
            drawn += piece + gap
        }
    }
}

/** Goth, darkwave: pointed arches, one inside the other, like a cathedral's windows. */
private fun DrawScope.drawArches() {
    fun arch(cx: Float, base: Float, halfWidth: Float, legHeight: Float, apex: Float, alpha: Float) {
        val path = Path().apply {
            moveTo(cx - halfWidth, base)
            lineTo(cx - halfWidth, base - legHeight)
            quadraticTo(cx - halfWidth, apex + (base - legHeight - apex) * 0.35f, cx, apex)
            quadraticTo(cx + halfWidth, apex + (base - legHeight - apex) * 0.35f, cx + halfWidth, base - legHeight)
            lineTo(cx + halfWidth, base)
        }
        drawPath(path, ink(alpha), style = Stroke(s * 0.012f, join = StrokeJoin.Miter))
    }
    val base = h * 1.02f
    for ((cx, big) in listOf(w * 0.64f to 1f, w * 0.24f to 0.62f)) {
        for (k in 0 until 4) {
            val shrink = 1f - k * 0.2f
            arch(cx, base, s * 0.2f * big * shrink, h * 0.3f * big * shrink, base - h * 0.78f * big * shrink, 0.24f - k * 0.04f)
        }
    }
}

/** Hard rock: a wall of amplifier cabinets — each with its two speakers — stacked in brick rows. */
private fun DrawScope.drawAmps() {
    val cw = s * 0.34f
    val ch = s * 0.4f
    val gap = s * 0.03f
    var row = 0
    var y = h * 0.26f
    while (y < h) {
        var x = -cw * 0.25f + (row % 2) * cw * 0.5f
        while (x < w) {
            drawRoundRect(ink(0.2f), Offset(x, y), Size(cw, ch), CornerRadius(s * 0.02f), style = Stroke(s * 0.01f))
            for (k in 0..1) {
                val c = Offset(x + cw / 2, y + ch * (0.27f + k * 0.46f))
                drawCircle(ink(0.2f), cw * 0.28f, c, style = Stroke(s * 0.01f))
                drawCircle(ink(0.26f), cw * 0.1f, c)
            }
            x += cw + gap
        }
        y += ch + gap
        row++
    }
}

/** Dance, disco, synthpop: a mirror ball hanging in a burst of light. */
private fun DrawScope.drawDiscoBall() {
    val c = Offset(w * 0.68f, h * 0.55f)
    val r = s * 0.28f
    for (i in 0 until 18) {
        val a = 2 * PI * i / 18 + 0.1
        val from = c + Offset((cos(a) * r * 1.2f).toFloat(), (sin(a) * r * 1.2f).toFloat())
        val to = c + Offset((cos(a) * r * (1.7f + (i % 3) * 0.25f)).toFloat(), (sin(a) * r * (1.7f + (i % 3) * 0.25f)).toFloat())
        drawLine(ink(0.14f), from, to, s * 0.012f, StrokeCap.Round)
    }
    drawLine(ink(0.2f), Offset(c.x, 0f), Offset(c.x, c.y - r), s * 0.01f)
    val n = 9
    val tile = r * 2 / n
    for (i in 0 until n) for (j in 0 until n) {
        val u = (i + 0.5f) / n * 2 - 1
        val v = (j + 0.5f) / n * 2 - 1
        if (u * u + v * v <= 1f) {
            drawRect(ink(0.1f + ((i * 7 + j * 3) % 5) * 0.05f), Offset(c.x + u * r - tile * 0.45f, c.y + v * r - tile * 0.45f), Size(tile * 0.9f, tile * 0.9f))
        }
    }
}

/** Hardstyle and its kin: sharp, over-driven spikes standing on the bass line. */
private fun DrawScope.drawSpikes() {
    val base = h * 0.96f
    val count = 10
    for (i in 0 until count) {
        val x0 = w * (0.02f + i * 0.098f)
        val tall = h * (0.22f + ((i * 37) % 7) / 7f * 0.42f)
        val path = Path().apply {
            moveTo(x0, base)
            lineTo(x0 + w * 0.045f, base - tall)
            lineTo(x0 + w * 0.09f, base)
            close()
        }
        drawPath(path, ink(0.14f + (i % 3) * 0.06f))
    }
    drawLine(ink(0.2f), Offset(0f, base), Offset(w, base), s * 0.012f)
}

/** Drum & bass, jungle, breakbeat: broken, jagged lines that never settle into a pattern. */
private fun DrawScope.drawBrokenBeat(seed: Int) {
    val random = Random(seed)
    for (row in 0 until 4) {
        val y0 = h * (0.38f + row * 0.15f)
        val path = Path().apply {
            moveTo(-w * 0.05f, y0)
            var x = -w * 0.05f
            while (x < w * 1.05f) {
                x += w * (0.03f + random.nextFloat() * 0.09f)
                lineTo(x, y0 + (random.nextFloat() - 0.5f) * h * 0.14f)
            }
        }
        drawPath(path, ink(0.16f + row * 0.03f), style = Stroke(s * 0.012f, join = StrokeJoin.Miter))
    }
}

/** Trance: beams of light fanning up from one point. */
private fun DrawScope.drawRays() {
    val o = Offset(w * 0.5f, h * 1.06f)
    val len = maxOf(w, h) * 1.3f
    for (i in 0 until 9) {
        val a = Math.toRadians(205.0 + i * 16.25)
        val half = Math.toRadians(3.2)
        val path = Path().apply {
            moveTo(o.x, o.y)
            lineTo(o.x + (cos(a - half) * len).toFloat(), o.y + (sin(a - half) * len).toFloat())
            lineTo(o.x + (cos(a + half) * len).toFloat(), o.y + (sin(a + half) * len).toFloat())
            close()
        }
        drawPath(path, ink(0.1f + (i % 3) * 0.05f))
    }
    drawCircle(ink(0.25f), s * 0.03f, o - Offset(0f, s * 0.06f))
}

/** House: the kick on every beat — rows of circles whose size follows the 4/4. */
private fun DrawScope.drawFourOnFloor() {
    val sizes = floatArrayOf(1f, 0.45f, 0.7f, 0.45f)
    for (row in 0 until 5) for (col in 0 until 4) {
        val c = Offset(w * (0.14f + col * 0.24f), h * (0.36f + row * 0.15f))
        val r = s * 0.06f * sizes[col]
        if (col == 0) drawCircle(ink(0.26f), r, c) else drawCircle(ink(0.2f), r, c, style = Stroke(s * 0.01f))
    }
}

/** Techno: a step sequencer — a strict grid, some steps lit. */
private fun DrawScope.drawStepGrid(seed: Int) {
    val random = Random(seed)
    val cell = s * 0.085f
    val gap = s * 0.022f
    val cols = ((w - gap) / (cell + gap)).toInt().coerceAtLeast(4)
    for (row in 0 until 5) for (col in 0 until cols) {
        val topLeft = Offset(gap + col * (cell + gap), h * 0.34f + row * (cell + gap))
        if (random.nextFloat() < 0.28f) drawRect(ink(0.26f), topLeft, Size(cell, cell))
        else drawRect(ink(0.14f), topLeft, Size(cell, cell), style = Stroke(s * 0.006f))
    }
}

/** Metalcore, post-hardcore, nu metal: shattered, sharp-edged pieces. */
private fun DrawScope.drawShards(seed: Int) {
    val random = Random(seed)
    for (k in 0 until 16) {
        val c = Offset(w * (0.05f + random.nextFloat() * 0.95f), h * (0.3f + random.nextFloat() * 0.65f))
        val r = s * (0.05f + random.nextFloat() * 0.13f)
        val path = Path().apply {
            for (i in 0 until 3) {
                val a = random.nextFloat() * 2 * PI
                val p = Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat())
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(path, ink(0.1f + random.nextFloat() * 0.2f))
    }
}

/** Industrial: gears, a big one and a small one meshed with it. */
private fun DrawScope.drawGears() {
    fun gear(c: Offset, radius: Float, teeth: Int, alpha: Float) {
        val unit = (2 * PI / teeth).toFloat()
        val path = Path().apply {
            for (i in 0 until teeth) {
                val a0 = i * unit
                val points = listOf(a0 to 0.8f, a0 + unit * 0.12f to 1f, a0 + unit * 0.38f to 1f, a0 + unit * 0.5f to 0.8f)
                for ((k, pt) in points.withIndex()) {
                    val x = c.x + (cos(pt.first.toDouble()) * radius * pt.second).toFloat()
                    val y = c.y + (sin(pt.first.toDouble()) * radius * pt.second).toFloat()
                    if (i == 0 && k == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            close()
        }
        drawPath(path, ink(alpha), style = Stroke(s * 0.014f, join = StrokeJoin.Round))
        drawCircle(ink(alpha), radius * 0.28f, c, style = Stroke(s * 0.014f))
    }
    gear(Offset(w * 0.72f, h * 0.56f), s * 0.3f, 14, 0.24f)
    gear(Offset(w * 0.3f, h * 0.8f), s * 0.17f, 9, 0.2f)
}

/** R&B, soul: a vinyl record — grooves around its label. */
private fun DrawScope.drawRecordGrooves() {
    val c = Offset(w * 0.7f, h * 0.55f)
    val r = s * 0.36f
    for (i in 0 until 14) {
        drawCircle(ink(0.08f + (i % 3) * 0.04f), r * (0.4f + i * 0.043f), c, style = Stroke(s * 0.006f))
    }
    drawCircle(ink(0.22f), r * 0.3f, c)
    drawCircle(ink(0.4f), r * 0.04f, c)
    drawArc(ink(0.2f), -60f, 40f, false, c - Offset(r * 0.9f, r * 0.9f), Size(r * 1.8f, r * 1.8f), style = Stroke(s * 0.014f, cap = StrokeCap.Round))
}

/** Video games: a pixel-art invader, and pixels scattered around it. */
private fun DrawScope.drawPixels(seed: Int) {
    val sprite = listOf(
        "..#.....#..",
        "...#...#...",
        "..#######..",
        ".##.###.##.",
        "###########",
        "#.#######.#",
        "#.#.....#.#",
        "...##.##...",
    )
    val cell = s * 0.045f
    val origin = Offset(w * 0.55f, h * 0.48f)
    for ((y, line) in sprite.withIndex()) for ((x, ch) in line.withIndex()) {
        if (ch == '#') drawRect(ink(0.26f), origin + Offset(x * cell, y * cell), Size(cell * 0.92f, cell * 0.92f))
    }
    val random = Random(seed)
    repeat(26) {
        val px = (random.nextFloat() * (w / cell)).toInt() * cell
        val py = h * 0.28f + (random.nextFloat() * ((h * 0.7f) / cell)).toInt() * cell
        drawRect(ink(0.08f + random.nextFloat() * 0.12f), Offset(px, py), Size(cell * 0.92f, cell * 0.92f))
    }
}

/** A name that isn't a genre we know: a plain label — "it's a tag, just not one we can draw". */
private fun DrawScope.drawTag() {
    val c = Offset(w * 0.7f, h * 0.55f)
    val l = s * 0.45f
    rotate(-18f, c) {
        val path = Path().apply {
            moveTo(c.x - l * 0.5f, c.y - l * 0.28f)
            lineTo(c.x + l * 0.2f, c.y - l * 0.28f)
            lineTo(c.x + l * 0.55f, c.y)
            lineTo(c.x + l * 0.2f, c.y + l * 0.28f)
            lineTo(c.x - l * 0.5f, c.y + l * 0.28f)
            close()
        }
        drawPath(path, ink(0.3f), style = Stroke(s * 0.016f, join = StrokeJoin.Round))
        drawCircle(ink(0.3f), s * 0.03f, Offset(c.x - l * 0.36f, c.y), style = Stroke(s * 0.012f))
        drawLine(ink(0.22f), Offset(c.x - l * 0.2f, c.y - l * 0.07f), Offset(c.x + l * 0.2f, c.y - l * 0.07f), s * 0.014f, StrokeCap.Round)
        drawLine(ink(0.22f), Offset(c.x - l * 0.2f, c.y + l * 0.08f), Offset(c.x + l * 0.08f, c.y + l * 0.08f), s * 0.014f, StrokeCap.Round)
    }
}

/** Thrash: a saw blade — rows of fast, sharp teeth all leaning one way. */
private fun DrawScope.drawThrash() {
    val tooth = s * 0.06f
    for (row in 0 until 6) {
        val y0 = h * (0.32f + row * 0.115f)
        val path = Path().apply {
            moveTo(-tooth, y0)
            var x = -tooth
            while (x < w + tooth) {
                lineTo(x, y0 - tooth * 1.4f)
                lineTo(x + tooth, y0)
                x += tooth
            }
            lineTo(w + tooth, y0 + tooth * 2f)
            lineTo(-tooth, y0 + tooth * 2f)
            close()
        }
        drawPath(path, ink(0.07f + row * 0.02f))
        drawPath(path, ink(0.18f), style = Stroke(s * 0.006f))
    }
}

/** Doom: slow and heavy — a sinking sun behind a few thick, lazy waves. */
private fun DrawScope.drawDoom() {
    drawCircle(ink(0.16f), s * 0.26f, Offset(w * 0.7f, h * 0.62f))
    for (i in 0 until 3) {
        val y0 = h * (0.5f + i * 0.17f)
        val path = Path().apply {
            moveTo(-w * 0.1f, y0)
            cubicTo(w * 0.2f, y0 - h * 0.12f, w * 0.55f, y0 + h * 0.12f, w * 1.1f, y0)
        }
        drawPath(path, ink(0.12f + i * 0.05f), style = Stroke(s * 0.12f, cap = StrokeCap.Round))
    }
}

/** Progressive: a staircase climbing step by step, each one a little taller. */
private fun DrawScope.drawProgStairs() {
    val steps = 8
    val stepW = w / steps
    val base = h * 0.97f
    for (i in 0 until steps) {
        val top = base - h * (0.12f + i * 0.085f)
        drawRect(ink(0.08f + i * 0.02f), Offset(i * stepW, top), Size(stepW * 0.92f, base - top))
        drawRect(ink(0.2f), Offset(i * stepW, top), Size(stepW * 0.92f, base - top), style = Stroke(s * 0.007f))
    }
}
