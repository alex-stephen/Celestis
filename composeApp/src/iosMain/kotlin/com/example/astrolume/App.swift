package com.example.astrolume

@main
struct AstrolumeApp: App {
    init() {
        KoinHelperKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}