package dev.tvlauncher.ui.theme

import dev.tvlauncher.core.config.Pattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every pattern has a style, and full-colour patterns are drawn without the accent tint or a left fade. */
class PatternStyleTest {
    @Test
    fun everyPatternHasAStyleInRange() {
        for (p in Pattern.entries) {
            val s = styleOf(p)
            assertTrue("$p alpha", s.alpha in 0f..1f)
            assertTrue("$p leftFade", s.leftFade in 0f..1f)
        }
    }

    @Test
    fun fullColourPatternsAreTheBrandOnes() {
        assertEquals(setOf(Pattern.RIBBONS, Pattern.PRISM), Pattern.entries.filter { styleOf(it).fullColour }.toSet())
    }
}
