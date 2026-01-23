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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontFamily
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
                        LedViewModel.ControlMode.SETTINGS -> "Settings"
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
            
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
            
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
// STATIC COLOR PICKER - TV Remote Friendly
// ============================================================
@Composable
fun TVStaticColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    // Convert initial color to HSV
    val initialHsv = remember(selectedColor) {
        val r = (selectedColor.red * 255).toInt()
        val g = (selectedColor.green * 255).toInt()
        val b = (selectedColor.blue * 255).toInt()
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        hsv
    }
    
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    
    // Calculate current color from HSV
    val currentColor = remember(hue, saturation, brightness) {
        val androidColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
        Color(androidColor)
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Instantly apply color when HSV values change
        LaunchedEffect(hue, saturation, brightness) {
            onColorSelected(currentColor)
        }
        
        // Header with current color preview
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current color preview (large)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(3.dp, TextPrimary, RoundedCornerShape(16.dp))
                )
                
                Column {
                    Text(
                        text = "Live Color",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "RGB(${(currentColor.red * 255).toInt()}, ${(currentColor.green * 255).toInt()}, ${(currentColor.blue * 255).toInt()})",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    Text(
                        text = "Changes apply instantly",
                        fontSize = 12.sp,
                        color = AccentBlue
                    )
                }
            }
        }
        
        // Hue Control (Color selection)
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Color (Hue)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${hue.toInt()}°",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                // Hue gradient bar (visual only)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = (0..360 step 30).map { h ->
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(h.toFloat(), 1f, 1f)))
                                }
                            )
                        )
                ) {
                    // Position indicator
                    val indicatorPosition = (hue / 360f) * maxWidth.value
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .offset(x = indicatorPosition.dp - 3.dp)
                            .background(Color.White)
                            .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
                    )
                }
                
                // Hue buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorControlButton(
                        onClick = { hue = ((hue - 30f + 360f) % 360f) },
                        text = "◀ -30°",
                        modifier = Modifier.weight(1f)
                    )
                    ColorControlButton(
                        onClick = { hue = ((hue - 10f + 360f) % 360f) },
                        text = "◀ -10°",
                        modifier = Modifier.weight(1f)
                    )
                    ColorControlButton(
                        onClick = { hue = ((hue + 10f) % 360f) },
                        text = "+10° ▶",
                        modifier = Modifier.weight(1f)
                    )
                    ColorControlButton(
                        onClick = { hue = ((hue + 30f) % 360f) },
                        text = "+30° ▶",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Saturation Control
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saturation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${(saturation * 100).toInt()}%",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                // Saturation gradient bar
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, brightness)))
                                )
                            )
                        )
                ) {
                    // Position indicator
                    val indicatorPosition = saturation * maxWidth.value
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .offset(x = indicatorPosition.dp - 3.dp)
                            .background(Color.Black)
                            .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                    )
                }
                
                // Saturation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorControlButton(
                        onClick = { saturation = (saturation - 0.25f).coerceAtLeast(0f) },
                        text = "◀ -25%",
                        modifier = Modifier.weight(1f)
                    )
                    ColorControlButton(
                        onClick = { saturation = (saturation - 0.1f).coerceAtLeast(0f) },
                        text = "◀ -10%",
                        modifier = Modifier.weight(1f)
                    )
                    ColorControlButton(
                        onClick = { saturation = (saturation + 0.1f).coerceAtMost(1f) },
                        text = "+10% ▶",
                        modifier = Modifier.weight(1f)
                    )
                    ColorControlButton(
                        onClick = { saturation = (saturation + 0.25f).coerceAtMost(1f) },
                        text = "+25% ▶",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Quick Presets
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Presets",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                
                val presetColors = listOf(
                    Color.White,
                    Color.Red,
                    Color(0xFFFFA500),
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color(0xFF800080),
                    Color(0xFFFFC0CB)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.take(5).forEach { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = false,
                            onClick = {
                                val hsv = FloatArray(3)
                                android.graphics.Color.RGBToHSV(
                                    (color.red * 255).toInt(),
                                    (color.green * 255).toInt(),
                                    (color.blue * 255).toInt(),
                                    hsv
                                )
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                                onColorSelected(color)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.drop(5).forEach { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = false,
                            onClick = {
                                val hsv = FloatArray(3)
                                android.graphics.Color.RGBToHSV(
                                    (color.red * 255).toInt(),
                                    (color.green * 255).toInt(),
                                    (color.blue * 255).toInt(),
                                    hsv
                                )
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                                onColorSelected(color)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Focusable button for color control - works with TV remote
 */
@Composable
fun ColorControlButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> AccentBlue
            else -> CardBg
        },
        animationSpec = tween(100),
        label = "bg"
    )
    
    val borderColor = when {
        isFocused -> TextPrimary
        else -> BorderColor
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
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
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}
// ============================================================
// EFFECTS CONTROL - Premium with Categories
// ============================================================
@Composable
fun TVEffectsControl(
    selectedEffect: LedEffect,
    effectSpeed: Float,
    onEffectSelected: (LedEffect) -> Unit,
    onSpeedChanged: (Float) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(selectedEffect.category) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with current effect
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Effect",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = selectedEffect.icon,
                            fontSize = 24.sp
                        )
                        Text(
                            text = selectedEffect.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        
        // Category selector
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LedEffect.Category.entries.forEach { category ->
                        EffectTile(
                            onClick = { selectedCategory = category },
                            text = category.displayName,
                            isSelected = category == selectedCategory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Effects grid for selected category
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${selectedCategory.displayName} Effects",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                
                val effectsInCategory = LedEffect.getByCategory(selectedCategory)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    effectsInCategory.chunked(4).forEach { rowEffects ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEffects.forEach { effect ->
                                EffectTileWithIcon(
                                    onClick = { onEffectSelected(effect) },
                                    text = effect.displayName,
                                    icon = effect.icon,
                                    isSelected = effect == selectedEffect,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(4 - rowEffects.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        
        // Speed control with progress bar
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            .fillMaxWidth(1f - (effectSpeed / 0.5f))  // Inverse: higher speed = shorter bar
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentBlue)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinimalButton(
                        onClick = { onSpeedChanged((effectSpeed + 0.05f).coerceAtMost(0.5f)) },
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
                        onClick = { onSpeedChanged((effectSpeed - 0.05f).coerceAtLeast(0.01f)) },
                        text = "Faster",
                        small = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun EffectTileWithIcon(
    onClick: () -> Unit,
    text: String,
    icon: String,
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
            .height(64.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
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
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
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

// ============================================================
// SETTINGS CONTROL COMPONENTS
// ============================================================

@Composable
fun TVSettingsControl(
    isPowerOn: Boolean,
    ledMode: com.kristof.brgbc.ble.LedMode,
    selectedNativeEffect: com.kristof.brgbc.ble.NativeEffect,
    nativeEffectSpeed: Int,
    temperature: Int,
    grayscaleLevel: Int,
    rgbPinOrder: com.kristof.brgbc.ble.RgbPinOrder,
    dynamicSensitivity: Int,
    onPowerToggle: (Boolean) -> Unit,
    onModeChange: (com.kristof.brgbc.ble.LedMode) -> Unit,
    onNativeEffectSelected: (com.kristof.brgbc.ble.NativeEffect) -> Unit,
    onEffectSpeedChange: (Int) -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onGrayscaleChange: (Int) -> Unit,
    onRgbOrderChange: (com.kristof.brgbc.ble.RgbPinOrder) -> Unit,
    onDynamicSensitivityChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Power Control
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Power",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isPowerOn) "LED strip is ON" else "LED strip is OFF",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
                
                MinimalButton(
                    onClick = { onPowerToggle(!isPowerOn) },
                    text = if (isPowerOn) "Turn Off" else "Turn On",
                    filled = !isPowerOn
                )
            }
        }
        
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        
        // LED Mode Selector
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "LED Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.kristof.brgbc.ble.LedMode.entries.chunked(3).forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowModes.forEach { mode ->
                                EffectTile(
                                    onClick = { onModeChange(mode) },
                                    text = mode.displayName,
                                    isSelected = mode == ledMode,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowModes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        
        // Mode-specific controls
        when (ledMode) {
            com.kristof.brgbc.ble.LedMode.RGB -> {
                // RGB mode is handled by the Static Color picker in the main UI
                SectionCard {
                    Column {
                        Text(
                            text = "RGB Mode",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Use the Color mode tab to set RGB colors",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    }
                }
            }
            
            com.kristof.brgbc.ble.LedMode.GRAYSCALE -> {
                TVSliderControl(
                    title = "Grayscale Level",
                    value = grayscaleLevel,
                    onValueChange = onGrayscaleChange,
                    minValue = 0,
                    maxValue = 100,
                    minLabel = "Black",
                    maxLabel = "White"
                )
            }
            
            com.kristof.brgbc.ble.LedMode.TEMPERATURE -> {
                TVSliderControl(
                    title = "Color Temperature",
                    value = temperature,
                    onValueChange = onTemperatureChange,
                    minValue = 0,
                    maxValue = 100,
                    minLabel = "Cold",
                    maxLabel = "Warm"
                )
            }
            
            com.kristof.brgbc.ble.LedMode.EFFECT -> {
                TVNativeEffectsControl(
                    selectedEffect = selectedNativeEffect,
                    effectSpeed = nativeEffectSpeed,
                    onEffectSelected = onNativeEffectSelected,
                    onSpeedChange = onEffectSpeedChange
                )
            }
            
            com.kristof.brgbc.ble.LedMode.DYNAMIC -> {
                TVSliderControl(
                    title = "Microphone Sensitivity",
                    value = dynamicSensitivity,
                    onValueChange = onDynamicSensitivityChange,
                    minValue = 0,
                    maxValue = 100,
                    minLabel = "Low",
                    maxLabel = "High"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SectionCard {
                    Text(
                        text = "⚠️ Dynamic mode requires on-board microphone. May not work on all controllers.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        }
        
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        
        // RGB Pin Order Configuration
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "RGB Pin Order",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fix color mapping if colors are wrong",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.kristof.brgbc.ble.RgbPinOrder.entries.take(3).forEach { order ->
                        EffectTile(
                            onClick = { onRgbOrderChange(order) },
                            text = order.displayName,
                            isSelected = order == rgbPinOrder,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.kristof.brgbc.ble.RgbPinOrder.entries.drop(3).forEach { order ->
                        EffectTile(
                            onClick = { onRgbOrderChange(order) },
                            text = order.displayName,
                            isSelected = order == rgbPinOrder,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TVNativeEffectsControl(
    selectedEffect: com.kristof.brgbc.ble.NativeEffect,
    effectSpeed: Int,
    onEffectSelected: (com.kristof.brgbc.ble.NativeEffect) -> Unit,
    onSpeedChange: (Int) -> Unit
) {
    var selectedCategory by remember { 
        mutableStateOf(selectedEffect.category) 
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Speed control
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Effect Speed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "$effectSpeed%",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
                
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
                            .fillMaxWidth(effectSpeed / 100f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentBlue)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinimalButton(
                        onClick = { onSpeedChange((effectSpeed - 10).coerceAtLeast(0)) },
                        text = "Slower",
                        small = true,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalButton(
                        onClick = { onSpeedChange(50) },
                        text = "Reset",
                        small = true,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalButton(
                        onClick = { onSpeedChange((effectSpeed + 10).coerceAtMost(100)) },
                        text = "Faster",
                        small = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Category selector
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Effect Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.kristof.brgbc.ble.NativeEffect.Category.entries.forEach { category ->
                        EffectTile(
                            onClick = { selectedCategory = category },
                            text = category.displayName.replace(" Effects", "").replace("Colors", ""),
                            isSelected = category == selectedCategory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Effects grid for selected category
        SectionCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${selectedCategory.displayName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                
                val effectsInCategory = com.kristof.brgbc.ble.NativeEffect.getByCategory(selectedCategory)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    effectsInCategory.chunked(4).forEach { rowEffects ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEffects.forEach { effect ->
                                EffectTile(
                                    onClick = { onEffectSelected(effect) },
                                    text = effect.displayName.replace("Gradient ", "").replace("Blink ", "").replace("Jump ", ""),
                                    isSelected = effect == selectedEffect,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(4 - rowEffects.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TVSliderControl(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 0,
    maxValue: Int = 100,
    minLabel: String = "$minValue",
    maxLabel: String = "$maxValue"
) {
    SectionCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "$value%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            
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
                        .fillMaxWidth((value - minValue).toFloat() / (maxValue - minValue))
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextPrimary)
                )
            }
            
            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = minLabel, fontSize = 12.sp, color = TextMuted)
                Text(text = maxLabel, fontSize = 12.sp, color = TextMuted)
            }
            
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalButton(
                    onClick = { onValueChange(minValue) },
                    text = "Min",
                    small = true,
                    modifier = Modifier.weight(1f)
                )
                MinimalButton(
                    onClick = { onValueChange((value - 25).coerceAtLeast(minValue)) },
                    text = "-25",
                    small = true,
                    modifier = Modifier.weight(1f)
                )
                MinimalButton(
                    onClick = { onValueChange((value + 25).coerceAtMost(maxValue)) },
                    text = "+25",
                    small = true,
                    modifier = Modifier.weight(1f)
                )
                MinimalButton(
                    onClick = { onValueChange(maxValue) },
                    text = "Max",
                    small = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

