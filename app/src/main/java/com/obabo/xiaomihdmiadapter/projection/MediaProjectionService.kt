package com.obabo.xiaomihdmiadapter.projection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Display
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.obabo.xiaomihdmiadapter.MainActivity
import com.obabo.xiaomihdmiadapter.R
import com.obabo.xiaomihdmiadapter.core.DisplaySelector
import com.obabo.xiaomihdmiadapter.rotation.RotationController
import com.obabo.xiaomihdmiadapter.state.AdapterStateStore
import com.obabo.xiaomihdmiadapter.state.AdapterStatus

class MediaProjectionService : Service(), InputSurfaceListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var displayManager: DisplayManager
    private lateinit var notificationManager: NotificationManager
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: HdmiPresentation? = null
    private var activeDisplayId: Int? = null
    private var captureSpec: CaptureSpec? = null
    private var actualCaptureWidth: Int? = null
    private var actualCaptureHeight: Int? = null
    private lateinit var rotationController: RotationController

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            mainHandler.post {
                stopAdapter(updateState = true, stopProjection = false)
            }
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            mainHandler.post {
                handleCapturedContentResize(width, height)
            }
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == activeDisplayId) {
                mainHandler.post {
                    failAndStop("HDMI display was disconnected")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        displayManager = getSystemService(DisplayManager::class.java)
        notificationManager = getSystemService(NotificationManager::class.java)
        rotationController = RotationController(this)
        createNotificationChannel()
        displayManager.registerDisplayListener(displayListener, mainHandler)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAdapter(intent)
            ACTION_STOP -> stopAdapter(updateState = true)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(displayListener)
        releaseSession(stopProjection = true)
        super.onDestroy()
    }

    override fun onInputSurfaceReady(surface: Surface, captureSpec: CaptureSpec) {
        mainHandler.post {
            createVirtualDisplay(surface, captureSpec)
        }
    }

    override fun onInputSurfaceDestroyed() {
        mainHandler.post {
            virtualDisplay?.release()
            virtualDisplay = null
        }
    }

    override fun onRendererError(message: String, throwable: Throwable?) {
        mainHandler.post {
            failAndStop(message)
        }
    }

    private fun startAdapter(intent: Intent) {
        AdapterStateStore.update(AdapterStatus.Starting)
        startForegroundCompat(buildNotification())

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode == 0 || resultData == null) {
            failAndStop("Screen capture permission data was missing")
            return
        }

        val display = DisplaySelector.findBestPresentationDisplay(displayManager)
        if (display == null) {
            failAndStop("No HDMI display is connected")
            return
        }

        releaseSession(stopProjection = true)
        startForegroundCompat(buildNotification())
        AdapterStateStore.update(AdapterStatus.Starting)

        rotationController.tryLockLandscape()
        val landscapeCaptureSpec = intent.captureSpecFromExtras()
            ?: CaptureSpecProvider.currentLandscape(this)
        if (landscapeCaptureSpec == null) {
            failAndStop(CaptureSpecProvider.LANDSCAPE_REQUIRED_MESSAGE)
            return
        }
        captureSpec = landscapeCaptureSpec

        val manager = getSystemService(MediaProjectionManager::class.java)
        projection = manager.getMediaProjection(resultCode, resultData).also {
            it.registerCallback(projectionCallback, mainHandler)
        }

        showPresentation(display, landscapeCaptureSpec)
    }

    private fun showPresentation(display: Display, captureSpec: CaptureSpec) {
        try {
            activeDisplayId = display.displayId
            presentation = HdmiPresentation(
                context = this,
                display = display,
                captureSpec = captureSpec,
                surfaceListener = this
            ).also { it.show() }
        } catch (throwable: Throwable) {
            failAndStop("Could not open HDMI presentation")
        }
    }

    private fun createVirtualDisplay(surface: Surface, captureSpec: CaptureSpec) {
        val activeProjection = projection ?: run {
            failAndStop("Screen capture session is not active")
            return
        }

        virtualDisplay?.release()
        virtualDisplay = activeProjection.createVirtualDisplay(
            "HdmiAdapterCapture",
            captureSpec.width,
            captureSpec.height,
            captureSpec.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            object : VirtualDisplay.Callback() {
                override fun onStopped() {
                    mainHandler.post {
                        virtualDisplay = null
                    }
                }
            },
            mainHandler
        )
        updateOnState()
    }

    private fun handleCapturedContentResize(width: Int, height: Int) {
        val expected = captureSpec ?: return
        actualCaptureWidth = width
        actualCaptureHeight = height

        if (CaptureContentValidator.isSuspiciouslySmaller(expected, width, height)) {
            failAndStop(CaptureContentValidator.FULL_DISPLAY_REQUIRED_MESSAGE)
            return
        }

        updateOnState()
    }

    private fun updateOnState() {
        val expected = captureSpec ?: return
        AdapterStateStore.update(
            AdapterStatus.On(
                displayName = presentation?.display?.name ?: "HDMI display",
                expectedCaptureWidth = expected.width,
                expectedCaptureHeight = expected.height,
                actualCaptureWidth = actualCaptureWidth,
                actualCaptureHeight = actualCaptureHeight
            )
        )
    }

    private fun failAndStop(message: String) {
        AdapterStateStore.update(AdapterStatus.Error(message))
        releaseSession(stopProjection = true)
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopAdapter(updateState: Boolean, stopProjection: Boolean = true) {
        releaseSession(stopProjection = stopProjection)

        if (updateState) {
            AdapterStateStore.update(AdapterStatus.Off)
        }
        stopForegroundCompat()
        stopSelf()
    }

    private fun releaseSession(stopProjection: Boolean) {
        virtualDisplay?.release()
        virtualDisplay = null

        presentation?.dismiss()
        presentation = null
        activeDisplayId = null
        captureSpec = null
        actualCaptureWidth = null
        actualCaptureHeight = null

        if (stopProjection) {
            projection?.let {
                runCatching { it.unregisterCallback(projectionCallback) }
                runCatching { it.stop() }
            }
        } else {
            projection?.let {
                runCatching { it.unregisterCallback(projectionCallback) }
            }
        }
        projection = null

        rotationController.restoreIfNeeded()
    }

    private fun startForegroundCompat(notification: Notification) {
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MediaProjectionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hdmi)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .build()
    }

    companion object {
        private const val ACTION_START = "com.obabo.xiaomihdmiadapter.action.START"
        private const val ACTION_STOP = "com.obabo.xiaomihdmiadapter.action.STOP"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val EXTRA_CAPTURE_WIDTH = "capture_width"
        private const val EXTRA_CAPTURE_HEIGHT = "capture_height"
        private const val EXTRA_CAPTURE_DENSITY_DPI = "capture_density_dpi"
        private const val CHANNEL_ID = "hdmi_adapter"
        private const val NOTIFICATION_ID = 1001

        fun start(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            captureSpec: CaptureSpec
        ) {
            val intent = Intent(context, MediaProjectionService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)
                .putExtra(EXTRA_CAPTURE_WIDTH, captureSpec.width)
                .putExtra(EXTRA_CAPTURE_HEIGHT, captureSpec.height)
                .putExtra(EXTRA_CAPTURE_DENSITY_DPI, captureSpec.densityDpi)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }

    private fun Intent.captureSpecFromExtras(): CaptureSpec? {
        val width = getIntExtra(EXTRA_CAPTURE_WIDTH, 0)
        val height = getIntExtra(EXTRA_CAPTURE_HEIGHT, 0)
        val densityDpi = getIntExtra(EXTRA_CAPTURE_DENSITY_DPI, 0)
        if (width <= 0 || height <= 0 || densityDpi <= 0) return null

        return CaptureSpec(
            width = width,
            height = height,
            densityDpi = densityDpi
        ).takeIf(CaptureSpecProvider::isLandscape)
    }
}
