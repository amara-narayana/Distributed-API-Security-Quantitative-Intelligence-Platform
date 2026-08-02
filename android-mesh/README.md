# Android Mesh Node Service

## Overview
This module provides an Android service that acts as a mesh node in the distributed API security testing platform. The service runs in the foreground and coordinates security test execution across the mesh network.

## Features
- **Foreground Service**: Runs continuously in the background with persistent notification
- **WireGuard Integration**: Secure tunnel connectivity for mesh communication
- **Job Polling**: Automatically polls the backend for security testing jobs
- **Health Monitoring**: Sends periodic heartbeats to maintain device registration
- **Result Reporting**: Submits test results back to the central backend

## Requirements
- Android 8.0 (API level 26) or higher
- Android Studio Arctic Fox or later
- Java 17 JDK

## Building

### Prerequisites
1. Install Android Studio
2. Install Java 17 JDK
3. Set up ANDROID_HOME environment variable

### Build Steps
```bash
cd android-mesh

# Using Gradle wrapper (recommended)
./gradlew assembleDebug

# Or using Android Studio
# 1. Open this directory in Android Studio
# 2. File > Sync Project with Gradle Files
# 3. Build > Build APK(s)
```

### Build Output
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

## Installation

### Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Manual Installation
Transfer the APK to your Android device and install it manually.

## Configuration

### Backend URL
Update the `SERVER_URL` constant in `MeshNodeService.kt`:
```kotlin
private const val SERVER_URL = "http://YOUR_SERVER_IP:8080/api"
```

For Android emulator, use `10.0.2.2` to access localhost on your host machine.

### WireGuard Configuration
The WireGuard configuration template is located at:
`app/src/main/res/xml/wireguard_config.xml`

Replace the placeholder values:
- `{{DEVICE_PRIVATE_KEY}}`: Generated per-device private key
- `{{COORDINATOR_PUBLIC_KEY}}`: Public key of the coordinator node
- `{{COORDINATOR_ENDPOINT}}`: IP/domain of the coordinator

## Permissions Required
- `INTERNET`: Network communication
- `ACCESS_NETWORK_STATE`: Network status monitoring
- `FOREGROUND_SERVICE`: Run as foreground service
- `WAKE_LOCK`: Keep device awake during operations
- `RECEIVE_BOOT_COMPLETED`: Auto-start on device boot (optional)

## Architecture

### Components
1. **MeshNodeService**: Main foreground service orchestrator
2. **WireGuardManager**: Manages secure tunnel connections
3. **JobPoller**: Polls backend for available jobs
4. **HealthMonitor**: Sends periodic heartbeats
5. **ResultReporter**: Submits completed job results

### Communication Flow
```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Device    │────▶│  Coordinator │────▶│   Backend   │
│   (Mesh)    │◀────│   (Server)   │◀────│   (API)     │
└─────────────┘     └──────────────┘     └─────────────┘
      │                    │                    │
      │  WireGuard Tunnel  │  HTTP/REST         │
      └────────────────────┴────────────────────┘
```

## Development

### Running Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Code Style
This project follows Kotlin coding conventions. Run formatting:
```bash
./gradlew ktlintFormat
```

## Troubleshooting

### Service Not Starting
1. Check that all permissions are granted
2. Verify backend URL is accessible
3. Check LogCat for error messages:
```bash
adb logcat -s MeshNodeService
```

### Connection Issues
1. Ensure device has internet connectivity
2. Verify backend server is running
3. Check firewall settings

### WireGuard Tunnel Fails
1. Verify WireGuard configuration is correct
2. Check that coordinator is reachable
3. Ensure UDP port 51820 is not blocked

## License
MIT License
