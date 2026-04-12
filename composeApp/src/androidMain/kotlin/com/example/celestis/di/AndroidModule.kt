package com.example.celestis.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.celestis.AndroidPlatform
import com.example.celestis.Platform
import com.example.celestis.database.AppDatabase
import com.example.celestis.network.NetworkMonitor
import com.example.celestis.notifications.AndroidNotificationScheduler
import com.example.celestis.notifications.AndroidPushNotificationManager
import com.example.celestis.notifications.NotificationScheduler
import com.example.celestis.notifications.PushNotificationManager
import com.example.celestis.sync.AndroidSyncManager
import com.example.celestis.sync.BackgroundSyncManager
import com.example.celestis.ui.utils.AndroidShareManager
import com.example.celestis.ui.utils.ShareManager
import org.koin.dsl.module

val androidModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = get(),
            name = "celestis.db"
        )
    }

    single<AppDatabase> {
        AppDatabase(driver = get())
    }

    single<Platform> { AndroidPlatform(get()) }
    single<ShareManager> { AndroidShareManager(get<Platform>().context) }
    single { NetworkMonitor(get()) }
    single<BackgroundSyncManager> { AndroidSyncManager(get()) }
    single<NotificationScheduler> { AndroidNotificationScheduler(get()) }
    single<PushNotificationManager> { AndroidPushNotificationManager(get()) }
}