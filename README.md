# DHTV - IPTV Multi Branch

DHTV is a modern IPTV Multi Branch built with Jetpack Compose, designed to provide a seamless streaming experience for Android TV.

## Features

- Modern UI built with Jetpack Compose
- TV-optimized interface using Android TV components
- Video streaming capabilities
- Firebase integration for backend services
- QR code scanning functionality
- Smooth animations and transitions
- Material 3 design system

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 35
- **Architecture**: MVVM with Compose
- **Key Libraries**:
  - AndroidX Core KTX
  - Material 3
  - Android TV Foundation & Material
  - Retrofit for networking
  - Coil for image loading
  - Firebase (Database & Storage)
  - Ktor for HTTP client
  - ExoPlayer (Media3) for video playback
  - Lottie for animations
  - ZXing for QR code scanning

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Set up your Firebase project and add `google-services.json`
4. Configure signing keys in `keystore.properties`
5. Build and run the application

## Building the Project

```bash
# Using Gradle
./gradlew assembleDebug   # For debug build
./gradlew assembleRelease # For release build
```

## Requirements

- Android Studio Arctic Fox or newer
- JDK 11
- Android SDK 35
- Gradle 8.0 or newer