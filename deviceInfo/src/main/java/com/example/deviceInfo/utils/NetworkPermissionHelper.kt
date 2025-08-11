package com.example.deviceInfo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class NetworkPermissionHelper(private val context: Context) {

    companion object {
        const val NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE
        const val WIFI_STATE = Manifest.permission.ACCESS_WIFI_STATE
        const val PHONE_STATE = Manifest.permission.READ_PHONE_STATE
    }

    private val permissionMap = mapOf(
        "networkState" to NETWORK_STATE,
        "wifiState" to WIFI_STATE,
        "phoneState" to PHONE_STATE
    )

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(): Boolean {
        return permissionMap.values.all { hasPermission(it) }
    }

    fun getMissingPermissions(): List<String> {
        return permissionMap.values.filter { !hasPermission(it) }
    }

    fun getPermissionStatus(): Map<String, Boolean> {
        return permissionMap.mapValues { (_, permission) -> hasPermission(permission) }
    }

    // Convenient methods for specific features
    fun canCollectWifiInfo(): Boolean = hasPermission(WIFI_STATE) && hasPermission(NETWORK_STATE)
    fun canCollectCellularInfo(): Boolean = hasPermission(PHONE_STATE)
    fun canCollectNetworkStatus(): Boolean = hasPermission(NETWORK_STATE)
}