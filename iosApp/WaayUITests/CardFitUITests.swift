import XCTest

/// Spec 002 FR-001 / SC-001: with 7 cards every one of the 64 numbers is on screen at once,
/// without scrolling, on a phone at the default text size.
final class CardFitUITests: XCTestCase {
    @MainActor
    func testSevenCardsFitWithoutScrolling() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-resetPreferences"]
        app.launch()

        let settings = app.buttons["toolbar.settings"]
        XCTAssertTrue(settings.waitForExistence(timeout: 10))
        settings.tap()
        app.buttons["7 cards (1–127)"].tap()
        app.navigationBars.buttons.element(boundBy: 0).tap()
        app.buttons["intro.ready"].tap()
        XCTAssertTrue(app.buttons["card.yes"].waitForExistence(timeout: 5))

        let numbers = app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'number.'"))
        XCTAssertEqual(numbers.count, 64)
        for number in numbers.allElementsBoundByIndex {
            XCTAssertTrue(number.isHittable, "\(number.identifier) is not on screen")
        }
    }
}
