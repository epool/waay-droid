import XCTest

/// Spec 002 US2 on iOS, contract U1, U2, U4 and U6 (card-screen-ui.md): right is Yes, left is No, short
/// or vertical drags answer nothing. The flick (U3), cooldown and mid-drag cases are covered on
/// Android: synthesized XCUITest drags carry no release velocity, and can't act within 300 ms or
/// inspect the screen mid-gesture. The iOS flick is checked by hand (quickstart M5).
final class SwipeAnswerUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-resetPreferences"]
        app.launch()
        let ready = app.buttons["intro.ready"]
        XCTAssertTrue(ready.waitForExistence(timeout: 10))
        ready.tap()
        XCTAssertTrue(card.waitForExistence(timeout: 5))
    }

    private var card: XCUIElement { app.otherElements["card.surface"] }
    private var progress: XCUIElement { app.staticTexts["card.progress"] }

    /// Waits out the 300 ms answer cooldown (ADR-012), as a reading player would.
    private func readTheCard() { Thread.sleep(forTimeInterval: 0.35) }

    @MainActor
    private func waitForProgress(toLeave before: String) {
        let changed = NSPredicate { _, _ in !self.progress.exists || self.progress.label != before }
        wait(for: [XCTNSPredicateExpectation(predicate: changed, object: nil)], timeout: 5)
    }

    // U1, U2: a whole game by swiping only reveals the secret.
    @MainActor
    func testSwipingRightForYesAndLeftForNoRevealsTheSecret() {
        for _ in 0..<5 {
            readTheCard()
            let before = progress.label
            if app.staticTexts["number.27"].exists { card.swipeRight() } else { card.swipeLeft() }
            waitForProgress(toLeave: before)
        }
        let message = app.staticTexts["result.message"]
        XCTAssertTrue(message.waitForExistence(timeout: 5))
        XCTAssertTrue(message.label.contains("27"))
    }

    // U4: a short, slow drag springs back and answers nothing.
    @MainActor
    func testAShortDragAnswersNothing() {
        readTheCard()
        let start = card.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        start.press(
            forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 40, dy: 0)), withVelocity: .slow,
            thenHoldForDuration: 0.3)
        Thread.sleep(forTimeInterval: 1)
        XCTAssertEqual(progress.label, "Card 1 of 5")
    }

    // U6: a vertical drag never answers.
    @MainActor
    func testAVerticalDragAnswersNothing() {
        readTheCard()
        card.swipeUp()
        Thread.sleep(forTimeInterval: 1)
        XCTAssertEqual(progress.label, "Card 1 of 5")
    }
}
