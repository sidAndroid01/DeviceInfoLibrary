package com.example.deviceInfo.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.deviceInfo.data.models.base.DeviceInfo
import com.example.deviceInfo.utils.DeviceInfoResult

interface DeviceInfoCollector<T : DeviceInfo> {
    suspend fun collect(): DeviceInfoResult<T>

    fun isAvailable(): Boolean

    // Get required permissions for this collector
    fun getRequiredPermissions(): List<String>

    // Get minimum API level required
    fun getMinimumApiLevel(): Int

    fun getDescription(): String
}

abstract class BaseDeviceInfoCollector<T : DeviceInfo> : DeviceInfoCollector<T> {
    protected fun safeExecute(block: () -> T): DeviceInfoResult<T> {
        return try {
            DeviceInfoResult.Success(block())
        } catch (securityException: SecurityException) {
            DeviceInfoResult.PermissionDenied
        } catch (exception: Exception) {
            DeviceInfoResult.Error(exception, "Failed to collect ${getDescription()}")
        }
    }

    protected fun checkApiLevel(): Boolean {
        return Build.VERSION.SDK_INT >= getMinimumApiLevel()
    }

    protected fun checkPermissions(context: Context): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun isAvailable(): Boolean {
        return checkApiLevel()
    }
}