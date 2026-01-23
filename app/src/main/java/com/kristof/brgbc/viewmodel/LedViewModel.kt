package com.kristof.brgbc.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.kristof.brgbc.ble.LedEffect
import com.kristof.brgbc.ble.LedMode
import com.kristof.brgbc.ble.NativeEffect
import com.kristof.brgbc.ble.RgbPinOrder
import com.kristof.brgbc.data.SettingsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for LED control state management with persistent storage
 */
class LedViewModel(application: Application) : AndroidViewModel(application) {
    
    enum class ControlMode {
        SCREEN_SYNC,
        STATIC_COLOR,
        EFFECTS,
        SETTINGS
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
        val audioSyncEnabled: Boolean = true,
        
        // Settings state
        val ledMode: LedMode = LedMode.RGB,
        val selectedNativeEffect: NativeEffect = NativeEffect.GRADIENT_RGB,
        val nativeEffectSpeed: Int = 50,
        val temperature: Int = 50,
        val temperatureMode: Int = 5,
        val grayscaleLevel: Int = 100,
        val rgbPinOrder: RgbPinOrder = RgbPinOrder.RGB,
        val dynamicSensitivity: Int = 50,
        val isPowerOn: Boolean = true
    )
    
    private val preferences = SettingsPreferences(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        // Load saved settings on init
        val savedState = preferences.loadState()
        _uiState.value = savedState
        
        // Sync audio sync state to the holder
        com.kristof.brgbc.capture.LedControllerHolder.isAudioSyncEnabled = savedState.audioSyncEnabled
    }
    
    var mediaProjectionResultCode: Int = Activity.RESULT_CANCELED
    var mediaProjectionResultData: Intent? = null
    
    /**
     * Save current settings to persistent storage
     */
    fun saveSettings() {
        preferences.saveState(_uiState.value)
    }
    
    fun setConnected(connected: Boolean, deviceName: String? = null) {
        _uiState.value = _uiState.value.copy(
            isConnected = connected,
            deviceName = if (connected) deviceName else null
        )
    }
    
    fun setControlMode(mode: ControlMode) {
        _uiState.value = _uiState.value.copy(controlMode = mode)
        saveSettings()
    }
    
    fun setBrightness(level: Int) {
        _uiState.value = _uiState.value.copy(brightness = level)
        saveSettings()
    }
    
    fun setStaticColor(color: Color) {
        _uiState.value = _uiState.value.copy(staticColor = color)
        saveSettings()
    }
    
    fun setSelectedEffect(effect: LedEffect) {
        _uiState.value = _uiState.value.copy(selectedEffect = effect)
        saveSettings()
    }
    
    fun setEffectSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(effectSpeed = speed)
        saveSettings()
    }
    
    fun setScreenSyncActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isScreenSyncActive = active)
        // Don't save this - it's a runtime state
    }
    
    fun setAutoStartEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoStartEnabled = enabled)
        saveSettings()
    }

    fun setAudioSyncEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(audioSyncEnabled = enabled)
        com.kristof.brgbc.capture.LedControllerHolder.isAudioSyncEnabled = enabled
        saveSettings()
    }
    
    // ============================================================
    // SETTINGS FUNCTIONS
    // ============================================================
    
    fun setLedMode(mode: LedMode) {
        _uiState.value = _uiState.value.copy(ledMode = mode)
        saveSettings()
    }
    
    fun setSelectedNativeEffect(effect: NativeEffect) {
        _uiState.value = _uiState.value.copy(selectedNativeEffect = effect)
        saveSettings()
    }
    
    fun setNativeEffectSpeed(speed: Int) {
        _uiState.value = _uiState.value.copy(nativeEffectSpeed = speed.coerceIn(0, 100))
        saveSettings()
    }
    
    fun setTemperature(temp: Int) {
        _uiState.value = _uiState.value.copy(temperature = temp.coerceIn(0, 100))
        saveSettings()
    }
    
    fun setTemperatureMode(mode: Int) {
        _uiState.value = _uiState.value.copy(temperatureMode = mode.coerceIn(0, 10))
        saveSettings()
    }
    
    fun setGrayscaleLevel(level: Int) {
        _uiState.value = _uiState.value.copy(grayscaleLevel = level.coerceIn(0, 100))
        saveSettings()
    }
    
    fun setRgbPinOrder(order: RgbPinOrder) {
        _uiState.value = _uiState.value.copy(rgbPinOrder = order)
        saveSettings()
    }
    
    fun setDynamicSensitivity(sensitivity: Int) {
        _uiState.value = _uiState.value.copy(dynamicSensitivity = sensitivity.coerceIn(0, 100))
        saveSettings()
    }
    
    fun setPowerOn(isOn: Boolean) {
        _uiState.value = _uiState.value.copy(isPowerOn = isOn)
        saveSettings()
    }
}

