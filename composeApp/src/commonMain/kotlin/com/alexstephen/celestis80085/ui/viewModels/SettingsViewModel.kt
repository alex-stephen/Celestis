package com.alexstephen.celestis80085.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexstephen.celestis80085.data.ApodRepository
import com.alexstephen.celestis80085.network.NetworkMonitor
import com.alexstephen.celestis80085.ui.utils.AppActionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLowDataMode: Boolean = false,
    val cacheMessage: String? = null,
    val isClearingCache: Boolean = false
)

class SettingsViewModel(
    private val repository: ApodRepository,
    networkMonitor: NetworkMonitor,
    private val appActionManager: AppActionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(isLowDataMode = networkMonitor.isLowDataMode)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun clearCache() {
        if (_uiState.value.isClearingCache) return

        _uiState.value = _uiState.value.copy(isClearingCache = true, cacheMessage = null)
        viewModelScope.launch {
            runCatching {
                repository.clearNonFavoriteCache()
            }.onSuccess { removedCount ->
                _uiState.value = _uiState.value.copy(
                    isClearingCache = false,
                    cacheMessage = if (removedCount == 0L) {
                        "No cached items to clear."
                    } else {
                        "Cleared $removedCount cached item${if (removedCount == 1L) "" else "s"}. Favorites were kept."
                    }
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isClearingCache = false,
                    cacheMessage = "Could not clear cache. Please try again."
                )
            }
        }
    }

    fun openNotificationSettings() = appActionManager.openNotificationSettings()

    fun reportBug() = appActionManager.reportBug()

    fun leaveReview() = appActionManager.leaveReview()

    fun shareApp() = appActionManager.shareApp()
}
