package com.example.celestis.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.celestis.IOSPlatform
import com.example.celestis.Platform
import com.example.celestis.database.AppDatabase
import com.example.celestis.network.NetworkMonitor
import com.example.celestis.sync.BackgroundSyncManager
import com.example.celestis.sync.IosSyncManager
import com.example.celestis.ui.utils.IosShareManager
import com.example.celestis.ui.utils.ShareManager
import org.koin.dsl.module

val iosModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = "celestis.db"
        )
    }

    single<AppDatabase> {
        AppDatabase(driver = get())
    }

    single<Platform> { IOSPlatform() }
    single<ShareManager> { IosShareManager() }
    single { NetworkMonitor() }
    single<BackgroundSyncManager> { IosSyncManager(get(), get(), get<Platform>().context) }
}
