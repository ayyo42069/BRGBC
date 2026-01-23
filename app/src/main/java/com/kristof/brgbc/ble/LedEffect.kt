package com.kristof.brgbc.ble

/**
 * Enum representing different LED effects - Premium collection
 * Organized by category for easy browsing
 */
enum class LedEffect(val displayName: String, val category: Category, val icon: String) {
    // Nature Effects
    RAINBOW("Rainbow", Category.NATURE, "🌈"),
    AURORA("Aurora", Category.NATURE, "🌌"),
    OCEAN("Ocean Wave", Category.NATURE, "🌊"),
    SUNSET("Sunset", Category.NATURE, "🌅"),
    FIRE("Fire", Category.NATURE, "🔥"),
    CANDLE("Candle", Category.NATURE, "🕯️"),
    LIGHTNING("Lightning", Category.NATURE, "⚡"),
    
    // Ambient Effects
    BREATHE("Breathe", Category.AMBIENT, "💨"),
    HEARTBEAT("Heartbeat", Category.AMBIENT, "❤️"),
    ORBIT("Orbit", Category.AMBIENT, "🪐"),
    METEOR("Meteor", Category.AMBIENT, "☄️"),
    PLASMA("Plasma", Category.AMBIENT, "🟣"),
    NEON_PULSE("Neon Pulse", Category.AMBIENT, "💜"),
    COLOR_WAVE("Color Wave", Category.AMBIENT, "🎨"),
    
    // Temperature Effects
    ICE("Ice", Category.TEMPERATURE, "❄️"),
    LAVA("Lava", Category.TEMPERATURE, "🌋"),
    
    // Party Effects  
    POLICE("Police", Category.PARTY, "🚔"),
    DISCO("Disco", Category.PARTY, "🪩"),
    STROBE("Strobe", Category.PARTY, "⚪"),
    
    // Relaxation
    RELAXATION("Relaxation", Category.RELAXATION, "🧘");
    
    enum class Category(val displayName: String) {
        NATURE("Nature"),
        AMBIENT("Ambient"),
        TEMPERATURE("Temperature"),
        PARTY("Party"),
        RELAXATION("Relaxation")
    }
    
    companion object {
        fun getByCategory(category: Category): List<LedEffect> {
            return entries.filter { it.category == category }
        }
    }
}

