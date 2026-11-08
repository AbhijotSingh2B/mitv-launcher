package dev.tvlauncher.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.core.graphics.PathParser
import androidx.compose.ui.text.drawText
import kotlin.random.Random

/*
 * Per-tile background motifs. Every one is drawn once into a cached alpha mask (see ThemedBackground),
 * so none of this runs per frame.
 */

/** Material Symbols Rounded outline glyphs (Apache 2.0), 960-unit viewBox at 0,-960. */
private val mediaGlyphs: List<Path> by lazy {
    listOf(
        "m426-330 195-125q14-9 14-25t-14-25L426-630q-15-10-30.5-1.5T380-605v250q0 18 15.5 26.5T426-330Zm54 250q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z",
        "M840-640q32 0 56 24t24 56v80q0 7-2 15t-4 15L794-168q-9 20-30 34t-44 14H280v-520l240-238q15-15 35.5-17.5T595-888q19 10 28 28t4 37l-45 183h258Zm-480 34v406h360l120-280v-80H480l54-220-174 174ZM160-120q-33 0-56.5-23.5T80-200v-360q0-33 23.5-56.5T160-640h120v80H160v360h120v80H160Zm200-80v-406 406Z",
        "M200-200q-17 0-28.5-11.5T160-240q0-17 11.5-28.5T200-280h40v-280q0-83 50-147.5T420-792v-28q0-25 17.5-42.5T480-880q25 0 42.5 17.5T540-820v28q80 20 130 84.5T720-560v280h40q17 0 28.5 11.5T800-240q0 17-11.5 28.5T760-200H200Zm280-300Zm0 420q-33 0-56.5-23.5T400-160h160q0 33-23.5 56.5T480-80ZM320-280h320v-280q0-66-47-113t-113-47q-66 0-113 47t-47 113v280Z",
        "M400-120q-66 0-113-47t-47-113q0-66 47-113t113-47q23 0 42.5 5.5T480-418v-382q0-17 11.5-28.5T520-840h160q17 0 28.5 11.5T720-800v80q0 17-11.5 28.5T680-680H560v400q0 66-47 113t-113 47Z",
        "M480-147q-14 0-28.5-5T426-168l-69-63q-106-97-191.5-192.5T80-634q0-94 63-157t157-63q53 0 100 22.5t80 61.5q33-39 80-61.5T660-854q94 0 157 63t63 157q0 115-85 211T602-230l-68 62q-11 11-25.5 16t-28.5 5Zm-38-543q-29-41-62-62.5T300-774q-60 0-100 40t-40 100q0 52 37 110.5T285.5-410q51.5 55 106 103t88.5 79q34-31 88.5-79t106-103Q726-465 763-523.5T800-634q0-60-40-100t-100-40q-47 0-80 21.5T518-690q-7 10-17 15t-21 5q-11 0-21-5t-17-15Zm38 189Z",
        "M160-80q-33 0-56.5-23.5T80-160v-400q0-33 23.5-56.5T160-640h640q33 0 56.5 23.5T880-560v400q0 33-23.5 56.5T800-80H160Zm0-80h640v-400H160v400Zm271-61 184-122q9-6 9-17t-9-17L431-499q-10-7-20.5-1.5T400-483v246q0 12 10.5 17.5T431-221ZM200-680q-17 0-28.5-11.5T160-720q0-17 11.5-28.5T200-760h560q17 0 28.5 11.5T800-720q0 17-11.5 28.5T760-680H200Zm120-120q-17 0-28.5-11.5T280-840q0-17 11.5-28.5T320-880h320q17 0 28.5 11.5T680-840q0 17-11.5 28.5T640-800H320ZM160-160v-400 400Z",
        "m411-320 223-143q9-6 9-17t-9-17L411-640q-10-7-20.5-1T380-623v286q0 12 10.5 18t20.5-1ZM160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h640q33 0 56.5 23.5T880-720v480q0 33-23.5 56.5T800-160H160Zm0-80h640v-480H160v480Zm0 0v-480 480Z",
        "M480-400q-50 0-85-35t-35-85v-240q0-50 35-85t85-35q50 0 85 35t35 85v240q0 50-35 85t-85 35Zm0-240Zm-40 480v-83q-92-13-157.5-78T203-479q-2-17 9-29t28-12q17 0 28.5 11.5T284-480q14 70 69.5 115T480-320q72 0 127-45.5T676-480q4-17 15.5-28.5T720-520q17 0 28 12t9 29q-14 91-79 157t-158 79v83q0 17-11.5 28.5T480-120q-17 0-28.5-11.5T440-160Zm40-320q17 0 28.5-11.5T520-520v-240q0-17-11.5-28.5T480-800q-17 0-28.5 11.5T440-760v240q0 17 11.5 28.5T480-480Z",
    ).map { PathParser.createPathFromPathData(it).asComposePath() }
}

