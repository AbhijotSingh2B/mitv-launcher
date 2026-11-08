package dev.tvlauncher.core.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupGateTest {
    @Test
    fun freshInstallNeverOpensAtBoot() {
        assertFalse(StartupGate.shouldOpenAtBoot(openOnStartup = true, serviceEnabled = false, homeAliasEnabled = false))
    }

    @Test
    fun eitherOptInAllowsIt() {
        assertTrue(StartupGate.shouldOpenAtBoot(openOnStartup = true, serviceEnabled = true, homeAliasEnabled = false))
        assertTrue(StartupGate.shouldOpenAtBoot(openOnStartup = true, serviceEnabled = false, homeAliasEnabled = true))
    }

    @Test
    fun theSettingCanTurnItOff() {
        assertFalse(StartupGate.shouldOpenAtBoot(openOnStartup = false, serviceEnabled = true, homeAliasEnabled = true))
    }
}
