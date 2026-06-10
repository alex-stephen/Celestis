package com.alexstephen.celestis80085.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.alexstephen.celestis80085.data.ApodRepository
import com.alexstephen.celestis80085.data.toResponse
import com.alexstephen.celestis80085.model.ApodResponse
import com.alexstephen.celestis80085.network.NetworkMonitor
import com.alexstephen.celestis80085.ui.utils.ImagePrefetcher
import com.alexstephen.celestis80085.ui.utils.LinkGenerator
import com.alexstephen.celestis80085.ui.utils.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val todayApod: ApodResponse,
        val randomApod: ApodResponse? = null,
        val selectedHdUrl: String? = null,
        val isOfflineMode: Boolean = false
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: ApodRepository,
    private val imageLoader: ImageLoader,
    private val context: PlatformContext,
    private val shareManager: ShareManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val todayApodFlow = repository.observeLatestApod()

    // The Random Stream (Managed in memory, then saved if favorited)
    private val _randomApod = MutableStateFlow<ApodResponse?>(null)
    private val _selectedHdUrl = MutableStateFlow<String?>(null)
    
    // Track image loading state for synchronized transitions
    private val _isImageLoading = MutableStateFlow(false)
    val isImageLoading: StateFlow<Boolean> = _isImageLoading.asStateFlow()
    
    // Internal State - Derived from today's APOD, random selection, and network connectivity
    val uiState: StateFlow<HomeUiState> = combine(
        todayApodFlow,
        _randomApod,
        _selectedHdUrl,
        networkMonitor.isOnline
    ) { today, random, hdUrl, isOnline ->
        if (today == null) {
            HomeUiState.Loading
        } else {
            HomeUiState.Success(
                todayApod = today,
                randomApod = random,
                selectedHdUrl = hdUrl,
                isOfflineMode = !isOnline
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    private val _isShowingRandom = MutableStateFlow(false)
    val isShowingRandom: StateFlow<Boolean> = _isShowingRandom.asStateFlow()

    private val randomQueue = ArrayDeque<ApodResponse>()
    private val TARGET_CAPACITY = 20  
    private val REFILL_THRESHOLD = 10 
    private val BATCH_SIZE = 10

    private val _isRefilling = MutableStateFlow(false)
    val isRefilling: StateFlow<Boolean> = _isRefilling.asStateFlow()
    private var prefetchJob: Job? = null


    init {
        checkAndRefreshTodayApod()
        refillQueue(initial = true)

        viewModelScope.launch(Dispatchers.IO) {
            repository.pruneCacheIfNeeded()
        }
    }
    
    /**
     * Checks if we need to fetch today's APOD. This should be called:
     * - On app launch (init)
     * - When app returns from background (via onResume)
     * 
     * This ensures the user always sees the latest APOD without manual refresh.
     */
    fun checkAndRefreshTodayApod() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = Clock.System.now().toEpochMilliseconds()

                val currentApod = todayApodFlow.firstOrNull()
                
                if (currentApod == null || currentApod.date != now.toString()) {
                    repository.refreshLatest()
                }
            } catch (e: Exception) {
                // Silent fail - we'll show cached data
            }
        }
    }

    /**
     * The Snappy Random Logic: Updates the UI immediately so the transition starts right away,
     * then warms the next queued image in the background so subsequent taps are instant.
     */
    fun showNextRandom() {
        if (randomQueue.isEmpty()) {
            fetchEmergencySingle()
            return
        }

        viewModelScope.launch {
            _isImageLoading.value = true

            val next = randomQueue.removeFirst()

            // Update UI immediately — shimmer covers while Coil loads from network/disk
            _randomApod.value = next
            _isShowingRandom.value = true

            // Pre-warm the next item so the following tap is instant
            randomQueue.firstOrNull()?.url?.let { nextUrl ->
                launch(Dispatchers.IO) {
                    imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(nextUrl)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    )
                }
            }

            // Check if we need to top up the tank
            if (randomQueue.size <= REFILL_THRESHOLD) {
                refillQueue()
            }
        }
    }

    /** Called by the UI when the AsyncImage finishes rendering — clears the shimmer. */
    fun onImageLoaded() {
        _isImageLoading.value = false
    }
    
    private fun refillQueue(initial: Boolean = false) {
        if (prefetchJob?.isActive == true) return

        prefetchJob = viewModelScope.launch(Dispatchers.Default) {
            _isRefilling.value = true
            try {
                val needed = if (initial) 5 else BATCH_SIZE // Start with 5 for faster initial load
                val isOnline = networkMonitor.isOnline.firstOrNull() ?: true
                
                val newItems = if (isOnline) {
                    // Online: Fetch from API
                    repository.fetchRandom(needed)
                } else {
                    // Offline: Get random from cache
                    repository.getRandomCachedApods(needed).map { it.toResponse() }
                }

                randomQueue.addAll(newItems)
                
                // Cache images in background (only if online, images should already be cached if offline)
                if (isOnline) {
                    ImagePrefetcher.prefetchApodBatch(
                        imageLoader = imageLoader,
                        context = context,
                        apods = newItems,
                        scope = this
                    )
                }

                if (initial && _randomApod.value == null) {
                    _randomApod.value = randomQueue.removeFirstOrNull()
                }
                
                // If we initially only fetched 5, fill up the rest of the target capacity now
                if (initial && randomQueue.size < TARGET_CAPACITY) {
                     val remaining = TARGET_CAPACITY - randomQueue.size
                     val moreItems = if (isOnline) {
                         repository.fetchRandom(remaining)
                     } else {
                         repository.getRandomCachedApods(remaining).map { it.toResponse() }
                     }
                     randomQueue.addAll(moreItems)
                     if (isOnline) {
                         ImagePrefetcher.prefetchApodBatch(imageLoader, context, moreItems, this)
                     }
                }
            } catch (e: Exception) {
                // Network error - silent fallback to cache
                try {
                    val cachedItems = repository.getRandomCachedApods(if (initial) 5 else BATCH_SIZE)
                        .map { it.toResponse() }
                    randomQueue.addAll(cachedItems)
                    if (initial && _randomApod.value == null) {
                        _randomApod.value = randomQueue.removeFirstOrNull()
                    }
                } catch (cacheError: Exception) {
                    // No cached items available
                }
            } finally {
                _isRefilling.value = false
            }
        }
    }

    private fun fetchEmergencySingle() {
        viewModelScope.launch(Dispatchers.Default) {
            _isRefilling.value = true
            try {
                val isOnline = networkMonitor.isOnline.firstOrNull() ?: true
                val item = if (isOnline) {
                    repository.fetchRandom(1).firstOrNull()
                } else {
                    repository.getRandomCachedApods(1).firstOrNull()?.toResponse()
                }
                _randomApod.value = item
            } catch (e: Exception) {
                // Try cache as fallback
                try {
                    _randomApod.value = repository.getRandomCachedApods(1).firstOrNull()?.toResponse()
                } catch (cacheError: Exception) {
                    // No cached items
                }
            } finally {
                _isRefilling.value = false
                refillQueue() 
            }
        }
    }

    fun showToday() {
        _isShowingRandom.value = false
    }

    fun toggleFavorite(date: String, isFav: Boolean) {
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshLatest()
        }
    }
    
    /**
     * Called when app resumes from background to check for new APOD.
     */
    fun onAppResume() {
        checkAndRefreshTodayApod()
    }

    /**
     * Shows the standard image. HD is avoided here to prevent large downloads
     * on weak Wi-Fi or cellular data.
     */
    fun showHdImage(hdUrl: String?, standardUrl: String?) {
        _selectedHdUrl.value = standardUrl ?: hdUrl
    }

    fun hideHdImage() {
        _selectedHdUrl.value = null
    }

    fun shareApod() {
        val state = uiState.value as? HomeUiState.Success ?: return
        val apod = when {
            isShowingRandom.value -> state.randomApod
            else -> state.todayApod
        } ?: return
        
        val deepLink = LinkGenerator.generatePhotoLink(apod.date)
        val shareText = """
            Check out this space photo: ${apod.title}
            
            View in Celestis: $deepLink
        """.trimIndent()

        shareManager.shareData(title = apod.title ?: "Celestis Share", text = shareText)
    }
}
