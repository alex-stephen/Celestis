package com.example.astrolume

import com.example.astrolume.di.commonModule
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