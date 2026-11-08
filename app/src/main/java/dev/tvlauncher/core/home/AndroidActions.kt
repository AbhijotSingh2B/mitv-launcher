package dev.tvlauncher.core.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.provider.Settings

class AndroidActions(
    private val context: Context,
    /** Called when the user opens the TV's own home screen on purpose. */
    private val onTvHomeOpened: () -> Unit = {},
) : Actions {

    override fun openApp(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        return start(intent)
    }

    override fun switchInput(inputId: String): Boolean =
        start(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId)))

    override fun openSettings(): Boolean = start(Intent(Settings.ACTION_SETTINGS))

    override fun openTvHome(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homes = context.packageManager.queryIntentActivities(home, 0)
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName && !it.name.endsWith("FallbackHome") }
        val pick = TV_HOMES.firstNotNullOfOrNull { p -> homes.firstOrNull { it.packageName == p } }
            ?: homes.firstOrNull()
            ?: return false
        onTvHomeOpened()
        return start(Intent(home).setClassName(pick.packageName, pick.name))
    }

    private companion object {
        /** Preferred stock home screens: Google TV Home, then PatchWall. */
        val TV_HOMES = listOf("com.google.android.tvlauncher", "com.mitv.tvhome.atv")
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    } catch (e: RuntimeException) {
        false
    }
}
