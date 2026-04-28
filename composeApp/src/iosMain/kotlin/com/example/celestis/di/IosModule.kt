package com.example.celestis.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.celestis.IOSPlatform
import com.example.celestis.Platform
import com.example.celestis.database.AppDatabase
import com.example.celestis.network.NetworkMonitor
import com.example.celestis.notifications.IosNotificationScheduler
import com.example.celestis.notifications.IosPushNotificationManager
import com.example.celestis.notifications.NotificationScheduler
import com.example.celestis.notifications.PushNotificationManager
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
    single<NotificationScheduler> { IosNotificationScheduler() }
    single<PushNotificationManager> { IosPushNotificationManager() }
    // NotificationScheduler is injected so IosSyncManager can schedule the fallback
    // notification after a successful background sync (in case the FCM push was missed).
    single<BackgroundSyncManager> { IosSyncManager(get(), get(), get<Platform>().context, get()) }
}