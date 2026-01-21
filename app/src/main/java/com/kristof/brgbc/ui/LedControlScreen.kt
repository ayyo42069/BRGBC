package com.kristof.brgbc.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kristof.brgbc.ble.LedEffect
import com.kristof.brgbc.viewmodel.LedViewModel

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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeviceDialog by remember { mutableStateOf(false) }
    
    // TV-optimized layout with larger elements and scrolling
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title
        item {
            Text(
                text = "LED Control",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        // Connection Status Card
        item {
            TVConnectionStatusCard(
                isConnected = uiState.isConnected,
                deviceName = uiState.deviceName,
                onScanClick = { showDeviceDialog = true },
                onDisconnectClick = onDisconnect
            )
        }
        
        // Mode Selector
        if (uiState.isConnected) {
            item {
                TVModeSelector(
                    selectedMode = uiState.controlMode,
                    onModeSelected = { mode ->
                        viewModel.setControlMode(mode)
                        // Stop any active operations when switching modes
                        onStopEffect()
                        if (uiState.isScreenSyncActive) {
                            onStopScreenSync()
                        }
                    }
                )
            }
            
            // Brightness Control
            item {
                TVBrightnessControl(
                    brightness = uiState.brightness,
                    onBrightnessChange = { level ->
                        viewModel.setBrightness(level)
                        onSetBrightness(level)
                    }
                )
            }
            
            // Mode-specific content
            when (uiState.controlMode) {
                LedViewModel.ControlMode.SCREEN_SYNC -> {
                    item {
                        TVScreenSyncControl(
                            isActive = uiState.isScreenSyncActive,
                            onStartClick = onStartScreenSync,
                            onStopClick = onStopScreenSync
                        )
                    }
                }
                
                LedViewModel.ControlMode.STATIC_COLOR -> {
                    item {
                        TVStaticColorPicker(
                            selectedColor = uiState.staticColor,
                            onColorSelected = { color ->
                                viewModel.setStaticColor(color)
                                onSetColor(color)
                            }
                        )
                    }
                }
                
                LedViewModel.ControlMode.EFFECTS -> {
                    item {
                        TVEffectsControl(
                            selectedEffect = uiState.selectedEffect,
                            effectSpeed = uiState.effectSpeed,
                            onEffectSelected = { effect ->
                                viewModel.setSelectedEffect(effect)
                                onStartEffect(effect, uiState.effectSpeed)
                            },
                            onSpeedChanged = { speed ->
                                viewModel.setEffectSpeed(speed)
                                // Restart effect with new speed
                                onStartEffect(uiState.selectedEffect, speed)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Device Scanner Dialog
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

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    deviceName: String?,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF1B5E20) else Color(0xFF424242)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (deviceName != null) {
                    Text(
                        text = deviceName,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Button(
                onClick = if (isConnected) onDisconnectClick else onScanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFD32F2F) else Color(0xFF2196F3)
                )
            ) {
                Text(if (isConnected) "Disconnect" else "Scan & Connect")
            }
        }
    }
}

@Composable
fun ModeSelector(
    selectedMode: LedViewModel.ControlMode,
    onModeSelected: (LedViewModel.ControlMode) -> Unit
) {
    Column {
        Text(
            text = "Mode",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LedViewModel.ControlMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                Button(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF616161)
                    )
                ) {
                    Text(
                        text = when (mode) {
                            LedViewModel.ControlMode.SCREEN_SYNC -> "Screen"
                            LedViewModel.ControlMode.STATIC_COLOR -> "Color"
                            LedViewModel.ControlMode.EFFECTS -> "Effects"
                        },
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BrightnessControl(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Brightness: $brightness%",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Slider(
            value = brightness.toFloat(),
            onValueChange = { onBrightnessChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFC107),
                activeTrackColor = Color(0xFFFFC107)
            )
        )
    }
}

@Composable
fun ScreenSyncControl(
    isActive: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Screen Sync",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = if (isActive) "LED is syncing with screen colors at 10 FPS" 
                       else "Capture screen colors and sync to LED strip",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Button(
                onClick = if (isActive) onStopClick else onStartClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                )
            ) {
                Text(if (isActive) "Stop Screen Sync" else "Start Screen Sync")
            }
        }
    }
}

@Composable
fun StaticColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Static Color",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Color Preview
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.CenterHorizontally)
            )
            
            // Preset Colors
            Text(
                text = "Presets",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            val presetColors = listOf(
                Color.White to "White",
                Color.Red to "Red",
                Color.Green to "Green",
                Color.Blue to "Blue",
                Color.Yellow to "Yellow",
                Color.Cyan to "Cyan",
                Color.Magenta to "Magenta",
                Color(0xFFFFA500) to "Orange",
                Color(0xFF800080) to "Purple",
                Color(0xFFFFC0CB) to "Pink"
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presetColors.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (color, name) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .border(
                                        width = if (color == selectedColor) 3.dp else 1.dp,
                                        color = if (color == selectedColor) Color(0xFFFFC107) else Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onColorSelected(color) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EffectsControl(
    selectedEffect: LedEffect,
    effectSpeed: Float,
    onEffectSelected: (LedEffect) -> Unit,
    onSpeedChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Effects",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Effect List
            LedEffect.entries.forEach { effect ->
                val isSelected = effect == selectedEffect
                Button(
                    onClick = { onEffectSelected(effect) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF616161)
                    )
                ) {
                    Text(effect.displayName)
                }
            }
            
            // Speed Control
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Effect Speed: ${String.format("%.2f", effectSpeed)}s",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Slider(
                value = effectSpeed,
                onValueChange = onSpeedChanged,
                valueRange = 0.01f..0.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9C27B0),
                    activeTrackColor = Color(0xFF9C27B0)
                )
            )
        }
    }
}

@Composable
fun AutoStartSetting(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto-start on launch",
                fontSize = 16.sp,
                color = Color.White
            )
            
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF81C784)
                )
            )
        }
    }
}

@Composable
fun DeviceScannerDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
    onScanClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth Devices") },
        text = {
            Column {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan for Devices")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (devices.isEmpty()) {
                    Text("No devices found. Tap 'Scan for Devices' to search.")
                } else {
                    devices.forEach { device ->
                        Button(
                            onClick = { onDeviceSelected(device) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(device.name ?: "Unknown Device")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
