package com.obabo.xiaomihdmiadapter.projection

import android.view.Surface

interface InputSurfaceListener {
    fun onInputSurfaceReady(surface: Surface, captureSpec: CaptureSpec)
    fun onInputSurfaceDestroyed()
    fun onRendererError(message: String, throwable: Throwable? = null)
}
