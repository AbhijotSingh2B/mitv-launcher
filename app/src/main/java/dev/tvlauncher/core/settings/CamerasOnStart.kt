package dev.tvlauncher.core.settings

/** What woke the launcher. */
enum class WakeTrigger { SCREEN_ON, COLD_START }

/** Decides whether "Open Cameras when the TV turns on" should switch input now. Pure, so it is unit tested. */
object CamerasOnStart {
    /** Only a home launch this soon after boot counts as "the TV turned on". */
    const val BOOT_WINDOW_MS = 5 * 60 * 1000L

    fun shouldOpen(enabled: Boolean, trigger: WakeTrigger, uptimeMs: Long, alreadyHandledColdStart: Boolean): Boolean {
        if (!enabled) return false
        return when (trigger) {
            WakeTrigger.SCREEN_ON -> true
            WakeTrigger.COLD_START -> !alreadyHandledColdStart && uptimeMs in 0 until BOOT_WINDOW_MS
        }
    }
}
