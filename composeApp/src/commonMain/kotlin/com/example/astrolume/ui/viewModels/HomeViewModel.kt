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
    private val TARGET_CAPACITY = 20  // Keep 20 items ready
    private val REFILL_THRESHOLD = 10 // Trigger refill when half are gone
    private val BATCH_SIZE = 10

    private val _isRefilling = MutableStateFlow(false)
    private var prefetchJob: Job? = null

    init {
        refreshAll()
        refillQueue(initial = true)

        viewModelScope.launch(Dispatchers.IO) {
            repository.pruneCacheIfNeeded()
        }
    }

    /**
     * The Snappy Random Logic: Uses the cached image instantly,
     * then fetches the next one in the background.
     */
    fun showNextRandom() {
        if (randomQueue.isEmpty()) {
            // Emergency fallback - should rarely happen with 20 capacity
            fetchEmergencySingle()
            return
        }

        val next = randomQueue.removeFirst()
        _randomApod.value = next
        _isShowingRandom.value = true

        // Check if we need to top up the tank
        if (randomQueue.size <= REFILL_THRESHOLD) {
            refillQueue()
        }
    }

    private fun refillQueue(initial: Boolean = false) {
        if (prefetchJob?.isActive == true) return

        prefetchJob = viewModelScope.launch {
            _isRefilling.value = true
            try {
                val needed = if (initial) TARGET_CAPACITY else BATCH_SIZE
                val newItems = repository.fetchRandom(needed)

                randomQueue.addAll(newItems)

                // If it was the first time ever, show the first item immediately
                if (initial && _randomApod.value == null) {
                    _randomApod.value = randomQueue.removeFirstOrNull()
                }
            } catch (e: Exception) {
                // Network error - we'll try again on the next swipe
            } finally {
                _isRefilling.value = false
            }
        }
    }

    private fun fetchEmergencySingle() {
        viewModelScope.launch {
            _isFetchingRandom.value = true
            try {
                val item = repository.fetchRandom(1).firstOrNull()
                _randomApod.value = item
            } finally {
                _isFetchingRandom.value = false
                refillQueue() // Attempt to fix the empty queue
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