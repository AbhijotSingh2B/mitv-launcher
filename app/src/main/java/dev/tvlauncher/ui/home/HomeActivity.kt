package dev.tvlauncher.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import dev.tvlauncher.App
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
            HomeScreen(
                controller = app.controller,
                icon = { pkg -> app.catalog.icon(pkg)?.asImageBitmap() },
                onShowAllApps = { /* Task 7 */ },
                homeSignal = homeSignal,
            )
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
