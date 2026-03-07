package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.data.toResponse
import com.example.astrolume.model.ApodResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class HomeViewModel(
    private val repository: ApodRepository
) : ViewModel() {

    // Internal State
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isShowingRandom = MutableStateFlow(false)
    val isShowingRandom: StateFlow<Boolean> = _isShowingRandom.asStateFlow()

    private val _isFetchingRandom = MutableStateFlow(false)
    val isFetchingRandom: StateFlow<Boolean> = _isFetchingRandom.asStateFlow()

    private var prefetchJob: Job? = null
    private var prefetchedRandom: ApodResponse? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.observeLatestApod().collect { apodEntity ->
                if (apodEntity != null) {
                    val today = apodEntity.toResponse()
                    val currentState = _uiState.value

                    if (currentState is HomeUiState.Success) {
                        _uiState.value = currentState.copy(todayApod = today)
                    } else {
                        _uiState.value = HomeUiState.Success(
                            todayApod = today,
                            randomApod = null
                        )
                        // Trigger the very first background prefetch
                        queueNextRandomInBackground()
                    }
                } else {
                    _uiState.value = HomeUiState.Error("No space data found.")
                }
            }
        }
    }

    /**
     * The Snappy Random Logic: Uses the cached image instantly,
     * then fetches the next one in the background.
     */
    fun showRandomNext() {
        val currentState = _uiState.value
        if (currentState !is HomeUiState.Success) return

        _isShowingRandom.value = true

        // Cancel any ongoing fetch if the user is spam-clicking
        prefetchJob?.cancel()

        prefetchJob = viewModelScope.launch {
            _isFetchingRandom.value = true
            try {
                // 1. Instant UI Swap: Use the one we pre-fetched (or fetch if they clicked too fast)
                val nextToDisplay = prefetchedRandom ?: repository.fetchRandom(1).firstOrNull()

                if (nextToDisplay != null) {
                    // Push it to the UI
                    _uiState.value = currentState.copy(randomApod = nextToDisplay)
                }

                // 2. Clear the cache
                prefetchedRandom = null

                // 3. Queue up the NEXT one for the next time they click the button
                val futureRandom = repository.fetchRandom(1).firstOrNull()
                prefetchedRandom = futureRandom

            } catch (e: Exception) {
                // Fail silently so the app doesn't crash during exploration
            } finally {
                _isFetchingRandom.value = false
            }
        }

    }

    fun showToday() {
        _isShowingRandom.value = false
    }

    private fun queueNextRandomInBackground() {
        if (prefetchJob?.isActive == true) return // Don't fetch if already fetching

        prefetchJob = viewModelScope.launch {
            try {
                val randomList = repository.fetchRandom(1)
                prefetchedRandom = randomList.firstOrNull()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleFavorite(date: String, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(date, isFav)
        }
    }

    fun refreshAll() {
        loadInitialData()
    }
}