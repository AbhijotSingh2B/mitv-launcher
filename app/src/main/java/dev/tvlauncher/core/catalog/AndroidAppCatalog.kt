package dev.tvlauncher.core.catalog

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.LruCache

class AndroidAppCatalog(private val context: Context) : AppCatalog {
    private val pm: PackageManager = context.packageManager
    private val cache = LruCache<String, Bitmap>(48)

    override fun installedTvApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName && it.packageName != "dev.tvlauncher" }
            .sortedBy { it.label.lowercase() }
    }

    override fun icon(packageName: String): Bitmap? {
        cache.get(packageName)?.let { return it }
        val bitmap = runCatching { render(loadDrawable(packageName)) }.getOrNull() ?: return null
        cache.put(packageName, bitmap)
        return bitmap
    }

    private fun loadDrawable(packageName: String): Drawable? {
        val info = pm.getApplicationInfo(packageName, 0)
        val res = if (info.banner != 0) info.banner else info.icon
        if (res != 0) {
            val fromDensity = runCatching {
                pm.getResourcesForApplication(packageName)
                    .getDrawableForDensity(res, DisplayMetrics.DENSITY_XXXHIGH, null)
            }.getOrNull()
            if (fromDensity != null) return fromDensity
        }
        return info.loadIcon(pm)
    }

    private fun render(drawable: Drawable?): Bitmap? {
        drawable ?: return null
        // Keep the drawable's own aspect ratio (longest side SIZE): a 16:9 banner must not be stretched.
        val iw = drawable.intrinsicWidth
        val ih = drawable.intrinsicHeight
        var w = SIZE
        var h = SIZE
        if (iw > 0 && ih > 0) {
            val scale = SIZE.toFloat() / maxOf(iw, ih)
            w = (iw * scale).toInt().coerceAtLeast(1)
            h = (ih * scale).toInt().coerceAtLeast(1)
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }

    private companion object {
        const val SIZE = 256
    }
}
