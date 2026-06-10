package com.alexstephen.celestis80085.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.alexstephen.celestis80085.CelestisApp
import com.alexstephen.celestis80085.MainActivity
import com.alexstephen.celestis80085.R
import com.alexstephen.celestis80085.data.ApodRepository
import com.alexstephen.celestis80085.sync.ApodSyncWorker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Expedited one-shot worker triggered by [ApodNotificationReceiver] at 10:00 AM local time.
 *
 * Determines TODAY's date at fire time — never reads a pre-stored date from SharedPrefs —
 * so the notification always displays the correct day's APOD regardless of when the FCM
 * silent push or [ApodSyncWorker] ran.
 *
 * Flow:
 * 1. Resolve today's date in the device's local timezone.
 * 2. Fetch today's APOD: SQLDelight cache first, network fallback.
 * 3. Ensure the image file is on disk; download via Coil if missing.
 * 4. Post a BigPictureStyle notification (or BigTextStyle if no image).
 */
class PostNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: ApodRepository by inject()
    private val imageLoader: ImageLoader by inject()

    override suspend fun doWork(): Result {
        // Always use today's local date — this is the single source of truth.
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

        val apod = try {
            repository.fetchApod(today)
        } catch (e: Exception) {
            // Network unavailable and today's APOD not in cache yet.
            // Fall back to whatever is newest in the DB rather than silently skipping.
            Log.w(TAG, "Could not fetch today's APOD ($today), falling back to latest cached: ${e.message}")
            repository.observeLatestApod().firstOrNull()?.let { response ->
                // Convert ApodResponse back to an entity-like object we can use
                return@let try {
                    repository.fetchApod(response.date)
                } catch (inner: Exception) {
                    null
                }
            }
        }

        if (apod == null) {
            Log.w(TAG, "No APOD data available — skipping notification")
            return Result.failure()
        }

        val imageUrl = if (apod.mediaType.equals("video", ignoreCase = true)) {
            apod.thumbnailUrl ?: apod.url
        } else {
            apod.url
        }

        ensureImageOnDisk(imageUrl, apod.date)
        postNotification(
            title = apod.title ?: "Astronomy Picture of the Day",
            date = apod.date
        )
        return Result.success()
    }

    /**
     * Downloads the APOD image to the local image directory if it isn't already there.
     * Uses Coil's disk cache as the source so we avoid a redundant network hit when the
     * image was already fetched for the widget.
     */
    private suspend fun ensureImageOnDisk(imageUrl: String?, date: String) {
        if (imageUrl == null) return
        val dest = ApodSyncWorker.getLocalImagePath(applicationContext, date)
        if (dest.exists()) {
            Log.d(TAG, "Image already on disk for $date")
            return
        }
        Log.d(TAG, "Image missing for $date — downloading now")
        try {
            val request = ImageRequest.Builder(applicationContext)
                .data(imageUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                imageLoader.diskCache?.openSnapshot(imageUrl)?.use { snapshot ->
                    dest.parentFile?.mkdirs()
                    snapshot.data.toFile().copyTo(dest, overwrite = true)
                    Log.d(TAG, "Image saved to disk for $date")
                } ?: Log.w(TAG, "Coil disk cache miss for $date — notification will have no image")
            } else {
                Log.w(TAG, "Coil failed to load image for $date — notification will have no image")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Image download failed for $date: ${e.message}")
        }
    }

    private fun postNotification(title: String, date: String) {
        val imageFile = ApodSyncWorker.getLocalImagePath(applicationContext, date)
        val bitmap = if (imageFile.exists()) {
            BitmapFactory.decodeFile(imageFile.absolutePath)
        } else null

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NOTIFICATION_DATE, date)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            applicationContext, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, CelestisApp.NOTIFICATION_CHANNEL_ID)
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

        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, builder.build())

        Log.d(TAG, "Posted APOD notification: \"$title\" ($date)")
    }

    companion object {
        private const val TAG = "PostNotificationWorker"
        private const val NOTIFICATION_ID = 42
        private const val WORK_NAME = "post_apod_notification"

        fun enqueue(context: Context) {
            val work = OneTimeWorkRequestBuilder<PostNotificationWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            // REPLACE so a duplicate alarm or rapid retry always kicks off a fresh attempt.
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                work
            )
            Log.d(TAG, "PostNotificationWorker enqueued")
        }
    }
}