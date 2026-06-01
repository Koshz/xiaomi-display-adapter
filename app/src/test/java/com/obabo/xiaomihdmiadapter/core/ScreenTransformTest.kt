package com.obabo.xiaomihdmiadapter.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import com.obabo.xiaomihdmiadapter.projection.CaptureSpec
import com.obabo.xiaomihdmiadapter.projection.CaptureSpecProvider

class ScreenTransformTest {
    @Test
    fun xiaomiUltraSourceStretchesToFullHdTarget() {
        val transform = ScreenTransform.stretchedFill(
            source = IntSize2(width = 3200, height = 1440),
            target = IntSize2(width = 1920, height = 1080)
        )

        assertEquals(0.6f, transform.scaleX, 0.0001f)
        assertEquals(0.75f, transform.scaleY, 0.0001f)
        assertEquals(1920, transform.outputWidth)
        assertEquals(1080, transform.outputHeight)
        assertFalse(transform.preservesAspectRatio)
    }

    @Test
    fun landscapeCaptureSpecIsAllowed() {
        val captureSpec = CaptureSpec(width = 3200, height = 1440, densityDpi = 560)

        assertEquals(true, CaptureSpecProvider.isLandscape(captureSpec))
    }

    @Test
    fun portraitCaptureSpecIsRejected() {
        val captureSpec = CaptureSpec(width = 1440, height = 3200, densityDpi = 560)

        assertEquals(false, CaptureSpecProvider.isLandscape(captureSpec))
    }
}
