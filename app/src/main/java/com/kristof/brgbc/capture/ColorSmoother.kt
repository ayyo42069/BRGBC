package com.kristof.brgbc.capture

/**
 * Smooths color transitions using linear interpolation
 * Prevents jarring color changes on LED strip
 */
class ColorSmoother(
    private val attackFactor: Float = 0.7f, // Fast rise (for flashes/explosions)
    private val decayFactor: Float = 0.15f  // Slow fall (smooth fade out)
) {
    
    private var currentR = 0f
    private var currentG = 0f
    private var currentB = 0f
    
    /**
     * Apply smoothing to target color
     * @param targetR Target red value (0-255)
     * @param targetG Target green value (0-255)
     * @param targetB Target blue value (0-255)
     * @return Smoothed RGB as Triple(r, g, b)
     */
    fun smooth(targetR: Int, targetG: Int, targetB: Int): Triple<Int, Int, Int> {
        // Apply Fast Attack / Slow Decay for each channel independently
        
        currentR = if (targetR > currentR) {
            lerp(currentR, targetR.toFloat(), attackFactor)
        } else {
            lerp(currentR, targetR.toFloat(), decayFactor)
        }
        
        currentG = if (targetG > currentG) {
            lerp(currentG, targetG.toFloat(), attackFactor)
        } else {
            lerp(currentG, targetG.toFloat(), decayFactor)
        }
        
        currentB = if (targetB > currentB) {
            lerp(currentB, targetB.toFloat(), attackFactor)
        } else {
            lerp(currentB, targetB.toFloat(), decayFactor)
        }
        
        return Triple(currentR.toInt(), currentG.toInt(), currentB.toInt())
    }
    
    /**
     * Reset to specific color (useful when switching modes)
     */
    /**
     * Reset to specific color (useful when switching modes)
     */
    fun reset(r: Int, g: Int, b: Int) {
        currentR = r.toFloat()
        currentG = g.toFloat()
        currentB = b.toFloat()
    }
    
    /**
     * Get current smoothed color
     */
    fun getCurrentColor(): Triple<Int, Int, Int> {
        return Triple(currentR.toInt(), currentG.toInt(), currentB.toInt())
    }
    
    /**
     * Linear interpolation between current and target
     */
    private fun lerp(current: Float, target: Float, factor: Float): Float {
        return current + (target - current) * factor
    }
}
