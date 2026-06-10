package com.alexstephen.celestis80085.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.alexstephen.celestis80085.sync.ApodSyncWorker

/**
 * AppWidgetProvider for the APOD widget.
 *
 * Handles widget lifecycle events and ensures the displayed APOD is always
 * up-to-date:
 *
 * • **Widget added / periodic update** (`onUpdate`) – triggers an expedited
 *   one-time sync so the widget shows content immediately.
 *
 * • **Date change** (`ACTION_DATE_CHANGED`, `ACTION_TIME_SET`) – triggers an
 *   expedited sync as soon as the system clock rolls over to a new day.  This
 *   is the primary safeguard against stale "yesterday's APOD" on the widget:
 *   even if WorkManager's 24-hour periodic job is delayed by Doze mode or
 *   OEM battery restrictions, the date-change broadcast fires reliably at
 *   midnight local time and kicks off a fresh sync.
 *
 * Glance handles most of the heavy lifting; this is a lean delegate to
 * [ApodWidget].
 */
class ApodWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ApodWidget()

    /**
     * Called when widgets are added or when the system requests a periodic
     * update (controlled by `updatePeriodMillis` in apod_widget_info.xml).
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        if (appWidgetIds.isNotEmpty()) {
            Log.d(TAG, "onUpdate – ${appWidgetIds.size} widget(s). Triggering immediate sync.")
            triggerImmediateSync(context)
        }
    }

    /**
     * Intercepts system broadcasts that signal a date change so we can kick
     * off a fresh sync the moment the day rolls over, independent of
     * WorkManager's periodic schedule.
     *
     * Handled actions:
     * • `ACTION_DATE_CHANGED` – fired at local midnight by the OS.
     * • `ACTION_TIME_CHANGED` – fired when the user or NTP adjusts the clock
     *                           (can also cause a date change).
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED -> {
                Log.d(TAG, "Date/time change detected (${intent.action}). Triggering sync.")
                // Use REPLACE so we always start a fresh sync on a new day,
                // even if a previous immediate sync is still pending.
                triggerImmediateSync(context, policy = ExistingWorkPolicy.REPLACE)
            }
        }
    }

    /**
     * Enqueues an expedited one-time sync.
     *
     * @param policy How to handle a pre-existing pending sync with the same
     *               unique name.  Defaults to [ExistingWorkPolicy.KEEP] for
     *               normal periodic updates (avoids cancelling an in-flight
     *               download).  Date-change events use [ExistingWorkPolicy.REPLACE]
     *               to guarantee a fresh attempt on the new day.
     */
    private fun triggerImmediateSync(
        context: Context,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateSync = OneTimeWorkRequestBuilder<ApodSyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(IMMEDIATE_SYNC_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC_TAG,
            policy,
            immediateSync
        )

        Log.d(TAG, "Expedited immediate sync enqueued (policy=$policy)")
    }

    companion object {
        private const val TAG = "ApodWidgetReceiver"
        private const val IMMEDIATE_SYNC_TAG = "apod_immediate_sync"
    }
}