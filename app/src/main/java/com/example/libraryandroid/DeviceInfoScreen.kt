package com.example.libraryandroid

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.deviceInfo.data.models.base.DeviceInfoReport
import com.example.deviceInfo.internal.DeviceInfoSDK

@Composable
fun DeviceInfoTestScreen(modifier: Modifier = Modifier) {
    var deviceReport by remember { mutableStateOf<DeviceInfoReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sdk = remember { DeviceInfoSDK.getInstance() }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            deviceReport = sdk.collectAllInfo()
            Log.d("DeviceInfoTest", "Successfully collected device info")
        } catch (e: Exception) {
            errorMessage = "Failed to collect device info: ${e.message}"
            Log.e("DeviceInfoTest", "Error collecting device info", e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Device Info SDK Test",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Loading indicator
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Collecting device information...")
            }
        }

        // Error display
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Display collected information
        deviceReport?.let { report ->
            // Test individual info collection
            TestHardwareInfo(report)
            Spacer(modifier = Modifier.height(16.dp))

            TestSystemInfo(report)
            Spacer(modifier = Modifier.height(16.dp))

            TestNetworkInfo(report)
            Spacer(modifier = Modifier.height(16.dp))

            // Show errors if any
            if (report.hasErrors()) {
                TestErrorsInfo(report)
            }
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    isError: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isError) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TestHardwareInfo(report: DeviceInfoReport) {
    InfoSection(title = "Hardware Information") {
        val hardwareInfo = report.getHardwareInfo()

        if (hardwareInfo != null) {
            InfoRow("Manufacturer", hardwareInfo.manufacturer)
            InfoRow("Model", hardwareInfo.model)
            InfoRow("Device", hardwareInfo.device)
            InfoRow("Board", hardwareInfo.board)
            InfoRow("CPU Architecture", hardwareInfo.cpuArchitecture)
            InfoRow("Supported ABIs", hardwareInfo.supportedAbis.joinToString(", "))
            InfoRow("Total RAM", formatBytes(hardwareInfo.totalRamBytes))
            InfoRow("Available RAM", formatBytes(hardwareInfo.availableRamBytes))
            InfoRow("Total Storage", formatBytes(hardwareInfo.totalStorageBytes))
            InfoRow("Available Storage", formatBytes(hardwareInfo.availableStorageBytes))
            InfoRow("Sensors Count", "${hardwareInfo.sensors.size}")

            // Show first few sensors as examples
            if (hardwareInfo.sensors.isNotEmpty()) {
                Text(
                    text = "Sample Sensors:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                hardwareInfo.sensors.forEach { sensor ->
                    Text(
                        text = "• ${sensor.name} (${sensor.type})",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        } else {
            Text("Hardware info not available", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun TestSystemInfo(report: DeviceInfoReport) {
    InfoSection(title = "System Information") {
        val systemInfo = report.getSystemInfo()

        if (systemInfo != null) {
            InfoRow("Android Version", systemInfo.androidVersion)
            InfoRow("API Level", "${systemInfo.apiLevel}")
            InfoRow("Build ID", systemInfo.buildId)
            InfoRow("Fingerprint", systemInfo.fingerprint.take(50) + "...")
            InfoRow("Security Patch", systemInfo.securityPatchLevel ?: "Unknown")
            InfoRow("Bootloader", systemInfo.bootloaderVersion ?: "Unknown")
            InfoRow("Kernel Version", systemInfo.kernelVersion ?: "Unknown")
            InfoRow("Locale", systemInfo.locale)
            InfoRow("Timezone", systemInfo.timeZone)
            InfoRow("System Uptime", formatUptime(systemInfo.systemUptime))
            InfoRow("Is Rooted", if (systemInfo.isRooted) "Yes ✅" else "No ⚠️")
            InfoRow("Developer Options", if (systemInfo.isDeveloperOptionsEnabled) "Enabled" else "Disabled")
            InfoRow("ADB Enabled", if (systemInfo.isAdbEnabled) "Yes" else "No")
            InfoRow("SELinux Status", systemInfo.seLinuxStatus)
            InfoRow("Is Emulator", if (systemInfo.isEmulator) "Yes" else "No")
            InfoRow("Play Services", systemInfo.playServicesVersion ?: "Not installed")
            InfoRow("Installer", systemInfo.installerPackageName ?: "Unknown")
        } else {
            Text("System info not available", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun TestNetworkInfo(report: DeviceInfoReport) {
    InfoSection(title = "Network Information") {
        val networkInfo = report.getNetworkInfo()

        if (networkInfo != null) {
            InfoRow("Connection Type", networkInfo.connectionType)
            InfoRow("Is Connected", if (networkInfo.isConnected) "Yes ✅" else "No ❌")
            InfoRow("VPN Active", if (networkInfo.vpnActive) "Yes" else "No")
            InfoRow("Capabilities", networkInfo.networkCapabilities.joinToString(", "))

            // WiFi Info
            networkInfo.wifiInfo?.let { wifi ->
                Text(
                    text = "WiFi Details:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                InfoRow("SSID", wifi.ssid ?: "Unknown")
                InfoRow("Signal Strength", "${wifi.signalStrength} dBm")
                InfoRow("Link Speed", "${wifi.linkSpeed} Mbps")
                InfoRow("Frequency", "${wifi.frequency} MHz")
            }

            // Cellular Info
            networkInfo.cellularInfo?.let { cellular ->
                Text(
                    text = "Cellular Details:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                InfoRow("Carrier", cellular.carrierName ?: "Unknown")
                InfoRow("Network Type", cellular.networkType)
                InfoRow("Country", cellular.countryIso ?: "Unknown")
                InfoRow("Is Roaming", if (cellular.isRoaming) "Yes" else "No")
            }
        } else {
            Text("Network info not available", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun TestErrorsInfo(report: DeviceInfoReport) {
    InfoSection(title = "Collection Errors", isError = true) {
        report.getErrors().forEach { error ->
            Text(
                text = "• $error",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Helper functions
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "Unknown"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }

    return "%.1f %s".format(value, units[unitIndex])
}

fun formatUptime(uptimeMs: Long): String {
    if (uptimeMs < 0) return "Unknown"

    val seconds = uptimeMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}