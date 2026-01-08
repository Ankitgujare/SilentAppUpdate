# DemoApp - Silent App Updater

## Overview
**DemoApp** is an experimental Android application designed to demonstrate the capability of **self-updating** or **silent installation** of APKs programmatically. It leverages Android's `PackageInstaller` API to install updates without user intervention (once permissions are granted) and manages high-level permissions like `MANAGE_EXTERNAL_STORAGE` to access APK files from the Downloads directory.

This project focuses on the mechanism of updating an installed app from a local file, handling the necessary permission flows for modern Android versions (Android 11+ / API 30+).

## features
- **Silent Update Installation**: Uses `PackageInstaller` to commit an update session, replacing the current app with a new APK.
- **Permission Management**:
    - Requests `REQUEST_INSTALL_PACKAGES` to allow installation from unknown sources.
    - Requests `MANAGE_EXTERNAL_STORAGE` (All Files Access) to read APKs from shared storage (`Downloads` folder).
    - Intelligent permission handling for Android 11+ (API 30+) using `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`.
- **Auto-Restart**: Automatically restarts the application upon successful update using a `BroadcastReceiver`.
- **Modern UI**: Built with **Jetpack Compose** (Material3) for a reactive and modern user interface.

## Architecture

The application is structured around a single Activity and a BroadcastReceiver:

### 1. `MainActivity.kt`
The entry point of the application. It handles the UI and the update logic.
- **UI Layer**: Uses Jetpack Compose to display the current version and an "Update App Silently" button.
- **Permission Logic**: Checks if `isExternalStorageManager()` is granted. If not, launches the appropriate system settings intent (`ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`).
- **Update Logic**:
    - `startUpdate()`: Verifies if `app_update.apk` exists in the standard Downloads directory.
    - `installFromSdCard()`: Opens a `PackageInstaller` session, streams the APK file content into the session, and commits it. It sets up a `PendingIntent` pointing to `InstallResultReceiver` to handle the result.

### 2. `InstallResultReceiver.kt`
A `BroadcastReceiver` listening for the installation status.
- **On Success**: Triggers a re-launch of the application so the user lands back in the updated app immediately.
- **Logging**: Logs success or failure messages for debugging.

## Technical Details

### Technologies Used
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material3)
- **Minimum SDK**: 28 (Android 9)
- **Target SDK**: 33 (Android 13)
- **Build System**: Gradle

### Key Permissions
The `AndroidManifest.xml` declares:
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

## Setup & Usage
1. **Build**: Build the APK using `gradlew assembleDebug`.
2. **Prepare Update**:
    - Rename the produced APK (or a newer version) to `app_update.apk`.
    - Push it to the device's Download folder: `adb push app-debug.apk /sdcard/Download/app_update.apk`.
3. **Run**: Install and run the app.
4. **Test Update**: Click "Update App Silently". Grant permissions if prompted. The app should reinstall and restart.

## Notes
- **Security**: The `MANAGE_EXTERNAL_STORAGE` permission is a high-risk permission restricted by Google Play policies. This approach is primarily for internal tools, enterprise apps, or sideloaded scenarios.
- **File Path**: The app currently hardcodes the search path to `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) / "app_update.apk"`.
