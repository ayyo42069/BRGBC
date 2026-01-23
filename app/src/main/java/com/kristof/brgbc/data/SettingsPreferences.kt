package com.kristof.brgbc.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kristof.brgbc.ble.LedEffect
import com.kristof.brgbc.ble.LedMode
import com.kristof.brgbc.ble.NativeEffect
import com.kristof.brgbc.ble.RgbPinOrder
import com.kristof.brgbc.viewmodel.LedViewModel

/**
 * Manages persistent storage of LED settings
 */
class SettingsPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "led_settings"
        
        // Keys
        private const val KEY_CONTROL_MODE = "control_mode"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_STATIC_COLOR = "static_color"
        private const val KEY_SELECTED_EFFECT = "selected_effect"
        private const val KEY_EFFECT_SPEED = "effect_speed"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_AUDIO_SYNC = "audio_sync"
        
        // Settings keys
        private const val KEY_LED_MODE = "led_mode"
        private const val KEY_NATIVE_EFFECT = "native_effect"
        private const val KEY_NATIVE_EFFECT_SPEED = "native_effect_speed"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TEMPERATURE_MODE = "temperature_mode"
        private const val KEY_GRAYSCALE = "grayscale"
        private const val KEY_RGB_ORDER = "rgb_order"
        private const val KEY_DYNAMIC_SENSITIVITY = "dynamic_sensitivity"
        private const val KEY_POWER_ON = "power_on"
    }
    
    fun saveState(state: LedViewModel.UiState) {
        prefs.edit().apply {
            putString(KEY_CONTROL_MODE, state.controlMode.name)
            putInt(KEY_BRIGHTNESS, state.brightness)
            putInt(KEY_STATIC_COLOR, state.staticColor.toArgb())
            putString(KEY_SELECTED_EFFECT, state.selectedEffect.name)
            putFloat(KEY_EFFECT_SPEED, state.effectSpeed)
            putBoolean(KEY_AUTO_START, state.autoStartEnabled)
            putBoolean(KEY_AUDIO_SYNC, state.audioSyncEnabled)
            
            // Settings
            putString(KEY_LED_MODE, state.ledMode.name)
            putString(KEY_NATIVE_EFFECT, state.selectedNativeEffect.name)
            putInt(KEY_NATIVE_EFFECT_SPEED, state.nativeEffectSpeed)
            putInt(KEY_TEMPERATURE, state.temperature)
            putInt(KEY_TEMPERATURE_MODE, state.temperatureMode)
            putInt(KEY_GRAYSCALE, state.grayscaleLevel)
            putString(KEY_RGB_ORDER, state.rgbPinOrder.name)
            putInt(KEY_DYNAMIC_SENSITIVITY, state.dynamicSensitivity)
            putBoolean(KEY_POWER_ON, state.isPowerOn)
            
            apply()
        }
    }
    
    fun loadState(): LedViewModel.UiState {
        return LedViewModel.UiState(
            controlMode = try {
                LedViewModel.ControlMode.valueOf(
                    prefs.getString(KEY_CONTROL_MODE, LedViewModel.ControlMode.STATIC_COLOR.name)!!
                )
            } catch (e: Exception) {
                LedViewModel.ControlMode.STATIC_COLOR
            },
            brightness = prefs.getInt(KEY_BRIGHTNESS, 100),
            staticColor = Color(prefs.getInt(KEY_STATIC_COLOR, Color.White.toArgb())),
            selectedEffect = try {
                LedEffect.valueOf(
                    prefs.getString(KEY_SELECTED_EFFECT, LedEffect.RAINBOW.name)!!
                )
            } catch (e: Exception) {
                LedEffect.RAINBOW
            },
            effectSpeed = prefs.getFloat(KEY_EFFECT_SPEED, 0.1f),
            autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, false),
            audioSyncEnabled = prefs.getBoolean(KEY_AUDIO_SYNC, true),
            
            // Settings
            ledMode = try {
                LedMode.valueOf(
                    prefs.getString(KEY_LED_MODE, LedMode.RGB.name)!!
                )
            } catch (e: Exception) {
                LedMode.RGB
            },
            selectedNativeEffect = try {
                NativeEffect.valueOf(
                    prefs.getString(KEY_NATIVE_EFFECT, NativeEffect.GRADIENT_RGB.name)!!
                )
            } catch (e: Exception) {
                NativeEffect.GRADIENT_RGB
            },
            nativeEffectSpeed = prefs.getInt(KEY_NATIVE_EFFECT_SPEED, 50),
            temperature = prefs.getInt(KEY_TEMPERATURE, 50),
            temperatureMode = prefs.getInt(KEY_TEMPERATURE_MODE, 5),
            grayscaleLevel = prefs.getInt(KEY_GRAYSCALE, 100),
            rgbPinOrder = try {
                RgbPinOrder.valueOf(
                    prefs.getString(KEY_RGB_ORDER, RgbPinOrder.RGB.name)!!
                )
            } catch (e: Exception) {
                RgbPinOrder.RGB
            },
            dynamicSensitivity = prefs.getInt(KEY_DYNAMIC_SENSITIVITY, 50),
            isPowerOn = prefs.getBoolean(KEY_POWER_ON, true)
        )
    }
}
