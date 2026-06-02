package com.obabo.xiaomihdmiadapter.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterStateReducerTest {
    @Test
    fun hdmiMissingMovesToMissingHdmi() {
        val next = AdapterStateReducer.reduce(AdapterStatus.Off, ControllerEvent.HdmiMissing)

        assertEquals(AdapterStatus.MissingHdmi, next)
    }

    @Test
    fun permissionDeniedMovesToError() {
        val next = AdapterStateReducer.reduce(
            AdapterStatus.NeedsCapturePermission,
            ControllerEvent.PermissionDenied
        )

        assertTrue(next is AdapterStatus.Error)
        assertEquals("Screen capture permission was denied", (next as AdapterStatus.Error).message)
    }

    @Test
    fun preparingLandscapeMovesToPreparingLandscape() {
        val next = AdapterStateReducer.reduce(
            AdapterStatus.Off,
            ControllerEvent.PreparingLandscape
        )

        assertEquals(AdapterStatus.PreparingLandscape, next)
    }

    @Test
    fun stopMovesToOff() {
        val next = AdapterStateReducer.reduce(
            AdapterStatus.On("HDMI", 1920, 1080, 1920, 1080),
            ControllerEvent.Stopped
        )

        assertEquals(AdapterStatus.Off, next)
    }

    @Test
    fun onStatusExposesExpectedAndActualCaptureDimensions() {
        val status = AdapterStatus.On(
            displayName = "HDMI",
            expectedCaptureWidth = 3200,
            expectedCaptureHeight = 1440,
            actualCaptureWidth = 3200,
            actualCaptureHeight = 1440
        )

        assertEquals(3200, status.expectedCaptureWidth)
        assertEquals(1440, status.expectedCaptureHeight)
        assertEquals(3200, status.actualCaptureWidth)
        assertEquals(1440, status.actualCaptureHeight)
    }
}
