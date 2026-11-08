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
        // Connected after a boot: the TV is probably showing Google TV Home right now.
        if (app.settings.homeButtonOpensLauncher && app.settings.openOnStartup) openLauncher()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME || !app.settings.homeButtonOpensLauncher) return false
        // Consume the press and act on release. The first DOWN is consumed before we can tell a short
        // press from a long one, so a held Home is swallowed too: the system's long-press Home action
        // doesn't run while this service is on.
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled && event.repeatCount == 0) openLauncher()
        return event.repeatCount == 0
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
                if (app.settings.openOnStartup && SystemClock.elapsedRealtime() < STARTUP_WINDOW_MS) openLauncher()
            }
            // Leaving Google TV Home for a real app ends the "opened on purpose" pass.
            pkg != GOOGLE_HOME_PACKAGE && pkg != SYSTEM_UI && pkg != packageName -> app.tvHomeRequested = false
        }
    }

    override fun onInterrupt() = Unit

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
        private const val SYSTEM_UI = "com.android.systemui"
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
