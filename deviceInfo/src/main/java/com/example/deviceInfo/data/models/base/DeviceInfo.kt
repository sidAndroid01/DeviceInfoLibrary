package com.example.deviceInfo.data.models.base

import com.example.deviceInfo.data.models.DeviceNetworkInfo
import com.example.deviceInfo.data.models.HardwareInfo
import com.example.deviceInfo.data.models.SystemInfo
import com.example.deviceInfo.utils.DeviceInfoResult

interface DeviceInfo {
    val lastCheckedTimeStamp: Long
}

data class DeviceInfoReport(
    private val data: Map<String, DeviceInfoResult<*>>,
    val generatedAt: Long = System.currentTimeMillis()
) {
    // safe casting to extract the desired type of data
    fun getHardwareInfo(): HardwareInfo? =
        (data["hardware"] as? DeviceInfoResult.Success<*>)?.data as? HardwareInfo

    fun getSystemInfo(): SystemInfo? =
        (data["system"] as? DeviceInfoResult.Success<*>)?.data as? SystemInfo

    fun getNetworkInfo(): DeviceNetworkInfo? =
        (data["network"] as? DeviceInfoResult.Success<*>)?.data as? DeviceNetworkInfo

    fun hasErrors(): Boolean = data.values.any { it is DeviceInfoResult.Error }
    fun getErrors(): List<String> = data.values.filterIsInstance<DeviceInfoResult.Error>().map { it.message }
    fun getAllData(): Map<String, DeviceInfoResult<*>> = data
}