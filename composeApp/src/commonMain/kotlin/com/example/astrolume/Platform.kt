package com.example.astrolume

import coil3.PlatformContext

interface Platform {
    val name: String
    val context: PlatformContext
}

expect fun getPlatform(): Platform