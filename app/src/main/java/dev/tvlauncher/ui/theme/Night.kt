package dev.tvlauncher.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.tvlauncher.core.config.ThemeSpec
import dev.tvlauncher.core.settings.LauncherSettings
import dev.tvlauncher.core.settings.NightMode
import java.util.Calendar

/** Testing override: `adb shell settings put global tvlauncher_night 1` (force night), `0` (force day), `delete` (auto). */
private const val OVERRIDE_KEY = "tvlauncher_night"

private fun isNightNow(context: Context, settings: LauncherSettings): Boolean {
    when (runCatching { Settings.Global.getString(context.contentResolver, OVERRIDE_KEY) }.getOrNull()) {
        "1" -> return true
        "0" -> return false
    }
    return when (settings.nightMode) {
        NightMode.ON -> true
        NightMode.OFF -> false
        NightMode.AUTO -> LauncherSettings.isNightHour(
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            settings.nightStartHour,
            settings.nightEndHour,
        )
    }
}

/**
 * True while night mode is active (launcher setting: Auto by the hour, Always on, or Off).
 * Re-checked every minute and whenever the settings change, so the screen dims on its own.
 */
@Composable
fun rememberIsNight(): Boolean {
    val context = LocalContext.current
    val settings = remember(context) { LauncherSettings(context) }
    var night by remember { mutableStateOf(isNightNow(context, settings)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                night = isNightNow(context, settings)
            }
        }
        val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            night = isNightNow(context, settings)
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        settings.registerListener(prefsListener)
        night = isNightNow(context, settings)
        onDispose {
            context.unregisterReceiver(receiver)
            settings.unregisterListener(prefsListener)
        }
    }
    return night
}

/** Launcher setting value, refreshed when it changes. */
@Composable
fun <T> rememberSetting(read: (LauncherSettings) -> T): T {
    val context = LocalContext.current
    val settings = remember(context) { LauncherSettings(context) }
    var value by remember { mutableStateOf(read(settings)) }
    DisposableEffect(settings) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> value = read(settings) }
        settings.registerListener(l)
        value = read(settings)
        onDispose { settings.unregisterListener(l) }
    }
    return value
}

/** Same hue, a little less saturated, much darker: a bright red becomes a deep oxblood. */
private fun dim(argb: Long, saturation: Float, value: Float): Long {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb.toInt(), hsv)
    hsv[1] *= saturation
    hsv[2] *= value
    return android.graphics.Color.HSVToColor(hsv).toLong() and 0xFFFFFFFFL
}

/** The night version of a theme (gentle on the eyes in a dark room). Pattern and accent are kept. */
fun ThemeSpec.forNight(): ThemeSpec = copy(
    id = "$id@night",
    top = dim(top, 0.85f, 0.40f),
    bottom = dim(bottom, 0.85f, 0.45f),
)
