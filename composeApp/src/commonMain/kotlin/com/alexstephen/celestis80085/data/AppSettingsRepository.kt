package com.alexstephen.celestis80085.data

import com.alexstephen.celestis80085.network.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(
    networkMonitor: NetworkMonitor,
    private val storage: AppSettingsStorage
) {
    private val _isLowDataMode = MutableStateFlow(storage.getLowDataMode() ?: networkMonitor.isLowDataMode)
    val isLowDataMode: StateFlow<Boolean> = _isLowDataMode.asStateFlow()

    fun setLowDataMode(enabled: Boolean) {
        storage.setLowDataMode(enabled)
        _isLowDataMode.value = enabled
    }
}
