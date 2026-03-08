package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.model.ApodResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val todayApodFlow = repository.observeLatestApod()

    // 2. The Random Stream (Managed in memory, then saved if favorited)
    private val _randomApod = MutableStateFlow<ApodResponse?>(null)
    // Internal State
    val uiState: StateFlow<HomeUiState> = combine(
        todayApodFlow,
        _randomApod
    ) { today, random ->
        if (today == null) {
            HomeUiState.Loading
        } else {
            HomeUiState.Success(
                todayApod = today,
                randomApod = random
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    private val _isShowingRandom = MutableStateFlow(false)
    val isShowingRandom: StateFlow<Boolean> = _isShowingRandom.asStateFlow()

    private val _isFetchingRandom = MutableStateFlow(false)
    val isFetchingRandom: StateFlow<Boolean> = _isFetchingRandom.asStateFlow()

    private val randomQueue = ArrayDeque<ApodResponse>()
    private val PREFETCH_THRESHOLD = 3

    private var prefetchJob: Job? = null

    init {
        refreshAll()
        refillQueueIfNeeded()
    }

    /**
     * The Snappy Random Logic: Uses the cached image instantly,
     * then fetches the next one in the background.
     */
    fun showRandomNext() {
        viewModelScope.launch {
            val nextToDisplay = randomQueue.removeFirstOrNull()

            if (nextToDisplay != null) {
                _randomApod.value = nextToDisplay
                _isShowingRandom.value = true
            } else {
                _isFetchingRandom.value = true
                val emergency = repository.fetchRandom(1).firstOrNull()
                _randomApod.value = emergency
                _isShowingRandom.value = true
                _isFetchingRandom.value = false
            }

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

    fun toggleFavorite(date: String, isFav: Boolean) {
        // Optimistic UI Updates
        val currentState = uiState.value as? HomeUiState.Success ?: return

        val isToday = currentState.todayApod.date == date
        val targetApod = if (isToday) currentState.todayApod else currentState.randomApod

        if (!isToday && currentState.randomApod?.date == date) {
            _randomApod.value = currentState.randomApod.copy(isFavorite = isFav)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(date, isFav, targetApod)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.refreshLatest()
        }
    }
}