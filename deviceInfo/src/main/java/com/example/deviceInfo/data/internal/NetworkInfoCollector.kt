package com.example.deviceInfo.data.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import com.example.deviceInfo.data.models.CellularDetails
import com.example.deviceInfo.data.models.DeviceNetworkInfo
import com.example.deviceInfo.data.models.WiFiDetails
import com.example.deviceInfo.utils.DeviceInfoResult
import com.example.deviceInfo.utils.NetworkPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkInfoCollector(private val context: Context) : BaseDeviceInfoCollector<DeviceNetworkInfo>() {

    private val permissionHelper = NetworkPermissionHelper(context)

    override fun getRequiredPermissions(): List<String> = listOf(
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE
    )

    override fun getMinimumApiLevel(): Int = Build.VERSION_CODES.ICE_CREAM_SANDWICH

    override fun getDescription(): String = "Network connectivity and WiFi information"

    override fun isAvailable(): Boolean {
        return super.isAvailable() && permissionHelper.canCollectNetworkStatus()
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_WIFI_STATE
    ])
    override suspend fun collect(): DeviceInfoResult<DeviceNetworkInfo> = withContext(Dispatchers.IO) {
        safeExecute {
            DeviceNetworkInfo(
                connectionType = if (permissionHelper.canCollectNetworkStatus()) {
                    getConnectionType()
                } else "Permission Required",

                isConnected = if (permissionHelper.canCollectNetworkStatus()) {
                    isConnected()
                } else false,

                wifiInfo = if (permissionHelper.canCollectWifiInfo()) {
                    getWifiInfo()
                } else null,

                cellularInfo = if (permissionHelper.canCollectCellularInfo()) {
                    getCellularInfo()
                } else null,

                networkCapabilities = if (permissionHelper.canCollectNetworkStatus()) {
                    getNetworkCapabilities()
                } else emptyList(),

                vpnActive = if (permissionHelper.canCollectNetworkStatus()) {
                    isVpnActive()
                } else false
            )
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isConnected(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo?.isConnectedOrConnecting == true
            }
        } catch (e: Exception) {
            false
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isVpnActive(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } else {
                // For older versions, VPN detection is more complex and less reliable
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getNetworkCapabilities(): List<String> {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

                networkCapabilities?.let { caps ->
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) capabilities.add("Internet")
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) capabilities.add("Validated")
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) capabilities.add("Unmetered")
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) capabilities.add("Direct")
                    else capabilities.add("VPN")
                }
            }

            capabilities
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission") // We check permissions above
    private fun getConnectionType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            when (connectivityManager.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Cellular"
                else -> "Unknown"
            }
        }
    }

    @SuppressLint("MissingPermission") // We check permissions in the caller
    private fun getWifiInfo(): WiFiDetails? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wifiManager.connectionInfo

            // Only return WiFi details if we're actually connected to WiFi
            if (connectionInfo != null && connectionInfo.networkId != -1) {
                WiFiDetails(
                    ssid = cleanSsid(connectionInfo.ssid),
                    bssid = connectionInfo.bssid,
                    signalStrength = connectionInfo.rssi,
                    linkSpeed = connectionInfo.linkSpeed,
                    frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        connectionInfo.frequency
                    } else -1,
                    ipAddress = formatIpAddress(connectionInfo.ipAddress),
                    macAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // MAC address randomization was introduced in Android 6.0
                        // Real MAC address is no longer available for privacy reasons
                        "02:00:00:00:00:00" // This is the randomized MAC placeholder
                    } else {
                        @Suppress("DEPRECATION")
                        connectionInfo.macAddress
                    },
                    networkId = connectionInfo.networkId
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun getCellularInfo(): CellularDetails? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            CellularDetails(
                carrierName = telephonyManager.networkOperatorName.takeIf { it.isNotBlank() },
                countryIso = telephonyManager.networkCountryIso.takeIf { it.isNotBlank() },
                mobileNetworkCode = telephonyManager.networkOperator.takeIf { it.length >= 3 }?.substring(3),
                mobileCountryCode = telephonyManager.networkOperator.takeIf { it.length >= 3 }?.substring(0, 3),
                networkType = getNetworkTypeString(telephonyManager.networkType),
                isRoaming = telephonyManager.isNetworkRoaming,
            )
        } catch (e: Exception) {
            null
        }
    }

    // Helper methods for network information
    private fun cleanSsid(rawSsid: String?): String? {
        // Android wraps SSID in quotes, so we need to clean that up
        return rawSsid?.let { ssid ->
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid.substring(1, ssid.length - 1)
            } else ssid
        }
    }

    private fun formatIpAddress(ip: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }

    private fun getNetworkTypeString(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            else -> "Unknown"
        }
    }
}