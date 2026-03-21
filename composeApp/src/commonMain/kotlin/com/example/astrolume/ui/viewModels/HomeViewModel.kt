package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.utils.ImagePrefetcher
import com.example.astrolume.ui.utils.LinkGenerator
import com.example.astrolume.ui.utils.ShareManager
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
        val randomApod: ApodResponse? = null,
        val selectedHdUrl: String? = null
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: ApodRepository,
    private val imageLoader: ImageLoader,
    private val context: PlatformContext,
    private val shareManager: ShareManager
) : ViewModel() {

    private val todayApodFlow = repository.observeLatestApod()

    // 2. The Random Stream (Managed in memory, then saved if favorited)
    private val _randomApod = MutableStateFlow<ApodResponse?>(null)
    private val _selectedHdUrl = MutableStateFlow<String?>(null)
    
    // Internal State - Derived from today's APOD and current random selection
    val uiState: StateFlow<HomeUiState> = combine(
        todayApodFlow,
        _randomApod,
        _selectedHdUrl
    ) { today, random, hdUrl ->
        if (today == null) {
            HomeUiState.Loading
        } else {
            HomeUiState.Success(
                todayApod = today,
                randomApod = random,
                selectedHdUrl = hdUrl
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
            fetchEmergencySingle()
            return
        }

        val next = randomQueue.removeFirst()
        _randomApod.value = next
        _isShowingRandom.value = true

        next.urlHD?.let { hdUrl ->
            viewModelScope.launch(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(hdUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                imageLoader.enqueue(request)
            }
        }

        // Check if we need to top up the tank
        if (randomQueue.size <= REFILL_THRESHOLD) {
            refillQueue()
        }
    }

    private fun refillQueue(initial: Boolean = false) {
        if (prefetchJob?.isActive == true) return

        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            _isRefilling.value = true
            try {
                // Efficient Batching: Only fetch what's needed to hit target
                val needed = if (initial) 5 else BATCH_SIZE // Start with 5 for faster initial load
                val newItems = repository.fetchRandom(needed)

                randomQueue.addAll(newItems)
                
                // PREDICTIVE PREFETCHING: Cache images in background
                ImagePrefetcher.prefetchApodBatch(
                    imageLoader = imageLoader,
                    context = context,
                    apods = newItems,
                    scope = this
                )

                if (initial && _randomApod.value == null) {
                    _randomApod.value = randomQueue.removeFirstOrNull()
                }
                
                // If we initially only fetched 5, fill up the rest of the target capacity now
                if (initial && randomQueue.size < TARGET_CAPACITY) {
                     val remaining = TARGET_CAPACITY - randomQueue.size
                     val moreItems = repository.fetchRandom(remaining)
                     randomQueue.addAll(moreItems)
                     ImagePrefetcher.prefetchApodBatch(imageLoader, context, moreItems, this)
                }
            } catch (e: Exception) {
                // Network error - silent fallback
            } finally {
                _isRefilling.value = false
            }
        }
    }

    private fun fetchEmergencySingle() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefilling.value = true
            try {
                val item = repository.fetchRandom(1).firstOrNull()
                _randomApod.value = item
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

    fun showHdImage(url: String?) {
        _selectedHdUrl.value = url
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
