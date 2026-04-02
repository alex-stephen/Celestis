package com.example.celestis.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.PlatformContext
import com.example.celestis.data.ApodRepository
import com.example.celestis.data.toResponse
import com.example.celestis.model.ApodResponse
import com.example.celestis.network.NetworkMonitor
import com.example.celestis.ui.utils.ImagePrefetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

sealed interface DiscoverUiState {
    object Loading : DiscoverUiState
    data class Success(
        val rangeApod: List<ApodResponse>,
        val searchQuery: String,
        val searchResults: PaginatedSearchState,
        val isRefreshing: Boolean = false,
        val isOfflineMode: Boolean = false
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

/**
 * Pagination state for date range queries
 */
data class RangePaginationState(
    val startDate: String = "",
    val endDate: String = "",
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false
)

@OptIn(FlowPreview::class)
class DiscoverViewModel(
    private val repository: ApodRepository,
    private val imageLoader: ImageLoader,
    private val context: PlatformContext,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    private val _rangeApod = MutableStateFlow<List<ApodResponse>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow(PaginatedSearchState())
    private val searchState: StateFlow<PaginatedSearchState> = _searchState.asStateFlow()

    private val _rangePaginationState = MutableStateFlow(RangePaginationState())

    private var searchJob: Job? = null

    // Group data-related flows
    private val dataState = combine(
        _rangeApod,
        _searchQuery,
        searchState
    ) { range, query, search ->
        Triple(range, query, search)
    }

    // Group UI state-related flows
    private val controlState = combine(
        _isRefreshing,
        _errorMessage,
        networkMonitor.isOnline
    ) { refreshing, error, isOnline ->
        Triple(refreshing, error, isOnline)
    }

    // Combine the two grouped flows for final UI state
    val uiState: StateFlow<DiscoverUiState> = combine(
        dataState,
        controlState
    ) { data, control ->
        val (range, query, search) = data
        val (refreshing, error, isOnline) = control
        
        when {
            error != null -> DiscoverUiState.Error(error)
            refreshing &&
            range.isEmpty() &&
            query.isEmpty() &&
            !search.isLoading -> DiscoverUiState.Loading
            else -> DiscoverUiState.Success(
                rangeApod = range,
                searchQuery = query,
                searchResults = search,
                isRefreshing = refreshing,
                isOfflineMode = !isOnline
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiscoverUiState.Loading
    )

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
        viewModelScope.launch(Dispatchers.Default) {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                val isOnline = networkMonitor.isOnline.firstOrNull() ?: true
                
                val randomFeed = if (isOnline) {
                    // Online: Fetch from API with pagination
                    val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                    val today = now.toLocalDateTime(TimeZone.UTC).date
                    val oneYearAgo = today.minus(1, DateTimeUnit.YEAR)

                    _rangePaginationState.value = RangePaginationState(
                        startDate = oneYearAgo.toString(),
                        endDate = today.toString(),
                        page = 0,
                        hasMore = false,
                        isLoadingMore = false
                    )

                    val results = repository.fetchRange(
                        start = oneYearAgo.toString(),
                        end = today.toString(),
                        page = 0,
                        limit = 30
                    )
                    
                    _rangePaginationState.value = _rangePaginationState.value.copy(
                        page = 1,
                        hasMore = results.size >= 30
                    )

                    results
                } else {
                    // Offline: Load all cached APODs
                    repository.observeAllCachedApods().firstOrNull()
                        ?.map { it.toResponse() } ?: emptyList()
                }
                
                _rangeApod.value = randomFeed
                
                if (randomFeed.isNotEmpty() && isOnline) {
                    val firstBatch = randomFeed.take(12)
                    prefetchVisibleImages(firstBatch)
                }
            } catch (e: Exception) {
                // Fallback to cached data
                try {
                    val cachedApods = repository.observeAllCachedApods().firstOrNull()
                        ?.map { it.toResponse() } ?: emptyList()
                    _rangeApod.value = cachedApods
                } catch (cacheError: Exception) {
                    // PRODUCTION: Only show error if we have NO local results to show
                    if (_rangeApod.value.isEmpty()) {
                        _errorMessage.value = "Unable to load photos. Please check your connection."
                    }
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
                    error = e.message ?: "Unable to search. Please try again."
                )
            }
        }
    }

    fun onDateRangeSelected(startDateMillis: Long?, endDateMillis: Long?) {
        if (startDateMillis == null) return

        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            
             _rangeApod.value = emptyList()
            try {
                // Convert millis to YYYY-MM-DD
                val startDate = formatMillisToIso(startDateMillis)
                // If endDate is null or same as start, fetch just one day
                val endDate = if (endDateMillis != null) formatMillisToIso(endDateMillis) else startDate

                // Reset pagination state with new date range
                _rangePaginationState.value = RangePaginationState(
                    startDate = startDate,
                    endDate = endDate,
                    page = 0,
                    hasMore = false,
                    isLoadingMore = false
                )

                // Fetch first page (page 0)
                val results = repository.fetchRange(startDate, endDate, page = 0, limit = 30)
                
                _rangeApod.value = results
                
                // Update pagination state
                _rangePaginationState.value = _rangePaginationState.value.copy(
                    page = 1, // Next page to fetch
                    hasMore = results.size >= 30 // If we got a full page, there might be more
                )
                
                // Clear search query to show the range results
                _searchQuery.value = ""
                
                // PREDICTIVE PREFETCHING: Prefetch date range results
                if (results.isNotEmpty()) {
                    prefetchVisibleImages(results.take(12))
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unable to load date range. Please try again."
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Load more results for the current date range (infinite scroll).
     * Call this when user scrolls near the bottom of the list.
     */
    fun loadMoreRangeResults() {
        val state = _rangePaginationState.value
        
        // Guard clauses: prevent concurrent loads or loading when no more data
        if (state.isLoadingMore || !state.hasMore) return
        if (state.startDate.isEmpty()) return
        
        viewModelScope.launch {
            // Mark as loading to prevent duplicate requests
            _rangePaginationState.value = state.copy(isLoadingMore = true)
            
            try {
                // Fetch the next page
                val newResults = repository.fetchRange(
                    start = state.startDate,
                    end = state.endDate,
                    page = state.page,
                    limit = 30
                )

                val updatedList = (_rangeApod.value + newResults)
                    .distinctBy { it.date }
                
                _rangeApod.value = updatedList
                
                // Update pagination state
                _rangePaginationState.value = state.copy(
                    page = state.page + 1,
                    hasMore = newResults.size >= 30, // Full page = might have more
                    isLoadingMore = false
                )
                
                // Prefetch new batch of images
                if (newResults.isNotEmpty()) {
                    prefetchVisibleImages(newResults.take(12))
                }
            } catch (e: Exception) {
                _rangePaginationState.value = state.copy(isLoadingMore = false)
                _errorMessage.value = e.message ?: "Unable to load more photos."
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
