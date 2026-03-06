package com.example.astrolume.di

import com.example.astrolume.data.ApodRepository
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.network.createHttpClient
import com.example.astrolume.service.NasaApi
import com.example.astrolume.ui.viewModels.ApodViewModel
import com.example.astrolume.ui.viewModels.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    // 1. Provide the HttpClient (previously configured with your "Turbo" settings)
    single { createHttpClient() }

    // 2. Provide the API Service
    single { NasaApi(get()) }

    single { AppDatabase(get()) }

    // 3. Provide the Repository
    // Note: 'get()' automatically finds the NasaApi and AppDatabase
    single { ApodRepository(get(), get()) }

    viewModel { ApodViewModel(get()) }

    viewModel { HomeViewModel(get()) }
}