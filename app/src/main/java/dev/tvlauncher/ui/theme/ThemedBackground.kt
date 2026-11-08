package dev.tvlauncher.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    val style = styleOf(t.pattern)
    val image = ImageBitmap(w, h, if (style.fullColour) ImageBitmapConfig.Argb8888 else ImageBitmapConfig.Alpha8)
    val size = Size(w.toFloat(), h.toFloat())
    if (style.fullColour) {
        CanvasDrawScope().draw(density, dir, Canvas(image), size) { drawPattern(t.pattern, Color.Unspecified, digits) }
        return image
    }
    CanvasDrawScope().draw(density, dir, Canvas(image), size) {
        val floor = style.leftFade
        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        drawPattern(t.pattern, Color.Black.copy(alpha = style.alpha), digits)
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
 * Pattern masks for the whole process, keyed by pattern and size. Home and the About screen share them,
 * so opening About or coming back home never re-renders a mask. One lock serialises every render:
 * one full-screen software draw at a time keeps the CPU free for the UI on this TV.
 */
private object MaskCache {
    val masks = mutableStateMapOf<String, ImageBitmap>()
    val renderLock = Mutex()
    fun key(p: Pattern, w: Int, h: Int) = "${p.name}@${w}x$h"
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
    reduceMotion: Boolean = false,
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
        fun maskOf(p: Pattern): ImageBitmap? = MaskCache.masks[MaskCache.key(p, w, h)]
        // Keyed so ordinary focus moves (theme already in prefetch) never restart the renders.
        val extra = theme.takeIf { th -> prefetch.none { it.pattern == th.pattern } }
        val wanted = remember(extra, prefetch) {
            (listOfNotNull(extra) + prefetch).distinctBy { it.pattern }.filter { it.pattern != Pattern.NONE }
        }
        val current by rememberUpdatedState(theme)
        LaunchedEffect(wanted, w, h) {
            val failed = HashSet<Pattern>()
            while (true) {
                // The theme on screen goes first, so it never waits behind the rest of the row.
                val next = wanted
                    .filter { maskOf(it.pattern) == null && it.pattern !in failed }
                    .sortedByDescending { it.pattern == current.pattern }
                    .firstOrNull() ?: break
                val mask = try {
                    MaskCache.renderLock.withLock {
                        // Another background may have rendered it while this one waited.
                        maskOf(next.pattern) ?: withContext(Dispatchers.Default) {
                            // Stored here, not after the lock, so a render finished just as this
                            // background leaves (home to About) is kept instead of thrown away.
                            renderMask(next, w, h, density, dir, digits).also {
                                MaskCache.masks[MaskCache.key(next.pattern, w, h)] = it
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                if (mask == null) failed += next.pattern else MaskCache.masks[MaskCache.key(next.pattern, w, h)] = mask
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
            from = to // fade done: nothing of the old theme is on screen any more
        }

        // About theme: a wave of light sweeps over the digit grid. It only runs while that theme is
        // on screen, so other tiles cost nothing extra per frame. Reduced motion: a still light.
        val lightOnScreen = to.pattern == Pattern.LIGHTGRID || from.pattern == Pattern.LIGHTGRID
        val sweep = remember { Animatable(0.62f) }
        LaunchedEffect(lightOnScreen, reduceMotion) {
            if (!lightOnScreen || reduceMotion) {
                sweep.snapTo(0.62f)
                return@LaunchedEffect
            }
            while (true) {
                sweep.snapTo(0f)
                sweep.animateTo(1f, tween(7000, easing = LinearEasing))
            }
        }
        val bitmapShaders = remember(w, h) { HashMap<String, android.graphics.BitmapShader>() }
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
                    val m = maskOf(t.pattern) ?: return
                    when {
                        styleOf(t.pattern).fullColour -> drawImage(m, alpha = alpha * patternScale)
                        t.pattern == Pattern.LIGHTGRID -> {
                            val tint = tints.getOrPut(t.id) { ColorFilter.tint(Color(t.accent), BlendMode.SrcIn) }
                            // The dark grid, barely there...
                            drawImage(m, alpha = alpha * 0.05f * patternScale, colorFilter = tint)
                            // ...and the light: a tilted band, clipped to the digits by the mask (one draw, no layer).
                            val bs = bitmapShaders.getOrPut(t.pattern.name) {
                                android.graphics.BitmapShader(
                                    m.asAndroidBitmap(),
                                    android.graphics.Shader.TileMode.CLAMP,
                                    android.graphics.Shader.TileMode.CLAMP,
                                )
                            }
                            val ph = sweep.value
                            val bw = sw * 0.20f
                            val cx = -sw * 0.35f + ph * sw * 1.7f
                            // A gentle wave: the band's tilt swings as it travels.
                            val tilt = 0.36f + 0.22f * kotlin.math.sin(ph * 6.2832f)
                            val light = Color(t.accent).copy(alpha = 1f)
                            val band = android.graphics.LinearGradient(
                                cx - bw, -bw * tilt, cx + bw, bw * tilt,
                                intArrayOf(
                                    0,
                                    light.copy(alpha = 0.22f).toArgb(),
                                    light.toArgb(),
                                    light.copy(alpha = 0.22f).toArgb(),
                                    0,
                                ),
                                floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
                                android.graphics.Shader.TileMode.CLAMP,
                            )
                            val lit = android.graphics.ComposeShader(bs, band, android.graphics.PorterDuff.Mode.SRC_IN)
                            // A faint wash of the same light over everything, so it reads as light passing by.
                            val wash = android.graphics.LinearGradient(
                                cx - bw * 1.6f, -bw * tilt * 1.6f, cx + bw * 1.6f, bw * tilt * 1.6f,
                                intArrayOf(0, light.copy(alpha = 0.07f).toArgb(), 0),
                                floatArrayOf(0f, 0.5f, 1f),
                                android.graphics.Shader.TileMode.CLAMP,
                            )
                            drawRect(ShaderBrush(wash), alpha = alpha * patternScale)
                            drawRect(ShaderBrush(lit), alpha = alpha * patternScale)
                        }
                        else -> {
                            val tint = tints.getOrPut(t.id) { ColorFilter.tint(Color(t.accent), BlendMode.SrcIn) }
                            drawImage(m, alpha = alpha * patternScale, colorFilter = tint)
                        }
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
