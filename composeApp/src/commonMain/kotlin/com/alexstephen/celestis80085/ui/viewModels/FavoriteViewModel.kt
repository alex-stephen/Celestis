package com.alexstephen.celestis80085.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexstephen.celestis80085.data.ApodRepository
import com.alexstephen.celestis80085.data.toResponse
import com.alexstephen.celestis80085.model.ApodResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FavoriteUiState {
    object Loading : FavoriteUiState
    data class Success(
        val favorites: List<ApodResponse>,
    ) : FavoriteUiState
    data class Error(val message: String) : FavoriteUiState
}

class FavoriteViewModel(
    private val repository: ApodRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<FavoriteUiState> = combine(
        repository.getLocalFavorites(), // This should be a Flow from SQLDelight
        _errorMessage,
        _isLoading
    ) { favEntities, error, loading ->
        when {
            error != null -> FavoriteUiState.Error(error)
            loading -> FavoriteUiState.Loading
            else -> FavoriteUiState.Success(
                favorites = favEntities.map { it.toResponse() }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoriteUiState.Loading
    )

    fun toggleFavorite(apod: ApodResponse) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(apod.date, !apod.isFavorite, apod)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update favorite."
            }
        }
    }
}