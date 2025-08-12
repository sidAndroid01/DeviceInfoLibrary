# Device Info Library
[![](https://jitpack.io/v/sidAndroid01/DeviceInfoLibrary.svg)](https://jitpack.io/#sidAndroid01/DeviceInfoLibrary)

A comprehensive Android library for collecting detailed device information including hardware specs, system info, and network details with async collection and built-in caching.

## About Device Info Library
Device Info Library is a powerful, easy-to-use Android library built entirely in Kotlin that simplifies the process of collecting comprehensive device information. It leverages coroutines for async data collection and provides detailed hardware, system, and network information with robust error handling and caching mechanisms.

## Why use Device Info Library?
- 🔧 **Complete Hardware Info**: CPU architecture, RAM, storage, sensors, and device specifications
- 🖥️ **Detailed System Info**: Android version, security patches, root detection, emulator detection
- 🌐 **Network Information**: WiFi details, cellular info, connection status, and VPN detection
- ⚡ **Async & Parallel**: Collect all information asynchronously with parallel processing for optimal performance.
- 🔄 Background Friendly: Automatically leverages default background threading, ensuring seamless, non-blocking information collection for your app's smooth experience.
- 💾 **Smart Caching**: Built-in caching system to avoid repeated expensive operations
- 🛡️ **Graceful Error Handling**: Works even when permissions are missing, providing available information
- 🎯 **Easy Integration**: Simple initialization and intuitive API
- 🔒 **Permission Aware**: Automatically handles different permission levels and provides clear feedback
- 📱 **Wide Compatibility**: Supports Android API 21+ with backward compatibility
- 🚀 **Lightweight**: Minimal dependencies, no UI components, pure data collection

## Installation

### Step 1. Add JitPack repository
## Add it in your root `settings.gradle` at the end of repositories:
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // Add this line
    }
}
```

### Step 2. Add DeviceInfoLibrary dependency
## Add this in your app level gradle dependecies
```
dependencies {
    implementation 'com.github.sidAndroid01:DeviceInfoLibrary:1.0.0'
}
```

### Step 3. Add these necessary permissios to your manifest
```
<!-- Normal permissions (automatically granted) -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Dangerous permission (requires runtime request for cellular info) -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

## Usage
```
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the SDK
        DeviceInfoSDK.initialize(this)
        
        // Start collecting device information
        collectDeviceInfo()
    }

    private fun collectDeviceInfo() {
      lifecycleScope.launch {
          val sdk = DeviceInfoSDK.getInstance()
          
          // Collect all information at once
          val deviceReport = sdk.collectAllInfo()
          
          // Access specific information types
          val hardwareInfo = deviceReport.getHardwareInfo()
          val systemInfo = deviceReport.getSystemInfo()
          val networkInfo = deviceReport.getNetworkInfo()
          
          // Use the collected information
          hardwareInfo?.let { hardware ->
              Log.d("DeviceInfo", "Device: ${hardware.manufacturer} ${hardware.model}")
              Log.d("DeviceInfo", "RAM: ${DeviceInfoUtils.formatRam(hardware.totalRamBytes)}")
              Log.d("DeviceInfo", "Storage: ${DeviceInfoUtils.formatStorage(hardware.totalStorageBytes)}")
          }
          
          systemInfo?.let { system ->
              Log.d("DeviceInfo", "Android: ${system.androidVersion}")
              Log.d("DeviceInfo", "Rooted: ${system.isRooted}")
              Log.d("DeviceInfo", "Security Patch: ${system.securityPatchLevel}")
          }
          
          networkInfo?.let { network ->
              Log.d("DeviceInfo", "Connection: ${network.connectionType}")
              Log.d("DeviceInfo", "Connected: ${network.isConnected}")
          }
      }
    }
  }
}
```

