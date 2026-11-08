package dev.tvlauncher.core.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CamerasOnStartTest {
    @Test
    fun settingOffNeverOpens() {
        for (t in WakeTrigger.entries) {
            assertFalse(CamerasOnStart.shouldOpen(false, t, 1_000L, alreadyHandledColdStart = false))
            assertFalse(CamerasOnStart.shouldOpen(false, t, 10 * 60_000L, alreadyHandledColdStart = true))
        }
    }

    @Test
    fun screenOnOpensWhenEnabled() {
        assertTrue(CamerasOnStart.shouldOpen(true, WakeTrigger.SCREEN_ON, 10 * 60 * 60_000L, alreadyHandledColdStart = true))
    }

    @Test
    fun coldStartOnlyJustAfterBootAndOnce() {
        assertTrue(CamerasOnStart.shouldOpen(true, WakeTrigger.COLD_START, 60_000L, alreadyHandledColdStart = false))
        assertFalse(CamerasOnStart.shouldOpen(true, WakeTrigger.COLD_START, 60_000L, alreadyHandledColdStart = true))
        assertFalse(CamerasOnStart.shouldOpen(true, WakeTrigger.COLD_START, CamerasOnStart.BOOT_WINDOW_MS, alreadyHandledColdStart = false))
    }
}
