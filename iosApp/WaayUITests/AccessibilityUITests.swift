import XCTest

/// FR-025 / FR-026 / FR-031: at the largest accessibility text size, in both orientations, every
/// phase stays reachable — the answers, the result and its action are hittable, nothing is clipped
/// away. Screenshots are attached to the result bundle for review.
final class AccessibilityUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = [
            "-resetPreferences",
            "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL",
        ]
        app.launch()
    }

    override func tearDown() {
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testLargestTextSizePortrait() {
        playARound(orientation: .portrait)
    }

    @MainActor
    func testLargestTextSizeLandscape() {
        playARound(orientation: .landscapeLeft)
    }

    @MainActor
    private func playARound(orientation: UIDeviceOrientation) {
        XCUIDevice.shared.orientation = orientation
        let ready = app.buttons["intro.ready"]
        XCTAssertTrue(ready.waitForExistence(timeout: 10))
        attachScreenshot("intro")
        scrollIntoView(ready)
        ready.tap()

        let progress = app.staticTexts["card.progress"]
        for card in 0..<5 {
            let yes = app.buttons["card.yes"]
            XCTAssertTrue(yes.waitForExistence(timeout: 5))
            XCTAssertTrue(yes.isHittable, "Yes is not reachable on card \(card + 1)")
            XCTAssertTrue(app.buttons["card.no"].isHittable, "No is not reachable on card \(card + 1)")
            if card == 0 { attachScreenshot("card") }
            let before = progress.label
            Thread.sleep(forTimeInterval: 0.35)  // FR-028 answer cooldown
            yes.tap()
            let answered = NSPredicate { _, _ in !progress.exists || progress.label != before }
            wait(for: [XCTNSPredicateExpectation(predicate: answered, object: nil)], timeout: 5)
        }

        let message = app.staticTexts["result.message"]
        XCTAssertTrue(message.waitForExistence(timeout: 5))
        XCTAssertTrue(message.label.contains("31"))
        attachScreenshot("result")
        let newGame = app.buttons["result.newGame"]
        scrollIntoView(newGame)
        XCTAssertTrue(newGame.isHittable)
    }

    @MainActor
    private func scrollIntoView(_ element: XCUIElement) {
        var attempts = 0
        while !element.isHittable && attempts < 5 {
            app.swipeUp()
            attempts += 1
        }
    }

    @MainActor
    private func attachScreenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "\(name)-\(XCUIDevice.shared.orientation.isLandscape ? "landscape" : "portrait")"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
