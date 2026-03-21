package com.example.astrolume.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.PlatformContext
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.data.toResponse
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.utils.ImagePrefetcher
import com.example.astrolume.ui.utils.LinkGenerator
import com.example.astrolume.ui.utils.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhotoDetailUiState {
    object Loading : PhotoDetailUiState
    data class Success(
        val apod: ApodResponse,
        val selectedHdUrl: String? = null
    ) : PhotoDetailUiState
    data class Error(val message: String) : PhotoDetailUiState
}

class PhotoDetailViewModel(
    private val repository: ApodRepository,
    private val imageLoader: ImageLoader,
    private val context: PlatformContext,
    private val shareManager: ShareManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PhotoDetailUiState>(PhotoDetailUiState.Loading)
    val uiState: StateFlow<PhotoDetailUiState> = _uiState.asStateFlow()

    fun loadApodByDate(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apodEntity = repository.fetchApod(date)
                val apodResponse = apodEntity.toResponse()
                _uiState.value = PhotoDetailUiState.Success(apodResponse)
                
                // PREDICTIVE HD PREFETCHING: Preload HD image immediately
                ImagePrefetcher.prefetchApodComplete(
                    imageLoader = imageLoader,
                    context = context,
                    apod = apodResponse,
                    scope = viewModelScope,
                    includeHd = true
                )
            } catch (e: Exception) {
                _uiState.value = PhotoDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value as? PhotoDetailUiState.Success ?: return
        val apod = currentState.apod
        val newFavoriteState = !apod.isFavorite

        // Optimistic UI update
        _uiState.value = currentState.copy(
            apod = apod.copy(isFavorite = newFavoriteState)
        )

        viewModelScope.launch {
            repository.toggleFavorite(apod.date, newFavoriteState, apod)
        }
    }

    fun showHdImage(url: String?) {
        val currentState = _uiState.value as? PhotoDetailUiState.Success ?: return
        _uiState.value = currentState.copy(selectedHdUrl = url)
    }

    fun hideHdImage() {
        val currentState = _uiState.value as? PhotoDetailUiState.Success ?: return
        _uiState.value = currentState.copy(selectedHdUrl = null)
    }

    fun shareApod() {
        val state = _uiState.value as? PhotoDetailUiState.Success ?: return
        val apod = state.apod
        val deepLink = LinkGenerator.generatePhotoLink(apod.date)
        val shareText = """
            Check out this space photo: ${apod.title}
            
            View in Celestis: $deepLink
        """.trimIndent()

        shareManager.shareData(title = apod.title ?: "Celestis Share", text = shareText)
    }
}
