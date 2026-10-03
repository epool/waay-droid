import Shared
import SwiftUI

@main
struct WaayApp: App {
    init() {
        InitKoinKt.doInitKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            #if DEBUG
            if ProcessInfo.processInfo.arguments.contains("-lifecycleProbe") {
                LifecycleProbeScreen()
            } else {
                ContentView()
            }
            #else
            ContentView()
            #endif
        }
    }
}
