package com.example.deviceInfo.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.example.deviceInfo.data.models.HardwareInfo
import com.example.deviceInfo.data.models.base.DeviceInfo

// Utility class for common operations
object DeviceInfoUtils {
    fun <T : DeviceInfo> safeExecute(block: () -> T): DeviceInfoResult<T> {
        return try {
            DeviceInfoResult.Success(block())
        } catch (securityException: SecurityException) {
            DeviceInfoResult.PermissionDenied
        } catch (exception: Exception) {
            DeviceInfoResult.Error(exception, "Failed")
        }
    }
}