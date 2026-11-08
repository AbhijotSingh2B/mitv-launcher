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
import dev.tvlauncher.core.catalog.InstalledApp
import dev.tvlauncher.core.home.LaunchResult
import dev.tvlauncher.ui.apps.AllAppsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import dev.tvlauncher.ui.settings.LauncherSettingsActivity
import dev.tvlauncher.ui.setup.SetupActivity

class HomeActivity : ComponentActivity() {
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Back on the home screen does nothing.
        onBackPressedDispatcher.addCallback(this) { }
        val app = application as App
        if (savedInstanceState == null) app.maybeOpenCamerasAtStartup()
        setContent {
            var showAllApps by rememberSaveable { mutableStateOf(false) }
            // Last loaded app list, shown instantly on repeat visits while a fresh list loads.
            var appsCache by remember { mutableStateOf<List<InstalledApp>?>(null) }
            val holder = rememberSaveableStateHolder()
            val icon: (String) -> androidx.compose.ui.graphics.ImageBitmap? =
                remember { { pkg -> app.catalog.icon(pkg)?.asImageBitmap() } }
            // Home pressed while on All apps returns to home.
            LaunchedEffect(homeSignal) { if (homeSignal > 0) showAllApps = false }

            if (showAllApps) {
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
                        homeSignal = homeSignal,
                    )
                }
            }
        }
    }

    // Home pressed while home is already showing: singleTask delivers the intent here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal++
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
