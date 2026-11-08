package dev.tvlauncher.core.home

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Turns the launcher's HOME alias on or off. The alias is declared disabled in the manifest, so
 * only an explicit ENABLED state counts as on. An app may change its own components freely.
 */
class HomeSwitch(context: Context) {
    private val pm: PackageManager = context.packageManager
    private val alias = ComponentName(context, "dev.tvlauncher.HomeAlias")

    var enabled: Boolean
        get() = try {
            pm.getComponentEnabledSetting(alias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (_: Exception) {
            false
        }
        set(value) {
            try {
                pm.setComponentEnabledSetting(
                    alias,
                    if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            } catch (_: Exception) {
                // Never crash the UI over this.
            }
        }
}
