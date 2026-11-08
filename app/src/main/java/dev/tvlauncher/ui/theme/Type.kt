package dev.tvlauncher.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.tvlauncher.R

/** Poppins (SIL OFL 1.1, see licenses/Poppins-OFL.txt): the geometric sans used across the launcher. */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/** Near-black ink used on white (focused) surfaces. */
val Ink = Color(0xFF17151F)

/** Emphasised standard easing, cubic-bezier(0.2, 0, 0, 1). */
val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Ease-out for entrances and exits. */
val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/** True when the user turned animations off (Developer options / Accessibility "Remove animations"). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
