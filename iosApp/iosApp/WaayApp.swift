import Shared
import SwiftUI

@main
struct WaayApp: App {
    init() {
        // UI tests launch with -resetPreferences to start from first-use defaults (FR-024).
        if ProcessInfo.processInfo.arguments.contains("-resetPreferences"),
           let bundleId = Bundle.main.bundleIdentifier {
            UserDefaults.standard.removePersistentDomain(forName: bundleId)
        }
        InitKoinKt.doInitKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
