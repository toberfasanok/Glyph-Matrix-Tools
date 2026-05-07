package com.tober.glyphmatrixtools.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

class AppService(
    context: Context
) {
    private val packageManager = context.packageManager

    fun getLaunchableApps(): List<App> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        )

        val seen = mutableSetOf<String>()
        val apps = mutableListOf<App>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName

            if (!seen.add(packageName)) continue

            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)

                val isAndroid = packageName.startsWith("com.android.") || packageName.startsWith("android.")
                val isPureSystem =
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                if (isAndroid) continue
                if (isPureSystem) continue

                apps.add(
                    App(
                        packageName = packageName,
                        label = resolveInfo.loadLabel(packageManager).toString(),
                        icon = iconToBitmap(resolveInfo.loadIcon(packageManager))
                    )
                )
            } catch (_: Throwable) {
            }
        }

        return apps.sortedBy {
            it.label.lowercase()
        }
    }

    private fun iconToBitmap(
        icon: Drawable?
    ): Bitmap? {
        if (icon == null) return null

        return try {
            if (icon is BitmapDrawable) {
                return icon.bitmap
            }

            val width = icon.intrinsicWidth.coerceAtLeast(1)
            val height = icon.intrinsicHeight.coerceAtLeast(1)
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            icon.setBounds(0, 0, canvas.width, canvas.height)
            icon.draw(canvas)

            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}
