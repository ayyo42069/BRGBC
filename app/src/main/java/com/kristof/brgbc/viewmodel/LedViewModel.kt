package com.kristof.brgbc.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.kristof.brgbc.ble.BleLedController
import com.kristof.brgbc.ble.LedEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for LED control state management
 */
class LedViewModel : ViewModel() {
    
    enum class ControlMode {
        SCREEN_SYNC,
        STATIC_COLOR,
        EFFECTS
    }
    
    data class UiState(
        val isConnected: Boolean = false,
        val deviceName: String? = null,
        val controlMode: ControlMode = ControlMode.STATIC_COLOR,
        val brightness: Int = 100,
        val staticColor: Color = Color.White,
        val selectedEffect: LedEffect = LedEffect.RAINBOW,
        val effectSpeed: Float = 0.1f,
        val isScreenSyncActive: Boolean = false,
        val autoStartEnabled: Boolean = false,
        val audioSyncEnabled: Boolean = true // Default to on for fun!
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        // Sync default audio sync state to the holder
        com.kristof.brgbc.capture.LedControllerHolder.isAudioSyncEnabled = _uiState.value.audioSyncEnabled
    }
    
    var mediaProjectionResultCode: Int = Activity.RESULT_CANCELED
    var mediaProjectionResultData: Intent? = null
    
    fun setConnected(connected: Boolean, deviceName: String? = null) {
        _uiState.value = _uiState.value.copy(
            isConnected = connected,
            deviceName = if (connected) deviceName else null
        )
    }
    
    fun setControlMode(mode: ControlMode) {
        _uiState.value = _uiState.value.copy(controlMode = mode)
    }
    
    fun setBrightness(level: Int) {
        _uiState.value = _uiState.value.copy(brightness = level)
    }
    
    fun setStaticColor(color: Color) {
        _uiState.value = _uiState.value.copy(staticColor = color)
    }
    
    fun setSelectedEffect(effect: LedEffect) {
        _uiState.value = _uiState.value.copy(selectedEffect = effect)
    }
    
    fun setEffectSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(effectSpeed = speed)
    }
    
    fun setScreenSyncActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isScreenSyncActive = active)
    }
    
    fun setAutoStartEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoStartEnabled = enabled)
    }

    fun setAudioSyncEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(audioSyncEnabled = enabled)
        com.kristof.brgbc.capture.LedControllerHolder.isAudioSyncEnabled = enabled
    }
}
