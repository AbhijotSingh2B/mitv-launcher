package dev.tvlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.ui.theme.Poppins

/** Material Symbols Rounded "dark_mode" (filled), Apache 2.0. */
private const val MOON =
    "M480-120q-151 0-255.5-104.5T120-480q0-138 90-239.5T440-838q13-2 23 3.5t16 14.5q6 9 6.5 21t-7.5 23q-17 26-25.5 55t-8.5 61q0 90 63 153t153 63q31 0 61.5-9t54.5-25q11-7 22.5-6.5T819-479q10 5 15.5 15t3.5 24q-14 138-117.5 229T480-120Z"

/** "Night" pill shown beside the clock while night mode is on. */
@Composable
fun NightBadge(modifier: Modifier = Modifier) {
    val moon = remember { Mark(MOON, null) }
    Row(
        modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
            .padding(start = 9.dp, end = 11.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarkImage(moon, Color(0xFFE9E4FF), Modifier.size(12.dp))
        Text(
            "Night",
            style = TextStyle(
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color.White.copy(alpha = 0.82f),
            ),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
