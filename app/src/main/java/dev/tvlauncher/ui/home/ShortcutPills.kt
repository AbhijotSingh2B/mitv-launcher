package dev.tvlauncher.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.core.catalog.InstalledApp
import dev.tvlauncher.ui.theme.Emphasized
import dev.tvlauncher.ui.theme.Ink
import dev.tvlauncher.ui.theme.Poppins

/** Material Symbols Rounded "home" (filled), Apache 2.0. */
private const val HOME_ICON =
    "M160-200v-360q0-19 8.5-36t23.5-28l240-180q21-16 48-16t48 16l240 180q15 11 23.5 28t8.5 36v360q0 33-23.5 56.5T720-120H600q-17 0-28.5-11.5T560-160v-200q0-17-11.5-28.5T520-400h-80q-17 0-28.5 11.5T400-360v200q0 17-11.5 28.5T360-120H240q-33 0-56.5-23.5T160-200Z"

private val PillHeight = 30.dp
private val NightFill = Color(0xFFD6D3DD)

/**
 * Thin pills under the tile row: "TV home" (the TV's own home screen), then the user's app
 * shortcuts from Launcher settings. Up always returns to the selected tile, not the nearest one.
 */
@Composable
fun ShortcutPills(
    shortcuts: List<InstalledApp>,
    icon: (String) -> ImageBitmap?,
    night: Boolean,
    reduceMotion: Boolean,
    enabled: Boolean,
    gutter: Dp,
    upTarget: FocusRequester,
    /** (what OK opens, focused): moving between pills may report the new focus before the old blur. */
    onFocusedLabel: (String, Boolean) -> Unit,
    onTvHome: () -> Unit,
    onOpen: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val home = remember { Mark(HOME_ICON, null) }
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = gutter, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "tv-home") {
            Pill(
                label = "TV home",
                night = night,
                reduceMotion = reduceMotion,
                enabled = enabled,
                upTarget = upTarget,
                onFocused = { onFocusedLabel("the TV home screen", it) },
                onClick = onTvHome,
            ) { tint -> MarkImage(home, tint, Modifier.size(15.dp)) }
        }
        items(shortcuts, key = { it.packageName }) { app ->
            Pill(
                label = app.label,
                night = night,
                reduceMotion = reduceMotion,
                enabled = enabled,
                upTarget = upTarget,
                onFocused = { onFocusedLabel(app.label, it) },
                onClick = { onOpen(app) },
            ) {
                val bmp = remember(app.packageName) { icon(app.packageName) }
                if (bmp != null) {
                    Image(
                        bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)),
                    )
                }
            }
        }
    }
}

/** A single "TV home" pill, for screens other than home (the About screen). */
@Composable
fun TvHomePill(night: Boolean, reduceMotion: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val home = remember { Mark(HOME_ICON, null) }
    Pill(
        label = "Open the TV home screen",
        night = night,
        reduceMotion = reduceMotion,
        enabled = true,
        upTarget = null,
        onFocused = {},
        onClick = onClick,
        modifier = modifier,
    ) { tint -> MarkImage(home, tint, Modifier.size(15.dp)) }
}

@Composable
private fun Pill(
    label: String,
    night: Boolean,
    reduceMotion: Boolean,
    enabled: Boolean,
    upTarget: FocusRequester?,
    onFocused: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (tint: Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocused(focused) }
    val t by animateFloatAsState(if (focused) 1f else 0f, tween(150, easing = Emphasized), label = "pill")
    val fill = if (night) NightFill else Color.White
    val content = lerp(Color.White.copy(alpha = 0.88f), Ink, t)
    Row(
        modifier
            .height(PillHeight)
            .graphicsLayer {
                val s = if (reduceMotion) 1f else 1f + 0.06f * t
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Color.Black.copy(alpha = 0.28f * (1f - t)), cornerRadius = r)
                drawRoundRect(Color.White.copy(alpha = 0.08f * (1f - t)), cornerRadius = r)
                val sw = 1.dp.toPx()
                drawRoundRect(
                    Color.White.copy(alpha = 0.16f * (1f - t)),
                    topLeft = Offset(sw / 2, sw / 2),
                    size = Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius(r.x - sw / 2),
                    style = Stroke(sw),
                )
                if (t > 0f) drawRoundRect(fill.copy(alpha = t), cornerRadius = r)
            }
            .focusProperties { if (upTarget != null) up = upTarget }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading(content)
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            maxLines = 1,
            style = TextStyle(
                fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp,
                color = content,
            ),
        )
    }
}
