package com.example.celestis.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/**
 * Main APOD widget implementation using Jetpack Glance.
 * 
 * Features:
 * - Responsive sizing: Adapts layout based on widget size
 * - No network calls: Reads from local cache and pre-downloaded images
 * - Reactive updates: Observes SQLDelight database for changes
 * - Modular UI: Separate composables for different sizes
 * 
 * Size Modes:
 * - Small (100x100): Image only
 * - Medium (250x200): Image + Title + Date
 * - Large (300x250): Image + Title + Date
 * 
 * Usage:
 * - Long press on home screen
 * - Select "Widgets"
 * - Find "Celestis APOD"
 * - Drag to home screen
 */
class ApodWidget : GlanceAppWidget() {
    
    /**
     * Responsive sizing configuration.
     * Glance will choose the best layout based on the widget's allocated space.
     */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),  // Small - Image only
            DpSize(250.dp, 200.dp),  // Medium - Image + Title + Date
            DpSize(300.dp, 250.dp)   // Large - Image + Title + Date (more space)
        )
    )
    
    /**
     * State definition that provides widget state from the SQLDelight database.
     */
    override val stateDefinition = ApodWidgetStateDefinition
    
    /**
     * Called by Glance to provide the widget's content.
     * This is where we compose the UI based on the current state.
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(content = {
                ApodWidgetContent()
            })
        }
    }
}
