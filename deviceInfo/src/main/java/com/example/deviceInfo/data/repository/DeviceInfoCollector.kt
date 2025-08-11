package com.example.deviceInfo.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.deviceInfo.data.models.base.DeviceInfo
import com.example.deviceInfo.utils.DeviceInfoResult

// new info collectors can be added over here
interface DeviceInfoCollector<T : DeviceInfo> {
    suspend fun collect(): DeviceInfoResult<T>

    fun isAvailable(): Boolean

    // Get required permissions for this collector
    fun getRequiredPermissions(): List<String>

    // Get minimum API level required
    fun getMinimumApiLevel(): Int

    fun getDescription(): String
}

// abstract to define more generic methods
abstract class BaseDeviceInfoCollector<T : DeviceInfo> : DeviceInfoCollector<T> {
    protected fun <T : DeviceInfo> safeExecute(block: () -> T): DeviceInfoResult<T> {
        return try {
            DeviceInfoResult.Success(block())
        } catch (securityException: SecurityException) {
            DeviceInfoResult.PermissionDenied
        } catch (exception: Exception) {
            DeviceInfoResult.Error(exception, "Failed")
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