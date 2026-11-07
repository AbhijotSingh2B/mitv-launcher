package dev.tvlauncher.core.home

import dev.tvlauncher.core.catalog.AppCatalog
import dev.tvlauncher.core.catalog.InputCatalog
import dev.tvlauncher.core.catalog.InstalledApp
import dev.tvlauncher.core.catalog.matchHdmiInput
import dev.tvlauncher.core.config.AllAppsTileSpec
import dev.tvlauncher.core.config.AppTileSpec
import dev.tvlauncher.core.config.HdmiTileSpec
import dev.tvlauncher.core.config.SettingsTileSpec
import dev.tvlauncher.core.config.ThemeSpec
import dev.tvlauncher.core.config.TileSpec
import dev.tvlauncher.core.config.themeFor

sealed interface Target {
    data class App(val packageName: String) : Target
    data class Hdmi(val inputId: String) : Target
    data object AllApps : Target
    data object Settings : Target
}

data class HomeTile(
    val key: String,
    val label: String,
    val hint: String,
    val theme: ThemeSpec,
    val target: Target?,
    val available: Boolean,
)

sealed interface LaunchResult {
    data object Opened : LaunchResult
    data object ShowAllApps : LaunchResult
    data class Failed(val message: String) : LaunchResult
}

interface Actions {
    fun openApp(packageName: String): Boolean
    fun switchInput(inputId: String): Boolean
    fun openSettings(): Boolean
}

interface HomeController {
    fun tiles(): List<HomeTile>
    fun allApps(): List<InstalledApp>
    fun open(tile: HomeTile): LaunchResult
    fun openApp(app: InstalledApp): LaunchResult
}

class DefaultHomeController(
    private val specs: List<TileSpec>,
    private val themes: Map<String, ThemeSpec>,
    private val apps: AppCatalog,
    private val inputs: InputCatalog,
    private val actions: Actions,
) : HomeController {

    override fun tiles(): List<HomeTile> {
        val installed = apps.installedTvApps().associateBy { it.packageName }
        val hdmi = inputs.hdmiInputs()
        return buildTiles(installed, hdmi).distinctBy { it.key }
    }

    private fun buildTiles(
        installed: Map<String, InstalledApp>,
        hdmi: List<dev.tvlauncher.core.catalog.HdmiInput>,
    ): List<HomeTile> {
        return specs.mapNotNull { spec ->
            val theme = themes.themeFor(spec.theme)
            when (spec) {
                is AppTileSpec -> installed[spec.packageName]?.let { app ->
                    HomeTile(
                        key = "app:${spec.packageName}",
                        label = spec.label ?: app.label,
                        hint = HINT_OPEN,
                        theme = theme,
                        target = Target.App(spec.packageName),
                        available = true,
                    )
                }
                is HdmiTileSpec -> {
                    val match = matchHdmiInput(spec.port, hdmi)
                    HomeTile(
                        key = "hdmi:${spec.port}",
                        label = spec.label ?: "HDMI ${spec.port}",
                        hint = if (match != null) HINT_SWITCH else HINT_UNAVAILABLE,
                        theme = theme,
                        target = match?.let { Target.Hdmi(it.id) },
                        available = match != null,
                    )
                }
                is AllAppsTileSpec -> HomeTile("allApps", spec.label ?: "All apps", HINT_OPEN, theme, Target.AllApps, true)
                is SettingsTileSpec -> HomeTile("settings", spec.label ?: "Settings", HINT_OPEN, theme, Target.Settings, true)
            }
        }
    }

    override fun allApps(): List<InstalledApp> = apps.installedTvApps()

    override fun open(tile: HomeTile): LaunchResult {
        val ok = when (val target = tile.target) {
            null -> false
            Target.AllApps -> return LaunchResult.ShowAllApps
            is Target.App -> safely { actions.openApp(target.packageName) }
            is Target.Hdmi -> safely { actions.switchInput(target.inputId) }
            Target.Settings -> safely { actions.openSettings() }
        }
        return if (ok) LaunchResult.Opened else LaunchResult.Failed("Couldn't open ${tile.label}")
    }

    override fun openApp(app: InstalledApp): LaunchResult =
        if (safely { actions.openApp(app.packageName) }) LaunchResult.Opened else LaunchResult.Failed("Couldn't open ${app.label}")

    private fun safely(block: () -> Boolean): Boolean = runCatching(block).getOrDefault(false)

    private companion object {
        const val HINT_OPEN = "Press OK to open"
        const val HINT_SWITCH = "Press OK to switch"
        const val HINT_UNAVAILABLE = "Not available on this TV"
    }
}
