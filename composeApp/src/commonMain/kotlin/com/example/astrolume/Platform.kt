package com.example.astrolume

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform