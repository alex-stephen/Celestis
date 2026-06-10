package com.alexstephen.celestis80085.di

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.size.Precision
import com.alexstephen.celestis80085.data.AppSettingsRepository
import com.alexstephen.celestis80085.Platform
import com.alexstephen.celestis80085.data.ApodRepository
import com.alexstephen.celestis80085.network.createHttpClient
import com.alexstephen.celestis80085.service.NasaApi
import com.alexstephen.celestis80085.ui.viewModels.DiscoverViewModel
import com.alexstephen.celestis80085.ui.viewModels.FavoriteViewModel
import com.alexstephen.celestis80085.ui.viewModels.HomeViewModel
import com.alexstephen.celestis80085.ui.viewModels.PhotoDetailViewModel
import com.alexstephen.celestis80085.ui.viewModels.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import okio.FileSystem
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@OptIn(ExperimentalCoilApi::class)
val commonModule = module {
    single { createHttpClient() }

    single { NasaApi(get()) }

    single { ApodRepository(get(), get(), get(), get())}

    single { AppSettingsRepository(get(), get()) }

    // Separate HttpClient for Coil with 6-second timeout for HD image downloads
    single(qualifier = org.koin.core.qualifier.named("coilHttpClient")) {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 6_000 // 6 second timeout for HD downloads
                connectTimeoutMillis = 6_000
                socketTimeoutMillis = 6_000
            }
        }
    }

    single {
        ImageLoader.Builder(get<Platform>().context)
            .components {
                // Add Ktor as the network engine for Coil
                // Uses dedicated HttpClient with 6-second timeout
                add(KtorNetworkFetcherFactory(
                    httpClient = { get<HttpClient>(qualifier = org.koin.core.qualifier.named("coilHttpClient")) }
                ))
            }
            .memoryCache {
                MemoryCache.Builder()
                    // Allocate 25% of available RAM for image caching
                    .maxSizePercent(get<Platform>().context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                    .maxSizeBytes(512L * 1024L * 1024L) // 512MB Disk Cache
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            // Optimization: Don't force exact pixel matching for HD popups
            // to save memory on downsampling
            .precision(Precision.INEXACT)
            .crossfade(true)
            .build()
    }

    viewModel { DiscoverViewModel(get(), get(), get<Platform>().context, get())}

    viewModel { FavoriteViewModel(get()) }

    viewModel { HomeViewModel(get(), get(), get<Platform>().context, get(), get(), get()) }

    viewModel { PhotoDetailViewModel(get(), get(), get<Platform>().context, get(), get()) }

    viewModel { SettingsViewModel(get(), get(), get()) }
}
