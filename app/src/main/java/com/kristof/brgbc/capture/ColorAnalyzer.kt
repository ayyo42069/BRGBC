package com.kristof.brgbc.capture

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced color analyzer that finds the MOST VIBRANT color on screen.
 * 
 * Instead of averaging all pixels (which washes out colors),
 * this finds the most saturated/vibrant color and uses that.
 * Perfect for matching the "vibe" of content with colored elements.
 */
object ColorAnalyzer {
    
    // Sampling configuration
    private const val SAMPLE_STEP = 8           // Sample every Nth pixel for speed
    private const val MIN_SATURATION = 0.25f    // Ignore colors less saturated than this
    private const val MIN_VALUE = 0.15f         // Ignore very dark pixels
    
    // Color bins for finding dominant vibrant color
    private const val HUE_BINS = 12            // 12 bins = 30 degrees each
    
    /**
     * Analyze bitmap to find the MOST VIBRANT dominant color.
     * 
     * Algorithm:
     * 1. Sample pixels across the screen
     * 2. Filter to only saturated, bright pixels (ignore blacks/grays)
     * 3. Bin by hue to find the dominant color family
     * 4. Return the most saturated example from that family
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
        
        // Track the single most saturated pixel per hue bin
        val bestPixelPerBin = IntArray(HUE_BINS) { Color.BLACK }
        val bestSatPerBin = FloatArray(HUE_BINS)
        
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
                    
                    // Track the most saturated pixel in this bin
                    if (sat > bestSatPerBin[hueBin]) {
                        bestSatPerBin[hueBin] = sat
                        bestPixelPerBin[hueBin] = pixel
                    }
                }
            }
        }
        
        // Find the dominant hue bin (most vibrant pixels)
        var dominantBin = -1
        var maxScore = 0f
        
        for (i in 0 until HUE_BINS) {
            if (hueCount[i] > 0) {
                // Score = count * average saturation (prefer both numerous AND saturated)
                val avgSat = hueSatSum[i] / hueCount[i]
                val score = hueCount[i] * avgSat
                
                if (score > maxScore) {
                    maxScore = score
                    dominantBin = i
                }
            }
        }
        
        // If no vibrant color found, fall back to weighted average
        if (dominantBin == -1 || maxScore < 1f) {
            return fallbackAnalyze(pixels, w, h)
        }
        
        // Use the most saturated pixel from the dominant bin
        val resultPixel = bestPixelPerBin[dominantBin]
        
        // Boost saturation for LED display
        Color.colorToHSV(resultPixel, hsv)
        hsv[1] = (hsv[1] * 1.3f).coerceIn(0f, 1f)  // Boost saturation
        hsv[2] = (hsv[2] * 1.2f).coerceIn(0f, 1f)  // Slight brightness boost
        
        val boosted = Color.HSVToColor(hsv)
        
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
     * Perceptual color distance
     */
    fun colorDistance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val dr = (r1 - r2) * 0.30
        val dg = (g1 - g2) * 0.59
        val db = (b1 - b2) * 0.11
        return sqrt(dr * dr + dg * dg + db * db)
    }
}
