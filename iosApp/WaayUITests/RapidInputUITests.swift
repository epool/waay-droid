import XCTest

/// FR-028, mirroring the Android RapidInputTest: a rapid double tap on an answer records a single
/// answer for the current card.
final class RapidInputUITests: XCTestCase {
    @MainActor
    func testDoubleTapOnYesRecordsOneAnswer() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-resetPreferences"]
        app.launch()

        let ready = app.buttons["intro.ready"]
        XCTAssertTrue(ready.waitForExistence(timeout: 10))
        ready.tap()
        let progress = app.staticTexts["card.progress"]
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        XCTAssertEqual(progress.label, "Card 1 of 5")

        Thread.sleep(forTimeInterval: 0.35)  // a real player reads the card first (ADR-012)
        app.buttons["card.yes"].doubleTap()

        let secondCard = NSPredicate(format: "label == %@", "Card 2 of 5")
        wait(for: [XCTNSPredicateExpectation(predicate: secondCard, object: progress)], timeout: 5)
        Thread.sleep(forTimeInterval: 1)  // time for a wrongly recorded second answer to show up
        XCTAssertEqual(progress.label, "Card 2 of 5", "The second tap must not answer card 2")
    }
}
