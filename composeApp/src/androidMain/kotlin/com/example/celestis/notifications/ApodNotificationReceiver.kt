package com.example.celestis.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.celestis.CelestisApp
import com.example.celestis.MainActivity
import com.example.celestis.R
import com.example.celestis.sync.ApodSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager at 10:00 AM local time.
 *
 * Reads the pending APOD title and date written by [AndroidNotificationScheduler],
 * loads the image already downloaded by [ApodSyncWorker], and posts a
 * [NotificationCompat.BigPictureStyle] notification.
 *
 * Tapping the notification opens [MainActivity] directly via an explicit Intent,
 * passing the APOD date as an extra so the app navigates straight to the detail screen.
 * No browser is involved.
 */
class ApodNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showNotification(context)
            } finally {
                result.finish()
            }
        }
    }

    private fun showNotification(context: Context) {
        val prefs = context.getSharedPreferences(
            AndroidNotificationScheduler.PREFS_NAME, Context.MODE_PRIVATE
        )
        val title = prefs.getString(AndroidNotificationScheduler.KEY_TITLE, null) ?: run {
            Log.w(TAG, "No pending APOD title found — skipping notification")
            return
        }
        val date = prefs.getString(AndroidNotificationScheduler.KEY_DATE, null) ?: ""

        // Load the locally cached APOD image 
        val imageFile = ApodSyncWorker.getLocalImagePath(context, date)
        val bitmap = if (imageFile.exists()) BitmapFactory.decodeFile(imageFile.absolutePath) else null

        // Explicit Intent — opens MainActivity directly, no browser chooser involved.
        // MainActivity reads EXTRA_NOTIFICATION_DATE and navigates to the detail screen.
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NOTIFICATION_DATE, date)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CelestisApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Photo of the Day")
            .setContentText("Checkout the photo of the day! $title")
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        if (bitmap != null) {
            builder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as android.graphics.Bitmap?)
                        .setSummaryText(title)
                )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText("Checkout the photo of the day! $title")
            )
        }

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, builder.build())

        Log.d(TAG, "Posted APOD notification: \"$title\"")
    }

    companion object {
        private const val TAG = "ApodNotificationReceiver"
        private const val NOTIFICATION_ID = 42
    }
}