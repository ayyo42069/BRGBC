package com.kristof.brgbc.capture

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

/**
 * Analyzes screen captures to extract dominant color
 * Uses grid sampling for performance (9 sample points)
 */
object ColorAnalyzer {
    
    private const val GRID_SIZE = 3 // 3x3 = 9 sample points
    
    /**
     * Extract dominant color from bitmap using weighted sampling
     * @param bitmap The screen capture bitmap
     * @return RGB color as Triple(r, g, b)
     */
    fun analyzeBitmap(bitmap: Bitmap): Triple<Int, Int, Int> {
        if (bitmap.width == 0 || bitmap.height == 0) {
            return Triple(0, 0, 0)
        }
        
        // Skip pixels to improve performance - aiming for ~400 samples (20x20 grid roughly)
        // If the bitmap is already downscaled by the service, this step can be smaller
        val skipX = (bitmap.width / 20).coerceAtLeast(1)
        val skipY = (bitmap.height / 20).coerceAtLeast(1)
        
        var totalWeight = 0f
        var weightedR = 0f
        var weightedG = 0f
        var weightedB = 0f
        
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (y in 0 until bitmap.height step skipY) {
            for (x in 0 until bitmap.width step skipX) {
                val pixel = pixels[y * bitmap.width + x]
                
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // Convert to HSV for weighting
                val hsv = FloatArray(3)
                Color.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val `val` = hsv[2]
                
                // Weight: Give much higher priority to saturated and bright pixels
                // Square the saturation to heavily penalize greyish colors
                // Value is important but we don't want to ignore dark saturated colors too much
                val weight = (sat * sat * sat) + (`val` * 0.1f) + 0.01f
                
                weightedR += r * weight
                weightedG += g * weight
                weightedB += b * weight
                totalWeight += weight
            }
        }
        
        if (totalWeight <= 0) return Triple(0, 0, 0)
        
        // Normalize to get base average
        var r = (weightedR / totalWeight).toInt()
        var g = (weightedG / totalWeight).toInt()
        var b = (weightedB / totalWeight).toInt()
        
        // --- PRO Color Enhancement ---
        // Convert to HSV for smart boosting
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        
        // 1. Boost Saturation: LEDs wash out easily, so we hyper-saturate
        hsv[1] = (hsv[1] * 1.5f).coerceIn(0f, 1f)
        
        // 2. Smart Brightness (Gamma Correction)
        // Mid-tones need more boost than highlights
        // Formula: NewV = V ^ 0.7 (brightens darks without clipping whites)
        // Then apply a global gain
        if (hsv[2] > 0.05f) { // Only boost if not pitch black
            hsv[2] = Math.pow(hsv[2].toDouble(), 0.7).toFloat() // Gamma
            hsv[2] = (hsv[2] * 1.2f).coerceIn(0f, 1f) // Gain
        } else {
             // Let it stay black to avoid noise
             hsv[2] = 0f 
        }

        val resultColor = Color.HSVToColor(hsv)
        
        return Triple(
            Color.red(resultColor),
            Color.green(resultColor),
            Color.blue(resultColor)
        )
    }
    
    /**
     * Calculate perceptual difference between two colors
     * Used to determine if color changed significantly
     */
    fun colorDistance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        return sqrt((dr * dr + dg * dg + db * db).toDouble())
    }
}
