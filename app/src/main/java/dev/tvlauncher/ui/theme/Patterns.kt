package dev.tvlauncher.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import dev.tvlauncher.core.config.Pattern
import kotlin.random.Random

private const val GRID_STEP_DP = 48f
private const val DOT_STEP_DP = 40f
private const val DOT_RADIUS_DP = 1.5f // 3 dp diameter
private const val RING_COUNT = 5

/** Fraction of the screen width (from the right edge) covered by the digit field. */
const val DIGITS_WIDTH_FRACTION = 0.55f
const val DIGITS_SEED = 42
const val DIGITS_COLUMNS = 14
const val DIGITS_ROWS = 14

/** One digit of the DIGITS pattern. x and y are fractions (0..1) of the digit field. */
data class DigitCell(val xFrac: Float, val yFrac: Float, val digit: Int)

/** Deterministic grid of single digits, one per cell with a little jitter. Pure; same seed gives same output. */
fun digitGrid(
    seed: Int = DIGITS_SEED,
    columns: Int = DIGITS_COLUMNS,
    rows: Int = DIGITS_ROWS,
): List<DigitCell> {
    val rnd = Random(seed)
    val out = ArrayList<DigitCell>(columns * rows)
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val jx = rnd.nextFloat() * 0.5f
            val jy = rnd.nextFloat() * 0.5f
            out += DigitCell((c + jx) / columns, (r + jy) / rows, rnd.nextInt(10))
        }
    }
    return out
}

/** Pre-measured digit layouts (index = digit 0..9) plus the cell list. Built once with remember. */
class DigitLayouts(val cells: List<DigitCell>, val layouts: List<TextLayoutResult>)

/**
 * Draws a faint pattern. Allocation free per frame. DIGITS needs [digits]; without it nothing is drawn.
 */
fun DrawScope.drawPattern(pattern: Pattern, color: Color, digits: DigitLayouts? = null) {
    val w = size.width
    val h = size.height
    when (pattern) {
        Pattern.NONE -> Unit
        Pattern.GRID -> {
            val step = GRID_STEP_DP * density
            val stroke = 1f * density
            var x = 0f
            while (x <= w) {
                drawLine(color, Offset(x, 0f), Offset(x, h), stroke)
                x += step
            }
            var y = 0f
            while (y <= h) {
                drawLine(color, Offset(0f, y), Offset(w, y), stroke)
                y += step
            }
        }
        Pattern.DOTS -> {
            val step = DOT_STEP_DP * density
            val r = DOT_RADIUS_DP * density
            var y = step / 2
            while (y < h) {
                var x = step / 2
                while (x < w) {
                    drawCircle(color, r, Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        Pattern.RINGS -> {
            val c = Offset(w * 0.75f, h * 0.35f)
            val base = h * 0.12f
            val stroke = 2f * density
            for (i in 1..RING_COUNT) {
                drawCircle(color, base * i, c, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            }
        }
        Pattern.DIGITS -> {
            if (digits == null) return
            val fieldW = w * DIGITS_WIDTH_FRACTION
            val left = w - fieldW
            val cells = digits.cells
            for (i in cells.indices) {
                val cell = cells[i]
                drawText(
                    digits.layouts[cell.digit],
                    color = color,
                    topLeft = Offset(left + cell.xFrac * fieldW, cell.yFrac * h),
                )
            }
        }
    }
}
