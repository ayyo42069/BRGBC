package com.kristof.brgbc.capture

import android.media.audiofx.Visualizer
import android.media.projection.MediaProjection
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Enhanced beat detection with smoother brightness transitions.
 * 
 * Key improvements:
 * - Adaptive threshold based on music dynamics
 * - Longer beat "tails" for more visible pulses
 * - Smoother ambient brightness between beats
 * - Genre-adaptive sensitivity
 */
class AudioAnalyzer {
    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val CAPTURE_SIZE = 1024
        
        // Beat detection - adaptive thresholds
        private const val BASE_JUMP_THRESHOLD = 1.10f   // Base 10% increase = beat
        private const val MIN_JUMP_THRESHOLD = 1.06f    // Minimum threshold for quiet music
        private const val MAX_JUMP_THRESHOLD = 1.25f    // Maximum threshold for loud/dynamic music
        private const val MIN_ENERGY_FOR_BEAT = 12f     // Lower threshold for sensitivity
        private const val BEAT_COOLDOWN = 3             // Slightly faster recovery between beats
        private const val BASS_THRESHOLD = 150f         // Lower bass threshold for more sensitivity
        
        // Beat visibility - longer tails
        private const val BEAT_HOLD_FRAMES = 3          // Hold beat flash longer
        private const val BEAT_TAIL_FRAMES = 8          // Gradual fade after beat
        
        // Output brightness - smoother transitions
        private const val BASE_BRIGHTNESS = 0.12f       // Slightly darker base for contrast
        private const val BEAT_BRIGHTNESS = 1.0f        // Full brightness on beat
        private const val OUTPUT_ATTACK = 0.92f         // Fast but not instant rise
        private const val OUTPUT_DECAY = 0.12f          // Much slower decay for visible tails
        private const val AMBIENT_DECAY = 0.08f         // Even slower for ambient level
        
        // Dynamic range tracking
        private const val DYNAMICS_WINDOW = 60          // Frames to track dynamics
        private const val DYNAMICS_SMOOTH = 0.05f       // Slow adaptation to dynamics
        
