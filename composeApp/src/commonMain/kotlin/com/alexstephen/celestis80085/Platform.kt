package com.alexstephen.celestis80085

import coil3.PlatformContext

interface Platform {
    val name: String
    val context: PlatformContext
}

expect fun getPlatform(): Platform