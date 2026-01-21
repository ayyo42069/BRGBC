package com.kristof.brgbc.ble

/**
 * ELK-BLEDOB/ELK-BLEDOM LED Strip Protocol
 * 
 * All commands follow the format: 7E XX XX XX XX XX XX XX EF
 * Where 7E is start byte and EF is end byte
 * Write to characteristic: 0000fff0-0000-1000-8000-00805f9b34fb
 */
object LedProtocol {
    
    private const val START_BYTE: Byte = 0x7E.toByte()
    private const val END_BYTE: Byte = 0xEF.toByte()
    
    /**
     * Set mode to RGB before setting colors
     * 7e 00 05 03 r g b 00 ef
     */
    fun buildColorPacket(r: Int, g: Int, b: Int, order: String = "rgb"): ByteArray {
        // Clamp values
        val rClamped = r.coerceIn(0, 255).toByte()
        val gClamped = g.coerceIn(0, 255).toByte()
        val bClamped = b.coerceIn(0, 255).toByte()
        
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x05,
            0x03,        // RGB mode
            rClamped,
            gClamped,
            bClamped,
            0x00,
            END_BYTE
        )
    }
    
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
     * Set mode to RGB (needed before setting RGB colors)
     * This is implicit when using buildColorPacket
     */
    fun buildSetRGBModePacket(): ByteArray {
        return byteArrayOf(
            START_BYTE,
            0x00,
            0x05,
            0x03,
            0xFF.toByte(), // Default to white
            0xFF.toByte(),
            0xFF.toByte(),
            0x00,
            END_BYTE
        )
    }
    
    // Native Effect IDs
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
}
