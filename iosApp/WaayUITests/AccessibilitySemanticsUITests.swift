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
        try audit()

        ready.tap()
        let yes = app.buttons["card.yes"]
        XCTAssertTrue(yes.waitForExistence(timeout: 5))
        XCTAssertEqual(yes.label, "Yes")
        XCTAssertEqual(app.buttons["card.no"].label, "No")
        let number = app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'number.'")).firstMatch
        XCTAssertEqual(number.label, String(number.identifier.dropFirst("number.".count)))
        try audit()

        let progress = app.staticTexts["card.progress"]
        for _ in 0..<5 {
            let before = progress.label
            Thread.sleep(forTimeInterval: 0.35)  // FR-028 answer cooldown
            app.buttons["card.yes"].tap()
            let answered = NSPredicate { _, _ in !progress.exists || progress.label != before }
            wait(for: [XCTNSPredicateExpectation(predicate: answered, object: nil)], timeout: 5)
        }
        XCTAssertTrue(app.staticTexts["result.message"].waitForExistence(timeout: 5))
        try audit()

        app.buttons["toolbar.settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        // Accepted findings on Settings, and only these (both checked by eye at Accessibility XXXL on
        // iOS 18.6 and 27, where every row and header scales and nothing of ours is clipped):
        // - Dynamic Type on the two section headers: a grouped list caps them below body text, which
        //   the audit reports as "partially unsupported";
        // - findings the audit cannot attach to any element (element is nil). On iOS 18 these are the
        //   system navigation bar (inline title, back button), which does not scale; iOS 26+ reports none.
        // Every other issue still fails.
        let sectionHeaders: Set<String> = ["Number of cards", "Language"]
        try audit { issue in
            guard let element = issue.element else { return true }
            return issue.auditType == .dynamicType && sectionHeaders.contains(element.label)
        }
    }

    /// Runs Xcode's accessibility audit, retrying only when the audit itself times out ("Audit failed
    /// to complete in time", seen on busy CI runners). Real accessibility findings fail at once.
    /// Text inside the navigation bar is capped by the system bar, so its Dynamic Type findings are
    /// accepted on every screen, as for the system's own title.
    @MainActor
    private func audit(_ accept: ((XCUIAccessibilityAuditIssue) throws -> Bool)? = nil) throws {
        let navigationBar = app.navigationBars.firstMatch.frame
        let handler: (XCUIAccessibilityAuditIssue) throws -> Bool = { issue in
            if issue.auditType == .dynamicType, let element = issue.element, navigationBar.contains(element.frame) {
                return true
            }
            return try accept?(issue) ?? false
        }
        let attempts = 3
        for attempt in 1...attempts {
            do {
                try app.performAccessibilityAudit(for: .all, handler)
                return
            } catch let error as NSError
                where error.domain == "com.apple.xcode.xctest.accessibilityAudit" && error.code == -56
                && attempt < attempts
            {
                continue
            }
        }
    }
}
