package com.alexstephen.celestis80085.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.alexstephen.celestis80085.IOSPlatform
import com.alexstephen.celestis80085.Platform
import com.alexstephen.celestis80085.database.AppDatabase
import com.alexstephen.celestis80085.network.NetworkMonitor
import com.alexstephen.celestis80085.notifications.IosNotificationScheduler
import com.alexstephen.celestis80085.notifications.IosPushNotificationManager
import com.alexstephen.celestis80085.notifications.NotificationScheduler
import com.alexstephen.celestis80085.notifications.PushNotificationManager
import com.alexstephen.celestis80085.sync.BackgroundSyncManager
import com.alexstephen.celestis80085.sync.IosSyncManager
import com.alexstephen.celestis80085.ui.utils.AppActionManager
import com.alexstephen.celestis80085.ui.utils.IosAppActionManager
import com.alexstephen.celestis80085.ui.utils.IosShareManager
import com.alexstephen.celestis80085.ui.utils.ShareManager
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
    single<AppActionManager> { IosAppActionManager() }
    single { NetworkMonitor() }
    single<NotificationScheduler> { IosNotificationScheduler() }
    single<PushNotificationManager> { IosPushNotificationManager() }
    // NotificationScheduler is injected so IosSyncManager can schedule the fallback
    // notification after a successful background sync (in case the FCM push was missed).
    single<BackgroundSyncManager> { IosSyncManager(get(), get(), get<Platform>().context, get()) }
}
