package com.kristof.brgbc.capture

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.min

/**
 * Advanced color smoother for LED transitions.
 * 
 * Uses HSV-based interpolation for more natural color transitions,
 * with separate control for hue, saturation, and brightness.
 * Includes momentum-based smoothing for fluid, organic animations.
 */
class ColorSmoother(
    // MUCH slower for smooth transitions on rapidly changing content
    private val colorAttack: Float = 0.15f,     // Slow hue transition (was 0.6)
    private val colorDecay: Float = 0.08f,      // Even slower decay
    private val brightnessAttack: Float = 0.5f, // Medium brightness response
    private val brightnessDecay: Float = 0.12f, // Slow brightness fade
    private val momentumFactor: Float = 0.5f    // Higher momentum = smoother motion
) {
    // Current HSV state
    private var currentH = 0f
    private var currentS = 0f
    private var currentV = 0f
    
    // Momentum (velocity) for fluid motion
    private var velocityH = 0f
    private var velocityS = 0f
    private var velocityV = 0f
    
    // Previous values for momentum calculation
    private var prevH = 0f
    private var prevS = 0f
    private var prevV = 0f
    
    // Minimum brightness threshold (avoid flicker near black)
    private val minBrightness = 0.02f
    
    /**
     * Apply smooth transition to target color.
     * Uses HSV interpolation for perceptually natural transitions.
     */
    fun smooth(targetR: Int, targetG: Int, targetB: Int): Triple<Int, Int, Int> {
        // Convert target to HSV
        val targetHSV = FloatArray(3)
        Color.RGBToHSV(targetR, targetG, targetB, targetHSV)
        
        val targetH = targetHSV[0]
        val targetS = targetHSV[1]
        val targetV = targetHSV[2]
        
        // Calculate hue delta (handle wraparound at 360°)
        var hueDelta = targetH - currentH
        if (hueDelta > 180f) hueDelta -= 360f
        if (hueDelta < -180f) hueDelta += 360f
        
        // Determine if we're "attacking" or "decaying" based on overall brightness
        val isAttacking = targetV > currentV
        
        // Apply momentum from previous frame
        velocityH = velocityH * momentumFactor + hueDelta * (1f - momentumFactor)
        velocityS = velocityS * momentumFactor + (targetS - currentS) * (1f - momentumFactor)
        velocityV = velocityV * momentumFactor + (targetV - currentV) * (1f - momentumFactor)
        
        // Hue: Smooth transition with wraparound handling
        val hueFactor = if (isAttacking) colorAttack else colorDecay
        currentH = (currentH + velocityH * hueFactor) % 360f
        if (currentH < 0f) currentH += 360f
        
        // Saturation: Follow target smoothly
        val satFactor = if (targetS > currentS) colorAttack else colorDecay
        currentS = lerp(currentS, targetS, satFactor)
        
        // Brightness: Fast attack, slow decay for punchy response to flashes
        val valFactor = if (isAttacking) brightnessAttack else brightnessDecay
        currentV = lerp(currentV, targetV, valFactor)
        
        // Apply minimum brightness threshold
        if (currentV < minBrightness && targetV < minBrightness) {
            currentV = 0f
        }
        
        // Store for next frame's momentum calculation
        prevH = currentH
        prevS = currentS
        prevV = currentV
        
        // Convert back to RGB
        val resultColor = Color.HSVToColor(floatArrayOf(currentH, currentS.coerceIn(0f, 1f), currentV.coerceIn(0f, 1f)))
        
        return Triple(
            Color.red(resultColor),
            Color.green(resultColor),
            Color.blue(resultColor)
        )
    }
    
    /**
     * Smooth with explicit control over responsiveness.
     * @param responsiveness 0.0 = max smoothing, 1.0 = instant/raw
     */
    fun smoothWithResponsiveness(targetR: Int, targetG: Int, targetB: Int, responsiveness: Float): Triple<Int, Int, Int> {
        val factor = responsiveness.coerceIn(0f, 1f)
        
        // Convert to HSV
        val targetHSV = FloatArray(3)
        Color.RGBToHSV(targetR, targetG, targetB, targetHSV)
        
        // Simple lerp with custom factor
        currentH = lerpHue(currentH, targetHSV[0], factor)
        currentS = lerp(currentS, targetHSV[1], factor)
        currentV = lerp(currentV, targetHSV[2], factor)
        
        val resultColor = Color.HSVToColor(floatArrayOf(currentH, currentS.coerceIn(0f, 1f), currentV.coerceIn(0f, 1f)))
        
        return Triple(
            Color.red(resultColor),
            Color.green(resultColor),
            Color.blue(resultColor)
        )
    }
    
    /**
     * Reset smoother to specific color (useful when switching modes or connecting)
     */
    fun reset(r: Int, g: Int, b: Int) {
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        currentH = hsv[0]
        currentS = hsv[1]
        currentV = hsv[2]
        prevH = currentH
        prevS = currentS
        prevV = currentV
        velocityH = 0f
        velocityS = 0f
        velocityV = 0f
    }
    
    /**
     * Get current smoothed color
     */
    fun getCurrentColor(): Triple<Int, Int, Int> {
        val resultColor = Color.HSVToColor(floatArrayOf(currentH, currentS.coerceIn(0f, 1f), currentV.coerceIn(0f, 1f)))
        return Triple(
            Color.red(resultColor),
            Color.green(resultColor),
            Color.blue(resultColor)
        )
    }
    
    /**
     * Get current color as HSV (useful for debugging or effects)
     */
    fun getCurrentHSV(): Triple<Float, Float, Float> {
        return Triple(currentH, currentS, currentV)
    }
    
    // ============================================
    // Helper functions
    // ============================================
    
    private fun lerp(current: Float, target: Float, factor: Float): Float {
        return current + (target - current) * factor
    }
    
    /**
     * Lerp for hue with wraparound handling (0-360 degrees)
     */
    private fun lerpHue(current: Float, target: Float, factor: Float): Float {
        var delta = target - current
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        
        var result = current + delta * factor
        if (result < 0f) result += 360f
        if (result >= 360f) result -= 360f
        return result
    }
}
