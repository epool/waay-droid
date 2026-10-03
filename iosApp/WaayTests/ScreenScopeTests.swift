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
}
