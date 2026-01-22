package com.kristof.brgbc.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kristof.brgbc.ble.LedEffect
import com.kristof.brgbc.ui.theme.*
import com.kristof.brgbc.viewmodel.LedViewModel

/**
 * Minimalist TV components - clean, modern design
 * Inspired by kristof.best website aesthetics
 */

private val ButtonRadius = 8.dp
private val CardRadius = 4.dp

// ============================================================
// CONNECTION STATUS
// ============================================================
@Composable
fun TVConnectionStatusCard(
    isConnected: Boolean,
    deviceName: String?,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) StatusGreen else StatusRed)
            )
            
            Column {
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (deviceName != null) {
                    Text(
                        text = deviceName,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }
        }
        
        MinimalButton(
            onClick = if (isConnected) onDisconnectClick else onScanClick,
            text = if (isConnected) "Disconnect" else "Scan & Connect",
            outlined = !isConnected
        )
    }
}

// ============================================================
// MODE SELECTOR
// ============================================================
@Composable
fun TVModeSelector(
    selectedMode: LedViewModel.ControlMode,
    onModeSelected: (LedViewModel.ControlMode) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LedViewModel.ControlMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                ModeTab(
                    onClick = { onModeSelected(mode) },
                    text = when (mode) {
                        LedViewModel.ControlMode.SCREEN_SYNC -> "Screen Sync"
                        LedViewModel.ControlMode.STATIC_COLOR -> "Color"
                        LedViewModel.ControlMode.EFFECTS -> "Effects"
                    },
                    isSelected = isSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================
// BRIGHTNESS CONTROL
// ============================================================
@Composable
fun TVBrightnessControl(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Brightness",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "$brightness%",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BorderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(brightness / 100f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextPrimary)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MinimalButton(
                onClick = { onBrightnessChange(0) },
                text = "Off",
                small = true,
                modifier = Modifier.weight(1f)
            )
            MinimalButton(
                onClick = { onBrightnessChange((brightness - 25).coerceAtLeast(0)) },
                text = "-25",
                small = true,
                modifier = Modifier.weight(1f)
            )
            MinimalButton(
                onClick = { onBrightnessChange((brightness + 25).coerceAtMost(100)) },
                text = "+25",
                small = true,
                modifier = Modifier.weight(1f)
            )
            MinimalButton(
                onClick = { onBrightnessChange(100) },
                text = "Max",
                small = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================
// SCREEN SYNC CONTROL
// ============================================================
@Composable
fun TVScreenSyncControl(
    isActive: Boolean,
    isAudioSyncEnabled: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onAudioSyncToggle: (Boolean) -> Unit
) {
    SectionCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Screen Sync",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isActive) "Syncing at 30 FPS" else "Match LED to screen",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
                
                MinimalButton(
                    onClick = if (isActive) onStopClick else onStartClick,
                    text = if (isActive) "Stop" else "Start",
                    filled = !isActive
                )
            }
            
            Divider(color = BorderColor, thickness = 1.dp)
            
            // Audio sync toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Audio Reactivity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Pulse brightness with audio",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                
                MinimalSwitch(
                    checked = isAudioSyncEnabled,
                    onCheckedChange = onAudioSyncToggle
                )
            }
        }
    }
}

