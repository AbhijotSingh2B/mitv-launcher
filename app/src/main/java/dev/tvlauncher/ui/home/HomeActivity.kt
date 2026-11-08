package dev.tvlauncher.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import dev.tvlauncher.App
import dev.tvlauncher.ui.info.AboutScreen
import dev.tvlauncher.core.home.Target
import dev.tvlauncher.core.config.ConfigParser
import dev.tvlauncher.BuildConfig
import dev.tvlauncher.core.catalog.InstalledApp
import dev.tvlauncher.core.home.LaunchResult
import dev.tvlauncher.ui.apps.AllAppsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import dev.tvlauncher.ui.settings.LauncherSettingsActivity
import dev.tvlauncher.ui.setup.SetupActivity

class HomeActivity : ComponentActivity() {
    // Increments on every Home press that reaches this activity. Saved, so a recreated activity
    // doesn't replay or lose presses against the saved "handled" counters below.
    private var homeSignal by mutableIntStateOf(0)
    // Whether the latest Home press came while home was already in front (Launcher3's rule): only
    // then does focus jump back to the first tile. Coming back from an app keeps your place.
    private var homeWasInFront = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeSignal = savedInstanceState?.getInt(KEY_HOME_SIGNAL) ?: 0
        // Back on the home screen does nothing.
        onBackPressedDispatcher.addCallback(this) { }
        val app = application as App
        if (savedInstanceState == null) app.maybeOpenCamerasAtStartup()
        setContent {
            var showAllApps by rememberSaveable { mutableStateOf(false) }
            var showInfo by rememberSaveable { mutableStateOf(false) }
            // Last loaded app list, shown instantly on repeat visits while a fresh list loads.
            var appsCache by remember { mutableStateOf<List<InstalledApp>?>(null) }
            val holder = rememberSaveableStateHolder()
            val icon: (String) -> androidx.compose.ui.graphics.ImageBitmap? =
                remember { { pkg -> app.catalog.icon(pkg)?.asImageBitmap() } }
            // Signal for HomeScreen: Home pressed while the home row itself was in front.
            var screenSignal by rememberSaveable { mutableIntStateOf(0) }
            var handledHome by rememberSaveable { mutableIntStateOf(homeSignal) }
            // Any Home press closes All apps or About. A press on the home row itself goes to HomeScreen.
            LaunchedEffect(homeSignal) {
                if (homeSignal != handledHome) {
                    handledHome = homeSignal
                    if (showAllApps || showInfo) {
                        showAllApps = false
                        showInfo = false
                    } else if (homeWasInFront) {
                        screenSignal++
                    }
                }
            }

            if (showInfo) {
                val infoTheme = remember {
                    app.controller.tiles().firstOrNull { it.target == Target.Info }?.theme ?: ConfigParser.DEFAULT_THEME
                }
                AboutScreen(
                    theme = infoTheme,
                    version = BuildConfig.VERSION_NAME,
                    onBack = { showInfo = false },
                    onOpenTvHome = { app.controller.openTvHome() },
                )
            } else if (showAllApps) {
                var failure by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(failure) {
                    if (failure != null) {
                        delay(3000)
                        failure = null
                    }
                }
                val apps by produceState(appsCache) {
                    value = withContext(Dispatchers.IO) { app.controller.allApps() }
                    appsCache = value
                }
                AllAppsScreen(
                    apps = apps.orEmpty(),
                    loading = apps == null,
                    icon = icon,
                    onOpen = { a ->
                        val r = app.controller.openApp(a)
                        if (r is LaunchResult.Failed) failure = r.message
                    },
                    onBack = { showAllApps = false },
                    failure = failure,
                )
            } else {
                holder.SaveableStateProvider("home") {
                    HomeScreen(
                        controller = app.controller,
                        icon = icon,
                        onShowAllApps = { showAllApps = true },
                        homeSignal = screenSignal,
                        onShowInfo = { showInfo = true },
                        onOpenLauncherSettings = {
                            startActivity(Intent(this@HomeActivity, LauncherSettingsActivity::class.java))
                        },
                    )
                }
            }
        }
    }

    // singleTask delivers every Home press here, whether home was in front or behind another app.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeWasInFront = hasWindowFocus() &&
            (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        homeSignal++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_HOME_SIGNAL, homeSignal)
    }

    private companion object {
        const val KEY_HOME_SIGNAL = "home_signal"
    }

    override fun onResume() {
        super.onResume()
        (application as App).tvHomeRequested = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(Intent(this, SetupActivity::class.java))
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    // Short press of Menu opens the launcher's own settings; a long press (above) opens Setup.
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.isTracking && !event.isCanceled) {
                startActivity(Intent(this, LauncherSettingsActivity::class.java))
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
