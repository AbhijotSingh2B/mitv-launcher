package dev.tvlauncher.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dev.tvlauncher.App
import dev.tvlauncher.ui.home.HomeActivity

/**
 * Makes the remote's Home button (and the TV starting up) open this launcher.
 *
 * Why a service: Google TV Home is a system app whose HOME filter has priority 2. A normal app's
 * filter is capped at 0, so Android always picks Google TV Home, shows no chooser and ignores
 * "set default home". This service, which the user switches on in Settings > Accessibility:
 * - catches short Home presses and opens this launcher instead, and
 * - when Google TV Home appears on its own (after start-up or waking), replaces it with this
 *   launcher, unless the user opened it on purpose from the "TV home" pill, and
 * - replaces PatchWall when it opens itself during the first minutes after the TV starts.
 * It reads no screen content: it only sees key events and the class name of the window in front.
 */
class HomeButtonService : AccessibilityService() {

    private val app get() = application as App

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Connected during start-up: the TV is probably showing Google TV Home or PatchWall right now.
        // Not on a later connect (the user just switched the service on, or the system rebound it).
        if (app.settings.homeButtonOpensLauncher && app.settings.openOnStartup && inStartupWindow() && !app.tvHomeRequested) {
            openLauncher()
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME || !app.settings.homeButtonOpensLauncher) return false
        // The first DOWN is consumed before a short press can be told from a long one, so the system's
        // long-press Home action doesn't run while this service is on. A short press opens this
        // launcher on release; a held Home (released after LONG_PRESS_MS) does nothing.
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled &&
            event.eventTime - event.downTime < LONG_PRESS_MS
        ) {
            openLauncher()
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString().orEmpty()
        when {
            pkg == GOOGLE_HOME_PACKAGE && cls == GOOGLE_HOME_ACTIVITY -> {
                if (app.settings.homeButtonOpensLauncher && !app.tvHomeRequested) openLauncher()
            }
            // PatchWall opens itself as the TV starts. Only in the first minutes after boot, so
            // opening PatchWall from All apps later still works.
            pkg == PATCHWALL_PACKAGE -> {
                if (app.settings.openOnStartup && inStartupWindow() && !app.tvHomeRequested) openLauncher()
            }
        }
        // The "opened on purpose" pass (TV home pill) ends only when this launcher is back in front
        // (HomeActivity.onResume). Voice search, permission dialogs and overlays fire window events
        // from other packages and must not end it.
    }

    override fun onInterrupt() = Unit

    private fun inStartupWindow() = SystemClock.elapsedRealtime() < STARTUP_WINDOW_MS

    private fun openLauncher() {
        runCatching {
            startActivity(
                Intent(this, HomeActivity::class.java)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            )
        }
    }

    companion object {
        private const val GOOGLE_HOME_PACKAGE = "com.google.android.tvlauncher"
        private const val GOOGLE_HOME_ACTIVITY = "com.google.android.tvlauncher.MainActivity"
        private const val LONG_PRESS_MS = 500L
        private const val PATCHWALL_PACKAGE = "com.mitv.tvhome.atv"
        private const val STARTUP_WINDOW_MS = 3 * 60_000L

        /** Whether the user has switched this service on in Settings > Accessibility. */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            return enabled.split(':').any { it.startsWith(context.packageName + "/") && it.endsWith("HomeButtonService") }
        }
    }
}
