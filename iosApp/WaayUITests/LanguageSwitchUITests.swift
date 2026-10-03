import XCTest

/// US5 (quickstart I1, I6): with the device in Spanish the game starts in Spanish; switching the app
/// to English mid-game re-renders the same card in English at once, without restarting the game.
final class LanguageSwitchUITests: XCTestCase {
    @MainActor
    func testSpanishDeviceThenEnglishMidGame() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-resetPreferences", "-AppleLanguages", "(es-MX)", "-AppleLocale", "es_MX"]
        app.launch()

        let ready = app.buttons["intro.ready"]
        XCTAssertTrue(ready.waitForExistence(timeout: 10))
        XCTAssertEqual(ready.label, "Estoy listo")
        ready.tap()

        let progress = app.staticTexts["card.progress"]
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        XCTAssertEqual(progress.label, "Carta 1 de 5")
        let numbersBefore = visibleNumbers(app)

        app.buttons["toolbar.settings"].tap()
        app.buttons["English"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        app.navigationBars.buttons.element(boundBy: 0).tap()

        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        XCTAssertEqual(progress.label, "Card 1 of 5")
        XCTAssertEqual(app.buttons["card.yes"].label, "Yes")
        XCTAssertEqual(visibleNumbers(app), numbersBefore, "The same card must stay on screen")
    }

    @MainActor
    private func visibleNumbers(_ app: XCUIApplication) -> Set<String> {
        let numbers = app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'number.'"))
        return Set(numbers.allElementsBoundByIndex.map(\.identifier))
    }
}
