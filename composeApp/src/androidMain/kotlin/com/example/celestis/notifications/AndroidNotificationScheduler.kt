package com.example.celestis.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AndroidNotificationScheduler(
    private val context: Context
) : NotificationScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleApodNotification(title: String, imageDate: String) {
        // Persist title + date so ApodNotificationReceiver can read them at fire time
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TITLE, title)
            .putString(KEY_DATE, imageDate)
            .apply()

        val triggerMs = calculateNext10AmLocal()
        val pendingIntent = buildPendingIntent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            Log.w(TAG, "Exact alarms not allowed; using inexact alarm for notification")
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        }

        Log.d(TAG, "Scheduled APOD notification for ${java.util.Date(triggerMs)}: \"$title\"")
    }

    override fun cancelPendingNotification() {
        alarmManager.cancel(buildPendingIntent())
        Log.d(TAG, "Cancelled pending APOD notification")
    }

    /**
     * Returns epoch-millis for the next 10:00:00 AM in the device's local timezone.
     * If the current time is already past 10 AM today, returns 10 AM tomorrow.
     */
    private fun calculateNext10AmLocal(): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 10)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (now.after(target)) {
            target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, ApodNotificationReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "AndroidNotificationScheduler"
        const val PREFS_NAME = "celestis_notifications"
        const val KEY_TITLE = "pending_apod_title"
        const val KEY_DATE = "pending_apod_date"
        private const val REQUEST_CODE = 1001
    }
}