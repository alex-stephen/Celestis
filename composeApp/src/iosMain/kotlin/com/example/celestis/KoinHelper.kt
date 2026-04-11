package com.example.celestis

import com.example.celestis.di.commonModule
import com.example.celestis.di.iosModule
import com.example.celestis.sync.BackgroundSyncManager
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import platform.Foundation.NSUserDefaults

/**
 * This class is consumed by Swift in Xcode.
 */
class KoinHelper : KoinComponent {
    fun getAppPlatform(): Platform = get()
}

fun initKoinIos() {
    startKoin {
        modules(commonModule, iosModule)
    }
}

/**
 * Initialises Koin and seeds the App Group UserDefaults with values the widget
 * extension needs before it ever makes a network call (e.g. the API base URL).
 * Call this exactly once from Swift's App.init().
 */
@OptIn(ExperimentalForeignApi::class)
fun doInitKoin() {
    initKoinIos()

    NSUserDefaults(suiteName = "group.com.example.celestis")?.let { defaults ->
        if (defaults.stringForKey("apod_api_base_url") == null) {
            defaults.setObject(BuildKonfig.BASE_URL, forKey = "apod_api_base_url")
            defaults.synchronize()
        }
    }
}

/**
 * Returns the BackgroundSyncManager from Koin for Swift to call.
 */
fun getBackgroundSyncManager(): BackgroundSyncManager? {
    return try {
        val helper = KoinHelper()
        helper.get<BackgroundSyncManager>()
    } catch (e: Exception) {
        null
    }
}