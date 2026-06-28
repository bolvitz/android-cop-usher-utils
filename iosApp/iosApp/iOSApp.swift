import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        // Start Koin (Room database, repositories, shared ViewModels).
        KoinIosKt.startKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
