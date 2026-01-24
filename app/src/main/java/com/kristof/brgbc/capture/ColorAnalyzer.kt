package com.kristof.brgbc.capture

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced color analyzer that finds the MOST VIBRANT color on screen
 * with temporal stability to prevent flickering between frames.
 * 
 * Key features:
 * - Finds dominant vibrant colors using hue binning
 * - Temporal smoothing prevents jumping between competing colors
 * - Hysteresis prevents oscillation when colors are equally dominant
 * - Adaptive saturation boost based on source content
 */
object ColorAnalyzer {
    
    // Sampling configuration
    private const val SAMPLE_STEP = 8           // Sample every Nth pixel for speed
    private const val MIN_SATURATION = 0.20f    // Ignore colors less saturated than this
    private const val MIN_VALUE = 0.12f         // Ignore very dark pixels
    
    // Color bins for finding dominant vibrant color
    private const val HUE_BINS = 12            // 12 bins = 30 degrees each
    
    // Temporal stability parameters
    private const val SCORE_SMOOTHING = 0.3f   // How fast bin scores update (lower = more stable)
    private const val HYSTERESIS = 1.4f        // Current bin needs 40% higher score to switch away
    private const val COLOR_BLEND_FACTOR = 0.25f // How fast output color blends to new dominant
    
    // State for temporal stability
    private val smoothedScores = FloatArray(HUE_BINS)
    private var currentDominantBin = -1
    private var lastOutputH = 0f
    private var lastOutputS = 0f
    private var lastOutputV = 0f
    private var isInitialized = false
    
    /**
     * Analyze bitmap to find the MOST VIBRANT dominant color with temporal stability.
     * 
     * Algorithm:
     * 1. Sample pixels across the screen
     * 2. Filter to only saturated, bright pixels (ignore blacks/grays)
     * 3. Bin by hue to find the dominant color family
     * 4. Apply temporal smoothing to prevent flickering
     * 5. Return smoothed output color
     */
    fun analyzeBitmap(bitmap: Bitmap): Triple<Int, Int, Int> {
        if (bitmap.width < 4 || bitmap.height < 4) {
            return Triple(0, 0, 0)
        }
        
        val w = bitmap.width
        val h = bitmap.height
        
        // Get pixels
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        
        // Hue histogram: count of saturated pixels in each hue bin
        val hueCount = IntArray(HUE_BINS)
        val hueSatSum = FloatArray(HUE_BINS)  // Sum of saturations for averaging
        val hueValSum = FloatArray(HUE_BINS)  // Sum of values for averaging
        
        // Track the single most saturated pixel per hue bin
        val bestPixelPerBin = IntArray(HUE_BINS) { Color.BLACK }
        val bestSatPerBin = FloatArray(HUE_BINS)
        
        // Track average hue within each bin for more accurate output
        val hueSum = FloatArray(HUE_BINS)
        
        val hsv = FloatArray(3)
        
        // Sample pixels
        for (y in 0 until h step SAMPLE_STEP) {
            for (x in 0 until w step SAMPLE_STEP) {
                val pixel = pixels[y * w + x]
                Color.colorToHSV(pixel, hsv)
                
                val sat = hsv[1]
                val value = hsv[2]
                
                // Only consider vibrant pixels (saturated AND bright enough)
                if (sat >= MIN_SATURATION && value >= MIN_VALUE) {
                    val hueBin = ((hsv[0] / 360f) * HUE_BINS).toInt().coerceIn(0, HUE_BINS - 1)
                    
                    hueCount[hueBin]++
                    hueSatSum[hueBin] += sat
                    hueValSum[hueBin] += value
                    hueSum[hueBin] += hsv[0]
                    
                    // Track the most saturated pixel in this bin
                    if (sat > bestSatPerBin[hueBin]) {
                        bestSatPerBin[hueBin] = sat
                        bestPixelPerBin[hueBin] = pixel
                    }
                }
            }
        }
        
        // Calculate raw scores for each bin
        val rawScores = FloatArray(HUE_BINS)
        for (i in 0 until HUE_BINS) {
            if (hueCount[i] > 0) {
                // Score = count * average saturation (prefer both numerous AND saturated)
                val avgSat = hueSatSum[i] / hueCount[i]
                rawScores[i] = hueCount[i] * avgSat
            }
        }
        
        // Apply temporal smoothing to scores
        for (i in 0 until HUE_BINS) {
            smoothedScores[i] = smoothedScores[i] * (1f - SCORE_SMOOTHING) + rawScores[i] * SCORE_SMOOTHING
        }
        
        // Find the best bin with hysteresis
        var bestBin = -1
        var bestScore = 0f
        
        for (i in 0 until HUE_BINS) {
            val effectiveScore = if (i == currentDominantBin) {
                // Current bin gets hysteresis boost - needs to be beaten by margin
                smoothedScores[i] * HYSTERESIS
            } else {
                smoothedScores[i]
            }
            
            if (effectiveScore > bestScore && smoothedScores[i] > 0.5f) {
                bestScore = effectiveScore
                bestBin = i
            }
        }
        
        // Update current dominant bin (with some persistence)
        if (bestBin >= 0) {
            currentDominantBin = bestBin
        }
        
        // If no vibrant color found, fall back to weighted average
        if (currentDominantBin == -1 || smoothedScores.all { it < 0.5f }) {
            return fallbackAnalyze(pixels, w, h)
        }
        
        // Calculate target color from the dominant bin
        val dominantBin = currentDominantBin
        val count = hueCount[dominantBin]
        
        val targetH: Float
        val targetS: Float
        val targetV: Float
        
        if (count > 0) {
            // Use average hue within the bin for more accuracy
            targetH = hueSum[dominantBin] / count
            targetS = hueSatSum[dominantBin] / count
            targetV = hueValSum[dominantBin] / count
        } else {
            // Fallback to best pixel
            Color.colorToHSV(bestPixelPerBin[dominantBin], hsv)
            targetH = hsv[0]
            targetS = hsv[1]
            targetV = hsv[2]
        }
        
        // Initialize or blend to target
        if (!isInitialized) {
            lastOutputH = targetH
            lastOutputS = targetS
            lastOutputV = targetV
            isInitialized = true
        } else {
            // Smooth blend to target color
            lastOutputH = lerpHue(lastOutputH, targetH, COLOR_BLEND_FACTOR)
            lastOutputS = lerp(lastOutputS, targetS, COLOR_BLEND_FACTOR)
            lastOutputV = lerp(lastOutputV, targetV, COLOR_BLEND_FACTOR)
        }
        
        // Apply adaptive saturation/brightness boost for LED display
        // Less boost for already-saturated colors, more for dull ones
        val satBoost = 1f + (1f - lastOutputS) * 0.5f  // 1.0x to 1.5x
        val valBoost = 1f + (1f - lastOutputV) * 0.3f  // 1.0x to 1.3x
        
        val finalS = (lastOutputS * satBoost).coerceIn(0f, 1f)
        val finalV = (lastOutputV * valBoost).coerceIn(0f, 1f)
        
        val boosted = Color.HSVToColor(floatArrayOf(lastOutputH, finalS, finalV))
        
        return Triple(
            Color.red(boosted),
            Color.green(boosted),
            Color.blue(boosted)
        )
    }
    
