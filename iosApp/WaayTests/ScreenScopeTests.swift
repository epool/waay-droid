import Foundation
import Shared
import Testing

struct ScreenScopeTests {
    @Test func closeIsIdempotent() {
        let scope = ScreenScope()
        scope.close()
        scope.close()
    }

    @Test func closeIsSafeFromABackgroundThread() async {
        let scope = ScreenScope()
        await Task.detached { scope.close() }.value
    }

    @MainActor
    @Test func probeViewModelIsCreatedLazilyAndReusedByTheScope() {
        let scope = ScreenScope()
        let first = SpikeProbes.shared.lifecycleProbe(scope: scope)
        first.increment()
        let second = SpikeProbes.shared.lifecycleProbe(scope: scope)
        #expect(first === second)
        #expect(second.count.value.intValue == 1)
        scope.close()
    }
}
