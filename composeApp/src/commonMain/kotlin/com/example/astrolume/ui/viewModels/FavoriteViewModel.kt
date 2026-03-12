package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.data.toResponse
import com.example.astrolume.model.ApodResponse
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
): ViewModel() {
    private val _favorites = MutableStateFlow<List<ApodResponse>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<FavoriteUiState> = combine(
    _favorites,
    _errorMessage,
    _isLoading
    ) { fav, error, loading ->
        if (error != null) {
            FavoriteUiState.Error(error)
        } else if (loading) {
            FavoriteUiState.Loading
        } else {
            FavoriteUiState.Success(
                favorites = fav,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoriteUiState.Loading
    )

    init {
        getFavorites()
    }

    fun getFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val favorites = repository.getLocalFavorites()
                _favorites.value = favorites.map { it.toResponse() }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load favorites."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(apod: ApodResponse) {
        viewModelScope.launch {
            try {
                // We flip the current status
                val newFavoriteStatus = !apod.isFavorite

                // Update the repository/database
                // Assuming your repository has a method like updateFavorite
                repository.toggleFavorite(apod.date, newFavoriteStatus, apod)

            } catch (e: Exception) {
                _errorMessage.value = "Failed to update favorite."
            }
        }
    }
}