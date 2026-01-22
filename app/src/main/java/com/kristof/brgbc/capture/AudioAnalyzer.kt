package com.kristof.brgbc.capture

import android.media.audiofx.Visualizer
import android.media.projection.MediaProjection
import android.util.Log
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Simple, reliable beat detection using frame-to-frame change detection.
 * 
 * Instead of comparing to rolling averages (which get stuck),
 * we detect beats by looking for sudden JUMPS in energy from one frame to the next.
 * This is more reliable and truly volume-independent.
 */
class AudioAnalyzer {
    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val CAPTURE_SIZE = 1024
        
        // Beat detection - frame-to-frame comparison
        private const val JUMP_THRESHOLD = 1.12f    // 12% increase from previous frame = beat
        private const val MIN_ENERGY_FOR_BEAT = 15f // Minimum raw energy to consider
        private const val BEAT_COOLDOWN = 4         // Minimum frames between beats (prevents spam)
        private const val BASS_THRESHOLD = 200f     // Bass energy needed for bass-triggered beat
        
        // Output brightness
        private const val BASE_BRIGHTNESS = 0.15f   // 15% base (darker for more contrast)
        private const val BEAT_BRIGHTNESS = 1.0f    // 100% on beat
        private const val OUTPUT_ATTACK = 0.95f     // Very fast rise
        private const val OUTPUT_DECAY = 0.25f      // Faster fade for visible pulses
        
        // Energy smoothing
        private const val ENERGY_SMOOTH = 0.5f      // Smooth raw energy
    }

    private var visualizer: Visualizer? = null
    private var isRecording = false
    
    // Energy tracking
    @Volatile private var currentEnergy = 0f
    @Volatile private var previousEnergy = 0f
    @Volatile private var smoothedEnergy = 0f
    
    // Beat state
    @Volatile private var isBeat = false
    @Volatile private var beatHoldFrames = 0  // Hold beat for visibility
    @Volatile private var cooldownFrames = 0  // Prevent beat spam
    
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
                
                Log.d(TAG, "Frame-to-frame beat detection started! Size: $targetSize")
                
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
        
        // BEAT DETECTION: Did energy jump up significantly?
        val jumpRatio = if (previousEnergy > 5f) smoothedEnergy / previousEnergy else 1f
        
        // Cooldown countdown
        if (cooldownFrames > 0) cooldownFrames--
        
        // BEAT DETECTION (only if not in cooldown)
        val beatTriggered = cooldownFrames == 0 && (
            // Primary: energy jump detection
            (jumpRatio > JUMP_THRESHOLD && smoothedEnergy > MIN_ENERGY_FOR_BEAT) ||
            // Secondary: strong bass hit
            (bassEnergy > BASS_THRESHOLD && jumpRatio > 1.08f && smoothedEnergy > MIN_ENERGY_FOR_BEAT)
        )
        
        if (beatTriggered) {
            isBeat = true
            beatHoldFrames = 2   // Short hold for snappy response
            cooldownFrames = BEAT_COOLDOWN  // Prevent immediate re-trigger
        } else if (beatHoldFrames > 0) {
            beatHoldFrames--
            isBeat = beatHoldFrames > 0
        } else {
            isBeat = false
        }
        
        // Debug
        frameCounter++
        if (frameCounter % 20 == 0) {
            Log.d(TAG, "E: %.0f | Prev: %.0f | Jump: %.2f | Bass: %.0f | Beat: %s".format(
                smoothedEnergy, previousEnergy, jumpRatio, bassEnergy, if (isBeat) "YES" else "no"
            ))
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
    }

    /**
     * Get brightness for LEDs (0.0 to 1.0)
     */
    fun getAmplitude(): Float {
        if (!isRecording) return 0f
        
        // Target brightness
        val targetOutput = if (isBeat) {
            BEAT_BRIGHTNESS
        } else {
            // Ambient based on current energy (normalized roughly)
            val normalized = (smoothedEnergy / 80f).coerceIn(0f, 1f)
            BASE_BRIGHTNESS + normalized * 0.2f
        }
        
        // Smooth output
        val factor = if (targetOutput > smoothedOutput) OUTPUT_ATTACK else OUTPUT_DECAY
        smoothedOutput = smoothedOutput + (targetOutput - smoothedOutput) * factor
        
        val finalOutput = smoothedOutput.coerceIn(0f, 1f)
        
        // Debug
        logCounter++
        if (logCounter % 40 == 0) {
            Log.d(TAG, "Output: %.2f | Target: %.2f | Beat: %s".format(
                finalOutput, targetOutput, if (isBeat) "YES!" else "no"
            ))
        }
        
        return finalOutput
    }
    
    fun getRawEnergy(): Float = currentEnergy
    fun getBeatIntensity(): Float = if (isBeat) 1f else 0f
}
