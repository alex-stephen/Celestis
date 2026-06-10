package com.alexstephen.celestis80085

import com.alexstephen.celestis80085.di.commonModule
import com.alexstephen.celestis80085.di.iosModule
import com.alexstephen.celestis80085.sync.BackgroundSyncManager
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

    NSUserDefaults(suiteName = "group.com.alexstephen.celestis80085")?.let { defaults ->
        defaults.setObject(BuildKonfig.BASE_URL, forKey = "apod_api_base_url")
        defaults.synchronize()
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
