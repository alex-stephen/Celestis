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

    private val randomQueue = ArrayDeque<ApodResponse>()
    private val PREFETCH_THRESHOLD = 3

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
                        refillQueueIfNeeded()
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

        viewModelScope.launch {
            // 1. Get the next available item from the queue
            val nextToDisplay = randomQueue.removeFirstOrNull()

            if (nextToDisplay != null) {
                _uiState.value = currentState.copy(randomApod = nextToDisplay)
            } else {
                // Emergency fetch if the user is faster than the internet
                _isFetchingRandom.value = true
                val emergency = repository.fetchRandom(1).firstOrNull()
                if (emergency != null) {
                    _uiState.value = currentState.copy(randomApod = emergency)
                }
                _isFetchingRandom.value = false
            }

            // 2. Refill the queue in the background
            refillQueueIfNeeded()
        }
    }

    private fun refillQueueIfNeeded() {
        // Don't start a new job if one is already filling the tank
        if (prefetchJob?.isActive == true) return

        prefetchJob = viewModelScope.launch {
            try {
                // Calculate how many we need to hit our target
                val needed = PREFETCH_THRESHOLD - randomQueue.size
                if (needed > 0) {
                    _isFetchingRandom.value = true
                    val newBatch = repository.fetchRandom(needed)
                    randomQueue.addAll(newBatch)
                }
            } catch (e: Exception) {
                // Fail silently
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