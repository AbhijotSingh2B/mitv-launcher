package dev.tvlauncher.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitGridTest {
    @Test fun deterministic() = assertEquals(digitGrid(), digitGrid())

    @Test fun differentSeedDiffers() = assertTrue(digitGrid(42) != digitGrid(7))

    @Test fun inBounds() {
        val g = digitGrid()
        assertEquals(DIGITS_COLUMNS * DIGITS_ROWS, g.size)
        assertTrue(g.all { it.xFrac in 0f..1f && it.yFrac in 0f..1f && it.digit in 0..9 })
    }
}
