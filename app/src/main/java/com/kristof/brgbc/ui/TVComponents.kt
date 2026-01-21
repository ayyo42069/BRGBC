package com.kristof.brgbc.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kristof.brgbc.ble.LedEffect
import com.kristof.brgbc.viewmodel.LedViewModel

/**
 * TV-optimized components with larger touch targets and better focus handling
 */

@Composable
fun TVConnectionStatusCard(
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
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (deviceName != null) {
                    Text(
                        text = deviceName,
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            TVButton(
                onClick = if (isConnected) onDisconnectClick else onScanClick,
                text = if (isConnected) "Disconnect" else "Scan & Connect",
                backgroundColor = if (isConnected) Color(0xFFD32F2F) else Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun TVModeSelector(
    selectedMode: LedViewModel.ControlMode,
    onModeSelected: (LedViewModel.ControlMode) -> Unit
) {
    Column {
        Text(
            text = "Mode",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LedViewModel.ControlMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                TVButton(
                    onClick = { onModeSelected(mode) },
                    text = when (mode) {
                        LedViewModel.ControlMode.SCREEN_SYNC -> "Screen Sync"
                        LedViewModel.ControlMode.STATIC_COLOR -> "Static Color"
                        LedViewModel.ControlMode.EFFECTS -> "Effects"
                    },
                    backgroundColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF616161),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TVBrightnessControl(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Brightness: $brightness%",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TVButton(
                onClick = { onBrightnessChange((brightness - 10).coerceAtLeast(0)) },
                text = "- 10%",
                modifier = Modifier.weight(1f)
            )
            
            TVButton(
                onClick = { onBrightnessChange((brightness - 25).coerceAtLeast(0)) },
                text = "- 25%",
                modifier = Modifier.weight(1f)
            )
            
            TVButton(
                onClick = { onBrightnessChange((brightness + 25).coerceAtMost(100)) },
                text = "+ 25%",
                modifier = Modifier.weight(1f)
            )
            
            TVButton(
                onClick = { onBrightnessChange((brightness + 10).coerceAtMost(100)) },
                text = "+ 10%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TVScreenSyncControl(
    isActive: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Screen Sync",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = if (isActive) "LED is syncing with screen colors at 10 FPS" 
                       else "Capture screen colors and sync to LED strip",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            TVButton(
                onClick = if (isActive) onStopClick else onStartClick,
                text = if (isActive) "Stop Screen Sync" else "Start Screen Sync",
                backgroundColor = if (isActive) Color(0xFFD32F2F) else Color(0xFF4CAF50),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TVStaticColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Static Color",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Color Preview
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(4.dp, Color.White, CircleShape)
                    .align(Alignment.CenterHorizontally)
            )
            
            // Preset Colors
            Text(
                text = "Select Color",
                fontSize = 24.sp,
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
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                presetColors.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { (color, name) ->
                            TVColorButton(
                                color = color,
                                isSelected = color == selectedColor,
                                onClick = { onColorSelected(color) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TVEffectsControl(
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
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Effects",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Effect List in a grid for better TV navigation
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LedEffect.entries.chunked(3).forEach { rowEffects ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowEffects.forEach { effect ->
                            val isSelected = effect == selectedEffect
                            TVButton(
                                onClick = { onEffectSelected(effect) },
                                text = effect.displayName,
                                backgroundColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF616161),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if less than 3 items
                        repeat(3 - rowEffects.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            // Speed Control
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Effect Speed",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TVButton(
                    onClick = { onSpeedChanged((effectSpeed - 0.05f).coerceAtLeast(0.01f)) },
                    text = "Slower",
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = String.format("%.2fs", effectSpeed),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp)
                )
                
                TVButton(
                    onClick = { onSpeedChanged((effectSpeed + 0.05f).coerceAtMost(0.5f)) },
                    text = "Faster",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Base TV Button Component with focus handling
@Composable
fun TVButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF2196F3)
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .then(
                if (isFocused) Modifier.border(4.dp, Color.White, RoundedCornerShape(8.dp))
                else Modifier
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TVColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .border(
                width = if (isSelected) 5.dp else if (isFocused) 4.dp else 2.dp,
                color = if (isSelected) Color(0xFFFFC107) else if (isFocused) Color.White else Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    )
}

@Composable
fun TVDeviceScannerDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
    onScanClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Bluetooth Devices",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                TVButton(
                    onClick = onScanClick,
                    text = "Scan for Devices",
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (devices.isEmpty()) {
                    Text(
                        text = "No devices found. Tap 'Scan for Devices' to search.",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Found ${devices.size} device(s):",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    
                    devices.forEach { device ->
                        TVButton(
                            onClick = { onDeviceSelected(device) },
                            text = device.name ?: "Unknown Device",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                TVButton(
                    onClick = onDismiss,
                    text = "Close",
                    backgroundColor = Color(0xFF616161),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
