package com.example.deviceInfo.utils

sealed class DeviceInfoResult<out T> {
    data class Success<T>(val data: T) : DeviceInfoResult<T>()
    data class Error(val exception: Throwable, val message: String) : DeviceInfoResult<Nothing>()
    object NotAvailable : DeviceInfoResult<Nothing>()
    object PermissionDenied : DeviceInfoResult<Nothing>()
}