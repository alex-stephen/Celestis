package com.example.astrolume.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Android implementation of NetworkMonitor using ConnectivityManager.
 * 
 * - isWifiActive: Checks for TRANSPORT_WIFI capability
 * - isLowDataMode: Checks if the active network is metered (cellular, hotspot, etc.)
 */
actual class NetworkMonitor(private val context: Context) {
    
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    actual val isWifiActive: Boolean
        get() {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    
    actual val isLowDataMode: Boolean
        get() {
            // On Android, we treat metered connections as "low data mode"
            // This includes cellular, mobile hotspots, and any connection marked as metered
            return connectivityManager.isActiveNetworkMetered
        }
}
