import XCTest

/// US6 / SC-005: preferences changed in Settings survive terminating and relaunching the app.
/// `-resetPreferences` starts from the first-use defaults (FR-024).
final class PreferencesPersistenceUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    @MainActor
    func testFreshInstallUsesTheDefaults() {
        let app = XCUIApplication()
        app.launchArguments = ["-resetPreferences"]
        app.launch()

        XCTAssertTrue(app.staticTexts["intro.message"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["intro.message"].label.contains("1 to 31"))
    }

    @MainActor
    func testSettingsSurviveARelaunch() {
        let app = XCUIApplication()
        app.launchArguments = ["-resetPreferences"]
        app.launch()

        openSettings(app)
        app.buttons["6 cards (1–63)"].tap()
        app.switches["settings.voice"].switches.firstMatch.tap()
        app.buttons["Español"].tap()
        XCTAssertTrue(app.navigationBars["Ajustes"].waitForExistence(timeout: 5))

        app.terminate()
        app.launchArguments = []
        app.launch()

        XCTAssertTrue(app.staticTexts["intro.message"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["intro.message"].label.contains("del 1 al 63"))
        openSettings(app)
        XCTAssertTrue(app.navigationBars["Ajustes"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.switches["settings.voice"].value as? String, "0")
    }

    @MainActor
    private func openSettings(_ app: XCUIApplication) {
        let settings = app.buttons["toolbar.settings"]
        XCTAssertTrue(settings.waitForExistence(timeout: 10))
        settings.tap()
    }
}
