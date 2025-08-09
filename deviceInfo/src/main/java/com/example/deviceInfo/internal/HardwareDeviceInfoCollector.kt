package com.example.deviceInfo.internal

import android.app.ActivityManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
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
                screenMetrics = getScreenMetrics(),
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
                    type = sensor.type,
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
}