/** YouTube: media glyphs (play, like, bell, note, heart, subscriptions, screen, mic) scattered on a jittered grid. */
internal fun DrawScope.drawMediaGlyphs(color: Color) {
    val rnd = Random(11)
    val cols = 10
    val rows = 6
    val cw = size.width / cols
    val ch = size.height / rows
    var k = rnd.nextInt(mediaGlyphs.size)
    for (r in 0 until rows) for (c in 0 until cols) {
        val cx = (c + 0.25f + rnd.nextFloat() * 0.5f) * cw
        val cy = (r + 0.25f + rnd.nextFloat() * 0.5f) * ch
        val px = (34f + rnd.nextFloat() * 22f) * density
        val rot = rnd.nextFloat() * 40f - 20f
        k = (k + 1 + rnd.nextInt(2)) % mediaGlyphs.size
        val s = px / 960f
        rotate(rot, Offset(cx, cy)) {
            translate(cx - px / 2, cy - px / 2) {
                scale(s, s, pivot = Offset.Zero) {
                    translate(0f, 960f) { drawPath(mediaGlyphs[k], color) }
                }
            }
        }
    }
}

/** Netflix: a fan of soft light ribbons sweeping up from the bottom right, with a brighter edge on each. */
internal fun DrawScope.drawRibbons(color: Color) {
    val w = size.width
    val h = size.height
    val origin = Offset(w * 1.02f, h * 1.15f)
    for (i in 0 until 9) {
        val f = i / 8f
        val end = Offset(w * (0.30f + 0.62f * f), -h * 0.1f)
        val ctrl1 = Offset(w * (0.98f - 0.10f * f), h * (0.75f - 0.2f * f))
        val ctrl2 = Offset(w * (0.40f + 0.55f * f), h * (0.35f - 0.1f * f))
        val p = Path().apply {
            moveTo(origin.x, origin.y)
            cubicTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, end.x, end.y)
        }
        val bandA = 0.35f + 0.65f * (1f - kotlin.math.abs(f - 0.55f) * 1.6f).coerceIn(0f, 1f)
        // Wide soft band, then a thin bright edge.
        drawPath(p, color.copy(alpha = color.alpha * 0.45f * bandA), style = Stroke((46f - 3f * i) * density, cap = StrokeCap.Round))
        drawPath(p, color.copy(alpha = color.alpha * 0.9f * bandA), style = Stroke(2.2f * density, cap = StrokeCap.Round))
    }
    // A glow pooled where the ribbons converge.
    drawCircle(
        Brush.radialGradient(
            listOf(color.copy(alpha = color.alpha * 0.9f), Color.Transparent),
            center = Offset(w * 0.97f, h * 1.0f),
            radius = h * 0.55f,
        ),
        radius = h * 0.55f,
        center = Offset(w * 0.97f, h * 1.0f),
    )
}

