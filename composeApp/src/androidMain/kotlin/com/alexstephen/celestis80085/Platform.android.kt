package com.alexstephen.celestis80085

import android.content.Context
import coil3.PlatformContext

class AndroidPlatform(private val androidContext: Context) : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"

    override val context: PlatformContext = androidContext as PlatformContext
}

actual fun getPlatform(): Platform {
    return object : Platform {
        override val name: String = "Android"
        override val context: PlatformContext = org.koin.core.context.GlobalContext.get().get<Context>() as PlatformContext
    }
}