package dev.tvlauncher.core.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonPrimitive

sealed interface TileSpec {
    val theme: String
    val label: String?
}

data class AppTileSpec(
    val packageName: String,
    override val label: String?,
    override val theme: String,
) : TileSpec

data class HdmiTileSpec(
    val port: Int,
    override val label: String?,
    override val theme: String,
) : TileSpec

data class AllAppsTileSpec(override val theme: String) : TileSpec {
    override val label: String? = "All apps"
}

data class SettingsTileSpec(override val theme: String) : TileSpec {
    override val label: String? = "Settings"
}

/** The "i" tile: opens the About screen (what the launcher is and how to use it). */
data class InfoTileSpec(override val label: String?, override val theme: String) : TileSpec

enum class Pattern { NONE, GRID, DOTS, RINGS, DIGITS, DOODLES, LINES, RIBBONS, VIEWFINDER, TILES, LIGHTGRID, PRISM }

data class ThemeSpec(
    val id: String,
    val top: Long,
    val bottom: Long,
    val accent: Long,
    val pattern: Pattern,
)

object ConfigParser {
    private val json = Json { ignoreUnknownKeys = true }

    val DEFAULT_TILES: List<TileSpec> = listOf(AllAppsTileSpec("default"), SettingsTileSpec("default"))
    val DEFAULT_THEME = ThemeSpec("default", 0xFF1E1B2E, 0xFF0B0A12, 0xFFFFFFFF, Pattern.DIGITS)

    fun tiles(json: String): List<TileSpec> = try {
        val arr = this.json.parseToJsonElement(json) as? JsonArray ?: return DEFAULT_TILES
        arr.mapNotNull { el -> (el as? JsonObject)?.let(::tile) }.ifEmpty { DEFAULT_TILES }
    } catch (e: SerializationException) {
        DEFAULT_TILES
    } catch (e: IllegalArgumentException) {
        DEFAULT_TILES
    }

    fun themes(json: String): Map<String, ThemeSpec> = try {
        val arr = this.json.parseToJsonElement(json) as? JsonArray ?: return emptyMap()
        arr.mapNotNull { el -> (el as? JsonObject)?.let(::theme) }.associateBy { it.id }
    } catch (e: SerializationException) {
        emptyMap()
    } catch (e: IllegalArgumentException) {
        emptyMap()
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun tile(o: JsonObject): TileSpec? {
        return try {
            val theme = o.str("theme") ?: "default"
            val label = o.str("label")
            when (o.str("type")) {
                "app" -> AppTileSpec(o.str("package") ?: return null, label, theme)
                "hdmi" -> HdmiTileSpec(o.int("port") ?: return null, label, theme)
                "allApps" -> AllAppsTileSpec(theme)
                "settings" -> SettingsTileSpec(theme)
                "info" -> InfoTileSpec(label, theme)
                else -> null
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun theme(o: JsonObject): ThemeSpec? {
        return try {
            ThemeSpec(
                id = o.str("id") ?: return null,
                top = color(o.str("top") ?: return null),
                bottom = color(o.str("bottom") ?: return null),
                accent = color(o.str("accent") ?: return null),
                pattern = o.str("pattern")?.let { p ->
                    Pattern.entries.firstOrNull { it.name.equals(p, ignoreCase = true) }
                } ?: Pattern.NONE,
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun color(s: String): Long {
        val hex = s.removePrefix("#")
        require(hex.length == 6) { "bad colour $s" }
        return 0xFF000000L or hex.toLong(16)
    }
}

fun Map<String, ThemeSpec>.themeFor(id: String): ThemeSpec = this[id] ?: ConfigParser.DEFAULT_THEME
