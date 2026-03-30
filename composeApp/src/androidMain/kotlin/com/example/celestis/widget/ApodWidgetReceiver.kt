package com.example.celestis.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.celestis.sync.ApodSyncWorker

/**
 * AppWidgetProvider for the APOD widget.
 * 
 * This receiver handles widget lifecycle events:
 * - Widget added to home screen (triggers immediate sync)
 * - Widget removed from home screen
 * - Widget update requests
 * - System broadcasts
 * 
 * Glance handles most of the heavy lifting, so this is a simple delegate
 * to the ApodWidget implementation.
 */
class ApodWidgetReceiver : GlanceAppWidgetReceiver() {
    
    /**
     * The GlanceAppWidget instance that this receiver manages.
     */
    override val glanceAppWidget: GlanceAppWidget = ApodWidget()
    
    /**
     * Called when widgets are added or enabled.
     * Triggers an immediate sync if this is the first widget being added.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        
        // If widgets are being added, trigger an immediate sync
        if (appWidgetIds.isNotEmpty()) {
            Log.d(TAG, "Widget(s) added/updated: ${appWidgetIds.size}. Triggering immediate sync.")
            triggerImmediateSync(context)
        }
    }
    
    /**
     * Triggers an immediate one-time sync to populate the widget with data.
     * This ensures the widget shows content immediately instead of waiting
     * until the scheduled 6:00 AM UTC sync.
     */
    private fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val immediateSync = OneTimeWorkRequestBuilder<ApodSyncWorker>()
            .setConstraints(constraints)
            .addTag(IMMEDIATE_SYNC_TAG)
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateSync)
        Log.d(TAG, "Immediate sync work enqueued")
    }
    
    companion object {
        private const val TAG = "ApodWidgetReceiver"
        private const val IMMEDIATE_SYNC_TAG = "apod_immediate_sync"
    }
}
