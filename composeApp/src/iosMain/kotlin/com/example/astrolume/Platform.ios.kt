package com.example.astrolume

import coil3.PlatformContext
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.ui.utils.IosShareManager
import com.example.astrolume.ui.utils.ShareManager
import org.koin.dsl.module

class IOSPlatform : Platform {
    override val name: String = "iOS"
    override val context: PlatformContext = PlatformContext.INSTANCE
}

val iosModule = module {
    single<Platform> { IOSPlatform() }

    single<AppDatabase> {
        AppDatabase(
            driver = get(),
        )
    }
    single<ShareManager> { IosShareManager() }
}

actual fun getPlatform(): Platform = IOSPlatform()