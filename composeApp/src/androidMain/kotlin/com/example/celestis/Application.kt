package com.example.celestis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.celestis.data.ApodRepository
import com.example.celestis.di.androidModule
import com.example.celestis.di.initKoin
import com.example.celestis.sync.ApodSyncWorker
import com.example.celestis.sync.BackgroundSyncManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class CelestisApp : Application() {

    private val syncManager: BackgroundSyncManager by inject()
    private val repository: ApodRepository by inject()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        // Firebase must be initialised before anything that depends on it (FCM, Crashlytics, etc.)
        FirebaseApp.initializeApp(this)

        super.onCreate()
        initKoin {
            androidContext(this@CelestisApp)
            androidLogger()
            modules(androidModule)
        }

        createNotificationChannel()

        // Schedule daily background sync for APOD
        syncManager.scheduleDailySync()

        // Trigger immediate sync on first launch if no data exists
        triggerInitialSyncIfNeeded()
    }

    /**
     * Creates the notification channel required on Android 8+ (API 26+).
     * Must be created before any notification is posted; safe to call repeatedly.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Astronomy Picture of the Day",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily photo of the day notification at 10 AM"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Checks if we have any cached APOD data. If not, triggers an immediate sync.
     * This ensures widgets and the app have data available immediately after installation.
     */
    private fun triggerInitialSyncIfNeeded() {
        applicationScope.launch {
            try {
                val apods = repository.observeAllCachedApods().first()
                if (apods.isEmpty()) {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    val immediateSync = OneTimeWorkRequestBuilder<ApodSyncWorker>()
                        .setConstraints(constraints)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .addTag("initial_sync")
                        .build()

                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        "initial_sync",
                        ExistingWorkPolicy.KEEP,
                        immediateSync
                    )
                    android.util.Log.d("CelestisApp", "No cached data found. Triggering expedited initial sync.")
                }
            } catch (e: Exception) {
                android.util.Log.e("CelestisApp", "Error checking for cached data", e)
            }
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "celestis_daily_apod"
    }
}