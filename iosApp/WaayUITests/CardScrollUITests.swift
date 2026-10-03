import XCTest

/// FR-003a: every card starts at the top of its numbers, however far the player scrolled the
/// previous card, so no number is hidden above the visible area.
final class CardScrollUITests: XCTestCase {
    @MainActor
    func testEachNewCardStartsAtTheTop() throws {
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

        let progress = app.staticTexts["card.progress"]
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        for _ in 0..<4 where progress.isHittable {
            app.scrollViews.firstMatch.swipeUp(velocity: .fast)
        }
        if progress.isHittable {
            throw XCTSkip("All 64 numbers fit on this screen, so the grid does not scroll")
        }

        Thread.sleep(forTimeInterval: 0.35) // FR-028 answer cooldown
        app.buttons["card.no"].tap()

        let secondCard = NSPredicate(format: "label == %@", "Card 2 of 7")
        wait(for: [XCTNSPredicateExpectation(predicate: secondCard, object: progress)], timeout: 5)
        XCTAssertTrue(progress.isHittable, "Card 2 should start at the top")
    }
}
