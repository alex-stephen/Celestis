package com.example.astrolume.di

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.example.astrolume.Platform
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.network.createHttpClient
import com.example.astrolume.service.NasaApi
import com.example.astrolume.ui.viewModels.ApodViewModel
import com.example.astrolume.ui.viewModels.DiscoverViewModel
import com.example.astrolume.ui.viewModels.HomeViewModel
import io.ktor.client.HttpClient
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
            .build()
    }

    viewModel { ApodViewModel(get()) }

    viewModel { DiscoverViewModel(get()) }

    viewModel { HomeViewModel(get()) }
}