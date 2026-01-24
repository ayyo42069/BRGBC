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
    
    // Effect crossfading state
    private var lastSentR = 0
    private var lastSentG = 0
    private var lastSentB = 0
    private var pendingCrossfade = false
    private var crossfadeStartR = 0
    private var crossfadeStartG = 0
    private var crossfadeStartB = 0
    
    // Crossfade parameters
    private val crossfadeDurationMs = 500L  // Transition time between effects
    private val crossfadeSteps = 20         // Number of steps in crossfade
    
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
     * Set LED color (with tracking for crossfade)
     */
    fun setColor(r: Int, g: Int, b: Int) {
        lastSentR = r
        lastSentG = g
        lastSentB = b
        val packet = LedProtocol.buildColorPacket(r, g, b, colorOrder)
        sendPacket(packet)
    }
    
    /**
     * Internal color set without tracking (for crossfade transitions)
     */
    private fun setColorInternal(r: Int, g: Int, b: Int) {
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
    
    // ============================================================
    // NEW HARDWARE COMMANDS
    // ============================================================
    
    /**
     * Set native effect (built-in hardware effect)
     */
    fun setNativeEffect(effect: NativeEffect) {
        stopEffect()
        val packet = LedProtocol.buildEffectPacket(effect.effectId)
        sendPacket(packet)
    }
    
    /**
     * Set effect speed (0-100)
     */
    fun setEffectSpeed(speed: Int) {
        val packet = LedProtocol.buildSpeedPacket(speed)
        sendPacket(packet)
    }
    
    /**
     * Set grayscale mode
     */
    fun setGrayscaleMode() {
        stopEffect()
        val packet = LedProtocol.buildGrayscaleModePacket()
        sendPacket(packet)
    }
    
    /**
     * Set temperature mode (0-10, 0=cold, 10=warm)
     */
    fun setTemperatureMode(temperature: Int) {
        stopEffect()
        val packet = LedProtocol.buildTemperatureModePacket(temperature)
        sendPacket(packet)
    }
    
    /**
     * Set dynamic/microphone mode
     */
    fun setDynamicMode(value: Int = 0) {
        stopEffect()
        val packet = LedProtocol.buildDynamicModePacket(value)
        sendPacket(packet)
    }
    
    /**
     * Set grayscale color (0-100, 0=black, 100=white)
     */
    fun setGrayscaleColor(level: Int) {
        val packet = LedProtocol.buildGrayscaleColorPacket(level)
        sendPacket(packet)
    }
    
    /**
     * Set temperature color (0-100, 0=cold, 100=warm)
     */
    fun setTemperatureColor(temperature: Int) {
        val packet = LedProtocol.buildTemperatureColorPacket(temperature)
        sendPacket(packet)
    }
    
    /**
     * Set dynamic mode sensitivity (0-100)
     */
    fun setDynamicSensitivity(sensitivity: Int) {
        val packet = LedProtocol.buildDynamicSensitivityPacket(sensitivity)
        sendPacket(packet)
    }
    
    /**
     * Set RGB pin order (1-6)
     */
    fun setRgbPinOrder(order: Int) {
        val packet = LedProtocol.buildRgbPinOrderPacket(order)
        sendPacket(packet)
    }
    
    /**
     * Set RGB pin order using RgbPinOrder enum
     */
    fun setRgbPinOrder(order: RgbPinOrder) {
        setRgbPinOrder(order.orderId)
    }

    
    /**
     * Start an LED effect
     */
    fun startEffect(effect: LedEffect, speed: Float = 0.1f) {
        stopEffect()
        
        effectJob = effectScope.launch {
            when (effect) {
                // Nature Effects
                LedEffect.RAINBOW -> effectRainbow(speed)
                LedEffect.AURORA -> effectAurora(speed)
                LedEffect.OCEAN -> effectOcean(speed)
                LedEffect.SUNSET -> effectSunset(speed)
                LedEffect.FIRE -> effectFire(speed)
                LedEffect.CANDLE -> effectCandle(speed)
                LedEffect.LIGHTNING -> effectLightning(speed)
                
                // Ambient Effects
                LedEffect.BREATHE -> effectBreathe(speed)
                LedEffect.HEARTBEAT -> effectHeartbeat(speed)
                LedEffect.ORBIT -> effectOrbit(speed)
                LedEffect.METEOR -> effectMeteor(speed)
                LedEffect.PLASMA -> effectPlasma(speed)
                LedEffect.NEON_PULSE -> effectNeonPulse(speed)
                LedEffect.COLOR_WAVE -> effectColorWave(speed)
                
                // Temperature Effects
                LedEffect.ICE -> effectIce(speed)
                LedEffect.LAVA -> effectLava(speed)
                
                // Party Effects
                LedEffect.POLICE -> effectPolice(speed)
                LedEffect.DISCO -> effectDisco(speed)
                LedEffect.STROBE -> effectStrobe(speed)
                
                // Relaxation
                LedEffect.RELAXATION -> effectRelaxation(speed)
            }
        }
    }
    
    // ============================================================
    // PREMIUM EFFECT IMPLEMENTATIONS
    // ============================================================
    
    /**
     * Rainbow - Smooth rainbow color cycling
     */
    private suspend fun effectRainbow(speed: Float) {
        var hue = 0f
        while (coroutineContext.isActive) {
            hue = (hue + 0.5f) % 360f
            val (r, g, b) = hsvToRgb(hue / 360f, 1f, 1f)
            setColor(r, g, b)
            delay((speed * 100).toLong().coerceAtLeast(10))
        }
    }
    
    /**
     * Aurora Borealis - Northern lights effect with greens, blues, and purples
     */
    private suspend fun effectAurora(speed: Float) {
        val colors = listOf(
            Triple(0, 255, 100),    // Green
            Triple(0, 200, 150),    // Teal
            Triple(50, 150, 255),   // Light Blue
            Triple(100, 100, 255),  // Purple Blue
            Triple(150, 50, 200),   // Purple
            Triple(50, 200, 100),   // Mint
        )
        var index = 0
        while (coroutineContext.isActive) {
            val current = colors[index]
            val next = colors[(index + 1) % colors.size]
            
            // Smooth transition between colors
            for (step in 0..50) {
                val ratio = step / 50f
                val r = lerp(current.first, next.first, ratio)
                val g = lerp(current.second, next.second, ratio)
                val b = lerp(current.third, next.third, ratio)
                
                // Add shimmer effect
                val shimmer = (sin(step * 0.3) * 20).toInt()
                setColor((r + shimmer).coerceIn(0, 255), g, b)
                delay((speed * 80).toLong().coerceAtLeast(10))
            }
            index = (index + 1) % colors.size
        }
    }
    
    /**
     * Ocean Wave - Calming blue-green wave effect
     */
    private suspend fun effectOcean(speed: Float) {
        var phase = 0.0
        while (coroutineContext.isActive) {
            phase += 0.05
            
            // Deep ocean to surface colors
            val wave = (sin(phase) + 1) / 2
            val r = (wave * 30).toInt()
            val g = (100 + wave * 100).toInt()
            val b = (150 + wave * 105).toInt()
            
            setColor(r, g, b)
            delay((speed * 100).toLong().coerceAtLeast(10))
        }
    }
    
    /**
     * Sunset - Warm orange, pink, and purple gradient
     */
    private suspend fun effectSunset(speed: Float) {
        val colors = listOf(
            Triple(255, 200, 50),   // Golden
            Triple(255, 150, 50),   // Orange
            Triple(255, 100, 80),   // Coral
            Triple(255, 80, 100),   // Pink
            Triple(200, 80, 150),   // Rose
            Triple(150, 80, 180),   // Purple
            Triple(200, 80, 150),   // Rose
            Triple(255, 80, 100),   // Pink
            Triple(255, 100, 80),   // Coral
            Triple(255, 150, 50),   // Orange
        )
        var index = 0
        while (coroutineContext.isActive) {
            val current = colors[index]
            val next = colors[(index + 1) % colors.size]
            
            for (step in 0..40) {
                val ratio = step / 40f
                val r = lerp(current.first, next.first, ratio)
                val g = lerp(current.second, next.second, ratio)
                val b = lerp(current.third, next.third, ratio)
                setColor(r, g, b)
                delay((speed * 100).toLong().coerceAtLeast(10))
            }
            index = (index + 1) % colors.size
        }
    }
    
    /**
     * Fire - Realistic flickering fire
     */
    private suspend fun effectFire(speed: Float) {
        while (coroutineContext.isActive) {
            val intensity = Random.nextDouble(0.6, 1.0)
            val flicker = Random.nextDouble(0.8, 1.0)
            
            val r = (255 * intensity * flicker).toInt()
            val g = (Random.nextInt(40, 120) * intensity).toInt()
            val b = (Random.nextInt(0, 20) * intensity).toInt()
            
            setColor(r, g, b)
            delay((speed * Random.nextLong(40, 120)).toLong().coerceAtLeast(10L))
        }
    }
    
    /**
     * Candle - Gentle flickering candle flame
     */
    private suspend fun effectCandle(speed: Float) {
        var baseIntensity = 0.9
        while (coroutineContext.isActive) {
            // Gentle random walk for natural movement
            baseIntensity += Random.nextDouble(-0.1, 0.1)
            baseIntensity = baseIntensity.coerceIn(0.7, 1.0)
            
            val flicker = Random.nextDouble(0.95, 1.0)
            val intensity = baseIntensity * flicker
            
            val r = (255 * intensity).toInt()
            val g = (150 * intensity).toInt()
            val b = (50 * intensity * 0.3).toInt()
            
            setColor(r, g, b)
            delay((speed * Random.nextLong(80, 150)).toLong().coerceAtLeast(20L))
        }
    }
    
    /**
     * Lightning - Dramatic lightning storm
     */
    private suspend fun effectLightning(speed: Float) {
        while (coroutineContext.isActive) {
            // Dark sky blue ambient
            setColor(10, 10, 30)
            delay(Random.nextLong((2000 * speed).toLong(), (5000 * speed).toLong()).coerceAtLeast(500))
            
            // Lightning strikes
            val strikes = Random.nextInt(1, 4)
            repeat(strikes) {
                // Bright flash
                setColor(255, 255, 255)
                delay(Random.nextLong(30, 80))
                
                // Brief dark
                setColor(100, 100, 150)
                delay(Random.nextLong(30, 60))
                
                // Secondary flash
                if (Random.nextDouble() > 0.5) {
                    setColor(200, 200, 255)
                    delay(Random.nextLong(20, 50))
                    setColor(50, 50, 80)
                    delay(Random.nextLong(40, 80))
                }
            }
            
            // Purple after-glow
            for (i in 80 downTo 10 step 10) {
                setColor(i / 2, i / 4, i)
                delay(50)
            }
        }
    }
    
    /**
     * Breathe - Smooth pulsing with color shift
     */
    private suspend fun effectBreathe(speed: Float) {
        var hue = 200f  // Start with calming blue
        while (coroutineContext.isActive) {
            // Breathe in
            for (i in 0..100 step 2) {
                val brightness = i / 100f
                val (r, g, b) = hsvToRgb(hue / 360f, 0.7f, brightness)
                setColor(r, g, b)
                delay((speed * 40).toLong().coerceAtLeast(5))
            }
            // Hold
            delay((speed * 300).toLong())
            
            // Breathe out
            for (i in 100 downTo 0 step 2) {
                val brightness = i / 100f
                val (r, g, b) = hsvToRgb(hue / 360f, 0.7f, brightness)
                setColor(r, g, b)
                delay((speed * 40).toLong().coerceAtLeast(5))
            }
            // Pause
            delay((speed * 500).toLong())
            
            // Shift hue slightly for variety
            hue = (hue + 15) % 360
        }
    }
    
    /**
     * Heartbeat - Realistic double-beat pattern
     */
    private suspend fun effectHeartbeat(speed: Float) {
        while (coroutineContext.isActive) {
            // First beat (lub) - strong
            for (i in 0..100 step 10) {
                val scale = i / 100f
                setColor((255 * scale).toInt(), (20 * scale).toInt(), (40 * scale).toInt())
                delay((speed * 15).toLong().coerceAtLeast(5))
            }
            for (i in 100 downTo 30 step 10) {
                val scale = i / 100f
                setColor((255 * scale).toInt(), (20 * scale).toInt(), (40 * scale).toInt())
                delay((speed * 15).toLong().coerceAtLeast(5))
            }
            
            delay((speed * 100).toLong())
            
            // Second beat (dub) - softer
            for (i in 30..70 step 10) {
                val scale = i / 100f
                setColor((255 * scale).toInt(), (20 * scale).toInt(), (40 * scale).toInt())
                delay((speed * 15).toLong().coerceAtLeast(5))
            }
            for (i in 70 downTo 5 step 10) {
                val scale = i / 100f
                setColor((255 * scale).toInt(), (20 * scale).toInt(), (40 * scale).toInt())
                delay((speed * 15).toLong().coerceAtLeast(5))
            }
            
            // Rest period
            setColor(10, 0, 5)
            delay((speed * 700).toLong())
        }
    }
    
    /**
     * Orbit - Color cycling with pulsing brightness
     */
    private suspend fun effectOrbit(speed: Float) {
        var angle = 0f
        while (coroutineContext.isActive) {
            angle = (angle + 2f) % 360f
            
            val (r, g, b) = hsvToRgb(angle / 360f, 1f, 1f)
            
            // Sine wave brightness modulation
            val brightness = (sin(Math.toRadians(angle.toDouble() * 2)) + 1) / 2 * 0.7 + 0.3
            
            setColor(
                (r * brightness).toInt(),
                (g * brightness).toInt(),
                (b * brightness).toInt()
            )
            
            delay((speed * 50).toLong().coerceAtLeast(10))
        }
    }
    
    /**
     * Meteor - Shooting star effect
     */
    private suspend fun effectMeteor(speed: Float) {
        while (coroutineContext.isActive) {
            // Build up - star approaching
            for (i in 0..100 step 5) {
                val brightness = i / 100f
                setColor(
                    (255 * brightness).toInt(),
                    (200 * brightness).toInt(),
                    (150 * brightness).toInt()
                )
                delay((speed * 30).toLong().coerceAtLeast(5))
            }
            
            // Streak - bright flash
            setColor(255, 255, 255)
            delay((speed * 50).toLong())
            
            // Tail fade with color shift to orange/red
            for (i in 100 downTo 0 step 3) {
                val brightness = i / 100f
                val hue = 30 + (100 - i) * 0.3f  // Shift from white to orange to red
                val (r, g, b) = hsvToRgb(hue / 360f, 1f - brightness * 0.5f, brightness)
                setColor(r, g, b)
                delay((speed * 20).toLong().coerceAtLeast(5))
            }
            
            // Darkness before next meteor
            setColor(0, 0, 0)
            delay((speed * Random.nextLong(500, 2000)).toLong().coerceAtLeast(200L))
        }
    }
    
    /**
     * Plasma - Sci-fi energy effect
     */
    private suspend fun effectPlasma(speed: Float) {
        var phase = 0.0
        while (coroutineContext.isActive) {
            phase += 0.1
            
            // Create plasma-like color mixing
            val plasma1 = sin(phase)
            val plasma2 = sin(phase * 1.5 + 1)
            val plasma3 = cos(phase * 0.7)
            
            val r = ((plasma1 + 1) / 2 * 150 + 50).toInt()
            val g = ((plasma2 + 1) / 2 * 80).toInt()
            val b = ((plasma3 + 1) / 2 * 150 + 100).toInt()
            
            setColor(r, g, b)
            delay((speed * 50).toLong().coerceAtLeast(10))
        }
    }
    
    /**
     * Neon Pulse - Cyberpunk-style neon glow
     */
    private suspend fun effectNeonPulse(speed: Float) {
        val neonColors = listOf(
            Triple(255, 0, 255),   // Magenta
            Triple(0, 255, 255),   // Cyan
            Triple(255, 0, 100),   // Hot Pink
            Triple(100, 0, 255),   // Purple
        )
        var colorIndex = 0
        
        while (coroutineContext.isActive) {
            val color = neonColors[colorIndex]
            
            // Quick pulse up
            for (i in 30..100 step 5) {
                val brightness = i / 100f
                setColor(
                    (color.first * brightness).toInt(),
                    (color.second * brightness).toInt(),
                    (color.third * brightness).toInt()
                )
                delay((speed * 20).toLong().coerceAtLeast(5))
            }
            
            // Hold at peak
            delay((speed * 200).toLong())
            
            // Slow fade
            for (i in 100 downTo 30 step 3) {
                val brightness = i / 100f
                setColor(
                    (color.first * brightness).toInt(),
                    (color.second * brightness).toInt(),
                    (color.third * brightness).toInt()
                )
                delay((speed * 25).toLong().coerceAtLeast(5))
            }
            
            colorIndex = (colorIndex + 1) % neonColors.size
        }
    }
    
    /**
     * Color Wave - Smooth color transitions
     */
    private suspend fun effectColorWave(speed: Float) {
        var hue = 0f
        while (coroutineContext.isActive) {
            hue = (hue + 0.3f) % 360f
            
            val saturation = 0.8f + sin(hue * 0.1f) * 0.2f
            val value = 0.9f + sin(hue * 0.05f) * 0.1f
            
            val (r, g, b) = hsvToRgb(hue / 360f, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
            setColor(r, g, b)
            delay((speed * 60).toLong().coerceAtLeast(10))
        }
    }
    
    /**
     * Ice - Cold blue shimmer
     */
    private suspend fun effectIce(speed: Float) {
        while (coroutineContext.isActive) {
            val shimmer = Random.nextDouble(0.8, 1.0)
            val sparkle = if (Random.nextDouble() > 0.95) 1.3 else 1.0
            
            val r = (180 * shimmer * sparkle).toInt().coerceAtMost(255)
            val g = (220 * shimmer * sparkle).toInt().coerceAtMost(255)
            val b = (255 * shimmer).toInt()
            
            setColor(r, g, b)
            delay((speed * Random.nextLong(50, 150)).toLong().coerceAtLeast(10L))
        }
    }
    
    /**
     * Lava - Molten rock effect
     */
    private suspend fun effectLava(speed: Float) {
        var phase = 0.0
        while (coroutineContext.isActive) {
            phase += 0.08
            
            val flow = (sin(phase) + 1) / 2
            val bubble = if (Random.nextDouble() > 0.9) Random.nextDouble(0.2, 0.5) else 0.0
            
            val r = (200 + (55 * flow) + (bubble * 55)).toInt().coerceAtMost(255)
            val g = (50 + (80 * flow) + (bubble * 30)).toInt().coerceAtMost(150)
            val b = (0 + (bubble * 20)).toInt()
            
            setColor(r, g, b)
            delay((speed * 80).toLong().coerceAtLeast(10))
        }
    }
    
    /**
     * Police - Classic police lights
     */
    private suspend fun effectPolice(speed: Float) {
        while (coroutineContext.isActive) {
            // Red flashes
            repeat(3) {
                setColor(255, 0, 0)
                delay((speed * 80).toLong().coerceAtLeast(20))
                setColor(50, 0, 0)
                delay((speed * 60).toLong().coerceAtLeast(20))
            }
            
            // Brief pause
            setColor(0, 0, 0)
            delay((speed * 50).toLong())
            
            // Blue flashes
            repeat(3) {
                setColor(0, 0, 255)
                delay((speed * 80).toLong().coerceAtLeast(20))
                setColor(0, 0, 50)
                delay((speed * 60).toLong().coerceAtLeast(20))
            }
            
            // Brief pause
            setColor(0, 0, 0)
            delay((speed * 50).toLong())
        }
    }
    
    /**
     * Disco - Random party colors
     */
    private suspend fun effectDisco(speed: Float) {
        val discoColors = listOf(
            Triple(255, 0, 0),     // Red
            Triple(0, 255, 0),     // Green
            Triple(0, 0, 255),     // Blue
            Triple(255, 255, 0),   // Yellow
            Triple(255, 0, 255),   // Magenta
            Triple(0, 255, 255),   // Cyan
            Triple(255, 128, 0),   // Orange
            Triple(128, 0, 255),   // Purple
        )
        
        while (coroutineContext.isActive) {
            val color = discoColors[Random.nextInt(discoColors.size)]
            setColor(color.first, color.second, color.third)
            delay((speed * Random.nextLong(100, 300)).toLong().coerceAtLeast(50L))
        }
    }
    
    /**
     * Strobe - Classic strobe effect
     */
    private suspend fun effectStrobe(speed: Float) {
        while (coroutineContext.isActive) {
            setColor(255, 255, 255)
            delay((speed * 30).toLong().coerceAtLeast(10))
            setColor(0, 0, 0)
            delay((speed * 70).toLong().coerceAtLeast(20))
        }
    }
    
    /**
     * Relaxation - Calm, slow color breathing
     */
    private suspend fun effectRelaxation(speed: Float) {
        val relaxColors = listOf(
            Triple(100, 150, 200),  // Calm blue
            Triple(100, 180, 150),  // Sage green
            Triple(150, 130, 180),  // Lavender
            Triple(180, 150, 130),  // Warm beige
        )
        var index = 0
        
        while (coroutineContext.isActive) {
            val current = relaxColors[index]
            val next = relaxColors[(index + 1) % relaxColors.size]
            
            // Very slow, smooth transition
            for (step in 0..100) {
                val ratio = step / 100f
                val r = lerp(current.first, next.first, ratio)
                val g = lerp(current.second, next.second, ratio)
                val b = lerp(current.third, next.third, ratio)
                
                // Add subtle breathing
                val breath = (sin(step * 0.1) * 0.1 + 0.9)
                setColor(
                    (r * breath).toInt(),
                    (g * breath).toInt(),
                    (b * breath).toInt()
                )
                delay((speed * 100).toLong().coerceAtLeast(30))
            }
            
            index = (index + 1) % relaxColors.size
        }
    }
    
    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================
    
    /**
     * Linear interpolation between two values
     */
    private fun lerp(start: Int, end: Int, ratio: Float): Int {
        return (start + (end - start) * ratio).toInt()
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
