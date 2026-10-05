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

    override func tearDown() {
        XCUIDevice.shared.appearance = .light
    }

    @MainActor
    func testEveryScreenIsLabelledAndPassesTheAccessibilityAudit() throws {
        try auditEveryScreen()
    }

    // Spec 002 FR-025 / SC-008: the same checks in dark mode (the card and glass change).
    // The system appearance switches the status bar; -forceDarkMode switches the app, because on iOS 27
    // simulators the relaunched app stays light. The guard makes sure the audit really sees dark mode.
    @MainActor
    func testEveryScreenPassesTheAccessibilityAuditInDarkMode() throws {
        XCUIDevice.shared.appearance = .dark
        app.terminate()
        app.launchArguments += ["-forceDarkMode"]
        app.launch()
        let message = app.staticTexts["intro.message"]
        XCTAssertTrue(message.waitForExistence(timeout: 10))
        XCTAssertLessThan(MeasuredContrast.fillLuminance(of: message), 0.1, "the app is not in dark mode")
        try auditEveryScreen()
    }

    @MainActor
    private func auditEveryScreen() throws {
        let ready = app.buttons["intro.ready"]
        XCTAssertTrue(ready.waitForExistence(timeout: 10))
        XCTAssertEqual(app.buttons["toolbar.newGame"].label, "New game")
        XCTAssertEqual(app.buttons["toolbar.settings"].label, "Settings")
        try audit()
        assertToolbarIconContrast()

        ready.tap()
        let yes = app.buttons["card.yes"]
        XCTAssertTrue(yes.waitForExistence(timeout: 5))
        XCTAssertEqual(yes.label, "Yes")
        XCTAssertEqual(app.buttons["card.no"].label, "No")
        let number = app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'number.'")).firstMatch
        XCTAssertEqual(number.label, String(number.identifier.dropFirst("number.".count)))
        // Spec 002 FR-012, FR-019: the draggable card reads as its question, then every number.
        let surface = app.otherElements["card.surface"]
        XCTAssertEqual(surface.staticTexts.firstMatch.label, "Is your number on this card?")
        let numbersOnCard = surface.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'number.'"))
        XCTAssertEqual(numbersOnCard.count, 16)
        try audit()
        assertGlassAnswerContrast()

        let progress = app.staticTexts["card.progress"]
        for _ in 0..<5 {
            let before = progress.label
            Thread.sleep(forTimeInterval: 0.35)  // FR-028 answer cooldown
            app.buttons["card.yes"].tap()
            let answered = NSPredicate { _, _ in !progress.exists || progress.label != before }
            wait(for: [XCTNSPredicateExpectation(predicate: answered, object: nil)], timeout: 5)
        }
        // ...and the game completed with the Yes button alone, no gestures (FR-012).
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
            // Glass answer controls are measured instead, on every run (assertGlassAnswerContrast).
            if issue.auditType == .contrast, Self.isGlass, let element = issue.element,
                Self.answerControls.contains(element.identifier)
            {
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

    /// The toolbar icons sit on the always-dark backdrop, in light and dark mode. The audit doesn't
    /// check icon contrast, so it is measured: at least 3:1 for non-text controls (WCAG 1.4.11). On
    /// iOS 18 they took the accent violet and measured below that (spec 002 T042).
    @MainActor
    private func assertToolbarIconContrast() {
        for identifier in ["toolbar.newGame", "toolbar.settings"] {
            let measured = MeasuredContrast.of(app.buttons[identifier])
            XCTAssertGreaterThanOrEqual(measured, 3, "\(identifier) measures \(measured):1")
        }
    }

    private static let answerControls: Set = ["card.yes", "card.no"]
    private static let isGlass = ProcessInfo.processInfo.operatingSystemVersion.majorVersion >= 26

    /// Liquid Glass answer controls (iOS 26+): the audit misreads glass both ways. It flagged white text
    /// on a `#301880` glass fill that measures 13.4:1, and it missed white text on near-white glass that
    /// measures 1.03:1 (spec 002 T032). So their contrast is measured from the rendered pixels on every
    /// run, and must reach 4.5:1 (FR-025). Every other control keeps the audit's own check.
    @MainActor
    private func assertGlassAnswerContrast() {
        guard Self.isGlass else { return }
        for identifier in Self.answerControls.sorted() {
            let measured = MeasuredContrast.of(app.buttons[identifier])
            XCTAssertGreaterThanOrEqual(measured, 4.5, "\(identifier) measures \(measured):1")
        }
    }
}
