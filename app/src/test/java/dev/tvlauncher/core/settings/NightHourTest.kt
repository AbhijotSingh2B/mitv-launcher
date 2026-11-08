package dev.tvlauncher.core.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightHourTest {
    @Test
    fun wrapsPastMidnight() {
        assertTrue(LauncherSettings.isNightHour(22, 22, 6))
        assertTrue(LauncherSettings.isNightHour(23, 22, 6))
        assertTrue(LauncherSettings.isNightHour(0, 22, 6))
        assertTrue(LauncherSettings.isNightHour(5, 22, 6))
        assertFalse(LauncherSettings.isNightHour(6, 22, 6))
        assertFalse(LauncherSettings.isNightHour(12, 22, 6))
        assertFalse(LauncherSettings.isNightHour(21, 22, 6))
    }

    @Test
    fun sameDayWindow() {
        assertTrue(LauncherSettings.isNightHour(1, 0, 7))
        assertFalse(LauncherSettings.isNightHour(7, 0, 7))
        assertFalse(LauncherSettings.isNightHour(23, 0, 7))
    }

    @Test
    fun emptyWindowIsNeverNight() {
        for (h in 0..23) assertFalse(LauncherSettings.isNightHour(h, 22, 22))
    }
}
