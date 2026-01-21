# BRGBC - LED Screen Sync for Android TV

An Android TV application that captures screen colors and synchronizes them to an ELK-BLEDOB/ELK-BLEDOM RGB LED strip via Bluetooth LE. Features screen sync mode, static color control, and 7 animated effects.

## Features

### 🎨 **Three Control Modes**

1. **Screen Sync**
   - Captures screen at 10 FPS
   - Analyzes dominant colors using grid sampling
   - Smooth color transitions to prevent jarring changes
   - Runs as background foreground service

2. **Static Color**
   - 10 preset colors (White, Red, Green, Blue, Yellow, Cyan, Magenta, Orange, Purple, Pink)
   - Visual color preview
   - Instant color changes

3. **Effects**
   - 🌈 **Rainbow**: Smooth color cycling through full spectrum
   - 💨 **Breathe**: Gentle brightness pulsing
   - 🚔 **Police**: Red/blue strobe pattern
   - 🌊 **Orbit**: Rainbow with synchronized brightness waves
   - ⚡ **Lightning**: Random lightning storm effect
   - ❤️ **Heartbeat**: Realistic heartbeat pulse
   - 🔥 **Fire**: Flickering flame simulation

### ⚙️ **Controls**

- **Brightness Slider**: 0-100%
- **Effect Speed Control**: Adjustable 0.01s - 0.5s per frame
- **Auto-start Setting**: Optional automatic activation on app launch
- **Device Scanner**: Automatic Bluetooth device discovery

## Technical Implementation

### Architecture

```
MainActivity (UI)
    ↓
LedViewModel (State Management)
    ↓
├── BleLedController (Bluetooth LE)
│   └── LedProtocol (Packet Builder)
│
└── ScreenCaptureService (10 FPS Capture)
    ├── ColorAnalyzer (Grid Sampling)
    └── ColorSmoother (Linear Interpolation)
```

### Key Components

#### **BLE Layer** (`ble/`)
- `LedProtocol.kt`: Protocol packet construction (7E...EF format)
- `BleLedController.kt`: Bluetooth LE communication, device scanning, connection management
- `LedEffect.kt`: Effect type enumeration

#### **Screen Capture** (`capture/`)
- `ScreenCaptureService.kt`: Foreground service with MediaProjection API
- `ColorAnalyzer.kt`: Extracts dominant color using 3x3 grid sampling
- `ColorSmoother.kt`: Linear interpolation for smooth transitions (20% per frame)

#### **UI** (`ui/`)
- `LedControlScreen.kt`: Complete Compose UI with mode tabs, controls, and device scanner
- Dark theme optimized for TV viewing

#### **ViewModel** (`viewmodel/`)
- `LedViewModel.kt`: Reactive state management with Kotlin Flow

## Requirements

### Hardware
- Android TV device (API 21+)
- ELK-BLEDOB or ELK-BLEDOM RGB LED strip
- Bluetooth LE support on TV

### Software
- Android Studio (latest version)
- Kotlin 1.9.22+
- Gradle 8.0+

## Setup Instructions

### 1. **Install Dependencies**

The project uses standard Android dependencies:
- Jetpack Compose (UI framework)
- Material3 (UI components)
- Lifecycle ViewModel (state management)
- Kotlinx Coroutines (async operations)

All dependencies are declared in `app/build.gradle.kts`.

### 2. **Configure Android Studio**

1. Open Android Studio
2. Select **File → Open** and choose the project directory
3. Wait for Gradle sync to complete
4. Ensure Java 11+ is configured:
   - **File → Project Structure → SDK Location**
   - Set JDK location if not auto-detected

### 3. **Build the Project**

```bash
# Via Gradle wrapper (command line)
./gradlew clean build

# Or use Android Studio
# Build → Make Project (Ctrl+F9)
```

### 4. **Deploy to Android TV**

#### **Method 1: USB Debugging**
1. Enable Developer Options on Android TV:
   - Settings → About → Build (tap 7 times)
2. Enable USB Debugging:
   - Settings → Developer Options → USB Debugging
3. Connect TV via USB
4. Click **Run** in Android Studio

