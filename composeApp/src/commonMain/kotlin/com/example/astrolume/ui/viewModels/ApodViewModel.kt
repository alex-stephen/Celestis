//package com.example.astrolume.ui.viewModels
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.astrolume.data.ApodRepository
//import com.example.astrolume.data.toResponse
//import com.example.astrolume.database.ApodEntity
//import com.example.astrolume.model.ApodResponse
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//class ApodViewModel(private val repository: ApodRepository) : ViewModel() {
//    // UI State moved here from the Composable
//    private val _results = MutableStateFlow<List<ApodResponse>>(emptyList())
//    val results = _results.asStateFlow()
//
//    private val _singleApod = MutableStateFlow<ApodEntity?>(null)
//    val singleApod = _singleApod.asStateFlow()
//
//    private val _statusMessage = MutableStateFlow("Ready")
//    val statusMessage = _statusMessage.asStateFlow()
//
//    private val _isLoading = MutableStateFlow(false)
//    val isLoading = _isLoading.asStateFlow()
//
//    fun testSingle() = viewModelScope.launch {
//        executeTest("Fetching Today's APOD") {
//            val res = repository.fetchApod("2026-03-06")
//            // Convert Entity to Response for UI display
//            _results.value = listOf(res.toResponse())
//        }
//    }
//
//    fun testRandom() = viewModelScope.launch {
//        executeTest("Fetching 5 Random Items") {
//            _results.value = repository.fetchRandom(5)
//        }
//    }
//
//    fun testRange() = viewModelScope.launch {
//        executeTest("Fetching 7 Day Range") {
//            _results.value = repository.fetchRange("2024-01-01", "2024-01-07")
//        }
//    }
//
//    fun testSearch(q: String) = viewModelScope.launch {
//        executeTest("Searching Atlas for '$q'") {
//            _results.value = repository.search(q)
//        }
//    }
//
//    private suspend fun executeTest(label: String, block: suspend () -> Unit) {
//        _isLoading.value = true
//        _statusMessage.value = label
//        try {
//            block()
//            _statusMessage.value = "$label: Success"
//        } catch (e: Exception) {
//            _statusMessage.value = "Error: ${e.message}"
//        } finally {
//            _isLoading.value = false
//        }
//    }
//    fun search(query: String) {
//        viewModelScope.launch {
//            _statusMessage.value = "Searching..."
//            _results.value = repository.search(query)
//            _statusMessage.value = "Done"
//        }
//    }
//}