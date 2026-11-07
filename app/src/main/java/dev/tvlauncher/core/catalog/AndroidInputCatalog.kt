package dev.tvlauncher.core.catalog

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

class AndroidInputCatalog(private val context: Context) : InputCatalog {
    override fun hdmiInputs(): List<HdmiInput> = runCatching {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        manager.tvInputList
            .filter { it.isPassthroughInput && it.type == TvInputInfo.TYPE_HDMI }
            .map { info ->
                HdmiInput(
                    id = info.id,
                    label = info.loadCustomLabel(context)?.toString()
                        ?: info.loadLabel(context)?.toString()
                        ?: info.id,
                )
            }
    }.getOrDefault(emptyList())
}
