# Project: GTA SA Reversed Android (SAMP Mobile Launcher)

## Overview
This project is a reverse-engineered version of **Grand Theft Auto: San Andreas (Android)**, specifically designed as a launcher and client for **SAMP (San Andreas Multiplayer) Mobile**. It features a modern Android launcher interface and a complex native layer that hooks into the game engine.

### Key Technologies
- **Android Framework:** Java/Kotlin, Navigation Component, ViewBinding.
- **Native Layer (JNI/C++):** C++11, CMake, Shadowhook (for hooking), Opus (for audio), and custom SAMP implementation.
- **Build System:** Gradle (Kotlin DSL), NDK 26.2.11394342.
- **External Services:** Firebase (Analytics, Crashlytics NDK, Messaging), Volley, PRDownloader.

## Project Structure
- `app/src/main/java/com/rstarx/hexrays/`:
    - `.launcher`: Contains the Android UI and update logic (`MainActivity`, `SplashActivity`, `UpdateService`).
    - `.game`: Contains the bridge to the native game activity (`SAMP`).
- `app/src/main/cpp/`:
    - `samp/`: The core SAMP client implementation, including networking, game hooks, and GUI.
    - `opus/`: Opus audio codec for voice chat.
- `app/src/main/assets/`: Static assets required for the launcher and game.
- `app/src/main/res/`: Android UI resources.

## Build and Run
The project uses Gradle with Kotlin DSL.

### Building APKs
- **Debug Build:** `./gradlew assembleDebug`
- **Release Build:** `./gradlew assembleRelease`

*Note: The project includes a custom Gradle task that automatically copies the built APKs to `C:\laragon\www` after a successful build.*

### Testing
- **Unit Tests:** `./gradlew test`
- **Android Instrumented Tests:** `./gradlew connectedAndroidTest`

## Development Conventions
- **JNI Bridge:** Native methods are primarily bridged in `com.nathan.djavarp.game.SAMP` and implemented in `app/src/main/cpp/samp/java/`.
- **Hooking:** Uses **Shadowhook** for intercepting and modifying game engine functions in `main.cpp` and other files within `app/src/main/cpp/samp/`.
- **Data Updates:** The `UpdateService` handles downloading game data from `https://samp-mobile.shop/files.json`.
- **GPU Specifics:** The updater filters texture files based on the device GPU (Adreno/DXT, Mali/ETC, PowerVR/PVR).

## Usage
- The app starts with `SplashActivity`, which transitions to `MainActivity` or `UpdateActivity`.
- `UpdateService` ensures the game files are present in the app's external data directory before launching the game.
- The game itself is launched via the `com.nathan.djavarp.game.SAMP` activity, which initializes the native engine.
