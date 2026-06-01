package com.obabo.xiaomihdmiadapter.rotation

import android.content.Context
import android.provider.Settings
import android.view.Surface

class RotationController(
    private val context: Context
) {
    private var restoreState: RotationState? = null

    fun tryLockLandscape(): Boolean {
        if (!Settings.System.canWrite(context)) return false

        val resolver = context.contentResolver
        restoreState = RotationState(
            accelerometerRotation = readInt(Settings.System.ACCELEROMETER_ROTATION),
            userRotation = readInt(Settings.System.USER_ROTATION)
        )

        return runCatching {
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            Settings.System.putInt(resolver, Settings.System.USER_ROTATION, Surface.ROTATION_90)
        }.isSuccess
    }

    fun restoreIfNeeded() {
        val state = restoreState ?: return
        restoreState = null
        if (!Settings.System.canWrite(context)) return

        val resolver = context.contentResolver
        state.accelerometerRotation?.let {
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, it)
        }
        state.userRotation?.let {
            Settings.System.putInt(resolver, Settings.System.USER_ROTATION, it)
        }
    }

    private fun readInt(name: String): Int? {
        return runCatching {
            Settings.System.getInt(context.contentResolver, name)
        }.getOrNull()
    }

    private data class RotationState(
        val accelerometerRotation: Int?,
        val userRotation: Int?
    )
}
