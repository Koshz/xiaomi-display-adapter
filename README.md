# HDMI Adapter

Android APK prototype for Xiaomi 14 Ultra HDMI fullscreen output.

The app uses Android screen capture consent (`MediaProjection`), opens a
fullscreen `Presentation` on the HDMI display, and renders the captured phone
screen into the external display with independent X/Y scaling so a 1920x1080
monitor is filled instead of preserving the phone aspect ratio with black bars.

## Requirements

- Android Studio or Android SDK command line tools.
- JDK 17 or newer. This machine has a JetBrains JBR at:
  `C:\Program Files\JetBrains\PyCharm 2025.2.1.1\jbr`
- Android SDK platform 35.
- Xiaomi 14 Ultra or another Android phone with USB-C HDMI output.
- A 1920x1080 HDMI monitor.

## Build

```powershell
$env:JAVA_HOME="C:\Program Files\JetBrains\PyCharm 2025.2.1.1\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat assembleDebug
```

If Android SDK is not installed, install Android Studio or set `ANDROID_HOME`
to an SDK that contains platform 35.

## Runtime notes

- The first ON press shows the Android screen-capture consent dialog.
- Apps that block screenshots or DRM capture will not render through this v1
  pipeline.
- Rotation lock is optional. Grant system-settings write access with ADB if you
  want the app to lock landscape while ON and restore rotation when OFF:

```powershell
adb shell appops set com.obabo.xiaomihdmiadapter android:write_settings allow
```

Without this permission, rotate the phone manually before enabling.
