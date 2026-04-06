package com.example.celestis.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.glance.state.GlanceStateDefinition
import com.example.celestis.data.ApodRepository
import com.example.celestis.sync.ApodSyncWorker
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Custom GlanceStateDefinition that provides APOD widget state from the SQLDelight database.
 * 
 * This implementation:
 * - Reads the latest APOD from the local cache (no network calls)
 * - Constructs the local image file path for Glance to load
 * - Maps database entity to widget-friendly state
 * - Uses Koin for dependency injection
 * 
 * Glance will call getDataStore() to get the current state whenever the widget needs to update.
 */
object ApodWidgetStateDefinition : GlanceStateDefinition<ApodWidgetState>, KoinComponent {
    
    private val repository: ApodRepository by inject()
    
    /**
     * Called by Glance to get the current widget state.
     * This method reads from the SQLDelight database and constructs the state.
     */
    override suspend fun getDataStore(
        context: Context,
        fileKey: String
    ): DataStore<ApodWidgetState> {
        return ApodWidgetStateDataStore(context, repository)
    }
    
    /**
     * Returns the file location where Glance would typically store state.
     * Since we're using a custom data store that reads from SQLDelight,
     * this is just for Glance's internal tracking.
     */
    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "apod_widget_state_$fileKey")
    }
}

/**
 * Custom DataStore implementation that provides ApodWidgetState from the repository.
 * This bridges Glance's state system with our SQLDelight database.
 */
private class ApodWidgetStateDataStore(
    private val context: Context,
    private val repository: ApodRepository
) : DataStore<ApodWidgetState> {
    
    /**
     * Glance will collect this Flow to get state updates.
     * We map the repository's Flow to our widget state.
     */
    override val data: kotlinx.coroutines.flow.Flow<ApodWidgetState>
        get() = kotlinx.coroutines.flow.flow {
            // Collect the latest APOD from the database
            repository.observeLatestApodForWidget().collect { apodEntity ->
                val state = if (apodEntity != null) {
                    val mediaType = apodEntity.mediaType ?: "image"
                    val isVideo = mediaType.equals("video", ignoreCase = true)
                    
                    // Construct the local image/thumbnail path
                    val imagePath = ApodSyncWorker.getLocalImagePath(context, apodEntity.date)
                    
                    // For videos, we can show the widget even without a thumbnail
                    // For images, we need the image file to exist
                    val canShowWidget = if (isVideo) {
                        // Video: can show even without thumbnail (will show "Tap to view Video")
                        true
                    } else {
                        // Image: must have the image file
                        imagePath.exists()
                    }
                    
                    if (canShowWidget) {
                        ApodWidgetState.Success(
                            title = apodEntity.title ?: "Astronomy Picture of the Day",
                            date = formatDate(apodEntity.date),
                            imagePath = if (imagePath.exists()) imagePath.absolutePath else null,
                            mediaType = mediaType
                        )
                    } else {
                        // Image not downloaded yet, show loading
                        ApodWidgetState.Loading
                    }
                } else {
                    // No data in database yet
                    ApodWidgetState.NoData
                }
                
                emit(state)
            }
        }
    
    /**
     * Not used in our implementation since we read from SQLDelight.
     * Glance state is read-only from the widget's perspective.
     */
    override suspend fun updateData(transform: suspend (t: ApodWidgetState) -> ApodWidgetState): ApodWidgetState {
        // We don't support updating state from the widget
        // State updates come from ApodSyncWorker -> Database -> this DataStore
        return data.first()
    }
    
    /**
     * Formats a date string from YYYY-MM-DD to a more readable format.
     */
    private fun formatDate(dateString: String): String {
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                
                val monthName = when (month) {
                    "01" -> "Jan"
                    "02" -> "Feb"
                    "03" -> "Mar"
                    "04" -> "Apr"
                    "05" -> "May"
                    "06" -> "Jun"
                    "07" -> "Jul"
                    "08" -> "Aug"
                    "09" -> "Sep"
                    "10" -> "Oct"
                    "11" -> "Nov"
                    "12" -> "Dec"
                    else -> month
                }
                
                "$monthName $day, $year"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
