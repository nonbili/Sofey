# Sofey

An Android app that puts **Power**, **Volume Up**, and **Volume Down** on-screen — skip the physical buttons.

Built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

## Features

- **Volume Pads**: Adjust media volume with a tap.
- **Power Button**: Two modes to turn off or lock the screen.
    - **Accessibility (Recommended)**: Mimics the power button; respects system lock delay.
    - **Device Admin**: Forces an immediate lock.
- **Material You**: Wallpaper-aware colors and smooth spring animations.

## First-time setup (Accessibility Mode)

To turn off the screen without forcing an immediate pattern unlock:

1. **Enable Service**: Tap **"Switch to Accessibility"** in the app.
2. **Unblock (Android 13+)**: If it says "Restricted setting":
    - Go to **Settings > Apps > Sofey**.
    - Tap **⋮ (top-right)** > **"Allow restricted settings"**.
    - Return to Accessibility settings and turn on Sofey.
3. **Prevent Instant Lock**:
    - Go to **Security > Screen Lock (Gear icon ⚙️)**.
    - Turn **OFF** "Power button instantly locks".

## Build

```bash
./gradlew :app:assembleDebug   # Build debug APK
./gradlew :app:installDebug    # Build and install
```

For release builds, signing is pre-configured in `app/build.gradle.kts` and uses `keystore.properties` if available.
