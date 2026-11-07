package dev.tvlauncher.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import dev.tvlauncher.core.config.Pattern
import dev.tvlauncher.core.config.ThemeSpec

@Composable
fun ThemedBackground(theme: ThemeSpec, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    // Ten layouts (digits 0-9) measured once and shared by every theme; the cell list is seeded.
    val digits = remember(measurer) {
        val style = TextStyle(fontSize = 22.sp, fontFamily = FontFamily.Monospace)
        DigitLayouts(digitGrid(), List(10) { measurer.measure(it.toString(), style) })
    }
    Crossfade(
        targetState = theme,
        modifier = modifier.fillMaxSize(),
        animationSpec = tween(400),
        label = "bg",
    ) { t ->
        val top = Color(t.top)
        val bottom = Color(t.bottom)
        val patternColor = Color(t.accent).copy(alpha = 0.08f)
        val brush = remember(t.top, t.bottom) { Brush.verticalGradient(listOf(top, bottom)) }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(brush)
            drawPattern(t.pattern, patternColor, digits)
        }
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
