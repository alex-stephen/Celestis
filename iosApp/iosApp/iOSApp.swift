import SwiftUI
import ComposeApp
import BackgroundTasks

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
    
    func handleAppRefresh(task: BGAppRefreshTask) {
        // Get sync manager from Koin
        guard let syncManager = KoinHelperKt.getBackgroundSyncManager() else {
            task.setTaskCompleted(success: false)
            return
        }
        
        // Schedule next refresh
        syncManager.scheduleDailySync()
        
        // Create operation for the background work
        let operation = BlockOperation {
            // This will be called from Kotlin side when work completes
        }
        
        // If the task is about to expire, cancel the operation
        task.expirationHandler = {
            operation.cancel()
        }
        
        // Call Kotlin sync logic
        IosSyncManagerCompanion.shared.handleBackgroundTask(
            syncManager: syncManager,
            onComplete: { success in
                task.setTaskCompleted(success: success.boolValue)
            }
        )
    }
}