### Important apis
## Collect all info
```
val deviceReport = sdk.collectAllInfo()
```
## Collect hardware info
```
 val hardwareInfo = deviceReport.getHardwareInfo()
```
## Collect system info
```
 val systemInfo = deviceReport.getSystemInfo()
```
## Collect network info
```
 val networkInfo = deviceReport.getNetworkInfo()
```
## Collect specific info
```
lifecycleScope.launch {
    val sdk = DeviceInfoSDK.getInstance()
    
    // Get only hardware information
    val hardwareInfo = sdk.getHardwareInfo()
    hardwareInfo?.let { hardware ->
        println("CPU: ${hardware.cpuArchitecture}")
        println("Supported ABIs: ${hardware.supportedAbis.joinToString()}")
        println("Sensors: ${hardware.sensors.size} sensors available")
        
        // Format storage and RAM for display
        val formattedRam = DeviceInfoUtils.formatRam(hardware.totalRamBytes)
        val formattedStorage = DeviceInfoUtils.formatStorage(hardware.totalStorageBytes)
        println("Memory: $formattedRam RAM, $formattedStorage Storage")
    }
    
    // Get only system information
    val systemInfo = sdk.getSystemInfo()
    systemInfo?.let { system ->
        println("Android: ${system.androidVersion}")
        println("Build: ${system.buildId}")
        println("Fingerprint: ${system.fingerprint}")
        println("Security Level: ${system.getSecurityLevel()}") // Custom security assessment
        println("Uptime: ${system.getUptimeFormatted()}") // Formatted uptime
    }
    
    // Get only network information
    val networkInfo = sdk.getNetworkInfo()
    networkInfo?.let { network ->
        println("Connection: ${network.connectionType}")
        println("Status: ${if (network.isConnected) "Connected" else "Disconnected"}")
        
        // WiFi details (if available)
        network.wifiInfo?.let { wifi ->
            println("WiFi: ${wifi.ssid} (${wifi.signalStrength} dBm)")
            println("Speed: ${wifi.linkSpeed} Mbps")
        }
        
        // Cellular details (if available)
        network.cellularInfo?.let { cellular ->
            println("Carrier: ${cellular.carrierName}")
            println("Network: ${cellular.networkType}")
        }
    }
}
```
## Configurations 
```
// Default configuration
DeviceInfoSDK.initialize(this)

// Custom configuration
val config = DeviceInfoSDK.Config(
    enableCaching = true,           // Enable/disable caching
    cacheExpirationMinutes = 30,    // Cache expiration time
    includeUnavailableInfo = false, // Include failed collections in report
    logLevel = DeviceInfoSDK.LogLevel.DEBUG // Set logging level
)

DeviceInfoSDK.initialize(this, config)
```
## Cache management
```
val sdk = DeviceInfoSDK.getInstance()

// Check cache status
val cacheStats = sdk.getCacheStats()
Log.d("Cache", "Cached items: ${cacheStats["totalCachedItems"]}")

// Clear specific cache
sdk.clearCache("hardware")

// Clear all cache
sdk.clearCache()

// Check if specific category is cached
val isCached = sdk.isCached("system")
```
## Utility functions
```
// Format bytes to human readable format
val formattedRam = DeviceInfoUtils.formatRam(hardwareInfo.totalRamBytes)
val formattedStorage = DeviceInfoUtils.formatStorage(hardwareInfo.totalStorageBytes)

// Device categorization
val category = DeviceInfoUtils.getDeviceCategory(hardwareInfo)
// Returns: "Flagship Phone", "High-end Phone", "Mid-range Phone", or "Budget Phone"

// Check if device is tablet
val isTablet = DeviceInfoUtils.isTablet(context)

// Get Android version name from API level
val versionName = DeviceInfoUtils.getAndroidVersionName(Build.VERSION.SDK_INT)
```
## Error handling
```
val deviceReport = sdk.collectAllInfo()

// Check for errors
if (deviceReport.hasErrors()) {
    val errors = deviceReport.getErrors()
    errors.forEach { error ->
        Log.e("DeviceInfo", "Collection error: $error")
    }
}

// Handle individual result types
when (val result = sdk.collectInfo<HardwareInfo>("hardware")) {
    is DeviceInfoResult.Success -> {
        val hardwareInfo = result.data
        // Use hardware info
    }
    is DeviceInfoResult.Error -> {
        Log.e("DeviceInfo", "Hardware collection failed: ${result.message}")
    }
    is DeviceInfoResult.PermissionDenied -> {
        Log.w("DeviceInfo", "Hardware collection needs permissions")
    }
    is DeviceInfoResult.NotAvailable -> {
        Log.w("DeviceInfo", "Hardware collection not available")
    }
}
```

### Sample integration 
```
class DeviceInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize with custom config
        val config = DeviceInfoSDK.Config(
            enableCaching = true,
            cacheExpirationMinutes = 60,
            logLevel = DeviceInfoSDK.LogLevel.INFO
        )
        DeviceInfoSDK.initialize(this, config)
        
        setContent {
            DeviceInfoScreen()
        }
    }
}

@Composable
fun DeviceInfoScreen() {
    var deviceReport by remember { mutableStateOf<DeviceInfoReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isLoading = true
        val sdk = DeviceInfoSDK.getInstance()
        deviceReport = sdk.collectAllInfo()
        isLoading = false
    }
    
    // Display the collected information in your UI
    // ... UI implementation
}
```

### Sample images
<img width="1080" height="2280" alt="Screenshot_20250811_182940" src="https://github.com/user-attachments/assets/8419a14d-0ff7-471a-a42d-cf15eb0e97ac" />
<img width="1080" height="2280" alt="Screenshot_20250811_182915" src="https://github.com/user-attachments/assets/80a84139-552d-4bf0-982b-7c1dbf86fd35" />


### License
Copyright (C) 2025 sidAndroid01

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

### Contribution
The library is in its nascent stage and is ready to be improved. 
Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## Steps 
- Head over to the [gitHub](https://github.com/sidAndroid01/DeviceInfoLibrary)
- Fork the repository
- Create your feature branch (git checkout -b feature/deviceInfoAdd)
- Commit your changes (git commit -m 'Add some more device info')
- Push to the branch (git push origin feature/AmazingFeature)
- Open a Pull Request

### Contact me -
[LinkedIn](https://www.linkedin.com/in/siddhant-mishra-95a91820a/)

**If this project helps you, show some ❤️ by putting a ⭐ on this project ✌️**



