package com.example.celestis

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