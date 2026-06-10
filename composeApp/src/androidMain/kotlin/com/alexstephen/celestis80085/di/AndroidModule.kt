package com.alexstephen.celestis80085.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.alexstephen.celestis80085.AndroidPlatform
import com.alexstephen.celestis80085.Platform
import com.alexstephen.celestis80085.database.AppDatabase
import com.alexstephen.celestis80085.network.NetworkMonitor
import com.alexstephen.celestis80085.notifications.AndroidNotificationScheduler
import com.alexstephen.celestis80085.notifications.AndroidPushNotificationManager
import com.alexstephen.celestis80085.notifications.NotificationScheduler
import com.alexstephen.celestis80085.notifications.PushNotificationManager
import com.alexstephen.celestis80085.sync.AndroidSyncManager
import com.alexstephen.celestis80085.sync.BackgroundSyncManager
import com.alexstephen.celestis80085.ui.utils.AndroidAppActionManager
import com.alexstephen.celestis80085.ui.utils.AndroidShareManager
import com.alexstephen.celestis80085.ui.utils.AppActionManager
import com.alexstephen.celestis80085.ui.utils.ShareManager
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
    single<AppActionManager> { AndroidAppActionManager(get<Platform>().context) }
    single { NetworkMonitor(get()) }
    single<BackgroundSyncManager> { AndroidSyncManager(get()) }
    single<NotificationScheduler> { AndroidNotificationScheduler(get()) }
    single<PushNotificationManager> { AndroidPushNotificationManager(get()) }
}
