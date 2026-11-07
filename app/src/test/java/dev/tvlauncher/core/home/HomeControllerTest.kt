package dev.tvlauncher.core.home

import android.graphics.Bitmap
import dev.tvlauncher.core.catalog.AppCatalog
import dev.tvlauncher.core.catalog.HdmiInput
import dev.tvlauncher.core.catalog.InputCatalog
import dev.tvlauncher.core.catalog.InstalledApp
import dev.tvlauncher.core.config.AllAppsTileSpec
import dev.tvlauncher.core.config.AppTileSpec
import dev.tvlauncher.core.config.HdmiTileSpec
import dev.tvlauncher.core.config.SettingsTileSpec
import dev.tvlauncher.core.config.TileSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeApps(var apps: List<InstalledApp>) : AppCatalog {
    override fun installedTvApps() = apps
    override fun icon(packageName: String): Bitmap? = null
}

private class FakeInputs(var inputs: List<HdmiInput>) : InputCatalog {
    override fun hdmiInputs() = inputs
}

private class FakeActions(
    var openAppResult: Boolean = true,
    var switchResult: Boolean = true,
    var settingsResult: Boolean = true,
) : Actions {
    val calls = mutableListOf<String>()
    override fun openApp(packageName: String): Boolean { calls += "app:$packageName"; return openAppResult }
    override fun switchInput(inputId: String): Boolean { calls += "input:$inputId"; return switchResult }
    override fun openSettings(): Boolean { calls += "settings"; return settingsResult }
}

class HomeControllerTest {
    private val yt = InstalledApp("com.yt", "YouTube")
    private val actions = FakeActions()

    private fun controller(
        specs: List<TileSpec>,
        apps: List<InstalledApp> = listOf(yt),
        inputs: List<HdmiInput> = listOf(HdmiInput("in1", "HDMI 1")),
    ) = DefaultHomeController(specs, emptyMap(), FakeApps(apps), FakeInputs(inputs), actions)

    @Test
    fun missingAppIsHidden() {
        val c = controller(listOf(AppTileSpec("com.missing", null, "default")))
        assertTrue(c.tiles().none { it.key == "app:com.missing" })
    }

    @Test
    fun appTileUsesInstalledLabelWhenNoneGiven() {
        val tile = controller(listOf(AppTileSpec("com.yt", null, "default"))).tiles().single()
        assertEquals("app:com.yt", tile.key)
        assertEquals("YouTube", tile.label)
        assertEquals("Press OK to open", tile.hint)
        assertEquals(Target.App("com.yt"), tile.target)
    }

    @Test
    fun missingHdmiIsUnavailable() {
        val tile = controller(listOf(HdmiTileSpec(2, null, "default"))).tiles().single()
        assertFalse(tile.available)
        assertEquals("Not available on this TV", tile.hint)
        assertNull(tile.target)
        assertEquals("hdmi:2", tile.key)
    }

    @Test
    fun hdmiTileHasSwitchHint() {
        val tile = controller(listOf(HdmiTileSpec(1, null, "default"))).tiles().single()
        assertTrue(tile.available)
        assertEquals("Press OK to switch", tile.hint)
        assertEquals(Target.Hdmi("in1"), tile.target)
        assertEquals("HDMI 1", tile.label)
    }

    @Test
    fun openFailedAppReturnsMessage() {
        actions.openAppResult = false
        val c = controller(listOf(AppTileSpec("com.yt", null, "default")))
        assertEquals(LaunchResult.Failed("Couldn't open YouTube"), c.open(c.tiles().single()))
    }

    @Test
    fun allAppsTileReturnsShowAllApps() {
        val c = controller(listOf(AllAppsTileSpec("default")))
        assertEquals(LaunchResult.ShowAllApps, c.open(c.tiles().single()))
        assertTrue(actions.calls.isEmpty())
    }

    @Test
    fun unavailableTileIsNotOpened() {
        val c = controller(listOf(HdmiTileSpec(2, null, "default")))
        assertEquals(LaunchResult.Failed("Couldn't open HDMI 2"), c.open(c.tiles().single()))
        assertTrue(actions.calls.none { it.startsWith("input:") })
    }

    @Test
    fun openAppFromAllApps() {
        val c = controller(emptyList())
        assertEquals(LaunchResult.Opened, c.openApp(yt))
        assertEquals(listOf("app:com.yt"), actions.calls)
    }

    @Test
    fun settingsTileOpensSettings() {
        val c = controller(listOf(SettingsTileSpec("default")))
        assertEquals(LaunchResult.Opened, c.open(c.tiles().single()))
        assertEquals(listOf("settings"), actions.calls)
    }
}
