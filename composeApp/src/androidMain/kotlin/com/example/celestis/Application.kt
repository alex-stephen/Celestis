package com.example.celestis

import android.app.Application
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
        super.onCreate()
        initKoin {
            androidContext(this@CelestisApp)
            androidLogger()
            modules(androidModule)
        }
        
        // Schedule daily background sync for APOD
        syncManager.scheduleDailySync()
        
        // Trigger immediate sync on first launch if no data exists
        triggerInitialSyncIfNeeded()
    }
    
    /**
     * Checks if we have any cached APOD data. If not, triggers an immediate sync.
     * This ensures widgets and the app have data available immediately after installation.
     */
    private fun triggerInitialSyncIfNeeded() {
        applicationScope.launch {
            try {
                // Check if we have any cached data
                val apods = repository.observeAllCachedApods().first()
                if (apods.isEmpty()) {
                    // No data exists, trigger immediate sync
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
}
