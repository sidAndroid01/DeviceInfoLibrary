package com.example.deviceInfo.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.example.deviceInfo.data.models.HardwareInfo
import com.example.deviceInfo.data.models.SystemInfo
import com.example.deviceInfo.utils.DeviceInfoResult
import com.example.deviceInfo.utils.DeviceInfoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TimeZone

class SystemInfoCollector(private val context: Context): BaseDeviceInfoCollector<SystemInfo>() {

    override fun getRequiredPermissions(): List<String> = emptyList() // Most system info doesn't require permissions

    override fun getMinimumApiLevel(): Int = Build.VERSION_CODES.ICE_CREAM_SANDWICH

    override fun getDescription(): String = "System configuration and build information"

    override suspend fun collect(): DeviceInfoResult<SystemInfo> {
        return safeExecute {
            SystemInfo(
                androidVersion = getAndroidVersion(),
                apiLevel = Build.VERSION.SDK_INT,
                buildId = Build.ID,
                buildDisplay = Build.DISPLAY,
                fingerprint = Build.FINGERPRINT,
                host = Build.HOST,
                user = Build.USER,
                securityPatchLevel = getSecurityPatchLevel(),
                bootloaderVersion = getBootloaderVersion(),
                baseband = getBasebandVersion(),
                kernelVersion = getKernelVersion(),
                locale = getCurrentLocale(),
                timeZone = getCurrentTimeZone(),
                systemUptime = getSystemUptime(),
                isRooted = isDeviceRooted(),
                isDeveloperOptionsEnabled = isDeveloperOptionsEnabled(),
                isAdbEnabled = isAdbEnabled(),
                seLinuxStatus = getSELinuxStatus(),
                playServicesVersion = getPlayServicesVersion(),
                isEmulator = isRunningOnEmulator(),
                installerPackageName = getInstallerPackageName()
            )
        }
    }

    private fun getAndroidVersion(): String {
        return "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    private fun getSecurityPatchLevel(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Build.VERSION.SECURITY_PATCH
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    private fun getBootloaderVersion(): String? {
        return try {
            Build.BOOTLOADER.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun getBasebandVersion(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                Build.getRadioVersion()?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getKernelVersion(): String? {
        return try {
            val process = Runtime.getRuntime().exec("uname -r")
            process.inputStream.bufferedReader().use { reader ->
                reader.readLine()?.trim()
            }
        } catch (e: Exception) {
            // Fallback: try reading from /proc/version
            try {
                val versionFile = File("/proc/version")
                if (versionFile.exists() && versionFile.canRead()) {
                    versionFile.readText().split(" ").getOrNull(2)
                } else {
                    null
                }
            } catch (fallbackException: Exception) {
                null
            }
        }
    }

    private fun getCurrentLocale(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0].toString()
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale.toString()
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getCurrentTimeZone(): String {
        return try {
            TimeZone.getDefault().id
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getSystemUptime(): Long {
        return try {
            SystemClock.elapsedRealtime()
        } catch (e: Exception) {
            -1L
        }
    }

    private fun isDeviceRooted(): Boolean {
        return try {
            // Multiple methods to detect root access
            checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
        } catch (e: Exception) {
            false
        }
    }

    // Root detection method 1: Check for common root binaries
    private fun checkRootMethod1(): Boolean {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )

        return rootPaths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    // Root detection method 2: Check for root management apps
    private fun checkRootMethod2(): Boolean {
        val rootApps = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk"
        )

        return rootApps.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            } catch (e: Exception) {
                false
            }
        }
    }

    // Root detection method 3: Try to execute su command
    private fun checkRootMethod3(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().use { it.readText() }
            result.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0
                ) != 0
            } else {
                @Suppress("DEPRECATION")
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.NAME,
                    0
                ) != 0
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isAdbEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.ADB_ENABLED,
                    0
                ) != 0
            } else {
                @Suppress("DEPRECATION")
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.ADB_ENABLED,
                    0
                ) != 0
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getSELinuxStatus(): String {
        return try {
            val process = Runtime.getRuntime().exec("getenforce")
            process.inputStream.bufferedReader().use { reader ->
                reader.readLine()?.trim() ?: "Unknown"
            }
        } catch (e: Exception) {
            // Fallback: try reading from /sys/fs/selinux/enforce
            try {
                val seLinuxFile = File("/sys/fs/selinux/enforce")
                if (seLinuxFile.exists() && seLinuxFile.canRead()) {
                    val enforceValue = seLinuxFile.readText().trim()
                    when (enforceValue) {
                        "1" -> "Enforcing"
                        "0" -> "Permissive"
                        else -> "Unknown"
                    }
                } else {
                    "Not Available"
                }
            } catch (fallbackException: Exception) {
                "Unknown"
            }
        }
    }

    private fun getPlayServicesVersion(): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                "com.google.android.gms",
                0
            )
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: PackageManager.NameNotFoundException) {
            null // Play Services not installed
        } catch (e: Exception) {
            null
        }
    }

    private fun isRunningOnEmulator(): Boolean {
        return try {
            // Multiple checks to detect emulator
            (Build.FINGERPRINT.startsWith("generic") ||
                    Build.FINGERPRINT.startsWith("unknown") ||
                    Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK built for x86") ||
                    Build.MANUFACTURER.contains("Genymotion") ||
                    Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                    "google_sdk" == Build.PRODUCT ||
                    Build.HARDWARE.contains("goldfish") ||
                    Build.HARDWARE.contains("ranchu"))
        } catch (e: Exception) {
            false
        }
    }

    private fun getInstallerPackageName(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (e: Exception) {
            null
        }
    }
}