        // Energy smoothing
        private const val ENERGY_SMOOTH = 0.4f          // Slightly more smoothing
    }

    private var visualizer: Visualizer? = null
    private var isRecording = false
    
    // Energy tracking
    @Volatile private var currentEnergy = 0f
    @Volatile private var previousEnergy = 0f
    @Volatile private var smoothedEnergy = 0f
    
    // Dynamic range tracking (for adaptive threshold)
    private var minRecentEnergy = Float.MAX_VALUE
    private var maxRecentEnergy = 0f
    private var dynamicRange = 1f
    private var adaptiveThreshold = BASE_JUMP_THRESHOLD
    
    // Beat state
    @Volatile private var isBeat = false
    @Volatile private var beatHoldFrames = 0   // Full brightness hold
    @Volatile private var beatTailFrames = 0   // Gradual fade
    @Volatile private var cooldownFrames = 0   // Prevent beat spam
    @Volatile private var beatIntensity = 0f   // 0-1 intensity for gradual fade
    
    // Ambient brightness (smoothed base level between beats)
    private var ambientBrightness = BASE_BRIGHTNESS
    
    // Output
    private var smoothedOutput = BASE_BRIGHTNESS
    
    // Bass from FFT
    @Volatile private var bassEnergy = 0f
    
    private var frameCounter = 0
    private var logCounter = 0

    fun start(mediaProjection: MediaProjection) {
        startVisualizer()
    }
    
    fun startVisualizer() {
        if (isRecording) {
            Log.w(TAG, "Visualizer already running")
            return
        }
        
        try {
            visualizer = Visualizer(0).apply {
                enabled = false
                
                val range = Visualizer.getCaptureSizeRange()
                val targetSize = CAPTURE_SIZE.coerceIn(range[0], range[1])
                captureSize = targetSize
                
                Log.d(TAG, "Enhanced beat detection started! Size: $targetSize")
                
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { processWaveform(it) }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { processFFT(it) }
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    true,
                    true
                )
                
                enabled = true
            }
            
            isRecording = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Visualizer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun processWaveform(waveform: ByteArray) {
        // Store previous
        previousEnergy = smoothedEnergy
        
        // Calculate current RMS
        var sum = 0.0
        for (sample in waveform) {
            val signedSample = (sample.toInt() and 0xFF) - 128
            sum += signedSample * signedSample
        }
        currentEnergy = sqrt(sum / waveform.size).toFloat()
        
        // Smooth the energy slightly to reduce noise
        smoothedEnergy = smoothedEnergy * (1f - ENERGY_SMOOTH) + currentEnergy * ENERGY_SMOOTH
        
        // Track dynamic range for adaptive threshold
        updateDynamicRange(smoothedEnergy)
        
        // Calculate adaptive threshold based on dynamic range
        // High dynamic range = higher threshold needed (energetic music)
        // Low dynamic range = lower threshold (quiet/ambient music)
        val dynamicsFactor = (dynamicRange / 50f).coerceIn(0f, 1f)  // Normalize
        adaptiveThreshold = MIN_JUMP_THRESHOLD + (MAX_JUMP_THRESHOLD - MIN_JUMP_THRESHOLD) * dynamicsFactor
        
        // BEAT DETECTION: Did energy jump up significantly?
        val jumpRatio = if (previousEnergy > 4f) smoothedEnergy / previousEnergy else 1f
        
        // Cooldown countdown
        if (cooldownFrames > 0) cooldownFrames--
        
        // Beat tail countdown
        if (beatTailFrames > 0) beatTailFrames--
        
        // BEAT DETECTION (only if not in cooldown)
        val beatTriggered = cooldownFrames == 0 && (
            // Primary: energy jump detection with adaptive threshold
            (jumpRatio > adaptiveThreshold && smoothedEnergy > MIN_ENERGY_FOR_BEAT) ||
            // Secondary: strong bass hit
            (bassEnergy > BASS_THRESHOLD && jumpRatio > 1.06f && smoothedEnergy > MIN_ENERGY_FOR_BEAT)
        )
        
        if (beatTriggered) {
            isBeat = true
            beatHoldFrames = BEAT_HOLD_FRAMES
            beatTailFrames = BEAT_TAIL_FRAMES
            cooldownFrames = BEAT_COOLDOWN
            beatIntensity = 1f
        } else if (beatHoldFrames > 0) {
            // Full brightness hold
            beatHoldFrames--
            isBeat = true
            beatIntensity = 1f
        } else if (beatTailFrames > 0) {
            // Gradual tail fade
            isBeat = false
            beatIntensity = beatTailFrames.toFloat() / BEAT_TAIL_FRAMES
        } else {
            isBeat = false
            beatIntensity = 0f
        }
        
        // Update ambient brightness (very slow tracking of average energy)
        val targetAmbient = BASE_BRIGHTNESS + (smoothedEnergy / 100f).coerceIn(0f, 0.25f)
        ambientBrightness = ambientBrightness * (1f - AMBIENT_DECAY) + targetAmbient * AMBIENT_DECAY
        
        // Debug
        frameCounter++
        if (frameCounter % 25 == 0) {
            Log.d(TAG, "E: %.0f | Jump: %.2f | Thresh: %.2f | Bass: %.0f | Beat: %s | Tail: %d".format(
                smoothedEnergy, jumpRatio, adaptiveThreshold, bassEnergy, 
                if (isBeat) "YES" else "no", beatTailFrames
            ))
        }
    }
    
    private fun updateDynamicRange(energy: Float) {
        // Update min/max tracking
        if (energy < minRecentEnergy) minRecentEnergy = energy
        if (energy > maxRecentEnergy) maxRecentEnergy = energy
        
        // Slowly decay the range tracking
        frameCounter++
        if (frameCounter % DYNAMICS_WINDOW == 0) {
            // Calculate dynamic range
            val range = maxRecentEnergy - minRecentEnergy
            dynamicRange = dynamicRange * (1f - DYNAMICS_SMOOTH) + range * DYNAMICS_SMOOTH
            
            // Reset for next window (with some hysteresis)
            minRecentEnergy = minRecentEnergy * 0.5f + Float.MAX_VALUE * 0.5f
            maxRecentEnergy = maxRecentEnergy * 0.8f  // Decay slower
        }
    }
    
    private fun processFFT(fft: ByteArray) {
        if (fft.size < 16) return
        
        var bass = 0f
        for (i in 1..8) {
            if (i * 2 + 1 < fft.size) {
                val real = fft[i * 2].toInt()
                val imag = fft[i * 2 + 1].toInt()
                bass += sqrt((real * real + imag * imag).toFloat())
            }
        }
        bassEnergy = bass
    }

    fun stop() {
        isRecording = false
        try {
            visualizer?.enabled = false
            visualizer?.release()
            Log.d(TAG, "Visualizer stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Visualizer: ${e.message}")
        }
        visualizer = null
        
        // Reset state
        minRecentEnergy = Float.MAX_VALUE
        maxRecentEnergy = 0f
        dynamicRange = 1f
    }

    /**
     * Get brightness for LEDs (0.0 to 1.0)
     * Now with smoother transitions and longer beat tails
     */
    fun getAmplitude(): Float {
        if (!isRecording) return 0f
        
        // Target brightness calculation
        val targetOutput = when {
            isBeat -> BEAT_BRIGHTNESS
            beatIntensity > 0f -> {
                // Gradual tail: blend from beat brightness to ambient
                ambientBrightness + (BEAT_BRIGHTNESS - ambientBrightness) * beatIntensity * 0.6f
            }
            else -> ambientBrightness
        }
        
        // Smooth output with asymmetric attack/decay
        val factor = if (targetOutput > smoothedOutput) OUTPUT_ATTACK else OUTPUT_DECAY
        smoothedOutput = smoothedOutput + (targetOutput - smoothedOutput) * factor
        
        val finalOutput = smoothedOutput.coerceIn(0f, 1f)
        
        // Debug
        logCounter++
        if (logCounter % 40 == 0) {
            Log.d(TAG, "Output: %.2f | Target: %.2f | Ambient: %.2f | Beat: %s | Intensity: %.2f".format(
                finalOutput, targetOutput, ambientBrightness, if (isBeat) "YES!" else "no", beatIntensity
            ))
        }
        
        return finalOutput
    }
    
    fun getRawEnergy(): Float = currentEnergy
    fun getBeatIntensity(): Float = beatIntensity
    fun isCurrentlyBeat(): Boolean = isBeat
}
