import XCTest

/// Spike (T013, analyze finding D1): a screen's shared ViewModel must survive a NavigationStack
/// push/pop. Removed in T043 when GameFlowUITests covers the real Settings round trip.
final class NavigationLifecycleUITests: XCTestCase {
    @MainActor
    func testViewModelSurvivesPushAndPop() {
        let app = XCUIApplication()
        app.launchArguments = ["-lifecycleProbe"]
        app.launch()

        let count = app.staticTexts["probe.count"]
        XCTAssertTrue(count.waitForExistence(timeout: 10))
        app.buttons["probe.increment"].tap()
        app.buttons["probe.increment"].tap()
        XCTAssertEqual(count.label, "2")

        app.buttons["probe.push"].tap()
        XCTAssertTrue(app.staticTexts["probe.pushed"].waitForExistence(timeout: 5))
        app.navigationBars.buttons.element(boundBy: 0).tap()

        XCTAssertTrue(count.waitForExistence(timeout: 5))
        XCTAssertEqual(count.label, "2", "ViewModel was cleared/recreated across push/pop")
    }
}
