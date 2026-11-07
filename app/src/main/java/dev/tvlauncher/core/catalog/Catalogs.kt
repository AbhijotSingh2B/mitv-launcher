package dev.tvlauncher.core.catalog

import android.graphics.Bitmap

data class InstalledApp(val packageName: String, val label: String)

interface AppCatalog {
    fun installedTvApps(): List<InstalledApp>
    fun icon(packageName: String): Bitmap?
}

data class HdmiInput(val id: String, val label: String)

interface InputCatalog {
    fun hdmiInputs(): List<HdmiInput>
}

/** Finds the input for HDMI [port]: by label ("HDMI 1" etc.), else the Nth input sorted by id. */
fun matchHdmiInput(port: Int, inputs: List<HdmiInput>): HdmiInput? {
    val wanted = "hdmi$port"
    inputs.firstOrNull { it.label.replace(" ", "").lowercase() == wanted }?.let { return it }
    return inputs.sortedBy { it.id }.getOrNull(port - 1)
}
