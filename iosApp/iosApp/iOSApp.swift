import SwiftUI
import ComposeApp
import BackgroundTasks

private func handleAppRefresh(task: BGAppRefreshTask) {
    // Get sync manager from Koin
    guard let syncManager = KoinHelperKt.getBackgroundSyncManager() as? IosSyncManager else {
        task.setTaskCompleted(success: false)
        return
    }

    // Schedule next refresh
    syncManager.scheduleDailySync()

    // If the task is about to expire, mark as failed
    task.expirationHandler = {
        task.setTaskCompleted(success: false)
    }

    // Call Kotlin sync logic
    syncManager.startBackgroundSync { success in
        task.setTaskCompleted(success: success.boolValue)
    }
}

@main
struct iOSApp: App {

    init() {
        // Register background task handler
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.example.celestis.refresh",
            using: nil
        ) { task in
            handleAppRefresh(task: task as! BGAppRefreshTask)
        }

        // Initialize Koin and schedule background sync
        KoinHelperKt.doInitKoin()

        // Get the sync manager from Koin and schedule daily sync
        if let syncManager = KoinHelperKt.getBackgroundSyncManager() {
            syncManager.scheduleDailySync()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle deep link URLs
                    MainViewControllerKt.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}
