package com.example.astrolume.di

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.size.Precision
import coil3.util.DebugLogger
import com.example.astrolume.Platform
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.network.createHttpClient
import com.example.astrolume.service.NasaApi
import com.example.astrolume.ui.viewModels.DiscoverViewModel
import com.example.astrolume.ui.viewModels.FavoriteViewModel
import com.example.astrolume.ui.viewModels.HomeViewModel
import com.example.astrolume.ui.viewModels.PhotoDetailViewModel
import io.ktor.client.HttpClient
import okio.FileSystem
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@OptIn(ExperimentalCoilApi::class)
val commonModule = module {
    single { createHttpClient() }

    single { NasaApi(get()) }

    single { ApodRepository(get(), get(), get(), get())}

    single {
        ImageLoader.Builder(get<Platform>().context)
            .components {
                // Add Ktor as the network engine for Coil
                add(KtorNetworkFetcherFactory(get<HttpClient>()))
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
            // Optimization: Don't force exact pixel matching for HD popups
            // to save memory on downsampling
            .precision(Precision.INEXACT)
            .crossfade(true)
            .build()
    }

    viewModel { DiscoverViewModel(get(), get(), get<Platform>().context) }

    viewModel { FavoriteViewModel(get()) }

    viewModel { HomeViewModel(get(), get(), get<Platform>().context) }

    viewModel { PhotoDetailViewModel(get(), get(), get<Platform>().context) }
}
