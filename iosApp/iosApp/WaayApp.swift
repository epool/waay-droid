import Shared
import SwiftUI

@main
struct WaayApp: App {
    init() {
        // UI tests launch with -resetPreferences to start from first-use defaults (FR-024).
        if ProcessInfo.processInfo.arguments.contains("-resetPreferences"),
            let bundleId = Bundle.main.bundleIdentifier
        {
            UserDefaults.standard.removePersistentDomain(forName: bundleId)
        }
        InitKoinKt.doInitKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(Self.forcedColorScheme)
        }
    }

    /// UI tests launch with -forceDarkMode to audit dark mode. On iOS 27 simulators,
    /// `XCUIDevice.appearance` switches the system but not the app it relaunches (spec 002 T032).
    private static let forcedColorScheme: ColorScheme? =
        ProcessInfo.processInfo.arguments.contains("-forceDarkMode") ? .dark : nil
}
