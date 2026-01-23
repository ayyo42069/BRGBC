package com.kristof.brgbc.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import com.kristof.brgbc.ble.LedEffect
import com.kristof.brgbc.ble.LedMode
import com.kristof.brgbc.ble.NativeEffect
import com.kristof.brgbc.ble.RgbPinOrder
import com.kristof.brgbc.ui.theme.*
import com.kristof.brgbc.viewmodel.LedViewModel

/**
 * LED Control Screen - Minimalist Design
 */
@Composable
fun LedControlScreen(
    viewModel: LedViewModel,
    onScanDevices: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onStartScreenSync: () -> Unit,
    onStopScreenSync: () -> Unit,
    onSetColor: (Color) -> Unit,
    onSetBrightness: (Int) -> Unit,
    onStartEffect: (LedEffect, Float) -> Unit,
    onStopEffect: () -> Unit,
    scannedDevices: List<BluetoothDevice>,
    modifier: Modifier = Modifier,
    // New settings callbacks
    onPowerToggle: (Boolean) -> Unit = {},
    onLedModeChange: (LedMode) -> Unit = {},
    onNativeEffectSelected: (NativeEffect) -> Unit = {},
    onNativeEffectSpeedChange: (Int) -> Unit = {},
    onTemperatureChange: (Int) -> Unit = {},
    onGrayscaleChange: (Int) -> Unit = {},
    onRgbOrderChange: (RgbPinOrder) -> Unit = {},
    onDynamicSensitivityChange: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeviceDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item(key = "header") {
                Column {
                    Text(
                        text = "LED CONTROL",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "RGB Strip Controller",
                        fontSize = 16.sp,
                        color = TextMuted
                    )
                }
            }
            
            // Divider
            item(key = "divider1") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Connection Status
            item(key = "connection") {
                TVConnectionStatusCard(
                    isConnected = uiState.isConnected,
                    deviceName = uiState.deviceName,
                    onScanClick = { showDeviceDialog = true },
                    onDisconnectClick = onDisconnect
                )
            }
            
            if (uiState.isConnected) {
                // Divider
                item(key = "divider2") {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
                
                // Mode Selector
                item(key = "mode") {
                    TVModeSelector(
                        selectedMode = uiState.controlMode,
                        onModeSelected = { mode ->
                            val previousMode = uiState.controlMode
                            
                            // Stop screen sync when leaving SCREEN_SYNC mode
                            if (previousMode == LedViewModel.ControlMode.SCREEN_SYNC && 
                                mode != LedViewModel.ControlMode.SCREEN_SYNC &&
                                uiState.isScreenSyncActive) {
                                onStopScreenSync()
                            }
                            
                            // Stop effects when entering STATIC_COLOR or SCREEN_SYNC
                            if (mode == LedViewModel.ControlMode.STATIC_COLOR || 
                                mode == LedViewModel.ControlMode.SCREEN_SYNC) {
                                if (previousMode == LedViewModel.ControlMode.EFFECTS) {
                                    onStopEffect()
                                }
                            }
                            
                            // Stop screen sync when entering EFFECTS mode
                            if (mode == LedViewModel.ControlMode.EFFECTS && 
                                previousMode == LedViewModel.ControlMode.SCREEN_SYNC &&
                                uiState.isScreenSyncActive) {
                                onStopScreenSync()
                            }
                            
                            viewModel.setControlMode(mode)
                        }
                    )
                }
                
                // Brightness (not shown in Settings mode)
                if (uiState.controlMode != LedViewModel.ControlMode.SETTINGS) {
                    item(key = "brightness") {
                        TVBrightnessControl(
                            brightness = uiState.brightness,
                            onBrightnessChange = { level ->
                                viewModel.setBrightness(level)
                                onSetBrightness(level)
                            }
                        )
                    }
                    
                    // Divider
                    item(key = "divider3") {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
                
                // Mode content
                when (uiState.controlMode) {
                    LedViewModel.ControlMode.SCREEN_SYNC -> {
                        item(key = "screen_sync") {
                            TVScreenSyncControl(
                                isActive = uiState.isScreenSyncActive,
                                isAudioSyncEnabled = uiState.audioSyncEnabled,
                                onStartClick = {
                                    onStopEffect() // Stop any running effect first
                                    onStartScreenSync()
                                },
                                onStopClick = onStopScreenSync,
                                onAudioSyncToggle = { viewModel.setAudioSyncEnabled(it) }
                            )
                        }
                    }
                    
                    LedViewModel.ControlMode.STATIC_COLOR -> {
                        item(key = "color") {
                            TVStaticColorPicker(
                                selectedColor = uiState.staticColor,
                                onColorSelected = { color ->
                                    onStopEffect() // Stop any running effect first
                                    viewModel.setStaticColor(color)
                                    onSetColor(color)
                                }
                            )
                        }
                    }
                    
                    LedViewModel.ControlMode.EFFECTS -> {
                        item(key = "effects") {
                            TVEffectsControl(
                                selectedEffect = uiState.selectedEffect,
                                effectSpeed = uiState.effectSpeed,
                                onEffectSelected = { effect ->
                                    viewModel.setSelectedEffect(effect)
                                    onStartEffect(effect, uiState.effectSpeed)
                                },
                                onSpeedChanged = { speed ->
                                    viewModel.setEffectSpeed(speed)
                                    onStartEffect(uiState.selectedEffect, speed)
                                }
                            )
                        }
                    }
                    
                    LedViewModel.ControlMode.SETTINGS -> {
                        item(key = "settings") {
                            TVSettingsControl(
                                isPowerOn = uiState.isPowerOn,
                                ledMode = uiState.ledMode,
                                selectedNativeEffect = uiState.selectedNativeEffect,
                                nativeEffectSpeed = uiState.nativeEffectSpeed,
                                temperature = uiState.temperature,
                                grayscaleLevel = uiState.grayscaleLevel,
                                rgbPinOrder = uiState.rgbPinOrder,
                                dynamicSensitivity = uiState.dynamicSensitivity,
                                onPowerToggle = { isOn ->
                                    viewModel.setPowerOn(isOn)
                                    onPowerToggle(isOn)
                                },
                                onModeChange = { mode ->
                                    viewModel.setLedMode(mode)
                                    onLedModeChange(mode)
                                },
                                onNativeEffectSelected = { effect ->
                                    viewModel.setSelectedNativeEffect(effect)
                                    onNativeEffectSelected(effect)
                                },
                                onEffectSpeedChange = { speed ->
                                    viewModel.setNativeEffectSpeed(speed)
                                    onNativeEffectSpeedChange(speed)
                                },
                                onTemperatureChange = { temp ->
                                    viewModel.setTemperature(temp)
                                    onTemperatureChange(temp)
                                },
                                onGrayscaleChange = { level ->
                                    viewModel.setGrayscaleLevel(level)
                                    onGrayscaleChange(level)
                                },
                                onRgbOrderChange = { order ->
                                    viewModel.setRgbPinOrder(order)
                                    onRgbOrderChange(order)
                                },
                                onDynamicSensitivityChange = { sensitivity ->
                                    viewModel.setDynamicSensitivity(sensitivity)
                                    onDynamicSensitivityChange(sensitivity)
                                }
                            )
                        }
                    }
                }
                
                // Bottom spacer
                item(key = "spacer") {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
    
    if (showDeviceDialog) {
        TVDeviceScannerDialog(
            devices = scannedDevices,
            onDeviceSelected = { device ->
                onConnectDevice(device)
                showDeviceDialog = false
            },
            onDismiss = { showDeviceDialog = false },
            onScanClick = onScanDevices
        )
    }
}

// Legacy compatibility wrappers
@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    deviceName: String?,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit
) = TVConnectionStatusCard(isConnected, deviceName, onScanClick, onDisconnectClick)

@Composable
fun ModeSelector(
    selectedMode: LedViewModel.ControlMode,
    onModeSelected: (LedViewModel.ControlMode) -> Unit
) = TVModeSelector(selectedMode, onModeSelected)

@Composable
fun BrightnessControl(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit
) = TVBrightnessControl(brightness, onBrightnessChange)

@Composable
fun ScreenSyncControl(
    isActive: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) = TVScreenSyncControl(isActive, false, onStartClick, onStopClick, {})

@Composable
fun StaticColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) = TVStaticColorPicker(selectedColor, onColorSelected)

@Composable
fun EffectsControl(
    selectedEffect: LedEffect,
    effectSpeed: Float,
    onEffectSelected: (LedEffect) -> Unit,
    onSpeedChanged: (Float) -> Unit
) = TVEffectsControl(selectedEffect, effectSpeed, onEffectSelected, onSpeedChanged)

@Composable
fun AutoStartSetting(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Auto-start",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Start sync on launch",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
            MinimalSwitch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun DeviceScannerDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
    onScanClick: () -> Unit
) = TVDeviceScannerDialog(devices, onDeviceSelected, onDismiss, onScanClick)
