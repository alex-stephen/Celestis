package com.alexstephen.celestis80085

import coil3.PlatformContext
import com.alexstephen.celestis80085.database.AppDatabase
import com.alexstephen.celestis80085.ui.utils.IosShareManager
import com.alexstephen.celestis80085.ui.utils.ShareManager
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