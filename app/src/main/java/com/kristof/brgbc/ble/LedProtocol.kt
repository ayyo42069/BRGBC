package com.kristof.brgbc.ble

/**
 * ELK-BLEDOB/ELK-BLEDOM LED Strip Protocol
 * 
 * All commands follow the format: 7E XX XX XX XX XX XX XX EF
 * Where 7E is start byte and EF is end byte
 * Write to characteristic: 0000fff3-0000-1000-8000-00805f9b34fb
 * 
 * Controller has 2 states (use set_power command):
 * - on: LEDs show current mode colors
 * - off: LEDs are off (current mode is saved)
 * 
 * Controller has 5 modes:
 * - mode_grayscale: can set grayscale from black to full white
 * - mode_temperature: can set brightness and temperature (cold to warm white)
 * - mode_effect: auto color change, can set patterns, brightness and speed
 * - mode_dynamic: microphone reactive (may require on-board mic)
 * - mode_rgb: can set rgb values and brightness
 */
object LedProtocol {
    
    private const val START_BYTE: Byte = 0x7E.toByte()
    private const val END_BYTE: Byte = 0xEF.toByte()
    
    // ============================================================
    // POWER CONTROL
    // ============================================================
    
    /**
     * Turn LED on
     * 7e 00 04 01 00 00 00 00 ef
     */
    fun buildPowerOnPacket(): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x04,
            0x01,
            0x00,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Turn LED off
     * 7e 00 04 00 00 00 00 00 ef
     */
    fun buildPowerOffPacket(): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x04,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    // ============================================================
    // BRIGHTNESS & SPEED CONTROL
    // ============================================================
    
