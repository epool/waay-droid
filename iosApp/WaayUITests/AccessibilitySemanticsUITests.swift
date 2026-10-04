import XCTest

/// FR-025, mirroring the Android gameIsUsableThroughSemantics: actions and numbers carry meaningful
/// labels, and Xcode's accessibility audit (labels, contrast, hit regions, Dynamic Type, clipping…)
/// passes on every screen: intro, card, result and Settings.
final class AccessibilitySemanticsUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-resetPreferences"]
        app.launch()
    }

    @MainActor
    func testEveryScreenIsLabelledAndPassesTheAccessibilityAudit() throws {
        let ready = app.buttons["intro.ready"]
        XCTAssertTrue(ready.waitForExistence(timeout: 10))
        XCTAssertEqual(app.buttons["toolbar.newGame"].label, "New game")
        XCTAssertEqual(app.buttons["toolbar.settings"].label, "Settings")
        try app.performAccessibilityAudit()

        ready.tap()
        let yes = app.buttons["card.yes"]
        XCTAssertTrue(yes.waitForExistence(timeout: 5))
        XCTAssertEqual(yes.label, "Yes")
        XCTAssertEqual(app.buttons["card.no"].label, "No")
        let number = app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'number.'")).firstMatch
        XCTAssertEqual(number.label, String(number.identifier.dropFirst("number.".count)))
        try app.performAccessibilityAudit()

        let progress = app.staticTexts["card.progress"]
        for _ in 0..<5 {
            let before = progress.label
            Thread.sleep(forTimeInterval: 0.35)  // FR-028 answer cooldown
            app.buttons["card.yes"].tap()
            let answered = NSPredicate { _, _ in !progress.exists || progress.label != before }
            wait(for: [XCTNSPredicateExpectation(predicate: answered, object: nil)], timeout: 5)
        }
        XCTAssertTrue(app.staticTexts["result.message"].waitForExistence(timeout: 5))
        try app.performAccessibilityAudit()

        app.buttons["toolbar.settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        // The two section headers do scale with Dynamic Type (checked at Accessibility XXXL), but a
        // grouped list caps them below body text, which the audit reports as "partially unsupported".
        // Only that finding, on those headers, is accepted; every other issue still fails.
        let sectionHeaders: Set<String> = ["Number of cards", "Language"]
        try app.performAccessibilityAudit { issue in
            issue.auditType == .dynamicType && sectionHeaders.contains(issue.element?.label ?? "")
        }
    }
}