#### **Method 2: Wireless Debugging (Android 11+)**
1. Enable Wireless Debugging on TV
2. In Android Studio:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```
3. Click **Run**

## Usage Guide

### First-Time Setup

1. **Launch App** on Android TV
2. **Grant Permissions** when prompted:
   - Bluetooth (for LED control)
   - Location (required for BLE scanning on Android <12)
   - Notifications (for foreground service)

### Connecting to LED Strip

1. Power on your LED strip
2. Tap **"Scan & Connect"** in the app
3. Wait for device to appear in list
4. Select your device (ELK-BLEDOB or ELK-BLEDOM)
5. Status will show **"Connected"** when ready

### Using Screen Sync

1. Select **"Screen"** mode
2. Tap **"Start Screen Sync"**
3. Grant **Screen Capture** permission (required by Android)
4. LED will now sync with screen colors at 10 FPS
5. A notification will confirm "Screen Sync Active"
6. Press HOME to use other apps - sync continues in background
7. Return to app and tap **"Stop Screen Sync"** when done

### Using Static Colors

1. Select **"Color"** mode
2. Tap any preset color
3. LED changes instantly
4. Adjust brightness slider as needed

### Using Effects

1. Select **"Effects"** mode
2. Tap desired effect (Rainbow, Breathe, Police, etc.)
3. Adjust **Effect Speed** slider
4. Effect runs immediately
5. Tap another effect to switch

## Permissions Explained

| Permission | Purpose | Required |
|------------|---------|----------|
| `BLUETOOTH_SCAN` | Scan for LED devices | Yes |
| `BLUETOOTH_CONNECT` | Connect to LED strip | Yes |
| `ACCESS_FINE_LOCATION` | BLE scan (Android <12) | Android <12 |
| `FOREGROUND_SERVICE` | Background screen sync | Yes |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Screen capture service | Yes |
| `POST_NOTIFICATIONS` | Service notification | Android 13+ |

## Troubleshooting

### LED strips shows wrong colors
**Solution**: The LED strip hardware may have different RGB pin order.
1. Try setting brightness to 100%
2. If still incorrect, modify `colorOrder` in `BleLedController.kt` (e.g., "grb", "brg")

### "Device not found" when scanning
**Solutions**:
- Ensure LED strip is powered on
- Move TV closer to LED strip
- Check Bluetooth is enabled on TV
- Try scanning again (may take 10 seconds)

### Screen sync permission required every time
**This is normal**: Android requires MediaProjection permission each time for security. Cannot be saved.

### App crashes on connection
**Solutions**:
- Verify all Bluetooth permissions are granted
- Check Android TV Bluetooth settings
- Try restarting the LED strip

### Colors are not smooth
**Solution**: Adjust smoothing factor in `ColorSmoother.kt`:
```kotlin
// Increase for more smoothing (slower response)
ColorSmoother(smoothingFactor = 0.3f)

// Decrease for less smoothing (faster response)
ColorSmoother(smoothingFactor = 0.1f)
```

## Development

### Project Structure

```
app/src/main/java/com/kristof/brgbc/
├── MainActivity.kt                 # Main entry point
├── ble/
│   ├── BleLedController.kt        # BLE communication
│   ├── LedEffect.kt               # Effect types
│   └── LedProtocol.kt             # Protocol packets
├── capture/
│   ├── ColorAnalyzer.kt           # Color extraction
│   ├── ColorSmoother.kt           # Transition smoothing
│   └── ScreenCaptureService.kt    # Screen capture service
├── ui/
│   ├── LedControlScreen.kt        # Main UI
│   └── theme/                     # Theme files
└── viewmodel/
    └── LedViewModel.kt            # State management
```

### Adding New Effects

1. Add effect to `LedEffect.kt`:
```kotlin
enum class LedEffect(val displayName: String) {
    // ... existing effects
    MY_EFFECT("My Effect")
}
```

2. Implement effect in `BleLedController.kt`:
```kotlin
private suspend fun effectMyEffect(speed: Float) {
    while (isActive) {
        // Your effect logic
        setColor(r, g, b)
        delay((speed * 1000).toLong())
    }
}
```

3. Add case in `startEffect()`:
```kotlin
when (effect) {
    // ... existing cases
    LedEffect.MY_EFFECT -> effectMyEffect(speed)
}
```

### Protocol Reference (ELK-BLEDOB/ELK-BLEDOM)

All commands: `7E XX XX XX XX XX XX XX EF`

| Command | Bytes | Description |
|---------|-------|-------------|
| Color | `7E 00 05 03 [R] [G] [B] 00 EF` | Set RGB color |
| Brightness | `7E 00 01 [level] 00 00 00 00 EF` | Set brightness (0-100) |
| Power On | `7E 00 04 F0 00 01 FF 00 EF` | Turn on |
| Power Off | `7E 00 04 00 00 00 FF 00 EF` | Turn off |

## Performance

- **Screen Capture**: 10 FPS (100ms intervals)
- **Color Smoothing**: 20% interpolation per frame
- **Grid Sampling**: 3x3 grid (9 sample points)
- **Battery Impact**: Moderate (continuous operation - TV should be plugged in)

## Credits

Based on the Python LED controller originally created for ELK-BLEDOB/ELK-BLEDOM strips.  
Ported to Android with screen capture integration.

## License

This project is for personal use. LED strip protocol reverse-engineered from existing implementations.
