package com.example.deviceInfo.data.models

import android.hardware.Sensor
import com.example.deviceInfo.data.models.base.DeviceInfo

class HardwareInfo(
    override val lastCheckedTimeStamp: Long = System.currentTimeMillis(),
    val manufacturer: String,
    val model: String,
    val device: String,
    val board: String,
    val cpuArchitecture: String,
    val supportedAbis: List<String>,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,
    val sensors: List<SensorInfo>
): DeviceInfo