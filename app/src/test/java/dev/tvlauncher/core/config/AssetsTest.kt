package dev.tvlauncher.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The shipped tiles.json and themes.json parse fully and reference each other correctly.\s*/
class AssetsTest {
    private fun asset(name: String): String {
        // Unit tests run from the app module, or from the project root in some setups.
        val f = listOf(File("src/main/assets/$name"), File("app/src/main/assets/$name")).first { it.exists() }
        return f.readText()
    }

    private val tilesJson = asset("tiles.json")
    private val themesJson = asset("themes.json")

    @Test
    fun everyTileParses() {
        val raw = Regex("[{]").findAll(tilesJson).count()
        assertEquals("a tile in tiles.json was dropped by the parser", raw, ConfigParser.tiles(tilesJson).size)
    }

    @Test
    fun everyThemeParsesAndIdsAreUnique() {
        val raw = Regex("[{]").findAll(themesJson).count()
        val themes = ConfigParser.themes(themesJson)
        assertEquals("a theme in themes.json was dropped or duplicated", raw, themes.size)
    }

    @Test
    fun everyTileThemeExists() {
        val themes = ConfigParser.themes(themesJson)
        val missing = ConfigParser.tiles(tilesJson).map { it.theme }.filterNot { it in themes }
        assertTrue("tiles reference unknown themes: $missing", missing.isEmpty())
    }

    @Test
    fun everyPatternNameIsKnown() {
        // An unknown name would silently become Pattern.NONE (a blank background).
        val names = Regex("\"pattern\"\\s*:\\s*\"(\\w+)\"").findAll(themesJson).map { it.groupValues[1] }.toList()
        val unknown = names.filter { n -> Pattern.entries.none { it.name.equals(n, ignoreCase = true) } }
        assertTrue("unknown patterns in themes.json: $unknown", unknown.isEmpty())
    }
}
