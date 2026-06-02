package com.obabo.xiaomihdmiadapter.projection

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.view.Window
import com.obabo.xiaomihdmiadapter.R

class HdmiPresentation(
    context: Context,
    display: Display,
    private val captureSpec: CaptureSpec,
    private val surfaceListener: InputSurfaceListener
) : Presentation(context, display, R.style.Theme_HdmiPresentation) {

    private var glSurfaceView: StretchGlSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val rendererView = StretchGlSurfaceView(context, captureSpec, surfaceListener)
        glSurfaceView = rendererView
        setContentView(
            rendererView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onStop() {
        glSurfaceView?.releaseRenderer()
        glSurfaceView = null
        super.onStop()
    }
}
