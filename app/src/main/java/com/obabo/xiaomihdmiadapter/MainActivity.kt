package com.obabo.xiaomihdmiadapter

import android.Manifest
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obabo.xiaomihdmiadapter.core.DisplaySelector
import com.obabo.xiaomihdmiadapter.core.ExternalDisplayInfo
import com.obabo.xiaomihdmiadapter.projection.CaptureSpec
import com.obabo.xiaomihdmiadapter.projection.CaptureSpecProvider
import com.obabo.xiaomihdmiadapter.projection.MediaProjectionService
import com.obabo.xiaomihdmiadapter.state.AdapterStateReducer
import com.obabo.xiaomihdmiadapter.state.AdapterStateStore
import com.obabo.xiaomihdmiadapter.state.AdapterStatus
import com.obabo.xiaomihdmiadapter.state.ControllerEvent
import com.obabo.xiaomihdmiadapter.ui.AdapterScreen
import com.obabo.xiaomihdmiadapter.ui.HdmiAdapterTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var displayManager: DisplayManager
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var landscapePreparationJob: Job? = null
    private var preparedCaptureSpec: CaptureSpec? = null

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
                        val captureSpec = preparedCaptureSpec
                            ?: CaptureSpecProvider.currentLandscape(this)
                        if (captureSpec == null) {
                            AdapterStateStore.update(
                                AdapterStatus.Error(CaptureSpecProvider.LANDSCAPE_REQUIRED_MESSAGE)
                            )
                        } else {
                            MediaProjectionService.start(
                                context = this,
                                resultCode = result.resultCode,
                                resultData = result.data!!,
                                captureSpec = captureSpec
                            )
                        }
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
                    requestedOrientation = if (status.requiresLandscape()) {
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
                    AdapterStateReducer.reduce(status, ControllerEvent.PreparingLandscape)
                )
                prepareLandscapeThenRequestCapture(onRequestCapture)
            }
        }
    }

    private fun prepareLandscapeThenRequestCapture(onRequestCapture: () -> Unit) {
        landscapePreparationJob?.cancel()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        landscapePreparationJob = lifecycleScope.launch {
            val isReady = waitForLandscapeWindow()
            if (!isReady) {
                AdapterStateStore.update(
                    AdapterStatus.Error("Rotate phone to landscape and try again")
                )
                return@launch
            }

            preparedCaptureSpec = CaptureSpecProvider.currentLandscape(this@MainActivity)
            if (preparedCaptureSpec == null) {
                AdapterStateStore.update(
                    AdapterStatus.Error(CaptureSpecProvider.LANDSCAPE_REQUIRED_MESSAGE)
                )
                return@launch
            }

            AdapterStateStore.update(
                AdapterStateReducer.reduce(
                    AdapterStatus.PreparingLandscape,
                    ControllerEvent.CapturePermissionRequested
                )
            )
            onRequestCapture()
        }
    }

    private suspend fun waitForLandscapeWindow(): Boolean {
        repeat(LANDSCAPE_WAIT_ATTEMPTS) {
            if (hasLandscapeWindowMetrics()) return true
            delay(LANDSCAPE_WAIT_DELAY_MS)
        }
        return false
    }

    private fun hasLandscapeWindowMetrics(): Boolean {
        val orientation = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val bounds = currentWindowBounds()
        return orientation && bounds.width() > bounds.height()
    }

    @Suppress("DEPRECATION")
    private fun currentWindowBounds(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val metrics = DisplayMetrics()
            val windowManager = getSystemService(WindowManager::class.java)
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
    }

    override fun onDestroy() {
        landscapePreparationJob?.cancel()
        super.onDestroy()
    }

    private fun AdapterStatus.requiresLandscape(): Boolean {
        return this is AdapterStatus.PreparingLandscape ||
            this is AdapterStatus.NeedsCapturePermission ||
            this is AdapterStatus.Starting ||
            this is AdapterStatus.On
    }

    companion object {
        private const val LANDSCAPE_WAIT_ATTEMPTS = 50
        private const val LANDSCAPE_WAIT_DELAY_MS = 100L
    }
}
