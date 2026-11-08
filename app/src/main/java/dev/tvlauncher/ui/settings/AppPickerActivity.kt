package dev.tvlauncher.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.App
import dev.tvlauncher.core.catalog.InstalledApp
import dev.tvlauncher.core.settings.LauncherSettings
import dev.tvlauncher.ui.home.Clock
import dev.tvlauncher.ui.theme.Ink
import dev.tvlauncher.ui.theme.ThemedBackground
import dev.tvlauncher.ui.theme.forNight
import dev.tvlauncher.ui.theme.rememberIsNight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the picker edits: app tiles on the home row, or shortcut pills under it. */
enum class PickerMode(val title: String, val blurb: String, val addedLabel: String) {
    ROW(
        "Home row apps",
        "Apps you pick become tiles on the home row, just before All apps. Press OK on an app to add or remove it.",
        "On home row",
    ),
    SHORTCUTS(
        "Shortcuts",
        "Apps you pick become small pills under the home row. Press OK on an app to add or remove it.",
        "Added",
    ),
}

/** Type a name, pick an app: adds it to (or removes it from) the home row or the shortcut pills. */
class AppPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = runCatching { PickerMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty()) }
            .getOrDefault(PickerMode.SHORTCUTS)
        val app = application as App
        setContent {
            AppPickerScreen(mode, load = { app.controller.allApps() })
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    companion object {
        private const val EXTRA_MODE = "mode"
        fun intent(context: Context, mode: PickerMode): Intent =
            Intent(context, AppPickerActivity::class.java).putExtra(EXTRA_MODE, mode.name)
    }
}

@Composable
private fun AppPickerScreen(mode: PickerMode, load: () -> List<InstalledApp>) {
    val context = LocalContext.current
    val settings = remember { LauncherSettings(context) }
    val night = rememberIsNight()
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }
    val apps by produceState<List<InstalledApp>?>(null) {
        value = withContext(Dispatchers.IO) { load() }.sortedBy { it.label.lowercase() }
    }
    fun chosen(): List<String> = if (mode == PickerMode.ROW) settings.homeRowApps else settings.shortcutApps
    fun toggle(pkg: String) {
        val next = LauncherSettings.toggled(chosen(), pkg)
        if (mode == PickerMode.ROW) settings.homeRowApps = next else settings.shortcutApps = next
        version++
    }
    val shown = apps.orEmpty().filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }

    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstKey.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ThemedBackground(if (night) SettingsTheme.forNight() else SettingsTheme, night = night)
        Row(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp)) {
            // Left: title, what this does, and a remote-friendly keyboard. The TV's own keyboard
            // can be the phone-only remote bridge, so typing must work with just the D-pad.
            Column(Modifier.width(300.dp).padding(top = 52.dp)) {
                Text(mode.title, style = Title.copy(fontSize = 30.sp, lineHeight = 36.sp))
                Spacer(Modifier.height(6.dp))
                Text(mode.blurb, style = Body.copy(fontSize = 12.sp, lineHeight = 16.sp))
                Spacer(Modifier.height(14.dp))
                QueryBox(query)
                Spacer(Modifier.height(10.dp))
                LetterKeyboard(
                    night = night,
                    firstKey = firstKey,
                    onKey = { query += it },
                    onSpace = { if (query.isNotEmpty() && !query.endsWith(" ")) query += " " },
                    onDelete = { query = query.dropLast(1) },
                    onClear = { query = "" },
                )
            }
            Column(Modifier.weight(1f).padding(top = 96.dp, start = 24.dp, end = 8.dp)) {
                val count = remember(version) { chosen().size }
                Text(
                    "$count added · ${shown.size} apps · Back to return",
                    style = Body.copy(color = Color.White.copy(alpha = 0.55f)),
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                when {
                    apps == null -> Text("Loading apps…", style = Body)
                    shown.isEmpty() -> Text("No app matches “${query.trim()}”.", style = Body)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.packageName }) { app ->
                        val row = remember(app.packageName) {
                            SettingItem(
                                title = app.label,
                                description = null,
                                value = { if (app.packageName in chosen()) "${mode.addedLabel} ✓" else "Add" },
                                change = { toggle(app.packageName) },
                                isAction = true,
                            )
                        }
                        SettingRow(row = row, version = version, night = night, onChange = { row.change(it) })
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 64.dp)) { Clock() }
    }
}

@Composable
private fun QueryBox(query: String) {
    Box(
        Modifier
            .width(282.dp)
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            if (query.isEmpty()) "Type a name with the keys below" else "$query▏",
            maxLines = 1,
            style = Body.copy(
                fontSize = 14.sp,
                color = if (query.isEmpty()) Color.White.copy(alpha = 0.45f) else Color.White,
            ),
        )
    }
}

private val Keys = ('A'..'Z').map { it.toString() } + ('0'..'9').map { it.toString() }

/** A 6 x 6 letter and number grid, then Space, Delete and Clear. Every key is a D-pad stop. */
@Composable
private fun LetterKeyboard(
    night: Boolean,
    firstKey: FocusRequester,
    onKey: (String) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Keys.chunked(6).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { c, k ->
                    KeyButton(
                        label = k,
                        width = 42.dp,
                        night = night,
                        onClick = { onKey(k.lowercase()) },
                        modifier = if (r == 0 && c == 0) Modifier.focusRequester(firstKey) else Modifier,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyButton("Space", 90.dp, night, onSpace)
            KeyButton("⌫", 90.dp, night, onDelete)
            KeyButton("Clear", 90.dp, night, onClear)
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    width: Dp,
    night: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val fill = if (night) Color(0xFFD6D3DD) else Color.White
    Box(
        modifier
            .width(width)
            .height(30.dp)
            .background(if (focused) fill else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = if (focused) 0f else 0.12f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = Body.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (focused) Ink else Color.White,
            ),
        )
    }
}
