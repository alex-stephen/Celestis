package com.example.celestis.network

import kotlinx.coroutines.flow.Flow

/**
 * Platform-aware network monitoring to determine connection type and data-saving mode.
 * 
 * This class helps optimize image loading based on network conditions:
 * - Only prefetch HD images when on Wi-Fi
 * - Respect platform data-saving modes (Android metered connections, iOS Low Data Mode)
 */
expect class NetworkMonitor {
    /**
     * Returns true if the device is currently connected via Wi-Fi.
     * On cellular or no connection, returns false.
     */
    val isWifiActive: Boolean
    
    /**
     * Returns true if the platform is in a data-saving mode:
     * - Android: Active network is metered (cellular, hotspot, etc.)
     * - iOS: Low Data Mode is enabled
     */
    val isLowDataMode: Boolean
    
    /**
     * Flow that emits true when the device has any network connectivity,
     * false when offline. Updates automatically when network state changes.
     */
    val isOnline: Flow<Boolean>
}
