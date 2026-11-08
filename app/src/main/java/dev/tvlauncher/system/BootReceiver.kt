package dev.tvlauncher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.tvlauncher.App
import dev.tvlauncher.core.home.HomeSwitch
import dev.tvlauncher.core.settings.StartupGate
import dev.tvlauncher.ui.home.HomeActivity

/**
 * Opens this launcher when the TV finishes starting up ("Open at start-up" in Launcher settings),
 * but only once the user has opted in to it as their home (see [StartupGate]).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? App ?: return
        val allowed = StartupGate.shouldOpenAtBoot(
            openOnStartup = app.settings.openOnStartup,
            serviceEnabled = HomeButtonService.isEnabled(context),
            homeAliasEnabled = HomeSwitch(context).enabled,
        )
        if (!allowed) return
        runCatching {
            context.startActivity(
                Intent(context, HomeActivity::class.java)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
