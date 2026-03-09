package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.model.ApodResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DiscoverUiState {
    object Loading : DiscoverUiState
    data class Success(
        val rangeApod: List<ApodResponse>,
        val searchResults: List<ApodResponse>
    ) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}

class DiscoverViewModel(
    private val repository: ApodRepository
) : ViewModel() {
    private val _rangeApod = MutableStateFlow<List<ApodResponse>>(emptyList())
    private val _searchResults = MutableStateFlow<List<ApodResponse>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DiscoverUiState> = combine(
        _rangeApod,
        _searchResults,
        _isLoading,
        _errorMessage
    ) { range, search, loading, error ->
        when {
            error != null -> DiscoverUiState.Error(error)
            loading && range.isEmpty() -> DiscoverUiState.Loading
            else -> DiscoverUiState.Success(rangeApod = range, searchResults = search)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiscoverUiState.Loading
    )


    init {
        showRange()
    }

    fun showRange() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // The Repository should handle Dispatchers.Default/IO internally
                val range = repository.fetchRange("2024-01-01", "2024-01-07")
                _rangeApod.value = range
            } catch (e: Exception) {
                _errorMessage.value = "Failed to sync with Astrolume servers: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}