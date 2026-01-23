package com.kristof.brgbc.ble

/**
 * Enum representing the native hardware effects available on the LED controller
 * These are built-in effects that run on the controller itself
 */
enum class NativeEffect(val displayName: String, val effectId: Byte, val category: Category) {
    // Single colors
    RED("Red", 0x80.toByte(), Category.SOLID),
    GREEN("Green", 0x81.toByte(), Category.SOLID),
    BLUE("Blue", 0x82.toByte(), Category.SOLID),
    YELLOW("Yellow", 0x83.toByte(), Category.SOLID),
    CYAN("Cyan", 0x84.toByte(), Category.SOLID),
    MAGENTA("Magenta", 0x85.toByte(), Category.SOLID),
    WHITE("White", 0x86.toByte(), Category.SOLID),
    
    // Jump effects
    JUMP_RGB("Jump RGB", 0x87.toByte(), Category.JUMP),
    JUMP_RGBYCMW("Jump All Colors", 0x88.toByte(), Category.JUMP),
    
    // Gradient effects
    GRADIENT_RGB("Gradient RGB", 0x89.toByte(), Category.GRADIENT),
    GRADIENT_RGBYCMW("Gradient All", 0x8A.toByte(), Category.GRADIENT),
    GRADIENT_R("Gradient Red", 0x8B.toByte(), Category.GRADIENT),
    GRADIENT_G("Gradient Green", 0x8C.toByte(), Category.GRADIENT),
    GRADIENT_B("Gradient Blue", 0x8D.toByte(), Category.GRADIENT),
    GRADIENT_Y("Gradient Yellow", 0x8E.toByte(), Category.GRADIENT),
    GRADIENT_C("Gradient Cyan", 0x8F.toByte(), Category.GRADIENT),
    GRADIENT_M("Gradient Magenta", 0x90.toByte(), Category.GRADIENT),
    GRADIENT_W("Gradient White", 0x91.toByte(), Category.GRADIENT),
    GRADIENT_RG("Gradient R→G", 0x92.toByte(), Category.GRADIENT),
    GRADIENT_RB("Gradient R→B", 0x93.toByte(), Category.GRADIENT),
    GRADIENT_GB("Gradient G→B", 0x94.toByte(), Category.GRADIENT),
    
    // Blink effects
    BLINK_RGBYCMW("Blink All", 0x95.toByte(), Category.BLINK),
    BLINK_R("Blink Red", 0x96.toByte(), Category.BLINK),
    BLINK_G("Blink Green", 0x97.toByte(), Category.BLINK),
    BLINK_B("Blink Blue", 0x98.toByte(), Category.BLINK),
    BLINK_Y("Blink Yellow", 0x99.toByte(), Category.BLINK),
    BLINK_C("Blink Cyan", 0x9A.toByte(), Category.BLINK),
    BLINK_M("Blink Magenta", 0x9B.toByte(), Category.BLINK),
    BLINK_W("Blink White", 0x9C.toByte(), Category.BLINK);
    
    enum class Category(val displayName: String) {
        SOLID("Solid Colors"),
        JUMP("Jump Effects"),
        GRADIENT("Gradient Effects"),
        BLINK("Blink Effects")
    }
    
    companion object {
        fun getByCategory(category: Category): List<NativeEffect> {
            return entries.filter { it.category == category }
        }
    }
}

/**
 * Enum representing LED strip modes
 */
enum class LedMode(val displayName: String, val description: String) {
    RGB("RGB Color", "Set custom RGB colors"),
    GRAYSCALE("Grayscale", "Black to white"),
    TEMPERATURE("Temperature", "Cold to warm white"),
    EFFECT("Effects", "Built-in patterns"),
    DYNAMIC("Dynamic", "Microphone reactive")
}

/**
 * Enum representing RGB pin order configurations
 */
enum class RgbPinOrder(val displayName: String, val orderId: Int) {
    RGB("RGB", 1),
    RBG("RBG", 2),
    GRB("GRB", 3),
    GBR("GBR", 4),
    BRG("BRG", 5),
    BGR("BGR", 6)
}
