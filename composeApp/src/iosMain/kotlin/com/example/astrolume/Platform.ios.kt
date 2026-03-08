package com.example.astrolume

import coil3.PlatformContext
import com.example.astrolume.database.AppDatabase
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
}

actual fun getPlatform(): Platform = IOSPlatform()