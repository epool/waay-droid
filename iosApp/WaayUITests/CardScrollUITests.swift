import XCTest

/// FR-003a in spec 002's fallback (FR-004): at Accessibility XXXL 64 numbers can't fit a phone, so
/// the card scrolls, and every card still starts at the top of its numbers.
final class CardScrollUITests: XCTestCase {
    @MainActor
    func testEachNewCardStartsAtTheTop() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = [
            "-resetPreferences", "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL",
        ]
        app.launch()

        let settings = app.buttons["toolbar.settings"]
        XCTAssertTrue(settings.waitForExistence(timeout: 10))
        settings.tap()
        let sevenCards = app.buttons["7 cards (1–127)"]
        for _ in 0..<6 where !sevenCards.isHittable { app.swipeUp() }  // Settings scrolls at AX sizes
        sevenCards.tap()
        app.navigationBars.buttons.element(boundBy: 0).tap()
        let ready = app.buttons["intro.ready"]
        if !ready.isHittable { app.swipeUp() }
        ready.tap()

        let progress = app.staticTexts["card.progress"]
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        let firstNumber = { self.topmostNumber(in: app) }
        for _ in 0..<4 where firstNumber().isHittable {
            app.scrollViews.firstMatch.swipeUp(velocity: .fast)
        }
        if firstNumber().isHittable {
            throw XCTSkip("All 64 numbers fit on this screen, so the card does not scroll")
        }

        Thread.sleep(forTimeInterval: 0.35)  // FR-028 answer cooldown
        let no = app.buttons["card.no"]
        if !no.isHittable { app.swipeUp() }
        no.tap()

        let secondCard = NSPredicate(format: "label == %@", "Card 2 of 7")
        wait(for: [XCTNSPredicateExpectation(predicate: secondCard, object: progress)], timeout: 5)
        XCTAssertTrue(firstNumber().isHittable, "Card 2 should start at the top")
    }

    /// The number shown highest on the card (smallest y): hidden once the card is scrolled down.
    @MainActor
    private func topmostNumber(in app: XCUIApplication) -> XCUIElement {
        let numbers = app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'number.'"))
        return numbers.allElementsBoundByIndex.min { $0.frame.minY < $1.frame.minY } ?? numbers.firstMatch
    }
}
