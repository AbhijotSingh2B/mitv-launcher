package dev.tvlauncher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import dev.tvlauncher.core.catalog.AndroidAppCatalog
import dev.tvlauncher.core.catalog.AndroidInputCatalog
import dev.tvlauncher.core.config.ConfigParser
import dev.tvlauncher.core.home.AndroidActions
import dev.tvlauncher.core.home.DefaultHomeController
import dev.tvlauncher.core.home.HomeController
import dev.tvlauncher.core.home.Target
import dev.tvlauncher.core.settings.CamerasOnStart
import dev.tvlauncher.core.settings.LauncherSettings
import dev.tvlauncher.core.settings.WakeTrigger

class App : Application() {
    lateinit var catalog: AndroidAppCatalog
        private set
    lateinit var controller: HomeController
        private set
    lateinit var settings: LauncherSettings
        private set

    /** True while the user is on Google TV Home by choice (the "TV home" pill), so it isn't replaced. */
    @Volatile var tvHomeRequested = false

    private var startupHandled = false

    override fun onCreate() {
        super.onCreate()
        catalog = AndroidAppCatalog(this)
        settings = LauncherSettings(this)
        val tiles = ConfigParser.tiles(readAsset("tiles.json"))
        val themes = ConfigParser.themes(readAsset("themes.json"))
        controller = DefaultHomeController(
            tiles, themes, catalog, AndroidInputCatalog(this), AndroidActions(this) { tvHomeRequested = true },
            rowApps = { settings.homeRowApps },
            shortcutApps = { settings.shortcutApps },
        )

        // "Open Cameras when the TV turns on": waking from standby turns the screen on. SCREEN_ON can only be
        // received by a runtime receiver, so this lives as long as the launcher process (kept alive as HOME).
        runCatching {
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (CamerasOnStart.shouldOpen(settings.openCamerasOnStart, WakeTrigger.SCREEN_ON, SystemClock.elapsedRealtime(), true)) {
                        openCameras()
                    }
                }
            }, IntentFilter(Intent.ACTION_SCREEN_ON))
        }
    }

    /** Called when home is first created: after a cold boot, honour "Open Cameras when the TV turns on". */
    fun maybeOpenCamerasAtStartup() {
        val open = CamerasOnStart.shouldOpen(
            settings.openCamerasOnStart, WakeTrigger.COLD_START, SystemClock.elapsedRealtime(), startupHandled,
        )
        startupHandled = true
        if (open) openCameras()
    }

    private fun openCameras() {
        runCatching {
            val hdmi = controller.tiles().filter { it.target is Target.Hdmi && it.available }
            val tile = hdmi.firstOrNull { it.label.contains("cam", ignoreCase = true) } ?: hdmi.firstOrNull()
            tile?.let { controller.open(it) }
        }
    }

    private fun readAsset(name: String): String =
        runCatching { assets.open(name).bufferedReader().use { it.readText() } }.getOrDefault("")
}
