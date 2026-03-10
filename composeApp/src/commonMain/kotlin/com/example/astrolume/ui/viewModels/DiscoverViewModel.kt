package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.model.ApodResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DiscoverUiState {
    object Loading : DiscoverUiState
    data class Success(
        val rangeApod: List<ApodResponse>,
        val searchResults: List<ApodResponse>,
        val searchQuery: String,
        val isPaging: Boolean = false
    ) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}

@OptIn(FlowPreview::class)
class DiscoverViewModel(
    private val repository: ApodRepository
) : ViewModel() {
    private val _rangeApod = MutableStateFlow<List<ApodResponse>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    private val _isSearching = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var currentSearchPage = 0
    private var isLastPage = false

    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchFlow = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeSearch(query)
            }
        }

    private val dataFlow = combine(_rangeApod, searchFlow, _searchQuery) { range, search, query ->
        Triple(range, search, query)
    }

    private val flagsFlow = combine(_isRefreshing, _isSearching, _errorMessage) { r, s, e ->
        Triple(r, s, e)
    }

    val uiState: StateFlow<DiscoverUiState> = combine(
        dataFlow,
        flagsFlow
    ) { data, flags ->
        val range = data.first
        val search = data.second
        val query = data.third
        
        val refreshing = flags.first
        val searching = flags.second
        val error = flags.third

        if (error != null) {
            DiscoverUiState.Error(error)
        } else if ((refreshing || searching) && range.isEmpty() && search.isEmpty()) {
            DiscoverUiState.Loading
        } else {
            DiscoverUiState.Success(
                rangeApod = range,
                searchResults = search,
                searchQuery = query,
                isPaging = searching
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiscoverUiState.Loading
    )

    init {
        // Initial Fetch for the Discovery Feed
        showRange()
    }

    fun showRange() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                // Fetch random APODs to fill the discovery feed
                val randomFeed = repository.fetchRandom(20)
                _rangeApod.value = randomFeed
            } catch (e: Exception) {
                // Only show error if we have NO local results to show
                if (_rangeApod.value.isEmpty()) {
                    _errorMessage.value = "Failed to load discovery feed. Check connection."
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun syncSearch(query: String) {
        if (isLastPage) return
        viewModelScope.launch {
            _isSearching.value = true
            _errorMessage.value = null
            try {
                // Network call to update the local DB
                val newResults = repository.search(query, currentSearchPage)
                if (newResults.isEmpty()) {
                    isLastPage = true
                }
            } catch (e: Exception) {
                // If we have local results, we might want to stay silent
                // If 0 results, show the error
                if (uiState.value is DiscoverUiState.Success &&
                    (uiState.value as DiscoverUiState.Success).searchResults.isEmpty()) {
                    _errorMessage.value = "Search unavailable. Check connection."
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun updateQuery(newQuery: String) {
        if (_searchQuery.value != newQuery) {
            _searchQuery.value = newQuery
        }
    }

    fun executeSearch() {
        val trimmed = _searchQuery.value.trim()
        if (trimmed.isNotBlank()) {
            currentSearchPage = 0
            isLastPage = false
            syncSearch(trimmed)
        }
    }

    fun loadNextSearchPage() {
        if (_isSearching.value || _searchQuery.value.isBlank() || isLastPage) return
        currentSearchPage++
        syncSearch(_searchQuery.value.trim())
    }
}
