package com.kristof.brgbc.capture

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sign

/**
 * Advanced color smoother for LED transitions.
 * 
 * Uses HSV-based interpolation with rate-limited transitions to prevent
 * flashes during rapid color changes. Implements a "target smoothing" 
 * approach where even the target itself is smoothed before transitioning,
 * preventing the LEDs from chasing rapidly changing screen content.
 */
class ColorSmoother(
    @Suppress("UNUSED_PARAMETER") unusedFactor: Float = 0f // Kept for API compatibility
) {
    // ============================================
    // Tunable Parameters
    // ============================================
    
    // Target smoothing - smooth the target color BEFORE transitioning to it
    // This prevents chasing rapidly changing colors (e.g., explosions, quick cuts)
    private val targetSmoothingFactor = 0.12f  // How fast target updates (lower = more stable)
    
    // Transition speed limits (max degrees/units per frame at 30fps)
    private val maxHueStepPerFrame = 8f      // Max hue change per frame (360° scale)
    private val maxSatStepPerFrame = 0.06f   // Max saturation change per frame (0-1 scale)
    private val maxValStepPerFrame = 0.08f   // Max brightness change per frame (0-1 scale)
    
    // Base transition speeds (applied after rate limiting)
    private val hueTransitionSpeed = 0.08f   // Base lerp factor for hue
    private val satTransitionSpeed = 0.10f   // Base lerp factor for saturation
    private val valTransitionSpeed = 0.12f   // Base lerp factor for brightness
    
    // Large change detection - slow down even more for big jumps
    private val largeHueChangeThreshold = 60f    // Degrees - considered a "large" hue jump
    private val largeChangeSlowdown = 0.4f       // Multiply speed by this for large changes
    
    // Minimum brightness threshold (avoid flicker near black)
    private val minBrightness = 0.02f
    
    // ============================================
    // State Variables
    // ============================================
    
    // Smoothed target - what we're transitioning TOWARDS (itself smoothed)
    private var targetH = 0f
    private var targetS = 0f
    private var targetV = 0f
    
    // Current display state - what the LEDs are actually showing
    private var currentH = 0f
    private var currentS = 0f
    private var currentV = 0f
    
    // Track previous raw input for change detection
    private var prevRawH = 0f
    private var prevRawS = 0f
    private var prevRawV = 0f
    
    // Rapid change detection
    private var rapidChangeCounter = 0
    private val rapidChangeThreshold = 3  // Frames of rapid change to trigger slowdown
    
    /**
     * Apply smooth transition to target color.
     * Uses a two-stage smoothing approach:
     * 1. Smooth the incoming target to prevent chasing rapid changes
     * 2. Rate-limit the transition from current to smoothed target
     */
    fun smooth(targetR: Int, targetG: Int, targetB: Int): Triple<Int, Int, Int> {
        // Convert raw input to HSV
        val rawHSV = FloatArray(3)
        Color.RGBToHSV(targetR, targetG, targetB, rawHSV)
        
        val rawH = rawHSV[0]
        val rawS = rawHSV[1]
        val rawV = rawHSV[2]
        
        // ============================================
        // Stage 1: Detect rapid changes and smooth the target
        // ============================================
        
        val rawHueDelta = hueDistance(rawH, prevRawH)
        val isRapidChange = rawHueDelta > 30f || abs(rawV - prevRawV) > 0.3f
        
        if (isRapidChange) {
            rapidChangeCounter = min(rapidChangeCounter + 1, rapidChangeThreshold + 2)
        } else {
            rapidChangeCounter = max(0, rapidChangeCounter - 1)
        }
        
        // Use slower target smoothing during rapid changes
        val effectiveTargetSmoothing = if (rapidChangeCounter >= rapidChangeThreshold) {
            targetSmoothingFactor * 0.5f  // Even slower during rapid changes
        } else {
            targetSmoothingFactor
        }
        
        // Smooth the target (the color we're transitioning towards)
        targetH = lerpHue(targetH, rawH, effectiveTargetSmoothing)
        targetS = lerp(targetS, rawS, effectiveTargetSmoothing)
        targetV = lerp(targetV, rawV, effectiveTargetSmoothing)
        
        // Store for next frame's change detection
        prevRawH = rawH
        prevRawS = rawS
        prevRawV = rawV
        
        // ============================================
        // Stage 2: Rate-limited transition to smoothed target
        // ============================================
        
        // Calculate deltas to smoothed target
        val hueDelta = hueDistance(targetH, currentH)
        val hueDirection = hueDirection(currentH, targetH)
        val satDelta = targetS - currentS
        val valDelta = targetV - currentV
        
        // Detect large changes and apply slowdown
        val isLargeHueChange = abs(hueDelta) > largeHueChangeThreshold
        val speedMultiplier = if (isLargeHueChange || rapidChangeCounter >= rapidChangeThreshold) {
            largeChangeSlowdown
        } else {
            1f
        }
        
        // Calculate desired step sizes (with speed multiplier)
        val desiredHueStep = hueDelta * hueTransitionSpeed * speedMultiplier
        val desiredSatStep = satDelta * satTransitionSpeed * speedMultiplier
        val desiredValStep = valDelta * valTransitionSpeed * speedMultiplier
        
        // Apply rate limiting - clamp to maximum step per frame
        val actualHueStep = clampMagnitude(desiredHueStep, maxHueStepPerFrame * speedMultiplier)
        val actualSatStep = clampMagnitude(desiredSatStep, maxSatStepPerFrame * speedMultiplier)
        val actualValStep = clampMagnitude(desiredValStep, maxValStepPerFrame * speedMultiplier)
        
        // Apply steps with correct direction for hue
        currentH = (currentH + actualHueStep * hueDirection + 360f) % 360f
        currentS = (currentS + actualSatStep).coerceIn(0f, 1f)
        currentV = (currentV + actualValStep).coerceIn(0f, 1f)
        
        // Apply minimum brightness threshold
        if (currentV < minBrightness && targetV < minBrightness) {
            currentV = 0f
        }
        
        // Convert back to RGB
        val resultColor = Color.HSVToColor(floatArrayOf(
            currentH, 
            currentS.coerceIn(0f, 1f), 
            currentV.coerceIn(0f, 1f)
        ))
        
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
    /**
     * Smooth with explicit control over responsiveness.
     * @param responsiveness 0.0 = max smoothing (very floaty), 1.0 = instant (raw)
     */
    fun smoothWithResponsiveness(targetR: Int, targetG: Int, targetB: Int, responsiveness: Float): Triple<Int, Int, Int> {
        // 1. Handle instant case (responsiveness 1.0)
        if (responsiveness >= 0.95f) {
            val hsv = FloatArray(3)
            Color.RGBToHSV(targetR, targetG, targetB, hsv)
            
            // Snap everything to target
            currentH = hsv[0]
            currentS = hsv[1]
            currentV = hsv[2]
            targetH = hsv[0]
            targetS = hsv[1]
            targetV = hsv[2]
            prevRawH = hsv[0]
            prevRawS = hsv[1]
            prevRawV = hsv[2]
            rapidChangeCounter = 0
            
            return Triple(targetR, targetG, targetB)
        }
        
        // 2. Calculate dynamic parameters based on responsiveness (0.0 - 0.95)
        // Map 0.0..1.0 to useful ranges for our parameters
        // Default responsiveness (approx 0.3) matches the hardcoded constants
        
        // Target smoothing: 0.02 (very slow) to 0.4 (very fast)
        val dynamicTargetSmoothing = 0.02f + (responsiveness * 0.38f)
        
        // Transition speeds: 0.01 (very slow) to 0.5 (very fast)
        val dynamicHueSpeed = 0.01f + (responsiveness * 0.49f)
        val dynamicSatSpeed = 0.01f + (responsiveness * 0.49f)
        val dynamicValSpeed = 0.01f + (responsiveness * 0.49f)
        
        // Max steps: Scale significantly with responsiveness
        val dynamicMaxHueStep = 1f + (responsiveness * 40f)
        val dynamicMaxSatStep = 0.01f + (responsiveness * 0.3f)
        val dynamicMaxValStep = 0.01f + (responsiveness * 0.4f)

        // ============================================
        // LOGIC DUPLICATION BEGINS
        // (We must duplicate logic to access the private state variables with dynamic params)
        // ============================================

        // Convert raw input to HSV
        val rawHSV = FloatArray(3)
        Color.RGBToHSV(targetR, targetG, targetB, rawHSV)
        
        val rawH = rawHSV[0]
        val rawS = rawHSV[1]
        val rawV = rawHSV[2]
        
        // --- Stage 1: Detect rapid changes ---
        
        val rawHueDelta = hueDistance(rawH, prevRawH)
        val isRapidChange = rawHueDelta > 30f || abs(rawV - prevRawV) > 0.3f
        
        if (isRapidChange) {
            rapidChangeCounter = min(rapidChangeCounter + 1, rapidChangeThreshold + 2)
        } else {
            rapidChangeCounter = max(0, rapidChangeCounter - 1)
        }
        
        // Use slower target smoothing during rapid changes
        val effectiveTargetSmoothing = if (rapidChangeCounter >= rapidChangeThreshold) {
            dynamicTargetSmoothing * 0.5f
        } else {
            dynamicTargetSmoothing
        }
        
        // Smooth the target (the color we're transitioning towards)
        targetH = lerpHue(targetH, rawH, effectiveTargetSmoothing)
        targetS = lerp(targetS, rawS, effectiveTargetSmoothing)
        targetV = lerp(targetV, rawV, effectiveTargetSmoothing)
        
        // Store for next frame
        prevRawH = rawH
        prevRawS = rawS
        prevRawV = rawV
        
        // --- Stage 2: Rate-limited transition ---
        
        // Calculate deltas
        val hueDelta = hueDistance(targetH, currentH)
        val hueDirection = hueDirection(currentH, targetH)
        val satDelta = targetS - currentS
        val valDelta = targetV - currentV
        
        // Detect large changes
        val isLargeHueChange = abs(hueDelta) > largeHueChangeThreshold
        val speedMultiplier = if (isLargeHueChange || rapidChangeCounter >= rapidChangeThreshold) {
            largeChangeSlowdown
        } else {
            1f
        }
        
        // Calculate desired step sizes
        val desiredHueStep = hueDelta * dynamicHueSpeed * speedMultiplier
        val desiredSatStep = satDelta * dynamicSatSpeed * speedMultiplier
        val desiredValStep = valDelta * dynamicValSpeed * speedMultiplier
        
        // Apply rate limiting
        val actualHueStep = clampMagnitude(desiredHueStep, dynamicMaxHueStep * speedMultiplier)
        val actualSatStep = clampMagnitude(desiredSatStep, dynamicMaxSatStep * speedMultiplier)
        val actualValStep = clampMagnitude(desiredValStep, dynamicMaxValStep * speedMultiplier)
        
        // Apply steps
        currentH = (currentH + actualHueStep * hueDirection + 360f) % 360f
        currentS = (currentS + actualSatStep).coerceIn(0f, 1f)
        currentV = (currentV + actualValStep).coerceIn(0f, 1f)
        
        // Apply minimum brightness threshold
        if (currentV < minBrightness && targetV < minBrightness) {
            currentV = 0f
        }
        
        // Convert back to RGB
        val resultColor = Color.HSVToColor(floatArrayOf(
            currentH, 
            currentS.coerceIn(0f, 1f), 
            currentV.coerceIn(0f, 1f)
        ))
        
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
        targetH = hsv[0]
        targetS = hsv[1]
        targetV = hsv[2]
        prevRawH = hsv[0]
        prevRawS = hsv[1]
        prevRawV = hsv[2]
        rapidChangeCounter = 0
    }
    
    /**
     * Get current smoothed color
     */
    fun getCurrentColor(): Triple<Int, Int, Int> {
        val resultColor = Color.HSVToColor(floatArrayOf(
            currentH, 
            currentS.coerceIn(0f, 1f), 
            currentV.coerceIn(0f, 1f)
        ))
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
        val delta = hueDistance(target, current)
        val direction = hueDirection(current, target)
        
        var result = current + delta * factor * direction
        if (result < 0f) result += 360f
        if (result >= 360f) result -= 360f
        return result
    }
    
    /**
     * Calculate shortest distance between two hues (always positive)
     */
    private fun hueDistance(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2)
        return if (diff > 180f) 360f - diff else diff
    }
    
    /**
     * Determine direction to rotate from current to target hue
     * Returns 1 for clockwise, -1 for counter-clockwise
     */
    private fun hueDirection(current: Float, target: Float): Float {
        val diff = target - current
        return when {
            diff > 180f -> -1f
            diff < -180f -> 1f
            diff >= 0f -> 1f
            else -> -1f
        }
    }
    
    /**
     * Clamp value to [-maxMagnitude, maxMagnitude]
     */
    private fun clampMagnitude(value: Float, maxMagnitude: Float): Float {
        return value.coerceIn(-maxMagnitude, maxMagnitude)
    }
}
