package com.example.deviceInfo.internal

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.deviceInfo.data.models.DeviceNetworkInfo
import com.example.deviceInfo.data.models.HardwareInfo
import com.example.deviceInfo.data.models.SystemInfo
import com.example.deviceInfo.data.models.base.DeviceInfo
import com.example.deviceInfo.data.models.base.DeviceInfoReport
import com.example.deviceInfo.data.repository.DeviceInfoCollector
import com.example.deviceInfo.data.repository.HardwareDeviceInfoCollector
import com.example.deviceInfo.data.repository.NetworkInfoCollector
import com.example.deviceInfo.data.repository.SystemInfoCollector
import com.example.deviceInfo.utils.DeviceInfoResult

class DeviceInfoSDK private constructor(private val context: Context) {

    // Configuration for the SDK
    data class Config(
        val enableCaching: Boolean = true,
        val cacheExpirationMinutes: Int = 30,
        val includeUnavailableInfo: Boolean = false,
        val logLevel: LogLevel = LogLevel.ERROR
    )

    enum class LogLevel { DEBUG, INFO, WARN, ERROR, NONE }

    private val collectors: Map<String, DeviceInfoCollector<*>> = mapOf(
        "hardware" to HardwareDeviceInfoCollector(context),
        "system" to SystemInfoCollector(context),
        "network" to NetworkInfoCollector(context)
    )

    private var config = Config()
    private val cache = mutableMapOf<String, Pair<DeviceInfoResult<*>, Long>>()

    companion object {
        @Volatile
        private var INSTANCE: DeviceInfoSDK? = null

        fun initialize(context: Context, config: Config = Config()): DeviceInfoSDK {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceInfoSDK(context.applicationContext).also {
                    it.config = config
                    INSTANCE = it
                }
            }
        }

        fun getInstance(): DeviceInfoSDK {
            return INSTANCE ?: throw IllegalStateException("DeviceInfoSDK not initialized. Call initialize() first.")
        }
    }

    // Main method to collect all device information
    suspend fun collectAllInfo(): DeviceInfoReport {
        val results = mutableMapOf<String, DeviceInfoResult<*>>()

        collectors.forEach { (category, collector) ->
            val result = collectWithCaching(category, collector)
            if (config.includeUnavailableInfo || result is DeviceInfoResult.Success) {
                results[category] = result
            }
        }

        return DeviceInfoReport(results, System.currentTimeMillis())
    }

    // Collect specific category of information
    private suspend inline fun <reified T : DeviceInfo> collectInfo(category: String): DeviceInfoResult<T> {
        val collector = collectors[category] ?: return DeviceInfoResult.Error(
            IllegalArgumentException("Unknown category: $category"),
            "Category '$category' not found"
        )

        @Suppress("UNCHECKED_CAST")
        return collectWithCaching(category, collector) as DeviceInfoResult<T>
    }

    // get the hardware info
    suspend fun getHardwareInfo(): HardwareInfo? {
        return when (val result = collectInfo<HardwareInfo>("hardware")) {
            is DeviceInfoResult.Success -> result.data
            else -> null
        }
    }

    // get the system info
    suspend fun getSystemInfo(): SystemInfo? {
        return when (val result = collectInfo<SystemInfo>("system")) {
            is DeviceInfoResult.Success -> result.data
            else -> null
        }
    }

    // get the network info
    suspend fun getNetworkInfo(): DeviceNetworkInfo? {
        return when (val result = collectInfo<DeviceNetworkInfo>("network")) {
            is DeviceInfoResult.Success -> result.data
            else -> null
        }
    }

    // Get information about what permissions are needed
    fun getRequiredPermissions(): Map<String, List<String>> {
        return collectors.mapValues { (_, collector) ->
            collector.getRequiredPermissions()
        }.filterValues { it.isNotEmpty() }
    }

    // Check which collectors are available
    fun getAvailableCollectors(): Map<String, Boolean> {
        return collectors.mapValues { (_, collector) ->
            collector.isAvailable()
        }
    }

    // Get missing permissions for each collector
    fun getMissingPermissions(): Map<String, List<String>> {
        return collectors.mapValues { (_, collector) ->
            collector.getRequiredPermissions().filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }.filterValues { it.isNotEmpty() }
    }

    // Update configuration
    fun updateConfig(newConfig: Config) {
        this.config = newConfig
        if (!newConfig.enableCaching) {
            clearCache()
        }
    }

    // Clear all cached data
    fun clearCache() {
        cache.clear()
    }

    // Clear specific category from cache
    fun clearCache(category: String) {
        cache.remove(category)
    }

    // Check if cached data exists for a category
    fun isCached(category: String): Boolean {
        val cached = cache[category]
        return cached != null && !isCacheExpired(cached.second)
    }

    // Get cache statistics
    fun getCacheStats(): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            "totalCachedItems" to cache.size,
            "validCachedItems" to cache.values.count { !isCacheExpired(it.second) },
            "expiredCachedItems" to cache.values.count { isCacheExpired(it.second) },
            "cacheExpirationMinutes" to config.cacheExpirationMinutes,
            "oldestCacheTimestamp" to cache.values.minOfOrNull { it.second },
            "newestCacheTimestamp" to cache.values.maxOfOrNull { it.second }
        ) as Map<String, Any>
    }

    // Private helper methods
    private suspend fun collectWithCaching(
        category: String,
        collector: DeviceInfoCollector<*>
    ): DeviceInfoResult<*> {
        if (config.enableCaching) {
            val cached = cache[category]
            if (cached != null && !isCacheExpired(cached.second)) {
                log(LogLevel.DEBUG, "Using cached data for category: $category")
                return cached.first
            }
        }

        log(LogLevel.DEBUG, "Collecting fresh data for category: $category")
        val result = collector.collect()

        if (config.enableCaching && result is DeviceInfoResult.Success) {
            cache[category] = result to System.currentTimeMillis()
        }

        return result
    }

    private fun isCacheExpired(timestamp: Long): Boolean {
        val expirationTime = config.cacheExpirationMinutes * 60 * 1000L
        return System.currentTimeMillis() - timestamp > expirationTime
    }

    private fun log(level: LogLevel, message: String) {
        if (level.ordinal >= config.logLevel.ordinal) {
            when (level) {
                LogLevel.DEBUG -> Log.d("DeviceInfoSDK", message)
                LogLevel.INFO -> Log.i("DeviceInfoSDK", message)
                LogLevel.WARN -> Log.w("DeviceInfoSDK", message)
                LogLevel.ERROR -> Log.e("DeviceInfoSDK", message)
                LogLevel.NONE -> {} // No logging
            }
        }
    }
}