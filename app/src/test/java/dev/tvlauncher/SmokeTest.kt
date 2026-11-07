package dev.tvlauncher

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun packageIsCorrect() {
        assertEquals("dev.tvlauncher", BuildConfig.APPLICATION_ID)
    }
}
