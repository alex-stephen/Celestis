import SwiftUI
import ComposeApp
import BackgroundTasks
import WidgetKit
import FirebaseCore
import FirebaseMessaging

// MARK: - Background task handler

private func handleAppRefresh(task: BGAppRefreshTask) {
    guard let syncManager = KoinHelperKt.getBackgroundSyncManager() as? IosSyncManager else {
        task.setTaskCompleted(success: false)
        return
    }

    // Always reschedule for the next day before the current run finishes, so
    // even if this task is killed the next one is already in the queue.
    syncManager.scheduleDailySync()

    task.expirationHandler = {
        task.setTaskCompleted(success: false)
    }

    syncManager.startBackgroundSync { success in
        task.setTaskCompleted(success: success.boolValue)
        if success.boolValue {
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}

// MARK: - App Delegate (APNs registration + FCM token bridging)

class CelestisAppDelegate: NSObject, UIApplicationDelegate,
                            MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        // Request notification permission on first launch.
        // For a content-delivery app like Celestis this is appropriate upfront;
        // move into an onboarding screen if a softer ask is preferred later.
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .sound, .badge]
        ) { granted, _ in
            guard granted else { return }
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }
        return true
    }

    // Forward the APNs device token to Firebase so it can map it to an FCM token.
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // Firebase has a fresh FCM token — store it and register with the Celestis backend.
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        UserDefaults.standard.set(token, forKey: "fcm_device_token")
        // Bridges into Kotlin: calls ApodRepository.registerDeviceToken(token, "ios")
        IosNotificationHelperKt.onFcmTokenReceived(token: token)
    }

    // Silent (data-only) APNs push received while app is in background or foreground.
    // The Celestis backend sends this at 05:15 UTC with the APOD title so the device
    // can schedule a local notification to fire at 10:00 AM local time.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        guard userInfo["type"] as? String == "daily_apod",
              let title = userInfo["apod_title"] as? String,
              let date  = userInfo["apod_date"]  as? String
        else {
            completionHandler(.noData)
            return
        }
        // Bridges into Kotlin: schedules UNCalendarNotificationTrigger for next 10 AM local.
        IosNotificationHelperKt.scheduleNotificationFromPush(title: title, date: date)
        completionHandler(.newData)
    }

    // Show notifications as banners + play sound even when the app is in the foreground.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}

// MARK: - App entry point

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(CelestisAppDelegate.self) var delegate

    init() {
        // Firebase must be configured before Koin boots (FCM needs it on first token fetch).
        FirebaseApp.configure()

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

        WidgetCenter.shared.reloadAllTimelines()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    MainViewControllerKt.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}