    /**
     * Set brightness (0-100)
     * 7e 00 01 brightness 00 00 00 00 ef
     * Note: Does not work in mode_effect (jump, gradient, blink), mode_grayscale, or mode_dynamic
     */
    fun buildBrightnessPacket(level: Int): ByteArray {
        val clampedLevel = level.coerceIn(0, 100).toByte()
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x01,
            clampedLevel,
            0x00,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set effect speed (0-100)
     * 7e 00 02 speed 00 00 00 00 ef
     */
    fun buildSpeedPacket(speed: Int): ByteArray {
        val clampedSpeed = speed.coerceIn(0, 100).toByte()
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x02,
            clampedSpeed,
            0x00,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    // ============================================================
    // MODE COMMANDS
    // ============================================================
    
    /**
     * Set mode to grayscale
     * 7e 00 03 00 01 00 00 00 ef
     * Will show last grayscale color
     */
    fun buildGrayscaleModePacket(): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x03,
            0x00,
            0x01,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set mode to temperature (cold to warm white)
     * 7e 00 03 temperature 02 00 00 00 ef
     * temperature: 128-138 (0x80-0x8a)
     *   0x80 = cold white
     *   0x85 = natural white
     *   0x8a = warm white
     */
    fun buildTemperatureModePacket(temperature: Int): ByteArray {
        val clampedTemp = temperature.coerceIn(0, 10)
        val tempByte = (0x80 + clampedTemp).toByte()
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x03,
            tempByte,
            0x02,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set mode to effect
     * 7e 00 03 effect 03 00 00 00 ef
     */
    fun buildEffectPacket(effectId: Byte): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x03,
            effectId,
            0x03,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set mode to dynamic (microphone reactive)
     * 7e 00 03 val 04 00 00 00 00 ef
     * Note: May not work without on-board microphone
     */
    fun buildDynamicModePacket(value: Int = 0): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x03,
            value.coerceIn(0, 255).toByte(),
            0x04,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    // ============================================================
    // COLOR COMMANDS FOR EACH MODE
    // ============================================================
    
    /**
     * Set color for grayscale mode (0-100)
     * 7e 00 05 01 grayscale 00 00 00 ef
     * grayscale: 0-100 (0x00 = black, 0x64 = white)
     */
    fun buildGrayscaleColorPacket(grayscale: Int): ByteArray {
        val clampedValue = grayscale.coerceIn(0, 100).toByte()
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x05,
            0x01,
            clampedValue,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set color for temperature mode (0-100)
     * 7e 00 05 02 temperature 00 00 00 ef
     * temperature: 0-100 (0x00 = cold, 0x64 = warm)
     */
    fun buildTemperatureColorPacket(temperature: Int): ByteArray {
        val clampedValue = temperature.coerceIn(0, 100).toByte()
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x05,
            0x02,
            clampedValue,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set color for RGB mode
     * 7e 00 05 03 r g b 00 ef
     */
    fun buildColorPacket(r: Int, g: Int, b: Int, order: String = "rgb"): ByteArray {
        val rClamped = r.coerceIn(0, 255).toByte()
        val gClamped = g.coerceIn(0, 255).toByte()
        val bClamped = b.coerceIn(0, 255).toByte()
        
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x05,
            0x03,
            rClamped,
            gClamped,
            bClamped,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set mode to RGB with default white color
     */
    fun buildSetRGBModePacket(): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x05,
            0x03,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0x00,
            END_BYTE
        )
    }
    
    // ============================================================
    // DYNAMIC MODE SETTINGS
    // ============================================================
    
    /**
     * Set value for dynamic mode
     * 7e 00 06 val 00 00 00 00 ef
     * Note: May not work on all controllers
     */
    fun buildDynamicValuePacket(value: Int): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x06,
            value.coerceIn(0, 255).toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set sensitivity for dynamic mode (0-100)
     * 7e 00 07 sensitivity 00 00 00 00 ef
     * Note: May not work on all controllers
     */
    fun buildDynamicSensitivityPacket(sensitivity: Int): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x07,
            sensitivity.coerceIn(0, 100).toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    // ============================================================
    // RGB PIN ORDER CONFIGURATION
    // ============================================================
    
    /**
     * Set RGB pin order using index (1-6)
     * 7e 00 08 rgb_order 00 00 00 00 ef
     * 1=RGB, 2=RBG, 3=GRB, 4=GBR, 5=BRG, 6=BGR
     */
    fun buildRgbPinOrderPacket(order: Int): ByteArray {
        val clampedOrder = order.coerceIn(1, 6).toByte()
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x08,
            clampedOrder,
            0x00,
            0x00,
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    /**
     * Set RGB pin order using custom channel mapping
     * 7e 00 81 c1 c2 c3 00 00 ef
     * c1, c2, c3: values 1-3, each used once
     * Example: 1,2,3 = RGB, 3,2,1 = BGR
     */
    fun buildCustomRgbPinOrderPacket(c1: Int, c2: Int, c3: Int): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x81.toByte(),
            c1.coerceIn(1, 3).toByte(),
            c2.coerceIn(1, 3).toByte(),
            c3.coerceIn(1, 3).toByte(),
            0x00,
            0x00,
            END_BYTE
        )
    }
    
    // ============================================================
    // NATIVE EFFECT IDS
    // ============================================================
    
    object Effects {
        // Single colors
        const val RED: Byte = 0x80.toByte()
        const val GREEN: Byte = 0x81.toByte()
        const val BLUE: Byte = 0x82.toByte()
        const val YELLOW: Byte = 0x83.toByte()
        const val CYAN: Byte = 0x84.toByte()
        const val MAGENTA: Byte = 0x85.toByte()
        const val WHITE: Byte = 0x86.toByte()
        
        // Jump effects
        const val JUMP_RGB: Byte = 0x87.toByte()
        const val JUMP_RGBYCMW: Byte = 0x88.toByte()
        
        // Gradient effects
        const val GRADIENT_RGB: Byte = 0x89.toByte()
        const val GRADIENT_RGBYCMW: Byte = 0x8A.toByte()
        const val GRADIENT_R: Byte = 0x8B.toByte()
        const val GRADIENT_G: Byte = 0x8C.toByte()
        const val GRADIENT_B: Byte = 0x8D.toByte()
        const val GRADIENT_Y: Byte = 0x8E.toByte()
        const val GRADIENT_C: Byte = 0x8F.toByte()
        const val GRADIENT_M: Byte = 0x90.toByte()
        const val GRADIENT_W: Byte = 0x91.toByte()
        const val GRADIENT_RG: Byte = 0x92.toByte()
        const val GRADIENT_RB: Byte = 0x93.toByte()
        const val GRADIENT_GB: Byte = 0x94.toByte()
        
        // Blink effects
        const val BLINK_RGBYCMW: Byte = 0x95.toByte()
        const val BLINK_R: Byte = 0x96.toByte()
        const val BLINK_G: Byte = 0x97.toByte()
        const val BLINK_B: Byte = 0x98.toByte()
        const val BLINK_Y: Byte = 0x99.toByte()
        const val BLINK_C: Byte = 0x9A.toByte()
        const val BLINK_M: Byte = 0x9B.toByte()
        const val BLINK_W: Byte = 0x9C.toByte()
    }
    
    // ============================================================
    // TEMPERATURE PRESETS
    // ============================================================
    
    object Temperature {
        const val COLD_WHITE = 0
        const val NATURAL_WHITE = 5
        const val WARM_WHITE = 10
    }
    
    // ============================================================
    // RGB PIN ORDER PRESETS
    // ============================================================
    
    object RgbOrder {
        const val RGB = 1
        const val RBG = 2
        const val GRB = 3
        const val GBR = 4
        const val BRG = 5
        const val BGR = 6
    }
}
