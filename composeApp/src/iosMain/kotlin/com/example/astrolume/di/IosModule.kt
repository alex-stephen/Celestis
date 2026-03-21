package com.example.astrolume.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.astrolume.IosPlatform
import com.example.astrolume.Platform
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.network.NetworkMonitor
import com.example.astrolume.ui.utils.IosShareManager
import com.example.astrolume.ui.utils.ShareManager
import org.koin.dsl.module

val iosModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = "astrolume.db"
        )
    }

    single<AppDatabase> {
        AppDatabase(driver = get())
    }

    single<Platform> { IosPlatform() }
    single<ShareManager> { IosShareManager() }
    single { NetworkMonitor() }
}
