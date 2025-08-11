package com.example.deviceInfo.data.models

import android.os.Build
import com.example.deviceInfo.data.models.base.DeviceInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SystemInfo(
    override val lastCheckedTimeStamp: Long = System.currentTimeMillis(),
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val androidVersion: String,
    val buildId: String,
    val buildDisplay: String,
    val fingerprint: String,
    val host: String,
    val user: String,
    val securityPatchLevel: String?,
    val bootloaderVersion: String?,
    val baseband: String?,
    val kernelVersion: String?,
    val locale: String,
    val timeZone: String,
    val systemUptime: Long,
    val isRooted: Boolean,
    val isDeveloperOptionsEnabled: Boolean,
    val isAdbEnabled: Boolean,
    val seLinuxStatus: String,
    val playServicesVersion: String?,
    val isEmulator: Boolean,
    val installerPackageName: String?
) : DeviceInfo {

    companion object {
        fun createDefault() = SystemInfo(
            androidVersion = "Unknown",
            buildId = "Unknown",
            buildDisplay = "Unknown",
            fingerprint = "Unknown",
            host = "Unknown",
            user = "Unknown",
            securityPatchLevel = null,
            bootloaderVersion = null,
            baseband = null,
            kernelVersion = null,
            locale = "Unknown",
            timeZone = "Unknown",
            systemUptime = -1L,
            isRooted = false,
            isDeveloperOptionsEnabled = false,
            isAdbEnabled = false,
            seLinuxStatus = "Unknown",
            playServicesVersion = null,
            isEmulator = false,
            installerPackageName = null
        )
    }

    // Utility methods for easier consumption
    fun getAndroidVersionNumber(): String {
        return androidVersion.substringBefore(" (")
    }

    fun isSecurityPatchRecent(monthsThreshold: Int = 6): Boolean {
        return securityPatchLevel?.let { patch ->
            try {
                val patchDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(patch)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -monthsThreshold)
                patchDate?.after(calendar.time) == true
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    fun getUptimeFormatted(): String {
        if (systemUptime < 0) return "Unknown"

        val seconds = systemUptime / 1000
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

    fun getSecurityLevel(): String {
        return when {
            isRooted -> "Compromised (Rooted)"
            isDeveloperOptionsEnabled && isAdbEnabled -> "Development (ADB Enabled)"
            isDeveloperOptionsEnabled -> "Development (Dev Options)"
            isSecurityPatchRecent(3) -> "High (Recent Patches)"
            isSecurityPatchRecent(6) -> "Medium (Older Patches)"
            else -> "Low (Outdated Patches)"
        }
    }
}