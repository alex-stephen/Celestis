package com.example.celestis

import coil3.PlatformContext

interface Platform {
    val name: String
    val context: PlatformContext
}

expect fun getPlatform(): Platform