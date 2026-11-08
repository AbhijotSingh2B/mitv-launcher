package dev.tvlauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.ui.theme.EaseOut
import dev.tvlauncher.ui.theme.Emphasized
import dev.tvlauncher.ui.theme.Ink
import dev.tvlauncher.ui.theme.Poppins

/** Material Symbols Rounded "tune" (filled), Apache 2.0. */
private const val TUNE =
    "M480-120q-17 0-28.5-11.5T440-160v-160q0-17 11.5-28.5T480-360q17 0 28.5 11.5T520-320v40h280q17 0 28.5 11.5T840-240q0 17-11.5 28.5T800-200H520v40q0 17-11.5 28.5T480-120Zm-320-80q-17 0-28.5-11.5T120-240q0-17 11.5-28.5T160-280h160q17 0 28.5 11.5T360-240q0 17-11.5 28.5T320-200H160Zm160-160q-17 0-28.5-11.5T280-400v-40H160q-17 0-28.5-11.5T120-480q0-17 11.5-28.5T160-520h120v-40q0-17 11.5-28.5T320-600q17 0 28.5 11.5T360-560v160q0 17-11.5 28.5T320-360Zm160-80q-17 0-28.5-11.5T440-480q0-17 11.5-28.5T480-520h320q17 0 28.5 11.5T840-480q0 17-11.5 28.5T800-440H480Zm160-160q-17 0-28.5-11.5T600-640v-160q0-17 11.5-28.5T640-840q17 0 28.5 11.5T680-800v40h120q17 0 28.5 11.5T840-720q0 17-11.5 28.5T800-680H680v40q0 17-11.5 28.5T640-600Zm-480-80q-17 0-28.5-11.5T120-720q0-17 11.5-28.5T160-760h320q17 0 28.5 11.5T520-720q0 17-11.5 28.5T480-680H160Z"

/** Material Symbols Rounded "tv" (filled), Apache 2.0. */
private const val TV =
    "M160-200q-33 0-56.5-23.5T80-280v-480q0-33 23.5-56.5T160-840h640q33 0 56.5 23.5T880-760v480q0 33-23.5 56.5T800-200H640v40q0 17-11.5 28.5T600-120H360q-17 0-28.5-11.5T320-160v-40H160Z"

private val NightFill = Color(0xFFD6D3DD)

/**
 * Shown when the Settings tile is pressed: pick the launcher's own settings or the TV's settings.
 * Scrim and cards enter staggered (opacity + 12 dp rise, 300 ms, 100 ms apart); Back closes it.
 */
@Composable
fun SettingsChooser(
    night: Boolean,
    reduceMotion: Boolean,
    onLauncherSettings: () -> Unit,
    onTvSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val scrim = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val cards = remember { Animatable(0f) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        scrim.animateTo(1f, tween(200, easing = EaseOut))
    }
    LaunchedEffect(Unit) {
        title.animateTo(1f, tween(300, delayMillis = 60, easing = EaseOut))
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
        cards.animateTo(1f, tween(300, delayMillis = 160, easing = EaseOut))
    }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrim.value }
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.88f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = if (reduceMotion) 0f else (1f - title.value) * rise
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Settings",
                    style = TextStyle(
                        fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 40.sp,
                        lineHeight = 48.sp, letterSpacing = (-0.8).sp, color = Color.White,
                    ),
                )
                Text(
                    "What would you like to change?",
                    style = TextStyle(fontFamily = Poppins, fontSize = 13.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.7f)),
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.graphicsLayer {
                    alpha = cards.value
                    translationY = if (reduceMotion) 0f else (1f - cards.value) * rise
                },
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ChoiceCard(
                    mark = remember { Mark(TUNE, null) },
                    title = "Launcher settings",
                    description = "Night mode, Cameras at start-up, motion",
                    night = night,
                    reduceMotion = reduceMotion,
                    onClick = onLauncherSettings,
                    modifier = Modifier.focusRequester(first),
                )
                ChoiceCard(
                    mark = remember { Mark(TV, null) },
                    title = "TV settings",
                    description = "Network, picture, sound, inputs and more",
                    night = night,
                    reduceMotion = reduceMotion,
                    onClick = onTvSettings,
                )
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    mark: Mark,
    title: String,
    description: String,
    night: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val t by animateFloatAsState(if (focused) 1f else 0f, tween(150, easing = Emphasized), label = "choice")
    val fill = if (night) NightFill else Color.White
    val shape = RoundedCornerShape(TileRadius)
    Column(
        modifier
            .size(250.dp, 168.dp)
            .graphicsLayer {
                val s = if (reduceMotion) 1f else 1f + 0.06f * t
                scaleX = s
                scaleY = s
                shadowElevation = 18.dp.toPx() * t
                this.shape = shape
                clip = true
            }
            .drawBehind {
                val r = CornerRadius(TileRadius.toPx())
                val a = 1f - t
                if (a > 0f) {
                    drawRoundRect(Color(0xFF15141A).copy(alpha = 0.92f * a), cornerRadius = r)
                    drawRoundRect(
                        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.12f * a), 0.7f to Color.White.copy(alpha = 0.03f * a)),
                        cornerRadius = r,
                    )
                    val sw = 1.dp.toPx()
                    drawRoundRect(
                        Color.White.copy(alpha = 0.14f * a),
                        topLeft = Offset(sw / 2, sw / 2),
                        size = Size(size.width - sw, size.height - sw),
                        cornerRadius = CornerRadius(r.x - sw / 2),
                        style = Stroke(sw),
                    )
                }
                if (t > 0f) drawRoundRect(fill.copy(alpha = t), cornerRadius = r)
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        MarkImage(mark, lerp(Color.White.copy(alpha = 0.9f), Ink, t), Modifier.size(34.dp))
        Column {
            Text(
                title,
                style = TextStyle(
                    fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp,
                    color = lerp(Color.White, Ink, t),
                ),
            )
            Text(
                description,
                style = TextStyle(
                    fontFamily = Poppins, fontSize = 12.sp, lineHeight = 16.sp,
                    color = lerp(Color.White.copy(alpha = 0.65f), Ink.copy(alpha = 0.65f), t),
                ),
            )
        }
    }
}
