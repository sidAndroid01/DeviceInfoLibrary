package com.example.deviceInfo.data.models

import android.os.Build
import com.example.deviceInfo.data.models.base.DeviceInfo

data class SystemInfo(
    override val lastCheckedTimeStamp: Long = System.currentTimeMillis(),
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val androidVersion: String,
    val securityPatchLevel: String?,
    val buildId: String,
    val fingerprint: String,
    val bootloaderVersion: String?,
    val locale: String,
    val timeZone: String,
    val isRooted: Boolean,
    val developmentSettingsEnabled: Boolean
) : DeviceInfo