package dev.tvlauncher.ui.settings

import android.app.Activity
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.core.config.Pattern
import dev.tvlauncher.core.config.ThemeSpec
import dev.tvlauncher.ui.theme.Emphasized
import dev.tvlauncher.ui.theme.Ink
import dev.tvlauncher.ui.theme.Poppins

// Shared styling for the Launcher settings and Home setup screens.

internal val SettingsTheme = ThemeSpec("launcher-settings", 0xFF2B2935, 0xFF0C0B11, 0xFFFFFFFF, Pattern.GRID)

internal val Title = TextStyle(
    fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp,
    letterSpacing = (-0.8).sp, color = Color.White,
)
internal val Body = TextStyle(fontFamily = Poppins, fontSize = 13.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.7f))

/** Hides the system bars; call from onWindowFocusChanged when the window has focus. */
internal fun Activity.hideSystemBars() {
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
}

internal class SettingItem(
    val title: String,
    val description: String?,
    val value: () -> String,
    val change: (dir: Int) -> Unit,
    val isAction: Boolean = false,
)

@Composable
internal fun SettingRow(row: SettingItem, version: Int, night: Boolean, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
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
