package dev.tvlauncher.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Scattered outline doodles (circles, squares, stars, triangles, plus signs) on a jittered grid. Seeded, so stable. */
internal fun DrawScope.drawDoodles(color: Color) {
    val rnd = Random(7)
    val cols = 9
    val rows = 5
    val cw = size.width / cols
    val ch = size.height / rows
    val stroke = Stroke(1.6f * density)
    for (r in 0 until rows) for (c in 0 until cols) {
        val cx = (c + 0.2f + rnd.nextFloat() * 0.6f) * cw
        val cy = (r + 0.2f + rnd.nextFloat() * 0.6f) * ch
        val rad = (14f + rnd.nextFloat() * 14f) * density
        val rot = rnd.nextFloat() * 60f - 30f
        val kind = rnd.nextInt(6)
        rotate(rot, Offset(cx, cy)) {
            when (kind) {
                0 -> drawCircle(color, rad, Offset(cx, cy), style = stroke)
                1 -> {
                    drawCircle(color, rad, Offset(cx, cy), style = stroke)
                    drawCircle(color, rad * 0.55f, Offset(cx, cy), style = stroke)
                }
                2 -> drawRoundRect(
                    color, Offset(cx - rad, cy - rad), Size(rad * 2, rad * 2), CornerRadius(rad * 0.3f), style = stroke,
                )
                3 -> drawPath(starPath(cx, cy, rad), color, style = stroke)
                4 -> drawPlayButton(cx, cy, rad, color, stroke)
                else -> {
                    drawLine(color, Offset(cx - rad, cy), Offset(cx + rad, cy), stroke.width)
                    drawLine(color, Offset(cx, cy - rad), Offset(cx, cy + rad), stroke.width)
                }
            }
        }
    }
}

private fun polyPath(cx: Float, cy: Float, rad: Float, n: Int): Path {
    val p = Path()
    for (i in 0 until n) {
        val a = (-90.0 + i * 360.0 / n) * Math.PI / 180.0
        val x = cx + rad * cos(a).toFloat()
        val y = cy + rad * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

private fun starPath(cx: Float, cy: Float, rad: Float): Path {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) rad else rad * 0.45f
        val a = (-90.0 + i * 36.0) * Math.PI / 180.0
        val x = cx + rr * cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

/** A handful of long, slow sine contours, like a faint line drawing. */
internal fun DrawScope.drawFlowLines(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(1.5f * density)
    for (i in 0 until 9) {
        val path = Path()
        val base = h * (0.1f + i * 0.1f)
        val amp = h * (0.05f + 0.01f * i)
        var x = 0f
        while (x <= w) {
            val y = base + amp * sin((x / w) * 6.28f * 1.3f + i * 0.7f)
            if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
            x += 12f * density
        }
        drawPath(path, color, style = stroke)
    }
}

/**
 * A blueprint grid with a test-card reticle on the right (rings, crosshair, four pointer wedges and
 * tick marks), like the reference's TV Settings theme. Rendered once into the cached background.
 */
internal fun DrawScope.drawReticle(color: Color) {
    val w = size.width
    val h = size.height
    val hair = 1f * density
    // Grid, faint.
    val step = 36f * density
    val gridColor = color.copy(alpha = color.alpha * 0.45f)
    var x = 0f
    while (x <= w) { drawLine(gridColor, Offset(x, 0f), Offset(x, h), hair); x += step }
    var y = 0f
    while (y <= h) { drawLine(gridColor, Offset(0f, y), Offset(w, y), hair); y += step }

    val c = Offset(w * 0.75f, h * 0.34f)
    val r = h * 0.17f
    val line = Stroke(1.5f * density)
    drawCircle(color, r, c, style = line)
    drawCircle(color, r * 0.62f, c, style = line)
    drawCircle(color, r * 0.16f, c, style = line)
    // Crosshair extending past the outer ring.
    drawLine(color, Offset(c.x - r * 1.25f, c.y), Offset(c.x + r * 1.25f, c.y), 1.5f * density)
    drawLine(color, Offset(c.x, c.y - r * 1.25f), Offset(c.x, c.y + r * 1.25f), 1.5f * density)
    // Four wedges pointing at the centre, between the inner and outer rings.
    val strong = color.copy(alpha = (color.alpha * 1.3f).coerceAtMost(1f))
    for (i in 0 until 4) {
        rotate(45f + i * 90f, c) {
            val p = Path().apply {
                moveTo(c.x, c.y - r * 0.24f)
                lineTo(c.x - r * 0.13f, c.y - r * 0.92f)
                lineTo(c.x + r * 0.13f, c.y - r * 0.92f)
                close()
            }
            drawPath(p, strong)
        }
    }
    // Tick marks around the outer ring.
    for (i in 0 until 36) {
        rotate(i * 10f, c) {
            val len = if (i % 3 == 0) r * 0.08f else r * 0.04f
            drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y - r - len), hair)
        }
    }
    // Small registration crosses in the corners of the reticle's box.
    for ((sx, sy) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) {
        val p = Offset(c.x + sx * r * 1.25f, c.y + sy * r * 1.15f)
        drawLine(color, Offset(p.x - 6 * density, p.y), Offset(p.x + 6 * density, p.y), hair)
        drawLine(color, Offset(p.x, p.y - 6 * density), Offset(p.x, p.y + 6 * density), hair)
    }
}

/** YouTube-ish doodles for the red theme: outlined play buttons mixed with the generic shapes. */
internal fun DrawScope.drawPlayButton(cx: Float, cy: Float, rad: Float, color: Color, stroke: Stroke) {
    drawRoundRect(
        color, Offset(cx - rad, cy - rad * 0.7f), Size(rad * 2, rad * 1.4f), CornerRadius(rad * 0.4f), style = stroke,
    )
    val p = Path().apply {
        moveTo(cx - rad * 0.28f, cy - rad * 0.36f)
        lineTo(cx + rad * 0.42f, cy)
        lineTo(cx - rad * 0.28f, cy + rad * 0.36f)
        close()
    }
    drawPath(p, color, style = stroke)
}

internal fun hypot2(dx: Float, dy: Float): Float = sqrt(dx * dx + dy * dy)
