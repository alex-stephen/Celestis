package com.example.celestis

import android.app.Application
import com.example.celestis.di.androidModule
import com.example.celestis.di.initKoin // We'll define this in step 4
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class CelestisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CelestisApp)
            androidLogger()
            modules(androidModule) // Add the driver provider we just made
        }
    }
}
