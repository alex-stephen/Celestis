package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.data.toResponse
import com.example.astrolume.model.ApodResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val todayApod: ApodResponse,
        val randomApod: ApodResponse? = null
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: ApodRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // repository.fetchApod() now defaults to the latest APOD
                val today = repository.fetchApod().toResponse()

                // Fetch 1 random APOD for the "Discovery" section
                val randomList = repository.fetchRandom(1)

                _uiState.value = HomeUiState.Success(
                    todayApod = today,
                    randomApod = randomList.firstOrNull()
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    message = e.message ?: "Could not reach the stars. Check your connection."
                )
            }
        }
    }

    fun toggleFavorite(date: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(date, isFavorite)
            // Optional: Re-fetch or update the state locally here
        }
    }
}