package com.alexstephen.celestis80085.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexstephen.celestis80085.data.AppSettingsRepository
import com.alexstephen.celestis80085.data.ApodRepository
import com.alexstephen.celestis80085.ui.utils.AppActionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLowDataMode: Boolean = false,
    val cacheMessage: String? = null,
    val isClearingCache: Boolean = false
)

class SettingsViewModel(
    private val repository: ApodRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val appActionManager: AppActionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettingsRepository.isLowDataMode.collect { isLowDataMode ->
                _uiState.update { it.copy(isLowDataMode = isLowDataMode) }
            }
        }
    }

    fun setLowDataMode(enabled: Boolean) {
        appSettingsRepository.setLowDataMode(enabled)
    }

    fun clearCache() {
        if (_uiState.value.isClearingCache) return

        _uiState.update { it.copy(isClearingCache = true, cacheMessage = null) }
        viewModelScope.launch {
            runCatching {
                repository.clearNonFavoriteCache()
            }.onSuccess { removedCount ->
                _uiState.update { state ->
                    state.copy(
                        isClearingCache = false,
                        cacheMessage = if (removedCount == 0L) {
                            "No cached items to clear."
                        } else {
                            "Cleared $removedCount cached item${if (removedCount == 1L) "" else "s"}. Favorites were kept."
                        }
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isClearingCache = false,
                        cacheMessage = "Could not clear cache. Please try again."
                    )
                }
            }
        }
    }

    fun openNotificationSettings() = appActionManager.openNotificationSettings()

    fun reportBug() = appActionManager.reportBug()

    fun leaveReview() = appActionManager.leaveReview()

    fun shareApp() = appActionManager.shareApp()
}
