import XCTest

/// US1 smoke flow on iOS, including interruptions mid-game: rotation, backgrounding and a Settings
/// round trip must keep the same card and progress (FR-029, FR-016b; analyze findings G3, D1).
final class GameFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-resetPreferences"]
        app.launch()
    }

    override func tearDown() {
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testPlaysAFullRoundSurvivingInterruptionsThenStartsANewGame() {
        let ready = app.buttons["intro.ready"]
        XCTAssertTrue(ready.waitForExistence(timeout: 10))
        ready.tap()

        answerTruthfully(secret: 27, cards: 2)
        let progress = app.staticTexts["card.progress"]
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        let progressBefore = progress.label

        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        XCTAssertEqual(progress.label, progressBefore)
        XCUIDevice.shared.orientation = .portrait
        XCTAssertEqual(progress.label, progressBefore)

        XCUIDevice.shared.press(.home)
        app.activate()
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        XCTAssertEqual(progress.label, progressBefore)

        app.buttons["toolbar.settings"].tap()
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        XCTAssertEqual(progress.label, progressBefore, "Game state lost across the Settings round trip")

        answerTruthfully(secret: 27, cards: 3)
        let message = app.staticTexts["result.message"]
        XCTAssertTrue(message.waitForExistence(timeout: 5))
        XCTAssertTrue(message.label.contains("27"))

        app.buttons["toolbar.newGame"].tap()
        XCTAssertTrue(app.buttons["intro.ready"].waitForExistence(timeout: 5))
    }

    @MainActor
    private func answerTruthfully(secret: Int, cards: Int) {
        for _ in 0..<cards {
            let yes = app.buttons["card.yes"]
            XCTAssertTrue(yes.waitForExistence(timeout: 5))
            let isOnCard = app.staticTexts["number.\(secret)"].exists
            (isOnCard ? yes : app.buttons["card.no"]).tap()
        }
    }
}
