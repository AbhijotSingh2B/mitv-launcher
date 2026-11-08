package dev.tvlauncher.ui.info

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.core.config.ThemeSpec
import dev.tvlauncher.ui.home.Clock
import dev.tvlauncher.ui.home.Mark
import dev.tvlauncher.ui.home.MarkImage
import dev.tvlauncher.ui.home.NightBadge
import dev.tvlauncher.ui.theme.Emphasized
import dev.tvlauncher.ui.theme.Poppins
import dev.tvlauncher.ui.theme.ThemedBackground
import dev.tvlauncher.ui.theme.forNight
import dev.tvlauncher.ui.theme.rememberIsNight
import dev.tvlauncher.ui.theme.rememberReduceMotion
import dev.tvlauncher.ui.theme.rememberSetting

/** Material Symbols Rounded "info" (filled), Apache 2.0. */
internal const val INFO_ICON =
    "M480-280q17 0 28.5-11.5T520-320v-160q0-17-11.5-28.5T480-520q-17 0-28.5 11.5T440-480v160q0 17 11.5 28.5T480-280Zm0-320q17 0 28.5-11.5T520-640q0-17-11.5-28.5T480-680q-17 0-28.5 11.5T440-640q0 17 11.5 28.5T480-600Zm0 520q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Z"

private val Title = TextStyle(
    fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp,
    letterSpacing = (-0.8).sp, color = Color.White,
)
private val Body = TextStyle(fontFamily = Poppins, fontSize = 13.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = 0.78f))
private val Heading = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = Color.White)

private fun hourLabel(h: Int): String = when {
    h == 0 -> "12 am"
    h < 12 -> "$h am"
    h == 12 -> "12 pm"
    else -> "${h - 12} pm"
}

private sealed interface Line
private class Para(val text: String) : Line
private class Key(val key: String, val text: String) : Line
private class Section(val title: String, val lines: List<Line>)

/**
 * What this launcher is and how to use it, over the About tile's animated light-wave grid.
 * Cards are focusable so the D-pad scrolls through them on the 540 dp tall screen. Back returns home.
 */
@Composable
fun AboutScreen(theme: ThemeSpec, version: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val night = rememberIsNight()
    val reduceMotion = rememberReduceMotion() || rememberSetting { it.reduceMotion }
    val showBadge = rememberSetting { it.showNightBadge }
    val start = rememberSetting { it.nightStartHour }
    val end = rememberSetting { it.nightEndHour }
    val info = remember { Mark(INFO_ICON, null) }

    val sections = listOf(
        Section(
            "What it is",
            listOf(
                Para(
                    "A calm, fast home screen made for this Mi TV, inspired by the Frame TV reel. One row of " +
                        "tiles opens your apps and switches inputs, and the whole screen takes on the colours of " +
                        "whatever you select.",
                ),
            ),
        ),
        Section(
            "Getting around",
            listOf(
                Key("◀ ▶", "Move between tiles"),
                Key("OK", "Open the selected tile"),
                Key("Back", "Does nothing on home, so you never fall out by accident"),
                Key("Home", "Always brings you back here"),
            ),
        ),
        Section(
            "The Menu button",
            listOf(
                Key("Press", "Launcher settings: night mode, Cameras at start-up, reduce motion"),
                Key("Hold", "Home setup: make this your home screen"),
            ),
        ),
        Section(
            "Your tiles",
            listOf(
                Para(
                    "Nuvio, Cameras (HDMI 2), YouTube, Netflix and Prime Video open straight away. All apps lists " +
                        "every app, including PatchWall. Settings lets you choose Launcher settings or TV settings.",
                ),
            ),
        ),
        Section(
            "Night mode",
            listOf(
                Para(
                    "From ${hourLabel(start)} to ${hourLabel(end)} the colours dim so they're easy on your eyes, " +
                        "and a moon shows next to the clock. Change the hours, or turn it on or off, in Launcher settings.",
                ),
            ),
        ),
        Section(
            "Made with",
            listOf(
                Para(
                    "Kotlin and Jetpack Compose for TV. Poppins font, Simple Icons and Material Symbols " +
                        "(licences in licenses/NOTICE.md). Version $version.",
                ),
            ),
        ),
    )

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ThemedBackground(if (night) theme.forNight() else theme, night = night, reduceMotion = reduceMotion)
        Row(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp)) {
            Column(Modifier.width(320.dp).padding(top = 118.dp)) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    MarkImage(info, Color.White.copy(alpha = 0.92f), Modifier.size(30.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text("Your TV,\nyour way", style = Title)
                Spacer(Modifier.height(10.dp))
                Text("Everything you need to know about this launcher.", style = Body)
            }
            LazyColumn(
                // Starts below the clock, so scrolled cards never pass behind it.
                modifier = Modifier.weight(1f).fillMaxSize().padding(top = 76.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp, start = 24.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sections.size) { i ->
                    InfoCard(sections[i], if (i == 0) Modifier.focusRequester(first) else Modifier)
                }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            if (night && showBadge) NightBadge(Modifier.padding(end = 12.dp))
            Clock()
        }
    }
}

/** A reading card: glass at rest; when focused it brightens and gains a white edge (no white flash). */
@Composable
private fun InfoCard(section: Section, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val t by animateFloatAsState(if (focused) 1f else 0f, tween(150, easing = Emphasized), label = "info")
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val r = CornerRadius(14.dp.toPx())
                drawRoundRect(Color.Black.copy(alpha = 0.34f), cornerRadius = r)
                drawRoundRect(Color.White.copy(alpha = 0.05f + 0.07f * t), cornerRadius = r)
                val sw = 1.dp.toPx()
                drawRoundRect(
                    Color.White.copy(alpha = 0.10f + 0.45f * t),
                    topLeft = Offset(sw / 2, sw / 2),
                    size = Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius(r.x - sw / 2),
                    style = Stroke(sw),
                )
            }
            .focusable(interactionSource = interaction)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(section.title, style = Heading)
        Spacer(Modifier.height(6.dp))
        section.lines.forEach { line ->
            when (line) {
                is Para -> Text(line.text, style = Body)
                is Key -> Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(64.dp)) {
                        Text(
                            line.key,
                            style = Body.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text(line.text, style = Body)
                }
            }
        }
    }
}
