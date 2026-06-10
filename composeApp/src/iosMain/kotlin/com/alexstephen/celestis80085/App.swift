package com.alexstephen.celestis80085

@main
struct CelestisApp: App {
    init() {
        KoinHelperKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}