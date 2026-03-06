package com.example.astrolume

import android.app.Application
import com.example.astrolume.di.androidModule
import com.example.astrolume.di.initKoin // We'll define this in step 4
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class AstrolumeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@AstrolumeApp)
            androidLogger()
            modules(androidModule) // Add the driver provider we just made
        }
    }
}
