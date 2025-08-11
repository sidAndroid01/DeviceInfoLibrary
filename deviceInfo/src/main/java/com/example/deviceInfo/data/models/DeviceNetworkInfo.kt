package com.example.deviceInfo.data.models

import com.example.deviceInfo.data.models.base.DeviceInfo

data class DeviceNetworkInfo(
    override val lastCheckedTimeStamp: Long = System.currentTimeMillis(),
    val connectionType: String,
    val isConnected: Boolean,
    val wifiInfo: WiFiDetails?,
    val cellularInfo: CellularDetails?,
    val networkCapabilities: List<String>,
    val vpnActive: Boolean,
) : DeviceInfo

data class WiFiDetails(
    val ssid: String?,
    val bssid: String?,
    val signalStrength: Int,
    val linkSpeed: Int,
    val frequency: Int,
    val ipAddress: String?,
    val macAddress: String?,
    val networkId: Int
)

data class CellularDetails(
    val carrierName: String?,
    val countryIso: String?,
    val mobileNetworkCode: String?,
    val mobileCountryCode: String?,
    val networkType: String,
    val isRoaming: Boolean
)