package com.example.celestis.network

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.Network.nw_interface_type_wifi
import platform.darwin.dispatch_get_main_queue
import kotlinx.cinterop.CPointer
import platform.Network.nw_path_t
import platform.Network.nw_path_is_constrained

/**
 * iOS implementation of NetworkMonitor using NWPathMonitor.
 * 
 * - isWifiActive: Checks if the current path uses Wi-Fi interface
 * - isLowDataMode: Checks the isConstrained property (detects iOS Low Data Mode)
 */
@OptIn(ExperimentalForeignApi::class)
actual class NetworkMonitor {
    
    private val pathMonitor = nw_path_monitor_create()
    
    @Volatile
    private var currentPath: CPointer<nw_path_t>? = null
    
    init {
        // Set up the path monitor to track network changes
        nw_path_monitor_set_update_handler(pathMonitor) { path ->
            currentPath = path
        }
        
        // Start monitoring on the main queue
        nw_path_monitor_set_queue(pathMonitor, dispatch_get_main_queue())
        nw_path_monitor_start(pathMonitor)
    }
    
    actual val isWifiActive: Boolean
        get() {
            val path = currentPath ?: return false
            
            // Check if the path is satisfied (connected)
            val status = nw_path_get_status(path)
            if (status != nw_path_status_satisfied) {
                return false
            }
            
            // Check if using Wi-Fi interface
            return nw_path_uses_interface_type(path, nw_interface_type_wifi)
        }
    
    actual val isLowDataMode: Boolean
        get() {
            val path = currentPath ?: return false
            
            // isConstrained returns true when iOS Low Data Mode is enabled
            return nw_path_is_constrained(path)
        }
}
