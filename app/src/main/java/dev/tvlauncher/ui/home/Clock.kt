package dev.tvlauncher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.ui.theme.Poppins
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Time text plus an AM/PM suffix (empty in 24-hour mode). */
private fun formatNow(context: Context): Pair<String, String> {
    val now = Date()
    return if (DateFormat.is24HourFormat(context)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) to ""
    } else {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(now) to
            SimpleDateFormat("a", Locale.getDefault()).format(now).uppercase()
    }
}

/** Small clock; follows the 24-hour setting and updates once a minute on ACTION_TIME_TICK. */
@Composable
fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(formatNow(context)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                text = formatNow(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        text = formatNow(context)
        onDispose { context.unregisterReceiver(receiver) }
    }
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text.first,
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontFeatureSettings = "tnum",
            ),
        )
        if (text.second.isNotEmpty()) {
            Text(
                text.second,
                fontFamily = Poppins,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 3.dp, bottom = 3.dp),
            )
        }
    }
}
