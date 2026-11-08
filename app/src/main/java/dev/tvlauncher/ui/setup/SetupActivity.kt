package dev.tvlauncher.ui.setup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Text
import dev.tvlauncher.BuildConfig
import dev.tvlauncher.core.home.HomeSwitch
import dev.tvlauncher.core.settings.LauncherSettings
import dev.tvlauncher.ui.home.Clock
import dev.tvlauncher.ui.home.HomeActivity
import dev.tvlauncher.ui.home.NightBadge
import dev.tvlauncher.ui.settings.Body
import dev.tvlauncher.ui.settings.LauncherSettingsActivity
import dev.tvlauncher.ui.settings.SettingItem
import dev.tvlauncher.ui.settings.SettingRow
import dev.tvlauncher.ui.settings.SettingsTheme
import dev.tvlauncher.ui.settings.Title
import dev.tvlauncher.ui.settings.hideSystemBars
import dev.tvlauncher.ui.theme.ThemedBackground
import dev.tvlauncher.ui.theme.forNight
import dev.tvlauncher.ui.theme.rememberIsNight

/** The app's LEANBACK_LAUNCHER entry ("Home" in the TV's app list): switch this launcher on as the home screen. */
class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetupScreen(
                onOpenHome = { startActivity(Intent(this, HomeActivity::class.java)) },
                onOpenSettings = { startActivity(Intent(this, LauncherSettingsActivity::class.java)) },
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }
}

@Composable
private fun SetupScreen(onOpenHome: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val homeSwitch = remember { HomeSwitch(context) }
    val settings = remember { LauncherSettings(context) }
    // Bumped on every change and on resume so the rows re-read their values.
    var version by remember { mutableIntStateOf(0) }
    val night = rememberIsNight()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) version++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val rows = remember {
        listOf(
            SettingItem("Use as home screen", "Lets you pick this launcher when you press Home", { if (homeSwitch.enabled) "On" else "Off" }, {
                homeSwitch.enabled = !homeSwitch.enabled
            }),
            SettingItem("Open home now", null, { "Open" }, { onOpenHome() }, isAction = true),
            SettingItem("Launcher settings", null, { "Open" }, { onOpenSettings() }, isAction = true),
        )
    }

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ThemedBackground(if (night) SettingsTheme.forNight() else SettingsTheme, night = night)
        Row(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp)) {
            Column(Modifier.width(300.dp).padding(top = 150.dp)) {
                Text("Home\nsetup", style = Title)
                Spacer(Modifier.height(12.dp))
                Text("Press OK or ‹ › to change a setting. Back to leave.", style = Body)
                Spacer(Modifier.height(8.dp))
                Text("Version " + BuildConfig.VERSION_NAME, style = Body.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)))
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentPadding = PaddingValues(top = 96.dp, bottom = 64.dp, start = 24.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rows.size) { i ->
                    val row = rows[i]
                    SettingRow(
                        row = row,
                        version = version,
                        night = night,
                        onChange = { d -> row.change(d); version++ },
                        modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
                item { HowToCard() }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            if (night && settings.showNightBadge) NightBadge(Modifier.padding(end = 12.dp))
            Clock()
        }
    }
}

@Composable
private fun HowToCard() {
    val step = Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = 0.8f))
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(Color.Black.copy(alpha = 0.26f), RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text("How to switch", style = Body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = Color.White))
        Spacer(Modifier.height(6.dp))
        Text(buildAnnotatedString {
            append("1.  Turn on "); withStyle(bold) { append("Use as home screen") }; append(" above.")
        }, style = step)
        Text(buildAnnotatedString {
            append("2.  Press "); withStyle(bold) { append("Home") }; append(" on the remote.")
        }, style = step)
        Text(buildAnnotatedString {
            append("3.  Choose "); withStyle(bold) { append("Home") }; append(" and select "); withStyle(bold) { append("Always") }; append(".")
        }, style = step)
        Spacer(Modifier.height(8.dp))
        Text(
            "No chooser? Another launcher is set as default. Go to Settings → Apps → See all apps → show system apps → " +
                "Google TV Home (or PatchWall) → Clear defaults, then press Home again. " +
                "On PatchWall you can also turn off its 'Set PatchWall as default launcher' option.",
            style = Body.copy(fontSize = 11.sp, lineHeight = 15.sp, color = Color.White.copy(alpha = 0.6f)),
        )
    }
}
