package com.kristof.brgbc.capture

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.kristof.brgbc.MainActivity
import com.kristof.brgbc.R
import com.kristof.brgbc.ble.BleLedController
import kotlinx.coroutines.*
import java.nio.ByteBuffer

/**
 * Foreground service that captures screen and syncs colors to LED strip
 * Runs at 10 FPS as specified by user
 */
class ScreenCaptureService : Service() {
    
    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_sync_channel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        
        private const val FPS = 30 // 30 FPS for smoother updates
        private const val FRAME_DELAY = 1000L / FPS // ~33ms
    }
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var captureJob: Job? = null
    
    private val colorSmoother = ColorSmoother(0.5f) // Slightly lower than 0.6 to balance the 3x FPS increase
    private var ledController: BleLedController? = null
    
    // Original screen metrics
    private var realScreenWidth = 0
    private var realScreenHeight = 0
    private var screenDensity = 0
    
    // Downscaled capture size (performance optimization)
    private var captureWidth = 0
    private var captureHeight = 0
    
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    
    override fun onCreate() {
        super.onCreate()
        try {
            Log.e(TAG, "ScreenCaptureService onCreate() STARTING")
            createNotificationChannel()
            getScreenMetrics()
            acquireWakeLock()
            Log.e(TAG, "ScreenCaptureService onCreate() COMPLETED")
        } catch (e: Exception) {
            Log.e(TAG, "CRASH IN onCreate(): ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "BRGBC::ScreenCaptureWakeLock"
            )
            wakeLock?.acquire()
            Log.e(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.e(TAG, "ACTION_START received")
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                
                Log.e(TAG, "ResultCode: $resultCode, ResultData: $resultData")
                
                if (resultCode != Activity.RESULT_OK || resultData == null) {
                    Log.e(TAG, "Invalid MediaProjection result - stopping service")
                    stopSelf()
                    return START_NOT_STICKY
                }
                
                Log.e(TAG, "Starting foreground service...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID, 
                        createNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                Log.e(TAG, "Foreground service started, now starting screen capture...")
                startScreenCapture(resultCode, resultData)
            }
            ACTION_STOP -> {
                Log.e(TAG, "ACTION_STOP received")
                stopScreenCapture()
                stopSelf()
            }
            else -> {
                Log.e(TAG, "Unknown action or null intent")
            }
        }
        
        return START_NOT_STICKY
    }

    private fun getScreenMetrics() {
        Log.e(TAG, "Getting screen metrics using DisplayManager...")
        // Use DisplayManager which is safe for Services (non-visual context)
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        
        val metrics = DisplayMetrics()
        
        if (display != null) {
            display.getRealMetrics(metrics)
            
            realScreenWidth = metrics.widthPixels
            realScreenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi
            
            // Calculate downscaled resolution for capture (maintain aspect ratio)
            // Target width roughly 160px is plenty for ambient color
            val scaleFactor = (realScreenWidth / 160).coerceAtLeast(1)
            captureWidth = realScreenWidth / scaleFactor
            captureHeight = realScreenHeight / scaleFactor
            
            Log.e(TAG, "Screen metrics: ${realScreenWidth}x${realScreenHeight}. Capture size: ${captureWidth}x${captureHeight}")
        } else {
            Log.e(TAG, "ERROR: Default display is null! Using fallback metrics.")
            realScreenWidth = 1920
            realScreenHeight = 1080
            screenDensity = 320
            captureWidth = 1920 / 10
            captureHeight = 1080 / 10
        }
    }
    
    private var lastRotation = -1

    private fun startScreenCapture(resultCode: Int, resultData: Intent) {
        try {
            Log.d(TAG, "Starting screen capture...")
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is NULL! ResultCode: $resultCode")
                return
            }
            
            // Register callback
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.e(TAG, "MediaProjection stopped by system")
                    stopSelf()
                }
            }, null)
            
            // Initialize Virtual Display
            setupVirtualDisplay()
            
            // Get LED controller instance
            ledController = LedControllerHolder.controller
            
            // Start capture loop
            startCaptureLoop()
            Log.d(TAG, "Capture loop started at $FPS FPS")
        } catch (e: Exception) {
            Log.e(TAG, "CRASH in startScreenCapture: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupVirtualDisplay() {
        try {
            if (mediaProjection == null) return

            // Re-fetch metrics to get current orientation dimensions
            getScreenMetrics()
            
            // Close existing if checking rotation
            virtualDisplay?.release()
            imageReader?.close()
            
            // Create ImageReader
            imageReader = ImageReader.newInstance(
                captureWidth,
                captureHeight,
                PixelFormat.RGBA_8888,
                2
            )
            
            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenSyncDisplay",
                captureWidth,
                captureHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null, 
                null
            )
            
            // Update rotation tracker
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            lastRotation = display?.rotation ?: 0
            
            Log.d(TAG, "Virtual Display setup: ${captureWidth}x${captureHeight} (Rot: $lastRotation)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up VirtualDisplay: ${e.message}")
            // Check for specific tokens indicating the permission is dead
            if (e.message?.contains("resultData") == true || e.message?.contains("instance") == true) {
                Log.e(TAG, "MediaProjection token invalidated (likely due to DRM/Secure content). Stopping service.")
                stopSelf()
            }
        }
    }

    private fun startCaptureLoop() {
        captureJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkRotation()
                    captureAndAnalyze()
                    delay(FRAME_DELAY)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in capture loop", e)
                    delay(500) // Backoff on error
                }
            }
        }
    }

    private fun checkRotation() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: return
        
        val currentRot = display.rotation
        
        // Also check if bounds changed (e.g. foldables, or some weird fullscreen modes)
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val currentWidth = metrics.widthPixels
        val currentHeight = metrics.heightPixels
        
        // Check if rotation changed OR dimensions swapped/changed significantly
        val dimensionsChanged = (currentWidth != realScreenWidth || currentHeight != realScreenHeight)
        
        if (currentRot != lastRotation || dimensionsChanged) {
            Log.i(TAG, "Display config changed (Rot: $lastRotation->$currentRot, Dim: $realScreenWidth x $realScreenHeight -> $currentWidth x $currentHeight). Resetting...")
            
            // Update "Real" dims immediately so we don't loop reset
            realScreenWidth = currentWidth
            realScreenHeight = currentHeight
            
            setupVirtualDisplay()
        }
    }

    private var lastLogTime = 0L

    private var blackFrameCount = 0

    private fun captureAndAnalyze() {
        val reader = imageReader ?: return

        val image = try {
            reader.acquireLatestImage()
        } catch (e: Exception) {
            // Log.w(TAG, "Failed to acquire image: ${e.message}")
            null
        } ?: return
        
        try {
            val bitmap = imageToBitmap(image)
            if (bitmap != null) {
                // Analyze
                val (r, g, b) = ColorAnalyzer.analyzeBitmap(bitmap)
                
                // --- Black Screen / Freeze Watchdog ---
                // Threshold raised to 25 to catch dark grey backgrounds (like 19,19,19) 
                // which often appear when video is tunneled/invisible.
                if (r < 25 && g < 25 && b < 25) {
                    
                    // Force LED off immediately for "near black"
                    val controller = LedControllerHolder.controller
                    if (controller != null && controller.isConnected) {
                        controller.setColor(0, 0, 0)
                    }

                    blackFrameCount++
                    if (blackFrameCount > 60) { 
                        if (blackFrameCount % 150 == 0) { // Log occasionally
                             Log.w(TAG, "DRM/Protected/Tunneled content detected (Screen is Dark). Waiting...")
                        }
                    }
                    // Skip the rest of processing for this frame
                    bitmap.recycle()
                    return
                } else {
                    blackFrameCount = 0
                }
                // --------------------------------------
                
                // Debug log every second
                val now = System.currentTimeMillis()
                if (now - lastLogTime > 1000) {
                    Log.d(TAG, "Analyzed Color: R=$r G=$g B=$b")
                    lastLogTime = now
                }
                
                // Smooth
                val (smoothR, smoothG, smoothB) = colorSmoother.smooth(r, g, b)
                
                // Send
                val controller = LedControllerHolder.controller
                if (controller != null && controller.isConnected) {
                    controller.setColor(smoothR, smoothG, smoothB)
                } else {
                    if (now - lastLogTime > 1000) { // piggyback on the 1s timer
                         Log.w(TAG, "Cannot sync: Controller not connected or null")
                    }
                }
                
                bitmap.recycle()
            }
        } catch (e: Exception) {
             Log.e(TAG, "Analyze error: ${e.message}")
        } finally {
            try { image.close() } catch (e: Exception) {}
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null
        
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * captureWidth
        
        val bitmap = Bitmap.createBitmap(
            captureWidth + rowPadding / pixelStride,
            captureHeight,
            Bitmap.Config.ARGB_8888
        )
        
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Crop to actual screen size if there's padding
        return if (rowPadding != 0) {
            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, captureWidth, captureHeight)
            bitmap.recycle()
            croppedBitmap
        } else {
            bitmap
        }
    }
    
    private fun stopScreenCapture() {
        captureJob?.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        captureJob = null
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "ScreenCaptureService onDestroy()")
        stopScreenCapture()
        serviceScope.cancel()
        
        // Release wake lock
        try {
            wakeLock?.release()
            Log.e(TAG, "Wake lock released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "LED screen synchronization service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Sync Active")
            .setContentText("Syncing screen colors to LED strip")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}

/**
 * Singleton holder for LED controller to share between Activity and Service
 */
object LedControllerHolder {
    var controller: BleLedController? = null
}
