import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
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
