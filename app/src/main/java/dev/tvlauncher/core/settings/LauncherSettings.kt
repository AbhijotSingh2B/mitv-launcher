package dev.tvlauncher.core.settings

import android.content.Context
import android.content.SharedPreferences

enum class NightMode(val label: String) { AUTO("Auto"), ON("Always on"), OFF("Off") }

/**
 * The launcher's own settings, stored in SharedPreferences. Every read has a safe default, so a
 * missing or corrupt value never breaks the home screen.
 */
class LauncherSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    var nightMode: NightMode
        get() = runCatching { NightMode.valueOf(prefs.getString(KEY_NIGHT_MODE, null) ?: "") }
            .getOrDefault(NightMode.AUTO)
        set(v) = prefs.edit().putString(KEY_NIGHT_MODE, v.name).apply()

    /** Hour (0-23) night starts in Auto mode. */
    var nightStartHour: Int
        get() = prefs.getInt(KEY_NIGHT_START, 22).coerceIn(0, 23)
        set(v) = prefs.edit().putInt(KEY_NIGHT_START, v.coerceIn(0, 23)).apply()

    /** Hour (0-23) night ends in Auto mode. */
    var nightEndHour: Int
        get() = prefs.getInt(KEY_NIGHT_END, 6).coerceIn(0, 23)
        set(v) = prefs.edit().putInt(KEY_NIGHT_END, v.coerceIn(0, 23)).apply()

    /** Switch straight to the Cameras input when the launcher first starts after the TV boots. Off by default. */
    var openCamerasOnStart: Boolean
        get() = prefs.getBoolean(KEY_CAMERAS_ON_START, false)
        set(v) = prefs.edit().putBoolean(KEY_CAMERAS_ON_START, v).apply()

    /** Calmer motion: no tile lift, no rising overlay text. Also on when the system turns animations off. */
    var reduceMotion: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_MOTION, false)
        set(v) = prefs.edit().putBoolean(KEY_REDUCE_MOTION, v).apply()

    /** The badge next to the clock while night mode is active. */
    var showNightBadge: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_BADGE, true)
        set(v) = prefs.edit().putBoolean(KEY_NIGHT_BADGE, v).apply()

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(l)

    companion object {
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_NIGHT_START = "night_start_hour"
        private const val KEY_NIGHT_END = "night_end_hour"
        private const val KEY_CAMERAS_ON_START = "open_cameras_on_start"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_NIGHT_BADGE = "show_night_badge"

        /** Whether [hour] falls in the night window [start, end), which may wrap past midnight. */
        fun isNightHour(hour: Int, start: Int, end: Int): Boolean = when {
            start == end -> false
            start < end -> hour in start until end
            else -> hour >= start || hour < end
        }
    }
}
