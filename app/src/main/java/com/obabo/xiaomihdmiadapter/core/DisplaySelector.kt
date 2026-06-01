package com.obabo.xiaomihdmiadapter.core

import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ExternalDisplayInfo(
    val displayId: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val refreshRate: Float
)

object DisplaySelector {
    fun findBestPresentationDisplay(displayManager: DisplayManager): Display? {
        return displayManager
            .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .filter { it.isValid }
            .maxWithOrNull(compareBy<Display> { displayScore(it) }.thenBy { it.displayId })
    }

    fun findPresentationInfo(displayManager: DisplayManager): ExternalDisplayInfo? {
        return findBestPresentationDisplay(displayManager)?.toExternalDisplayInfo()
    }

    fun presentationDisplayInfoFlow(displayManager: DisplayManager): Flow<ExternalDisplayInfo?> {
        return callbackFlow {
            fun sendCurrent() {
                trySend(findPresentationInfo(displayManager))
            }

            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = sendCurrent()
                override fun onDisplayRemoved(displayId: Int) = sendCurrent()
                override fun onDisplayChanged(displayId: Int) = sendCurrent()
            }

            sendCurrent()
            displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
            awaitClose { displayManager.unregisterDisplayListener(listener) }
        }
    }

    private fun displayScore(display: Display): Int {
        val mode = display.mode ?: return 0
        val width = mode.physicalWidth
        val height = mode.physicalHeight
        val landscapeWidth = maxOf(width, height)
        val landscapeHeight = minOf(width, height)

        var score = 1
        if (landscapeWidth == 1920 && landscapeHeight == 1080) score += 1_000
        if (landscapeWidth >= 1920 && landscapeHeight >= 1080) score += 250
        score += (mode.refreshRate.coerceAtMost(120f)).toInt()
        return score
    }

    private fun Display.toExternalDisplayInfo(): ExternalDisplayInfo {
        val mode = mode
        return ExternalDisplayInfo(
            displayId = displayId,
            name = name ?: "HDMI display",
            width = mode?.physicalWidth ?: 0,
            height = mode?.physicalHeight ?: 0,
            refreshRate = mode?.refreshRate ?: 0f
        )
    }
}
