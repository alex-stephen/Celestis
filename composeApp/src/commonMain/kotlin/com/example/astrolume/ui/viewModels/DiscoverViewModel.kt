package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.model.ApodResponse
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed interface DiscoverUiState {
    object Loading : DiscoverUiState
    data class Success(
        val rangeApod: List<ApodResponse>,
        val searchQuery: String,
        val searchResults: PaginatedSearchState,
        val isRefreshing: Boolean = false
    ) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}

/**
 * Cross-platform pagination state for search results
 */
data class PaginatedSearchState(
    val items: List<ApodResponse> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class DiscoverViewModel(
    private val repository: ApodRepository
) : ViewModel() {
    private val _rangeApod = MutableStateFlow<List<ApodResponse>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow(PaginatedSearchState())
    private val searchState: StateFlow<PaginatedSearchState> = _searchState.asStateFlow()

    val uiState: StateFlow<DiscoverUiState> = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading).apply {
        viewModelScope.launch {
            // Combine all state flows
            kotlinx.coroutines.flow.combine(
                _rangeApod,
                _searchQuery,
                _isRefreshing,
                _errorMessage,
                searchState
            ) { range, query, refreshing, error, search ->
                if (error != null) {
                    DiscoverUiState.Error(error)
                } else if (refreshing && range.isEmpty()) {
                    DiscoverUiState.Loading
                } else {
                    DiscoverUiState.Success(
                        rangeApod = range,
                        searchQuery = query,
                        searchResults = search,
                        isRefreshing = refreshing
                    )
                }
            }.collect { value = it }
        }
    }

    init {
        // Initial Fetch for the Discovery Feed
        showRange()
        
        // Debounce search query changes
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query, reset = true)
                    } else {
                        // Clear search results when query is empty
                        _searchState.value = PaginatedSearchState()
                    }
                }
        }
    }

    fun showRange() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                // Fetch random APODs to fill the discovery feed
                //TODO: get the current month
                val randomFeed = repository.fetchRange("2026-02-11", "2026-03-11")
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

    fun toggleFavorite(apod: ApodResponse) {
        viewModelScope.launch {
            try {
                val newFavoriteStatus = !apod.isFavorite
                repository.toggleFavorite(apod.date, newFavoriteStatus, apod)

                // Update the static preset list manually so the UI reacts immediately
                _rangeApod.value = _rangeApod.value.map { currentApod ->
                    if (currentApod.date == apod.date) {
                        currentApod.copy(isFavorite = newFavoriteStatus)
                    } else {
                        currentApod
                    }
                }
                
                // Also update search results if present
                _searchState.value = _searchState.value.copy(
                    items = _searchState.value.items.map { currentApod ->
                        if (currentApod.date == apod.date) {
                            currentApod.copy(isFavorite = newFavoriteStatus)
                        } else {
                            currentApod
                        }
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update favorite."
            }
        }
    }

    fun updateQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun executeSearch() {
        val query = _searchQuery.value.trim()
        if (query.isNotBlank()) {
            performSearch(query, reset = true)
        }
    }

    fun loadMoreSearchResults() {
        val currentState = _searchState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return
        
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return
        
        performSearch(query, reset = false)
    }

    private fun performSearch(query: String, reset: Boolean) {
        viewModelScope.launch {
            val currentState = _searchState.value
            
            if (reset) {
                _searchState.value = currentState.copy(
                    isLoading = true,
                    error = null,
                    page = 0,
                    items = emptyList(),
                    hasMore = true
                )
            } else {
                _searchState.value = currentState.copy(isLoadingMore = true, error = null)
            }

            try {
                val pageToLoad = if (reset) 0 else currentState.page
                val results = repository.searchWithPagination(query, pageToLoad)
                
                val newItems = if (reset) results else currentState.items + results
                val hasMore = results.size >= 20 // If we got a full page, there might be more
                
                _searchState.value = PaginatedSearchState(
                    items = newItems,
                    page = pageToLoad + 1,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = hasMore,
                    error = null
                )
            } catch (e: Exception) {
                _searchState.value = currentState.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }
}
