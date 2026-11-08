package dev.tvlauncher.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.core.home.HomeTile
import dev.tvlauncher.core.home.Target
import dev.tvlauncher.ui.theme.Emphasized
import dev.tvlauncher.ui.theme.Ink
import dev.tvlauncher.ui.theme.Poppins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val TileRadius = 16.dp
private const val FOCUS_SCALE = 1.1f
private const val FOCUS_MS = 150
private val NightTileFill = Color(0xFFD6D3DD)

/**
 * One home tile. At rest it is dark glass with a white mark; focused it turns white, lifts
 * (scale 1.1 + soft shadow) and the mark takes the brand colour. 150 ms, cubic-bezier(0.2, 0, 0, 1).
 * Under reduced motion there is no scale: only the colour change runs.
 */
@Composable
fun TileCard(
    tile: HomeTile,
    size: Dp,
    icon: (String) -> ImageBitmap?,
    reduceMotion: Boolean,
    night: Boolean = false,
    enabled: Boolean = true,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }

    val t by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(FOCUS_MS, easing = Emphasized),
        label = "tileFocus",
    )

    val mark = remember(tile.key) { markFor(tile) }
    val pkg = (tile.target as? Target.App)?.packageName
    val art by produceState<ImageBitmap?>(null, pkg, mark) {
        value = if (mark != null || pkg == null) null else withContext(Dispatchers.Default) { icon(pkg) }
    }

    val shape = RoundedCornerShape(TileRadius)
    // At night the lit tile is a soft off-white instead of pure white.
    val focusFill = if (night) NightTileFill else Color.White
    val restMark = if (night) 0.68f else 0.84f
    val dimmed = !tile.available

    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val s = if (reduceMotion) 1f else 1f + (FOCUS_SCALE - 1f) * t
                scaleX = s
                scaleY = s
                shadowElevation = 18.dp.toPx() * t
                this.shape = shape
                clip = true
                alpha = if (dimmed) 0.4f else 1f
            }
            .drawBehind {
                val r = CornerRadius(TileRadius.toPx())
                // Glass: a dark translucent fill with a faint light from above.
                if (t < 1f) {
                    val a = 1f - t
                    drawRoundRect(Color.Black.copy(alpha = 0.26f * a), cornerRadius = r)
                    drawRoundRect(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.10f * a),
                            0.6f to Color.White.copy(alpha = 0.03f * a),
                        ),
                        cornerRadius = r,
                    )
                    // Hairline edge so the glass reads on bright themes (structure, not depth).
                    val sw = 1.dp.toPx()
                    drawRoundRect(
                        Color.White.copy(alpha = 0.12f * a),
                        topLeft = Offset(sw / 2, sw / 2),
                        size = Size(this.size.width - sw, this.size.height - sw),
                        cornerRadius = CornerRadius(r.x - sw / 2),
                        style = Stroke(sw),
                    )
                }
                if (t > 0f) drawRoundRect(focusFill.copy(alpha = t), cornerRadius = r)
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
    ) {
        val markColor = lerp(Color.White.copy(alpha = restMark), mark?.brand ?: Ink, t)
        when {
            mark != null -> {
                val w = if (mark.wide) size * 0.70f else size * 0.42f
                val h = if (mark.wide) size * 0.30f else size * 0.42f
                MarkImage(
                    mark,
                    markColor,
                    Modifier.align(Alignment.Center).padding(bottom = size * 0.12f).size(w, h),
                    gradientAlpha = t,
                )
            }
            art != null -> {
                val a = art!!
                if (a.width.toFloat() / a.height > 1.2f) {
                    // A leanback banner: shown as a thumbnail with a concentric radius (16 - 10 = 6 dp).
                    Image(
                        a, null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.align(Alignment.TopCenter).padding(10.dp).fillMaxWidth()
                            .aspectRatio(16f / 9f).clip(RoundedCornerShape(TileRadius - 10.dp)),
                    )
                } else {
                    Image(
                        a, null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.align(Alignment.Center).padding(bottom = size * 0.12f)
                            .size(size * 0.46f),
                    )
                }
            }
        }
        Text(
            tile.label,
            color = lerp(Color.White.copy(alpha = 0.86f), Ink, t),
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 10.dp, bottom = 10.dp),
        )
    }
}
