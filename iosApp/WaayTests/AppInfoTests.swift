import Shared
import Testing

struct AppInfoTests {
    @Test func appNameComesFromSharedModule() {
        #expect(AppInfo.shared.NAME == "Wáay")
    }
}