    /**
     * Fallback: weighted average for scenes with no vibrant colors
     */
    private fun fallbackAnalyze(pixels: IntArray, w: Int, h: Int): Triple<Int, Int, Int> {
        var totalWeight = 0f
        var weightedR = 0f
        var weightedG = 0f
        var weightedB = 0f
        
        val hsv = FloatArray(3)
        
        for (y in 0 until h step SAMPLE_STEP) {
            for (x in 0 until w step SAMPLE_STEP) {
                val pixel = pixels[y * w + x]
                Color.colorToHSV(pixel, hsv)
                
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // Weight by saturation and brightness
                val weight = (hsv[1] * hsv[1] + hsv[2] * 0.1f + 0.01f)
                
                weightedR += r * weight
                weightedG += g * weight
                weightedB += b * weight
                totalWeight += weight
            }
        }
        
        if (totalWeight <= 0.001f) return Triple(0, 0, 0)
        
        val r = (weightedR / totalWeight).toInt().coerceIn(0, 255)
        val g = (weightedG / totalWeight).toInt().coerceIn(0, 255)
        val b = (weightedB / totalWeight).toInt().coerceIn(0, 255)
        
        // Boost for LEDs
        Color.RGBToHSV(r, g, b, hsv)
        if (hsv[2] > 0.1f) {
            hsv[1] = (hsv[1] * 1.5f).coerceIn(0f, 1f)
            hsv[2] = (hsv[2] * 1.2f).coerceIn(0f, 1f)
        }
        
        val boosted = Color.HSVToColor(hsv)
        return Triple(Color.red(boosted), Color.green(boosted), Color.blue(boosted))
    }
    
    /**
     * Reset analyzer state (call when starting new capture session)
     */
    fun reset() {
        for (i in 0 until HUE_BINS) {
            smoothedScores[i] = 0f
        }
        currentDominantBin = -1
        isInitialized = false
    }
    
    /**
     * Linear interpolation
     */
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
    
    /**
     * Perceptual color distance
     */
    fun colorDistance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val dr = (r1 - r2) * 0.30
        val dg = (g1 - g2) * 0.59
        val db = (b1 - b2) * 0.11
        return sqrt(dr * dr + dg * dg + db * db)
    }
}