/** Cameras: a camera viewfinder (corner brackets, rule-of-thirds lines, centre mark, REC dot) over faint scanlines. */
internal fun DrawScope.drawViewfinder(color: Color) {
    val w = size.width
    val h = size.height
    val hair = 1f * density
    // Scanlines.
    val scan = color.copy(alpha = color.alpha * 0.35f)
    var y = 0f
    while (y < h) { drawLine(scan, Offset(0f, y), Offset(w, y), hair); y += 5f * density }

    // Kept above the tile row (which starts near 50% of the height).
    val left = w * 0.54f
    val top = h * 0.08f
    val right = w * 0.93f
    val bottom = h * 0.45f
    val arm = 34f * density
    val thick = Stroke(3f * density, cap = StrokeCap.Round)
    fun corner(x: Float, yy: Float, dx: Float, dy: Float) {
        drawLine(color, Offset(x, yy), Offset(x + dx * arm, yy), thick.width, cap = StrokeCap.Round)
        drawLine(color, Offset(x, yy), Offset(x, yy + dy * arm), thick.width, cap = StrokeCap.Round)
    }
    corner(left, top, 1f, 1f)
    corner(right, top, -1f, 1f)
    corner(left, bottom, 1f, -1f)
    corner(right, bottom, -1f, -1f)
    // Rule of thirds.
    val thirds = color.copy(alpha = color.alpha * 0.5f)
    for (i in 1..2) {
        val x = left + (right - left) * i / 3f
        val yy = top + (bottom - top) * i / 3f
        drawLine(thirds, Offset(x, top + arm), Offset(x, bottom - arm), hair)
        drawLine(thirds, Offset(left + arm, yy), Offset(right - arm, yy), hair)
    }
    // Centre focus mark.
    val cx = (left + right) / 2
    val cy = (top + bottom) / 2
    val m = 16f * density
    drawRoundRect(color, Offset(cx - m, cy - m * 0.7f), Size(m * 2, m * 1.4f), CornerRadius(3f * density), style = Stroke(1.5f * density))
    // REC dot, top left inside the frame.
    drawCircle(color, 6f * density, Offset(left + arm + 6f * density, top + arm + 4f * density))
}

/** All apps: a faint grid of rounded app squares. */
internal fun DrawScope.drawAppGrid(color: Color) {
    val s = 46f * density
    val gap = 18f * density
    val r = CornerRadius(12f * density)
    var y = gap
    var row = 0
    while (y < size.height) {
        var x = gap + if (row % 2 == 1) (s + gap) / 2 else 0f
        while (x < size.width) {
            drawRoundRect(color, Offset(x, y), Size(s, s), r, style = Stroke(1.5f * density))
            x += s + gap
        }
        y += s + gap
        row++
    }
}

/** About tile: an even grid of small digits, varied in brightness. Lit by the moving light wave at draw time. */
internal fun DrawScope.drawNumberGrid(color: Color, digits: DigitLayouts) {
    val rnd = Random(29)
    val colW = 30f * density
    val rowH = 34f * density
    val glyphScale = 0.62f
    var y = rowH * 0.25f
    while (y < size.height) {
        var x = colW * 0.3f
        while (x < size.width) {
            val layout = digits.layouts[rnd.nextInt(10)]
            val a = 0.45f + rnd.nextFloat() * 0.55f
            scale(glyphScale, glyphScale, pivot = Offset(x, y)) {
                drawText(layout, color = color.copy(alpha = color.alpha * a), topLeft = Offset(x, y))
            }
            x += colW
        }
        y += rowH
    }
}

/** Simple Icons "Netflix" path (CC0), 24-unit viewBox. */
private const val NETFLIX_N =
    "M5.398 0v.006c3.028 8.556 5.37 15.175 8.348 23.596 2.344.058 4.85.398 4.854.398-2.8-7.924-5.923-16.747-8.487-24zm8.489 0v9.63L18.6 22.951c-.043-7.86-.004-15.913.002-22.95zM5.398 1.05V24c1.873-.225 2.81-.312 4.715-.398v-9.22z"

