package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.PlatformContext
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.utils.ImagePrefetcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

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
    private val repository: ApodRepository,
    private val imageLoader: ImageLoader,
    private val context: PlatformContext
) : ViewModel() {
    private val _rangeApod = MutableStateFlow<List<ApodResponse>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow(PaginatedSearchState())
    private val searchState: StateFlow<PaginatedSearchState> = _searchState.asStateFlow()

    private var searchJob: Job? = null

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
    
    /**
     * Prefetch images for visible APODs in the discovery feed.
     * This ensures images are cached when user scrolls.
     */
    fun prefetchVisibleImages(apods: List<ApodResponse>) {
        ImagePrefetcher.prefetchApodBatch(
            imageLoader = imageLoader,
            context = context,
            apods = apods,
            scope = viewModelScope
        )
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
                
                // PREDICTIVE PREFETCHING: Load first batch of images immediately
                if (randomFeed.isNotEmpty()) {
                    val firstBatch = randomFeed.take(12) // Prefetch first 12 images
                    prefetchVisibleImages(firstBatch)
                }
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

        // This locks out the other 4 rapid-fire scroll triggers.
        _searchState.value = currentState.copy(isLoadingMore = true, error = null)

        performSearch(query, reset = false)
    }

    private fun performSearch(query: String, reset: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val currentState = _searchState.value
            
            if (reset) {
                _searchState.value = currentState.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                    page = 0,
                    items = emptyList(),
                    hasMore = true
                )
            }

            try {
                val pageToLoad = if (reset) 0 else currentState.page
                val results = repository.searchWithPagination(query, pageToLoad)

                val newItems = if (reset) {
                    results
                } else {
                    (currentState.items + results).distinctBy { it.date }
                }
                val hasMore = results.size >= 20 // If we got a full page, there might be more
                _searchState.value = PaginatedSearchState(
                    items = newItems,
                    page = pageToLoad + 1,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = hasMore,
                    error = null
                )
                
                // PREDICTIVE PREFETCHING: Prefetch new search results
                if (results.isNotEmpty()) {
                    prefetchVisibleImages(results.take(12))
                }
            } catch (e: Exception) {
                _searchState.value = currentState.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    fun onDateRangeSelected(startDateMillis: Long?, endDateMillis: Long?) {
        if (startDateMillis == null) return

        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                // Convert millis to YYYY-MM-DD
                val startDate = formatMillisToIso(startDateMillis)
                // If endDate is null or same as start, fetch just one day
                val endDate = if (endDateMillis != null) formatMillisToIso(endDateMillis) else startDate

                val results = repository.fetchRange(startDate, endDate)
                _rangeApod.value = results
                // Clear search query to show the range results
                _searchQuery.value = ""
                
                // PREDICTIVE PREFETCHING: Prefetch date range results
                if (results.isNotEmpty()) {
                    prefetchVisibleImages(results.take(12))
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch dates: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // Helper (usually in a Utils file)
    private fun formatMillisToIso(millis: Long): String {
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)

        val localDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

        return localDateTime.date.toString()
    }
}