// ============================================================
// STATIC COLOR PICKER
// ============================================================
@Composable
fun TVStaticColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    SectionCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current color preview
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(CardRadius))
                        .background(selectedColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(CardRadius))
                )
                
                Column {
                    Text(
                        text = "Static Color",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Select a color for the LED strip",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }
            
            val presetColors = listOf(
                Color.White,
                Color.Red,
                Color.Green,
                Color.Blue,
                Color.Yellow,
                Color.Cyan,
                Color.Magenta,
                Color(0xFFFFA500),
                Color(0xFF800080),
                Color(0xFFFFC0CB)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                presetColors.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { color ->
                            ColorSwatch(
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

// ============================================================
// EFFECTS CONTROL
// ============================================================
@Composable
fun TVEffectsControl(
    selectedEffect: LedEffect,
    effectSpeed: Float,
    onEffectSelected: (LedEffect) -> Unit,
    onSpeedChanged: (Float) -> Unit
) {
    SectionCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Effects",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            // Effect grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LedEffect.entries.chunked(3).forEach { rowEffects ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEffects.forEach { effect ->
                            EffectTile(
                                onClick = { onEffectSelected(effect) },
                                text = effect.displayName,
                                isSelected = effect == selectedEffect,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowEffects.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Divider(color = BorderColor, thickness = 1.dp)
            
            // Speed control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = String.format("%.2fs", effectSpeed),
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalButton(
                    onClick = { onSpeedChanged((effectSpeed - 0.05f).coerceAtLeast(0.01f)) },
                    text = "Slower",
                    small = true,
                    modifier = Modifier.weight(1f)
                )
                MinimalButton(
                    onClick = { onSpeedChanged(0.1f) },
                    text = "Reset",
                    small = true,
                    modifier = Modifier.weight(1f)
                )
                MinimalButton(
                    onClick = { onSpeedChanged((effectSpeed + 0.05f).coerceAtMost(0.5f)) },
                    text = "Faster",
                    small = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================
// DEVICE SCANNER DIALOG
// ============================================================
@Composable
fun TVDeviceScannerDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
    onScanClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(CardRadius))
                .background(OffBlack)
                .border(1.dp, BorderColor, RoundedCornerShape(CardRadius))
                .padding(32.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Bluetooth Devices",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                MinimalButton(
                    onClick = onScanClick,
                    text = "Scan for Devices",
                    filled = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (devices.isEmpty()) {
                    Text(
                        text = "No devices found. Tap scan to search.",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = "Found ${devices.size} device(s)",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        devices.forEach { device ->
                            DeviceRow(
                                deviceName = device.name ?: "Unknown Device",
                                onClick = { onDeviceSelected(device) }
                            )
                        }
                    }
                }
                
                MinimalButton(
                    onClick = onDismiss,
                    text = "Close",
                    outlined = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================
// BASE COMPONENTS
// ============================================================

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(IntrinsicSize.Max)
                .background(AccentBlue)
        )
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            content = content
        )
    }
}

@Composable
fun MinimalButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    outlined: Boolean = false,
    small: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    val backgroundColor = when {
        filled -> TextPrimary
        isFocused -> CardBg
        else -> Color.Transparent
    }
    
    val textColor = when {
        filled -> PureBlack
        else -> TextPrimary
    }
    
    val borderColor = when {
        isFocused -> TextPrimary
        outlined -> BorderColor
        else -> Color.Transparent
    }
    
    val height = if (small) 48.dp else 56.dp
    val fontSize = if (small) 14.sp else 16.sp
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .scale(scale)
            .border(1.dp, borderColor, RoundedCornerShape(ButtonRadius))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else false
            },
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(ButtonRadius),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (filled || outlined) {
                Text(
                    text = "→",
                    fontSize = fontSize,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ModeTab(
    onClick: () -> Unit,
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> TextPrimary
            isFocused -> CardBg
            else -> Color.Transparent
        },
        animationSpec = tween(100),
        label = "bg"
    )
    
    val textColor = if (isSelected) PureBlack else TextPrimary
    val borderColor = if (isFocused && !isSelected) TextPrimary else BorderColor
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .border(1.dp, borderColor, RoundedCornerShape(ButtonRadius))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else false
            },
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(ButtonRadius)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    val borderWidth = when {
        isSelected -> 3.dp
        isFocused -> 2.dp
        else -> 1.dp
    }
    
    val borderColor = when {
        isSelected -> TextPrimary
        isFocused -> TextPrimary
        else -> BorderColor
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(CardRadius))
            .background(color)
            .border(borderWidth, borderColor, RoundedCornerShape(CardRadius))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else false
            }
    )
}

@Composable
fun EffectTile(
    onClick: () -> Unit,
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> AccentBlue
            isFocused -> CardBg
            else -> Color.Transparent
        },
        animationSpec = tween(100),
        label = "bg"
    )
    
    val borderColor = when {
        isFocused && !isSelected -> TextPrimary
        isSelected -> Color.Transparent
        else -> BorderColor
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .border(1.dp, borderColor, RoundedCornerShape(ButtonRadius))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else false
            },
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(ButtonRadius)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DeviceRow(
    deviceName: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor = if (isFocused) CardBg else Color.Transparent
    val borderColor = if (isFocused) TextPrimary else BorderColor
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ButtonRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(ButtonRadius))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else false
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = deviceName,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = "→",
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun MinimalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = TextPrimary,
            checkedTrackColor = AccentBlue,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = BorderColor
        )
    )
}
