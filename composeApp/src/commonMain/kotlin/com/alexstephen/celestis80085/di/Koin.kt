package com.alexstephen.celestis80085.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = {}) = startKoin {
    config?.invoke(this)
    modules(commonModule)
}

// Helper for iOS setup
fun initKoin() = initKoin {}