package com.artemiy.player.ui.components

/**
 * Classic "stack blur" (Mario Klingemann's well-known, widely-reused algorithm) — plain software
 * box-style blur over raw pixels, no RenderEffect/GPU shader involved at all. Only ever runs on
 * ~100px bitmaps, so even this pure-Kotlin implementation finishes near-instantly.
 */
fun stackBlur(bitmap: android.graphics.Bitmap, radius: Int): android.graphics.Bitmap {
    if (radius < 1) return bitmap
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val div = radius * 2 + 1
    val divSum = (div + 1) shr 1
    val divSum2 = divSum * divSum
    val mulLookup = IntArray(256 * divSum2) { it / divSum2 }
    val stack = Array(div) { IntArray(3) }

    var minY: IntArray
    val vMin = IntArray(maxOf(w, h))

    var y = 0
    while (y < h) {
        var rSum = 0; var gSum = 0; var bSum = 0
        var rOut = 0; var gOut = 0; var bOut = 0
        var rIn = 0; var gIn = 0; var bIn = 0
        var i = 0
        while (i < div) {
            val x = (i - radius).coerceIn(0, w - 1)
            val p = pixels[y * w + x]
            val s = stack[i]
            s[0] = (p shr 16) and 0xFF
            s[1] = (p shr 8) and 0xFF
            s[2] = p and 0xFF
            val weight = radius + 1 - kotlin.math.abs(i - radius)
            rSum += s[0] * weight; gSum += s[1] * weight; bSum += s[2] * weight
            if (i <= radius) { rOut += s[0]; gOut += s[1]; bOut += s[2] }
            else { rIn += s[0]; gIn += s[1]; bIn += s[2] }
            i++
        }
        var stackPointer = radius
        var x = 0
        while (x < w) {
            pixels[y * w + x] = (pixels[y * w + x] and -0x1000000) or
                (mulLookup[rSum] shl 16) or (mulLookup[gSum] shl 8) or mulLookup[bSum]
            rSum -= rOut; gSum -= gOut; bSum -= bOut
            var stackStart = stackPointer - radius + div
            if (stackStart >= div) stackStart -= div
            val sOut = stack[stackStart]
            rOut -= sOut[0]; gOut -= sOut[1]; bOut -= sOut[2]
            if (y == 0) vMin[x] = minOf(x + radius + 1, w - 1)
            val p = pixels[y * w + vMin[x]]
            sOut[0] = (p shr 16) and 0xFF; sOut[1] = (p shr 8) and 0xFF; sOut[2] = p and 0xFF
            rIn += sOut[0]; gIn += sOut[1]; bIn += sOut[2]
            rSum += rIn; gSum += gIn; bSum += bIn
            stackPointer++
            if (stackPointer >= div) stackPointer = 0
            val sIn = stack[stackPointer]
            rOut += sIn[0]; gOut += sIn[1]; bOut += sIn[2]
            rIn -= sIn[0]; gIn -= sIn[1]; bIn -= sIn[2]
            x++
        }
        y++
    }

    minY = vMin.copyOf()
    x@ for (x0 in 0 until w) {
        var rSum = 0; var gSum = 0; var bSum = 0
        var rOut = 0; var gOut = 0; var bOut = 0
        var rIn = 0; var gIn = 0; var bIn = 0
        var i = 0
        while (i < div) {
            val yy = (i - radius).coerceIn(0, h - 1) * w
            val s = stack[i]
            val p = pixels[yy + x0]
            s[0] = (p shr 16) and 0xFF; s[1] = (p shr 8) and 0xFF; s[2] = p and 0xFF
            val weight = radius + 1 - kotlin.math.abs(i - radius)
            rSum += s[0] * weight; gSum += s[1] * weight; bSum += s[2] * weight
            if (i <= radius) { rOut += s[0]; gOut += s[1]; bOut += s[2] }
            else { rIn += s[0]; gIn += s[1]; bIn += s[2] }
            i++
        }
        var stackPointer = radius
        var yy = 0
        while (yy < h) {
            val idx = yy * w + x0
            pixels[idx] = (pixels[idx] and -0x1000000) or
                (mulLookup[rSum] shl 16) or (mulLookup[gSum] shl 8) or mulLookup[bSum]
            rSum -= rOut; gSum -= gOut; bSum -= bOut
            var stackStart = stackPointer - radius + div
            if (stackStart >= div) stackStart -= div
            val sOut = stack[stackStart]
            rOut -= sOut[0]; gOut -= sOut[1]; bOut -= sOut[2]
            if (x0 == 0) minY[yy] = minOf(yy + radius + 1, h - 1) * w
            val p = pixels[minY[yy] + x0]
            sOut[0] = (p shr 16) and 0xFF; sOut[1] = (p shr 8) and 0xFF; sOut[2] = p and 0xFF
            rIn += sOut[0]; gIn += sOut[1]; bIn += sOut[2]
            rSum += rIn; gSum += gIn; bSum += bIn
            stackPointer++
            if (stackPointer >= div) stackPointer = 0
            val sIn = stack[stackPointer]
            rOut += sIn[0]; gOut += sIn[1]; bOut += sIn[2]
            rIn -= sIn[0]; gIn -= sIn[1]; bIn -= sIn[2]
            yy++
        }
    }

    val out = bitmap.copy(bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    return out
}

/**
 * The artist page's cover collage (two columns, as many rows as needed), shrunk to a tiny bitmap, a bit more saturated and blurred
 * — the same pre-blurred-bitmap trick as Now Playing's "живой блюр". A cover that's missing (or
 * not loaded yet) is a plain dark cell.
 */
fun blurredCollage(covers: List<android.graphics.Bitmap?>): android.graphics.Bitmap {
    val cell = 48
    val rows = (covers.size + 1) / 2
    val out = android.graphics.Bitmap.createBitmap(cell * 2, cell * rows.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawColor(0xFF2C2C2E.toInt())
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(1.4f) })
    }
    covers.forEachIndexed { i, cover ->
        if (cover == null) return@forEachIndexed
        val left = (i % 2) * cell
        val top = (i / 2) * cell
        canvas.drawBitmap(cover, null, android.graphics.Rect(left, top, left + cell, top + cell), paint)
    }
    return stackBlur(out, radius = 10)
}
