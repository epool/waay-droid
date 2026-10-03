import Shared
import SwiftUI

@main
struct WaayApp: App {
    var body: some Scene {
        WindowGroup {
            #if DEBUG
            if ProcessInfo.processInfo.arguments.contains("-lifecycleProbe") {
                LifecycleProbeScreen()
            } else {
                Text(AppInfo.shared.NAME)
            }
            #else
            Text(AppInfo.shared.NAME)
            #endif
        }
    }
}
