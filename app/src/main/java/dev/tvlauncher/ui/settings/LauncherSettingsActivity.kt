package dev.tvlauncher.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.core.config.Pattern
import dev.tvlauncher.core.config.ThemeSpec
import dev.tvlauncher.core.settings.LauncherSettings
import dev.tvlauncher.core.settings.NightMode
import dev.tvlauncher.ui.home.Clock
import dev.tvlauncher.ui.home.NightBadge
import dev.tvlauncher.ui.setup.SetupActivity
import dev.tvlauncher.ui.theme.Emphasized
import dev.tvlauncher.ui.theme.Ink
import dev.tvlauncher.ui.theme.Poppins
import dev.tvlauncher.ui.theme.ThemedBackground
import dev.tvlauncher.ui.theme.forNight
import dev.tvlauncher.ui.theme.rememberIsNight

/** The launcher's own settings (opened with a short press of Menu on the home screen). */
class LauncherSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherSettingsScreen(
                onOpenSetup = { startActivity(Intent(this, SetupActivity::class.java)) },
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        @Suppress("DEPRECATION")
        if (hasFocus) window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}

private val SettingsTheme = ThemeSpec("launcher-settings", 0xFF2B2935, 0xFF0C0B11, 0xFFFFFFFF, Pattern.GRID)

private val Title = TextStyle(
    fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp,
    letterSpacing = (-0.8).sp, color = Color.White,
)
private val Body = TextStyle(fontFamily = Poppins, fontSize = 13.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.7f))

private fun hourLabel(h: Int): String = when {
    h == 0 -> "12 am"
    h < 12 -> "$h am"
    h == 12 -> "12 pm"
    else -> "${h - 12} pm"
}

private val StartHours = listOf(19, 20, 21, 22, 23, 0)
private val EndHours = listOf(4, 5, 6, 7, 8, 9)

private fun <T> List<T>.step(current: T, dir: Int): T {
    val i = indexOf(current).let { if (it < 0) 0 else it }
    return this[(i + dir).mod(size)]
}

private class SettingItem(
    val title: String,
    val description: String?,
    val value: () -> String,
    val change: (dir: Int) -> Unit,
    val isAction: Boolean = false,
)

@Composable
fun LauncherSettingsScreen(onOpenSetup: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { LauncherSettings(context) }
    // Bumped on every change so the rows re-read their values.
    var version by remember { mutableIntStateOf(0) }
    val night = rememberIsNight()

    val rows = remember {
        listOf(
            SettingItem("Night mode", "Darker, dimmer colours for a dark room", { settings.nightMode.label }, { d ->
                settings.nightMode = NightMode.entries.step(settings.nightMode, d)
            }),
            SettingItem("Night starts", "When Auto night mode turns on", { hourLabel(settings.nightStartHour) }, { d ->
                settings.nightStartHour = StartHours.step(settings.nightStartHour, d)
            }),
            SettingItem("Night ends", "When Auto night mode turns off", { hourLabel(settings.nightEndHour) }, { d ->
                settings.nightEndHour = EndHours.step(settings.nightEndHour, d)
            }),
            SettingItem("Night badge", "A small moon next to the clock at night", { if (settings.showNightBadge) "Shown" else "Hidden" }, {
                settings.showNightBadge = !settings.showNightBadge
            }),
            SettingItem("Open Cameras when the TV turns on", "Switches straight to the camera input", { if (settings.openCamerasOnStart) "On" else "Off" }, {
                settings.openCamerasOnStart = !settings.openCamerasOnStart
            }),
            SettingItem("Reduce motion", "No tile lift or sliding text", { if (settings.reduceMotion) "On" else "Off" }, {
                settings.reduceMotion = !settings.reduceMotion
            }),
            SettingItem("Home screen setup", "Use as home screen, and more", { "Open" }, { onOpenSetup() }, isAction = true),
        )
    }

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ThemedBackground(if (night) SettingsTheme.forNight() else SettingsTheme, night = night)
        Row(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp)) {
            Column(Modifier.width(300.dp).padding(top = 150.dp)) {
                Text("Launcher\nsettings", style = Title)
                Spacer(Modifier.height(12.dp))
                Text("Press OK or ‹ › to change a setting. Back to return home.", style = Body)
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentPadding = PaddingValues(top = 96.dp, bottom = 64.dp, start = 24.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rows.size) { i ->
                    val row = rows[i]
                    SettingRow(
                        row = row,
                        version = version,
                        night = night,
                        onChange = { d -> row.change(d); version++ },
                        modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            if (night && settings.showNightBadge) NightBadge(Modifier.padding(end = 12.dp))
            Clock()
        }
    }
}

@Composable
private fun SettingRow(row: SettingItem, version: Int, night: Boolean, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val focusFill = if (night) Color(0xFFD6D3DD) else Color.White
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val t by animateFloatAsState(if (focused) 1f else 0f, tween(150, easing = Emphasized), label = "row")
    // [version] changes after every edit, so this row recomposes and re-reads its value.
    val value = remember(version) { row.value() }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .graphicsLayer { val s = 1f + 0.02f * t; scaleX = s; scaleY = s }
            .drawBehind {
                val r = CornerRadius(14.dp.toPx())
                drawRoundRect(Color.Black.copy(alpha = 0.26f * (1 - t)), cornerRadius = r)
                drawRoundRect(Color.White.copy(alpha = 0.06f * (1 - t)), cornerRadius = r)
                if (t > 0f) drawRoundRect(focusFill.copy(alpha = t), cornerRadius = r)
            }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || row.isAction) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { onChange(-1); true }
                    Key.DirectionRight -> { onChange(1); true }
                    else -> false
                }
            }
            .clickable(interactionSource = interaction, indication = null) { onChange(1) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.title,
                style = Body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = lerp(Color.White, Ink, t)),
            )
            row.description?.let {
                Text(it, style = Body.copy(fontSize = 11.sp, lineHeight = 15.sp, color = lerp(Color.White.copy(alpha = 0.6f), Ink.copy(alpha = 0.6f), t)))
            }
        }
        val valueColor = lerp(Color.White, Ink, t)
        Text(
            if (row.isAction) "$value  ›" else "‹  $value  ›",
            style = Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
