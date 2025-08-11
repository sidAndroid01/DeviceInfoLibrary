package com.example.deviceInfo.data.internal

import android.app.ActivityManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.deviceInfo.data.models.HardwareInfo
import com.example.deviceInfo.data.models.SensorInfo
import com.example.deviceInfo.utils.DeviceInfoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HardwareDeviceInfoCollector(private val context: Context): BaseDeviceInfoCollector<HardwareInfo>() {

    override fun getRequiredPermissions(): List<String> = emptyList()

    override fun getMinimumApiLevel(): Int = Build.VERSION_CODES.JELLY_BEAN

    override fun getDescription(): String = "Hardware specs"

    override suspend fun collect(): DeviceInfoResult<HardwareInfo> = withContext(Dispatchers.IO) {
        safeExecute {
            HardwareInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                device = Build.DEVICE,
                board = Build.BOARD,
                cpuArchitecture = getCpuArchitecture(),
                supportedAbis = getSupportedAbis(),
                totalRamBytes = getTotalRam(),
                availableRamBytes = getAvailableRam(),
                totalStorageBytes = getTotalStorage(),
                availableStorageBytes = getAvailableStorage(),
                sensors = getSensorInfo()
            )
        }
    }

    private fun getCpuArchitecture(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS[0]
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }
    }

    private fun getSupportedAbis(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.toList()
        } else {
            @Suppress("DEPRECATION")
            listOf(Build.CPU_ABI, Build.CPU_ABI2).filter { it.isNotEmpty() }
        }
    }

    private fun getTotalRam(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem
        } catch (e: Exception) {
            -1L
        }
    }

    private fun getSensorInfo(): List<SensorInfo> {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
                SensorInfo(
                    name = sensor.name,
                    type = getSensorTypeString(sensor.type),
                    vendor = sensor.vendor,
                    version = sensor.version,
                    maximumRange = sensor.maximumRange,
                    resolution = sensor.resolution,
                    power = sensor.power
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getAvailableRam(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            // availMem gives us memory available to the current app and system
            // This is not the same as "free" memory, but rather memory that can
            // be allocated without causing significant performance degradation
            memoryInfo.availMem
        } catch (e: Exception) {
            // Return -1 to indicate unavailable, rather than throwing
            // This allows the rest of the hardware collection to continue
            -1L
        }
    }

    private fun getTotalStorage(): Long {
        return try {
            // We want internal storage, which is where apps and user data live
            val internalDir = Environment.getDataDirectory()
            val statFs = StatFs(internalDir.path)

            // newer API when available for better accuracy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                statFs.blockCountLong * statFs.blockSizeLong
            } else {
                // Older API - need to be careful about integer overflow
                @Suppress("DEPRECATION")
                statFs.blockCount.toLong() * statFs.blockSize.toLong()
            }
        } catch (e: Exception) {
            // Some devices or custom ROMs might restrict access to storage stats
            -1L
        }
    }

    private fun getAvailableStorage(): Long {
        return try {
            val internalDir = Environment.getDataDirectory()
            val statFs = StatFs(internalDir.path)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                statFs.availableBlocksLong * statFs.blockSizeLong
            } else {
                @Suppress("DEPRECATION")
                statFs.availableBlocks.toLong() * statFs.blockSize.toLong()
            }
        } catch (e: Exception) {
            -1L
        }
    }

    // need to add more as when we find more sensors
    private fun getSensorTypeString(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            Sensor.TYPE_ORIENTATION -> "Orientation (Deprecated)"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_PRESSURE -> "Pressure"
            Sensor.TYPE_TEMPERATURE -> "Temperature (Deprecated)"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
            else -> "Unknown Type ($type)"
        }
    }
}