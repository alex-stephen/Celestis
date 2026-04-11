import SwiftUI
import ComposeApp
import BackgroundTasks
import WidgetKit

private func handleAppRefresh(task: BGAppRefreshTask) {
    // Retrieve sync manager from Koin.
    guard let syncManager = KoinHelperKt.getBackgroundSyncManager() as? IosSyncManager else {
        task.setTaskCompleted(success: false)
        return
    }

    // Always reschedule for the next day before the current run finishes, so
    // even if this task is killed the next one is already in the queue.
    syncManager.scheduleDailySync()

    // Mark the task as failed if iOS needs the resources back before we finish.
    task.expirationHandler = {
        task.setTaskCompleted(success: false)
    }

    // Run the Kotlin sync logic on a background thread.
    syncManager.startBackgroundSync { success in
        task.setTaskCompleted(success: success.boolValue)

        // After a successful sync the IosSyncManager has already written the
        // latest APOD metadata and image into the App Group container.
        // Telling WidgetKit to reload forces the timeline provider to run
        // getTimeline(), which picks up the freshly written data immediately.
        if success.boolValue {
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}

@main
struct iOSApp: App {

    init() {
        // Register the background-task handler BEFORE the app finishes launching.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.example.celestis.refresh",
            using: nil
        ) { task in
            handleAppRefresh(task: task as! BGAppRefreshTask)
        }

        // Boot Koin (also seeds App Group with API base URL for the widget).
        KoinHelperKt.doInitKoin()

        // Schedule the recurring daily sync.
        if let syncManager = KoinHelperKt.getBackgroundSyncManager() {
            syncManager.scheduleDailySync()
        }

        // Trigger a widget refresh on every cold launch so the home screen
        // always reflects any data that was written during the last sync.
        WidgetCenter.shared.reloadAllTimelines()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle deep links such as celestis://apod/2026-04-11
                    MainViewControllerKt.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}