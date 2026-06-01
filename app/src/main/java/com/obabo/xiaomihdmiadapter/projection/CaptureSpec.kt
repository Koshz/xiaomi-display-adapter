package com.obabo.xiaomihdmiadapter.projection

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

data class CaptureSpec(
    val width: Int,
    val height: Int,
    val densityDpi: Int
)

object CaptureSpecProvider {
    fun current(context: Context): CaptureSpec {
        val metrics = context.resources.displayMetrics
        val bounds = currentBounds(context, metrics)
        return CaptureSpec(
            width = bounds.width().coerceAtLeast(1),
            height = bounds.height().coerceAtLeast(1),
            densityDpi = metrics.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
        )
    }

    @Suppress("DEPRECATION")
    private fun currentBounds(context: Context, metrics: DisplayMetrics): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(WindowManager::class.java)
            windowManager.currentWindowMetrics.bounds
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
    }
}
