package dev.tvlauncher.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import dev.tvlauncher.core.config.Pattern
import dev.tvlauncher.core.config.ThemeSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun patternAlpha(p: Pattern) = when (p) {
    Pattern.DIGITS -> 0.30f
    Pattern.DOODLES -> 0.12f
    Pattern.LINES -> 0.14f
    Pattern.RINGS -> 0.20f
    Pattern.GRID -> 0.07f
    Pattern.RIBBONS -> 0.55f
    Pattern.VIEWFINDER -> 0.30f
    Pattern.TILES -> 0.10f
    else -> 0.09f
}

/** Where the pattern starts to show (alpha at the left edge): it stays clear of the title. */
private fun maskFloor(p: Pattern) = when (p) {
    Pattern.DIGITS, Pattern.RIBBONS -> 1f // already composed to the right
    Pattern.DOODLES -> 0.22f
    Pattern.VIEWFINDER -> 0.0f
    else -> 0.15f
}

/**
 * Renders a theme's pattern into a one-byte-per-pixel alpha mask (about 0.9 MB at 1280 x 720).
 * Pure software drawing, so it runs off the main thread; the mask is tinted with the accent at draw time.
 */
private fun renderMask(
    t: ThemeSpec,
    w: Int,
    h: Int,
    density: Density,
    dir: LayoutDirection,
    digits: DigitLayouts,
): ImageBitmap {
    val image = ImageBitmap(w, h, ImageBitmapConfig.Alpha8)
    val size = Size(w.toFloat(), h.toFloat())
    CanvasDrawScope().draw(density, dir, Canvas(image), size) {
        val floor = maskFloor(t.pattern)
        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        drawPattern(t.pattern, Color.Black.copy(alpha = patternAlpha(t.pattern)), digits)
        if (floor < 1f) {
            // Fade towards the left so the title area stays clean.
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = floor),
                    0.35f to Color.Black.copy(alpha = floor),
                    0.75f to Color.Black,
                    startX = 0f,
                    endX = size.width,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        drawContext.canvas.restore()
    }
    return image
}

/**
 * Full-screen themed background: vertical gradient, a soft light from the upper left, the theme's
 * pattern and a vignette. Gradients are drawn live on the GPU; each pattern is pre-rendered once,
 * off the main thread, for [theme] and every theme in [prefetch], so moving focus never rasterises
 * or uploads a full-screen bitmap. Changing theme cross-fades in 400 ms.
 */
@Composable
fun ThemedBackground(
    theme: ThemeSpec,
    modifier: Modifier = Modifier,
    prefetch: List<ThemeSpec> = emptyList(),
    night: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    // Ten layouts (digits 0-9) measured once and shared by every theme; the cell list is seeded.
    val digits = remember(measurer) {
        val style = TextStyle(fontSize = 26.sp, fontFamily = Poppins, fontWeight = FontWeight.Light)
        DigitLayouts(digitGrid(), List(10) { measurer.measure(it.toString(), style) })
    }
    val density = LocalDensity.current
    val dir = LocalLayoutDirection.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = constraints.maxWidth.coerceAtLeast(1)
        val h = constraints.maxHeight.coerceAtLeast(1)
        val masks = remember(w, h) { mutableStateMapOf<String, ImageBitmap>() }
        // Keyed so ordinary focus moves (theme already in prefetch) never restart the renders.
        val extra = theme.takeIf { th -> prefetch.none { it.pattern == th.pattern } }
        val wanted = remember(extra, prefetch) {
            (listOfNotNull(extra) + prefetch).distinctBy { it.pattern }.filter { it.pattern != Pattern.NONE }
        }
        LaunchedEffect(wanted, w, h) {
            // One at a time, in order, so the shared digit layouts are never drawn on two threads.
            for (t in wanted) {
                if (masks.containsKey(t.pattern.name)) continue
                val mask = try {
                    withContext(Dispatchers.Default) { renderMask(t, w, h, density, dir, digits) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } ?: continue
                masks[t.pattern.name] = mask
            }
        }

        // Manual two-theme cross-fade drawn with per-draw alpha: no offscreen layers, so a fade costs
        // two extra full-screen passes (gradient + pattern) on this GPU instead of a full layer.
        var from by remember { mutableStateOf(theme) }
        var to by remember { mutableStateOf(theme) }
        val progress = remember { Animatable(1f) }
        LaunchedEffect(theme) {
            if (theme.id == to.id) return@LaunchedEffect
            // Retargeted mid-fade: start from whichever theme dominates the screen right now.
            if (progress.value >= 0.5f) from = to
            to = theme
            progress.snapTo(0f)
            progress.animateTo(1f, tween(400))
        }
        // Night: a dimmer room light, quieter patterns and a deeper vignette.
        val glowScale = if (night) 0.35f else 1f
        val patternScale = if (night) 0.6f else 1f
        val vignetteAlpha = if (night) 0.55f else 0.42f
        val gradients = remember { HashMap<String, Brush>() }
        val tints = remember { HashMap<String, ColorFilter>() }

        Box(
            Modifier.fillMaxSize().drawWithCache {
                val sw = size.width
                val sh = size.height
                // Theme-independent light, drawn once on top of both themes.
                val glow = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.20f * glowScale),
                    0.45f to Color.White.copy(alpha = 0.07f * glowScale),
                    1f to Color.Transparent,
                    center = Offset(sw * 0.24f, -sh * 0.08f),
                    radius = sw * 0.72f,
                )
                // Vignette: darker towards the bottom and edges, so white tiles pop.
                val vignette = Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = vignetteAlpha)),
                    center = Offset(sw * 0.42f, sh * 0.30f),
                    radius = sw * 0.80f,
                )
                fun DrawScope.theme(t: ThemeSpec, alpha: Float) {
                    val g = gradients.getOrPut(t.id) { Brush.verticalGradient(listOf(Color(t.top), Color(t.bottom))) }
                    drawRect(g, alpha = alpha)
                    masks[t.pattern.name]?.let { m ->
                        val tint = tints.getOrPut(t.id) { ColorFilter.tint(Color(t.accent), BlendMode.SrcIn) }
                        drawImage(m, alpha = alpha * patternScale, colorFilter = tint)
                    }
                }
                onDrawBehind {
                    val p = progress.value
                    if (p >= 1f || from.id == to.id) {
                        theme(to, 1f)
                    } else {
                        theme(from, 1f)
                        theme(to, p)
                    }
                    drawRect(glow)
                    drawRect(vignette)
                }
            },
        )
    }
}

private fun previewTheme(p: Pattern) =
    ThemeSpec("preview-$p", 0xFF1E1B2E, 0xFF0B0A12, 0xFFFFFFFF, p)

@Preview(widthDp = 960, heightDp = 540, name = "NONE")
@Composable
private fun PreviewNone() = ThemedBackground(previewTheme(Pattern.NONE))

@Preview(widthDp = 960, heightDp = 540, name = "GRID")
@Composable
private fun PreviewGrid() = ThemedBackground(previewTheme(Pattern.GRID))

@Preview(widthDp = 960, heightDp = 540, name = "DOTS")
@Composable
private fun PreviewDots() = ThemedBackground(previewTheme(Pattern.DOTS))

@Preview(widthDp = 960, heightDp = 540, name = "RINGS")
@Composable
private fun PreviewRings() = ThemedBackground(previewTheme(Pattern.RINGS))

@Preview(widthDp = 960, heightDp = 540, name = "DIGITS")
@Composable
private fun PreviewDigits() = ThemedBackground(previewTheme(Pattern.DIGITS))
