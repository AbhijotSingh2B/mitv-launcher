package dev.tvlauncher.ui.setup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.tvlauncher.BuildConfig
import dev.tvlauncher.ui.home.HomeActivity
import dev.tvlauncher.ui.settings.LauncherSettingsActivity

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Home Setup " + BuildConfig.VERSION_NAME)
                Button(onClick = { startActivity(Intent(this@SetupActivity, HomeActivity::class.java)) }) {
                    Text("Open home now")
                }
                Button(onClick = { startActivity(Intent(this@SetupActivity, LauncherSettingsActivity::class.java)) }) {
                    Text("Launcher settings")
                }
            }
        }
    }
}
