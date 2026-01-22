# BRGBC - DIY Ambient TV Lighting

I bought a cheap 30m LED strip from AliExpress because I wanted some cool bias lighting for my TV. The remote control was basic, and the official app was a janky, ad-filled mess that barely worked. 

So, I built my own.

**BRGBC** is a native Android TV app that turns cheap ELK-BLEDOM Bluetooth LED strips into a high-performance ambient light system (like Ambilight, but for $10). It captures your screen in real-time and syncs the lights to match the action.

## 🚀 Features

### 🎬 High-Performance Screen Sync
- **30 FPS Capture:** Upgraded from the standard 10 FPS for buttery smooth visual updates that keep up with fast-paced action.
- **Smart Color Engine:**
  - **Vibrant Colors:** Automatically boosts saturation (1.5x) so your lights look rich and deep, not washed out.
  - **Intelligent Brightness:** Uses gamma correction (`pow(0.7)`) to make details visible in dark scenes without blinding you during bright flashes.
  - **Weighted Sampling:** The algorithm prioritizes vibrant pixels over boring grey ones to capture the true "mood" of the scene.
- **"Fast Attack, Slow Decay" Smoothing:** The lights react instantly to explosions and camera flashes (0.7 attack) but fade out elegantly during scene transitions (0.15 decay). No more "laggy" feeling lights.
- **🎵 Audio Reactivity:** Syncs brightness to the beat of the music. Music videos come alive as the lights pulse with the bass and dynamic range of the audio.

### 🛡️ Built for Real World Use
- **DRM & Tunneled Playback Protection:** The app is smart enough to know when you're watching protected content (like Netflix) or using Tunneled Playback (like SmartTube). Instead of crashing or getting stuck on a "grey screen," it detects the issue and handles it gracefully (lights dim to black).
- **Background Service:** Runs invisibly in the background. Start the sync, press Home, and open your favorite streaming app.
- **Resolution Aware:** Automatically handles screen rotation and resolution changes (e.g., switching from 4K UI to 1080p stream) without breaking the capture.

### 📱 Manual Control & Effects
If you just want some mood lighting:
- **Static Colors:** 10+ preset colors with instant switching.
- **Dynamic Effects:** Rainbow, Breathe, Police Strobe, Orbit, Lightning, Heartbeat, and Fire.
- **Full Control:** Adjust brightness and effect speed directly from the TV remote.

## 🛠️ Technical Implementation

### Architecture
Built with modern Android development standards:
- **Language:** Kotlin
- **UI:** Jetpack Compose for TV (Material3)
- **State Management:** MVVM with Kotlin Flow
- **Bluetooth:** Custom heavily optimized BLE controller for ELK-BLEDOM chips (`7E...EF` protocol)
- **Screen Capture:** `MediaProjection` API with hardware-accelerated `ImageReader` and dynamic downscaling.

### Protocol Reference (ELK-BLEDOM)
Reverse-engineered commands for the cheap AliExpress controllers:
| Command | Hex | Description |
|---------|-----|-------------|
| Color | `7E 00 05 03 [R] [G] [B] 00 EF` | Set RGB color |
| Brightness | `7E 00 01 [L] 00 00 00 00 EF` | Set brightness (0-100) |
| Power | `7E 00 04 [F0/00] 00 01 FF 00 EF` | F0=On, 00=Off |

## 📦 Requirements
- **Hardware:** Android TV (Android 8.0+)
- **Lights:** ELK-BLEDOM or ELK-BLEDOB Bluetooth LED Strip
- **Permissions:** 
  - Location/Bluetooth (to find the lights)
  - Screen Capture (to see what's watching)
  - Notifications (to keep the service alive)

## 🔧 Setup & Debug Build
1. **Clone the repo**
2. **Open in Android Studio** (Ladybug or newer recommended)
3. **Build & Run** on your Android TV (via ADB over WiFi)
   ```bash
   .\gradlew assembleDebug
   adb connect <TV_IP>:5555
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## ⚠️ Troubleshooting
- **Black/Grey Lights on Video:** If using SmartTube, enable Player->Developer->"Ambilight/Aspect Ratio/Video scale/Screenshots fix" in settings. For Netflix/Disney+, this is a system limitation (DRM) preventing screen capture. If u are an expert u can root your tv, u can use magisk to patch the system.
- **"Device Not Found":** Make sure the cheap Chinese app isn't still connected to the lights in the background on your phone!
- **Wrong Colors:** Some strips are GRB instead of RGB. You can tweak the `sendPacket` order in `BleLedController.kt` if needed.
- **Green Dot / Cast Icon in corner:** This is the Android System Privacy Indicator. It is forced by the OS for security (to warn you screen capture is active). It cannot be hidden by any app without Root. We have minimized the app's own notification to be invisible, but the system icon is mandatory.

---
*Built for fun because I refuse to use bad software. | Made by kristof.best*
