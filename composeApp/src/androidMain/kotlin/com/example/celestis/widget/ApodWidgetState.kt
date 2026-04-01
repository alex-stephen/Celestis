package com.example.celestis.widget

/**
 * Represents the UI state of the APOD widget.
 * This sealed class ensures type-safe state management in Glance.
 */
sealed class ApodWidgetState {
    /**
     * Successfully loaded APOD data from the cache.
     * 
     * @param title The APOD title
     * @param date The APOD date (YYYY-MM-DD format)
     * @param imagePath Absolute file path to the locally cached image
     */
    data class Success(
        val title: String,
        val date: String,
        val imagePath: String
    ) : ApodWidgetState()
    
    /**
     * Widget is loading or initializing.
     * Shown on first install or while waiting for WorkManager to sync.
     */
    data object Loading : ApodWidgetState()
    
    /**
     * No APOD data available in the cache.
     * This should rarely happen after the first sync.
     */
    data object NoData : ApodWidgetState()
    
    /**
     * Error occurred while fetching or loading APOD data.
     * Shows a user-friendly error message with retry option.
     */
    data class Error(
        val message: String = "Unable to load APOD"
    ) : ApodWidgetState()
}