private val netflixN: Path by lazy { PathParser.createPathFromPathData(NETFLIX_N).asComposePath() }

/**
 * Netflix, after its 2019 ident (Imaginary Forces): the ribbon N splitting into thin bands of light.
 * A giant N on the right filled with vertical light streaks, mostly reds with a few spectrum accents,
 * over a deep red base, with a red bloom pooled under it. Drawn in full colour (not a tinted mask).
 */
internal fun DrawScope.drawLumiereN() {
    val w = size.width
    val h = size.height
    val rnd = Random(5)
    // The whole N stays on screen, top right, so it reads as the letter (the tile row is glass over it).
    val nHeight = h * 0.76f
    val s = nHeight / 24f
    val left = w * 0.70f - 5.4f * s
    val top = h * 0.14f

    // Bloom under the letter.
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFE50914).copy(alpha = 0.34f), Color(0xFFE50914).copy(alpha = 0.08f), Color.Transparent),
            center = Offset(left + 12f * s, top + nHeight * 0.85f),
            radius = h * 0.75f,
        ),
        radius = h * 0.75f,
        center = Offset(left + 12f * s, top + nHeight * 0.85f),
    )

    val clip = Path().apply { addPath(netflixN) }
    translate(left, top) {
        scale(s, s, pivot = Offset.Zero) {
            // Deep base so the N reads as a letter.
            drawPath(clip, Color(0xFF5C030A).copy(alpha = 0.95f))
        }
    }
    val palette = listOf(
        0xFFE50914, 0xFFE50914, 0xFFE50914, 0xFFE50914, 0xFFB1060F, 0xFFB1060F, 0xFFB1060F,
        0xFFFF3D57, 0xFFFF3D57, 0xFFFF7A45, 0xFFFFC24B, 0xFFC03CFF, 0xFF4F7BFF, 0xFFFF4FA3,
    ).map { Color(it) }
    val scaled = Path().apply {
        addPath(netflixN)
        transform(androidx.compose.ui.graphics.Matrix().apply { translate(left, top); scale(s, s) })
    }
    clipPathCompat(scaled) {
        val nLeft = left + 5.2f * s
        val nRight = left + 18.8f * s
        var x = nLeft
        while (x < nRight) {
            val sw = (1f + rnd.nextFloat() * 3.5f) * density
            val c = palette[rnd.nextInt(palette.size)]
            val start = top - nHeight * 0.1f + rnd.nextFloat() * nHeight * 0.6f
            val len = nHeight * (0.45f + rnd.nextFloat() * 0.65f)
            val peak = 0.50f + rnd.nextFloat() * 0.40f
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to c.copy(alpha = peak),
                    0.65f to c.copy(alpha = peak * 0.85f),
                    1f to Color.Transparent,
                    startY = start,
                    endY = start + len,
                ),
                topLeft = Offset(x, start),
                size = Size(sw, len),
            )
            x += sw + (0.5f + rnd.nextFloat() * 2.5f) * density
        }
    }
    // A few long light bands rushing off the right side of the letter.
    for (i in 0 until 10) {
        val y = top + nHeight * (0.12f + rnd.nextFloat() * 0.76f)
        val c = palette[rnd.nextInt(palette.size)]
        val x0 = left + 15f * s
        drawRect(
            Brush.horizontalGradient(
                listOf(c.copy(alpha = 0.0f), c.copy(alpha = 0.35f), Color.Transparent),
                startX = x0,
                endX = w,
            ),
            topLeft = Offset(x0, y),
            size = Size(w - x0, (1f + rnd.nextFloat() * 1.5f) * density),
        )
    }
}

private inline fun DrawScope.clipPathCompat(path: Path, block: DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.canvas.clipPath(path)
    block()
    drawContext.canvas.restore()
}
