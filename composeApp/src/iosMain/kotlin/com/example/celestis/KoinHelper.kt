package com.example.celestis

import com.example.celestis.di.commonModule
import com.example.celestis.di.iosModule
import com.example.celestis.sync.BackgroundSyncManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

/**
 * This class is consumed by Swift in Xcode
 */
class KoinHelper : KoinComponent {
    // Allows Swift to get the ViewModel or Repository easily
    fun getAppPlatform(): Platform = get()
}

fun initKoinIos() {
    startKoin {
        modules(commonModule, iosModule)
    }
}

/**
 * Helper function for Swift to initialize Koin
 */
fun doInitKoin() {
    initKoinIos()
}

/**
 * Helper function for Swift to get BackgroundSyncManager from Koin
 */
fun getBackgroundSyncManager(): BackgroundSyncManager? {
    return try {
        val helper = KoinHelper()
        helper.get<BackgroundSyncManager>()
    } catch (e: Exception) {
        null
    }
}

