package com.alexstephen.celestis80085.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Fired by AlarmManager at 10:00 AM local time.
 *
 * This receiver's only job is to hand off to [PostNotificationWorker], which
 * determines today's date at that moment, fetches the APOD (DB cache → network),
 * ensures the image is on disk, and posts the notification.
 *
 * Keeping this receiver trivial means we are never blocked on a stale SharedPrefs
 * date written hours earlier — the worker always uses today's local date.
 */
class ApodNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmManager fired — enqueueing PostNotificationWorker")
        PostNotificationWorker.enqueue(context)
    }

    companion object {
        private const val TAG = "ApodNotificationReceiver"
    }
}