package com.obabo.xiaomihdmiadapter

import android.Manifest
import android.app.Activity
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obabo.xiaomihdmiadapter.core.DisplaySelector
import com.obabo.xiaomihdmiadapter.core.ExternalDisplayInfo
import com.obabo.xiaomihdmiadapter.projection.MediaProjectionService
import com.obabo.xiaomihdmiadapter.state.AdapterStateReducer
import com.obabo.xiaomihdmiadapter.state.AdapterStateStore
import com.obabo.xiaomihdmiadapter.state.AdapterStatus
import com.obabo.xiaomihdmiadapter.state.ControllerEvent
import com.obabo.xiaomihdmiadapter.ui.AdapterScreen
import com.obabo.xiaomihdmiadapter.ui.HdmiAdapterTheme

class MainActivity : ComponentActivity() {
    private lateinit var displayManager: DisplayManager
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayManager = getSystemService(DisplayManager::class.java)
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)

        setContent {
            HdmiAdapterTheme {
                val status by AdapterStateStore.status.collectAsStateWithLifecycle()
                val displayInfo by DisplaySelector.presentationDisplayInfoFlow(displayManager)
                    .collectAsStateWithLifecycle(initialValue = DisplaySelector.findPresentationInfo(displayManager))

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    // Foreground services can still run if the notification permission is denied.
                }

                val captureLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        MediaProjectionService.start(
                            context = this,
                            resultCode = result.resultCode,
                            resultData = result.data!!
                        )
                    } else {
                        AdapterStateStore.update(
                            AdapterStateReducer.reduce(status, ControllerEvent.PermissionDenied)
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                DisposableEffect(status) {
                    requestedOrientation = if (status is AdapterStatus.Starting || status is AdapterStatus.On) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                    onDispose { }
                }

                AdapterScreen(
                    status = status,
                    displayInfo = displayInfo,
                    onPowerClick = {
                        handlePowerClick(
                            status = status,
                            displayInfo = displayInfo,
                            onRequestCapture = {
                                captureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        )
                    }
                )
            }
        }
    }

    private fun handlePowerClick(
        status: AdapterStatus,
        displayInfo: ExternalDisplayInfo?,
        onRequestCapture: () -> Unit
    ) {
        when (status) {
            is AdapterStatus.On,
            is AdapterStatus.Starting -> MediaProjectionService.stop(this)

            else -> {
                if (displayInfo == null) {
                    AdapterStateStore.update(
                        AdapterStateReducer.reduce(status, ControllerEvent.HdmiMissing)
                    )
                    return
                }
                AdapterStateStore.update(
                    AdapterStateReducer.reduce(status, ControllerEvent.CapturePermissionRequested)
                )
                onRequestCapture()
            }
        }
    }
}
