package dev.tvlauncher.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigTest {
    @Test
    fun parsesAllFourTileTypes() {
        val json = """[
            {"type":"app","package":"com.example.app","label":"Ex","theme":"red"},
            {"type":"hdmi","port":2,"label":"HDMI 2","theme":"purple"},
            {"type":"allApps","theme":"grey"},
            {"type":"settings","theme":"grid"}
        ]"""
        val specs = ConfigParser.tiles(json)
        assertEquals(4, specs.size)
        assertTrue(specs[0] is AppTileSpec)
        assertEquals("com.example.app", (specs[0] as AppTileSpec).packageName)
        assertTrue(specs[1] is HdmiTileSpec)
        assertEquals(2, (specs[1] as HdmiTileSpec).port)
        assertTrue(specs[2] is AllAppsTileSpec)
        assertTrue(specs[3] is SettingsTileSpec)
    }

    @Test
    fun unknownTileTypeIsSkipped() {
        val json = """[
            {"type":"allApps","theme":"grey"},
            {"type":"weather"},
            {"type":"settings","theme":"grid"}
        ]"""
        assertEquals(2, ConfigParser.tiles(json).size)
    }

    @Test
    fun malformedJsonFallsBackToDefaults() {
        assertEquals(ConfigParser.DEFAULT_TILES, ConfigParser.tiles("{not json"))
    }

    @Test
    fun parsesThemeColoursAndPattern() {
        val json = """[{"id":"red","top":"#C8102E","bottom":"#7A0019","accent":"#FFFFFF","pattern":"dots"}]"""
        val t = ConfigParser.themes(json).getValue("red")
        assertEquals(0xFFC8102EL, t.top)
        assertEquals(Pattern.DOTS, t.pattern)
    }

    @Test
    fun unknownThemeUsesDefault() {
        val json = """[{"id":"red","top":"#C8102E","bottom":"#7A0019","accent":"#FFFFFF","pattern":"dots"}]"""
        assertEquals(ConfigParser.DEFAULT_THEME, ConfigParser.themes(json).themeFor("nope"))
    }
}
