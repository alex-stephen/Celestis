package com.alexstephen.celestis80085.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Android implementation of NetworkMonitor using ConnectivityManager.
 * 
 * - isWifiActive: Checks for TRANSPORT_WIFI capability
 * - isLowDataMode: Checks if the active network is metered (cellular, hotspot, etc.)
 * - isOnline: Flow that emits connectivity state changes
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
    
    actual val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()
            
            override fun onAvailable(network: Network) {
                networks.add(network)
                trySend(networks.isNotEmpty())
            }
            
            override fun onLost(network: Network) {
                networks.remove(network)
                trySend(networks.isNotEmpty())
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Emit initial state
        val initialState = connectivityManager.activeNetwork != null
        trySend(initialState)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
