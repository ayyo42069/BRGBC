package com.kristof.brgbc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kristof.brgbc.ble.BleLedController
import com.kristof.brgbc.ble.LedEffect
import com.kristof.brgbc.capture.LedControllerHolder
import com.kristof.brgbc.capture.ScreenCaptureService
import com.kristof.brgbc.ui.LedControlScreen
import com.kristof.brgbc.ui.theme.BRGBCTheme
import com.kristof.brgbc.viewmodel.LedViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: LedViewModel by viewModels()
    private lateinit var ledController: BleLedController
    private val scannedDevices = mutableStateListOf<BluetoothDevice>()
    
    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Permissions required for LED control", Toast.LENGTH_LONG).show()
        }
    }
    
    // MediaProjection permission launcher
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.mediaProjectionResultCode = result.resultCode
            viewModel.mediaProjectionResultData = result.data
            startScreenCaptureService()
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize LED controller - Singleton pattern to avoid replacing the active controller 
        // that the Service might be using
        if (LedControllerHolder.controller == null) {
            ledController = BleLedController(applicationContext)
            LedControllerHolder.controller = ledController
        } else {
            ledController = LedControllerHolder.controller!!
            Log.d("MainActivity", "Reusing existing LED controller instance")
        }
        
        // Set connection callback
        ledController.onConnectionStateChanged = { connected ->
            val deviceName = scannedDevices.firstOrNull()?.name ?: "LED Strip"
            viewModel.setConnected(connected, if (connected) deviceName else null)
            if (connected) {
                runOnUiThread {
                    Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Restore state if we are reconnecting to a running service/controller
        if (ledController.isConnected) {
            Log.d("MainActivity", "Restoring connection state: Connected")
            viewModel.setConnected(true, "LED Strip (Active)")
            // If screen sync service is running, we might want to update that state too, 
            // though ScreenCaptureService relies on the Service lifecycle. 
            // We can check if service is running if we had a way, but properly syncing 'isConnected' is the first step.
        }
        
        // Set service discovery callback - turn on AFTER services are discovered
        ledController.onServicesReady = {
            ledController.turnOn()
            Toast.makeText(this, "LED controller ready!", Toast.LENGTH_SHORT).show()
        }
        
        // Request permissions
        requestRequiredPermissions()
        
        setContent {
            BRGBCTheme {
                LedControlScreen(
                    viewModel = viewModel,
                    onScanDevices = ::scanForDevices,
                    onConnectDevice = ::connectToDevice,
                    onDisconnect = ::disconnectDevice,
                    onStartScreenSync = ::requestScreenCapturePermission,
                    onStopScreenSync = ::stopScreenCapture,
                    onSetColor = ::setStaticColor,
                    onSetBrightness = ::setBrightness,
                    onStartEffect = ::startEffect,
                    onStopEffect = ::stopEffect,
                    scannedDevices = scannedDevices,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Auto-scan and connect after a short delay to allow UI to initialize
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (hasBluetoothPermissions()) {
                autoScanAndConnect()
            }
        }, 1000)
    }
    
    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Audio Permission (Required for Sync)
        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        
        // Filter out already granted permissions
        val neededPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
        
        // Request battery optimization exemption
        requestBatteryOptimizationExemption()
    }
    
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent().apply {
                        action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Please disable battery optimization for this app", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun scanForDevices() {
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permissions not granted", Toast.LENGTH_SHORT).show()
            return
        }
        
        scannedDevices.clear()
        Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        
        ledController.scanForDevices(
            duration = 10000,
            onDeviceFound = { device ->
                if (!scannedDevices.contains(device)) {
                    scannedDevices.add(device)
                }
            },
            onScanComplete = {
                val message = if (scannedDevices.isEmpty()) {
                    "No devices found"
                } else {
                    "Found ${scannedDevices.size} device(s)"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    @SuppressLint("MissingPermission")
    private fun autoScanAndConnect() {
        Toast.makeText(this, "Searching for LED strip...", Toast.LENGTH_SHORT).show()
        
        ledController.scanForDevices(
            duration = 8000,
            onDeviceFound = { device ->
                if (!scannedDevices.contains(device)) {
                    scannedDevices.add(device)
                    // Auto-connect to first device found
                    if (scannedDevices.size == 1) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            connectToDevice(device)
                        }, 500)
                    }
                }
            },
            onScanComplete = {
                if (scannedDevices.isEmpty()) {
                    Toast.makeText(this, "No LED strips found nearby", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permissions not granted", Toast.LENGTH_SHORT).show()
            return
        }
        
        ledController.connect(device)
        Toast.makeText(this, "Connecting to ${device.name}...", Toast.LENGTH_SHORT).show()
    }
    
    private fun disconnectDevice() {
        ledController.disconnect()
        viewModel.setConnected(false)
    }
    
    private fun requestScreenCapturePermission() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }
    
    private fun startScreenCaptureService() {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, viewModel.mediaProjectionResultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, viewModel.mediaProjectionResultData)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start service: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
        
        viewModel.setScreenSyncActive(true)
        Toast.makeText(this, "Screen sync started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopScreenCapture() {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        startService(intent)
        viewModel.setScreenSyncActive(false)
    }
    
    private fun setStaticColor(color: Color) {
        val argb = color.toArgb()
        val r = android.graphics.Color.red(argb)
        val g = android.graphics.Color.green(argb)
        val b = android.graphics.Color.blue(argb)
        
        ledController.setColor(r, g, b)
    }
    
    private fun setBrightness(level: Int) {
        ledController.setBrightness(level)
    }
    
    private fun startEffect(effect: LedEffect, speed: Float) {
        ledController.startEffect(effect, speed)
    }
    
    private fun stopEffect() {
        ledController.stopEffect()
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Do NOT cleanup/disconnect here, as the Service might be using the controller.
        // The controller will be cleaned up when the process dies or when explicitly disconnected.
    }
}