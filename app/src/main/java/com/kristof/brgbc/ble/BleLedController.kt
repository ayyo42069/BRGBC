package com.kristof.brgbc.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.*
import kotlin.random.Random

/**
 * Bluetooth LE controller for ELK-BLEDOB/ELK-BLEDOM RGB LED strips
 */
class BleLedController(private val context: Context) {
    
    companion object {
        private const val TAG = "BleLedController"
        private val CHARACTERISTIC_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
        private const val DEFAULT_DEVICE_NAME = "ELK-BLEDOB"
        private const val FALLBACK_DEVICE_NAME = "ELK-BLEDOM"
    }
    
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    var isConnected = false
        private set
    
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onServicesReady: (() -> Unit)? = null
    
    private var effectJob: Job? = null
    private val effectScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    var colorOrder: String = "rgb" // Hardware color order
    
    /**
     * Scan for LED devices
     */
    @SuppressLint("MissingPermission")
    fun scanForDevices(
        duration: Long = 10000,
        onDeviceFound: (BluetoothDevice) -> Unit,
        onScanComplete: () -> Unit
    ) {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "Bluetooth LE scanner not available")
            onScanComplete()
            return
        }
        
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name
                if (name != null && (name.contains(DEFAULT_DEVICE_NAME) || name.contains(FALLBACK_DEVICE_NAME))) {
                    Log.d(TAG, "Found device: $name (${device.address})")
                    onDeviceFound(device)
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error: $errorCode")
                onScanComplete()
            }
        }
        
        scanner.startScan(scanCallback)
        
        // Stop scan after duration
        CoroutineScope(Dispatchers.Main).launch {
            delay(duration)
            scanner.stopScan(scanCallback)
            onScanComplete()
        }
    }
    
    /**
     * Connect to a specific Bluetooth device
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to ${device.name} (${device.address})")
        
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.e(TAG, "=== onConnectionStateChange: status=$status, newState=$newState ===")
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.e(TAG, "Connected to GATT server!")
                        isConnected = true
                        
                        Log.e(TAG, "Calling discoverServices()...")
                        try {
                            val result = gatt.discoverServices()
                            Log.e(TAG, "discoverServices() returned: $result")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error calling discoverServices(): ${e.message}")
                        }
                        
                        // Notify connection state AFTER starting discovery
                        onConnectionStateChanged?.invoke(true)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.e(TAG, "Disconnected from GATT server")
                        isConnected = false
                        onConnectionStateChanged?.invoke(false)
                        ledCharacteristic = null
                    }
                    else -> {
                        Log.e(TAG, "Unknown connection state: $newState")
                    }
                }
            }
            
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.e(TAG, "=== onServicesDiscovered CALLED ===")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "Services discovered successfully!")
                    Log.e(TAG, "Total services found: ${gatt.services.size}")
                    
                    // Log ALL services and characteristics for debugging
                    gatt.services.forEach { service ->
                        Log.e(TAG, "Service: ${service.uuid}")
                        service.characteristics.forEach { characteristic ->
                            val props = characteristic.properties
                            val canWrite = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                          (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                            Log.e(TAG, "  - Char: ${characteristic.uuid} (writable: $canWrite)")
                            
                            // Try to find our target characteristic
                            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                                ledCharacteristic = characteristic
                                Log.e(TAG, "  *** FOUND TARGET CHARACTERISTIC fff3! ***")
                            }
                            
                            // FALLBACK: If we can't find fff3, use ANY writable characteristic in fff0 service
                            if (ledCharacteristic == null && canWrite) {
                                val serviceUuidStr = service.uuid.toString().lowercase()
                                if (serviceUuidStr.contains("fff0")) {
                                    ledCharacteristic = characteristic
                                    Log.e(TAG, "  *** USING FALLBACK WRITABLE CHARACTERISTIC: ${characteristic.uuid} ***")
                                }
                            }
                        }
                    }
                    
                    if (ledCharacteristic != null) {
                        Log.e(TAG, "SUCCESS! Using characteristic: ${ledCharacteristic!!.uuid}")
                        // Notify that services are ready
                        onServicesReady?.invoke()
                    } else {
                        Log.e(TAG, "ERROR! No suitable characteristic found!")
                    }
                } else {
                    Log.e(TAG, "Service discovery FAILED with status: $status")
                }
            }
        }
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    /**
     * Disconnect from the current device
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopEffect()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        ledCharacteristic = null
        isConnected = false
    }
    
    /**
     * Send a packet to the LED strip
     */
    private var lastErrorLogTime = 0L

    @SuppressLint("MissingPermission")
    private fun sendPacket(data: ByteArray): Boolean {
        if (!isConnected) {
            if (System.currentTimeMillis() - lastErrorLogTime > 2000) {
                Log.w(TAG, "Cannot send packet: Not connected")
                lastErrorLogTime = System.currentTimeMillis()
            }
            return false
        }
        
        val char = ledCharacteristic
        if (char == null) {
            if (System.currentTimeMillis() - lastErrorLogTime > 2000) {
                Log.w(TAG, "Cannot send packet: Characteristic not found (but connected)")
                lastErrorLogTime = System.currentTimeMillis()
            }
            return false
        }
        
        // Use newer API (Android 13+) if available, otherwise fall back to deprecated method
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt?.writeCharacteristic(
                char,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
        } else {
            @Suppress("DEPRECATION")
            char.value = data
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            bluetoothGatt?.writeCharacteristic(char)
        }
        return true
    }
    
    /**
     * Set LED color
     */
    fun setColor(r: Int, g: Int, b: Int) {
        val packet = LedProtocol.buildColorPacket(r, g, b, colorOrder)
        sendPacket(packet)
    }
    
    /**
     * Set LED brightness (0-100)
     */
    fun setBrightness(level: Int) {
        val packet = LedProtocol.buildBrightnessPacket(level)
        sendPacket(packet)
    }
    
    /**
     * Turn LED on
     */
    fun turnOn() {
        val packet = LedProtocol.buildPowerOnPacket()
        sendPacket(packet)
    }
    
    /**
     * Turn LED off
     */
    fun turnOff() {
        stopEffect()
        val packet = LedProtocol.buildPowerOffPacket()
        sendPacket(packet)
    }
    
    /**
     * Stop any running effects
     */
    fun stopEffect() {
        effectJob?.cancel()
        effectJob = null
    }
    
    /**
     * Start an LED effect
     */
    fun startEffect(effect: LedEffect, speed: Float = 0.1f) {
        stopEffect()
        
        effectJob = effectScope.launch {
            when (effect) {
                LedEffect.RAINBOW -> effectRainbow(speed)
                LedEffect.BREATHE -> effectBreathe(255, 180, 50, speed)
                LedEffect.POLICE -> effectPolice(speed)
                LedEffect.ORBIT -> effectOrbit(speed)
                LedEffect.LIGHTNING -> effectLightning()
                LedEffect.HEARTBEAT -> effectHeartbeat(255, 0, 0)
                LedEffect.FIRE -> effectFire()
            }
        }
    }
    
    // --- Effect Implementations (ported from Python) ---
    
    private suspend fun effectRainbow(speed: Float) {
        while (coroutineContext.isActive) {
            for (i in 0 until 360 step 5) {
                val (r, g, b) = hsvToRgb(i / 360f, 1f, 1f)
                setColor(r, g, b)
                delay((speed * 1000).toLong())
            }
        }
    }
    
    private suspend fun effectBreathe(r: Int, g: Int, b: Int, speed: Float) {
        setColor(r, g, b)
        while (coroutineContext.isActive) {
            // Fade In
            for (i in 5..100 step 2) {
                setBrightness(i)
                delay((speed * 1000).toLong())
            }
            // Fade Out
            for (i in 100 downTo 5 step 2) {
                setBrightness(i)
                delay((speed * 1000).toLong())
            }
        }
    }
    
    private suspend fun effectPolice(speed: Float) {
        while (coroutineContext.isActive) {
            setColor(255, 0, 0)
            delay((speed * 1000).toLong())
            setColor(0, 0, 255)
            delay((speed * 1000).toLong())
            setColor(255, 0, 0)
            delay((speed * 500).toLong())
            setColor(0, 0, 255)
            delay((speed * 500).toLong())
        }
    }
    
    private suspend fun effectOrbit(speed: Float) {
        var angle = 0
        while (coroutineContext.isActive) {
            angle = (angle + 5) % 360
            
            val (r, g, b) = hsvToRgb(angle / 360f, 1f, 1f)
            
            // Sine wave brightness
            val rads = Math.toRadians((angle * 2).toDouble())
            val brightness = ((sin(rads) + 1) / 2 * 90 + 10).toInt()
            
            val scale = brightness / 100f
            setColor((r * scale).toInt(), (g * scale).toInt(), (b * scale).toInt())
            
            delay((speed * 1000).toLong())
        }
    }
    
    private suspend fun effectLightning() {
        while (coroutineContext.isActive) {
            delay(Random.nextLong(1000, 5000))
            
            // Flash 1-3 times
            repeat(Random.nextInt(1, 4)) {
                setColor(255, 255, 255)
                delay(Random.nextLong(50, 100))
                setColor(0, 0, 0)
                delay(Random.nextLong(50, 100))
            }
            
            // After-glow
            if (Random.nextDouble() > 0.5) {
                setColor(50, 0, 100)
                delay(200)
                setColor(0, 0, 0)
            }
        }
    }
    
    private suspend fun effectHeartbeat(r: Int, g: Int, b: Int) {
        while (coroutineContext.isActive) {
            // Beat 1 (Strong)
            for (i in 10..100 step 20) {
                val scale = i / 100f
                setColor((r * scale).toInt(), (g * scale).toInt(), (b * scale).toInt())
                delay(20)
            }
            for (i in 100 downTo 10 step 20) {
                val scale = i / 100f
                setColor((r * scale).toInt(), (g * scale).toInt(), (b * scale).toInt())
                delay(20)
            }
            
            delay(100)
            
            // Beat 2 (Weak)
            for (i in 10..60 step 20) {
                val scale = i / 100f
                setColor((r * scale).toInt(), (g * scale).toInt(), (b * scale).toInt())
                delay(20)
            }
            for (i in 60 downTo 0 step 10) {
                val scale = i / 100f
                setColor((r * scale).toInt(), (g * scale).toInt(), (b * scale).toInt())
                delay(20)
            }
            
            delay(800)
        }
    }
    
    private suspend fun effectFire() {
        while (coroutineContext.isActive) {
            val r = Random.nextInt(200, 256)
            val g = Random.nextInt(30, 101)
            val b = 0
            
            val brightness = Random.nextDouble(0.5, 1.0)
            setColor((r * brightness).toInt(), (g * brightness).toInt(), b)
            
            delay(Random.nextLong(50, 150))
        }
    }
    
    /**
     * Convert HSV to RGB
     */
    private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
        val h60 = h * 6
        val h60f = floor(h60)
        val hi = h60f.toInt() % 6
        val f = h60 - h60f
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)
        
        val (r, g, b) = when (hi) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        
        return Triple((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }
    
    fun cleanup() {
        disconnect()
        effectScope.cancel()
    }
}
