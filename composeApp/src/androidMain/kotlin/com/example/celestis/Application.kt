package com.example.celestis

import android.app.Application
import com.example.celestis.di.androidModule
import com.example.celestis.di.initKoin
import com.example.celestis.sync.BackgroundSyncManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class CelestisApp : Application() {
    
    private val syncManager: BackgroundSyncManager by inject()
    
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CelestisApp)
            androidLogger()
            modules(androidModule)
        }
        
        // Schedule daily background sync for APOD
        syncManager.scheduleDailySync()
    }
}
