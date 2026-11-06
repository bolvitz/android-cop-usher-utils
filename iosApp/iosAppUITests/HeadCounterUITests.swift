import XCTest

/// End-to-end smoke test for the head counter: venue -> area -> count -> undo/redo -> report -> lock -> history.
final class HeadCounterUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    /// Scenario: one usher creates a venue with one zone, counts, undoes and redoes, toggles the zone out of
    /// the total, prints the receipt, locks the count, then reopens the count from History.
    /// Expected: the total tracks every step and the receipt and lock behave as on the real device.
    /// Why: it is the only end-to-end guard over the redesigned counter, so it also attaches screenshots
    /// of each redesigned screen for visual review in the result bundle.
    func testCountUndoRedoAndLock() {
        // Given: a unique venue name, so leftovers from earlier runs never match the queries below.
        let suffix = String(Int(Date().timeIntervalSince1970) % 100_000)
        let venueName = "UITest Venue \(suffix)"
        let areaName = "Main Hall"

        // Create a venue.
        app.navigationBars["Venues"].buttons["Add Venue"].tap()
        type(venueName, into: app.textFields["Name"])
        type("UT\(suffix)", into: app.textFields["Code (e.g. MC)"])
        app.buttons["Add"].tap()

        // Add an area via the context menu.
        let venueCell = app.staticTexts[venueName]
        XCTAssertTrue(venueCell.waitForExistence(timeout: 5))
        venueCell.press(forDuration: 1.0)
        app.buttons["Manage Areas"].tap()
        app.navigationBars["Areas"].buttons["Add Area"].tap()
        type(areaName, into: app.textFields["Name"])
        app.buttons["Add"].tap()
        XCTAssertTrue(app.staticTexts[areaName].waitForExistence(timeout: 5))
        app.navigationBars["Areas"].buttons["Venues"].tap()

        // Start a count.
        XCTAssertTrue(venueCell.waitForExistence(timeout: 5))
        venueCell.tap()
        // The new-count sheet requires a counter name before it can start.
        let start = app.buttons["Start count"]
        XCTAssertTrue(start.waitForExistence(timeout: 5))
        XCTAssertFalse(start.isEnabled)
        type("Usher Test", into: app.textFields["Your name"])
        XCTAssertTrue(start.isEnabled)
        start.tap()

        let total = app.staticTexts["totalAttendance"]
        XCTAssertTrue(total.waitForExistence(timeout: 5))
        XCTAssertEqual(total.label, "0")

        // When: counting with the pad keys. Their labels are the accessibility names the redesigned
        // PadKey sets ("Add one to <zone>", "Subtract one from <zone>"), not the visible "+1" / "−1".
        let plus = app.buttons["Add one to \(areaName)"]
        let minus = app.buttons["Subtract one from \(areaName)"]
        plus.tap(); waitForTotal("1")
        plus.tap(); waitForTotal("2")
        plus.tap(); waitForTotal("3")
        minus.tap(); waitForTotal("2")
        // Then: the total follows each key press; keep this state as the counter screenshot.
        snapshot("Counter")

        app.buttons["Undo"].tap(); waitForTotal("3")
        app.buttons["Redo"].tap(); waitForTotal("2")

        // Excluding the area removes it from the total.
        // The inclusion switch is named by its current state, so the button label flips after each tap.
        app.buttons["Counted in total"].tap(); waitForTotal("0")
        app.buttons["Left out of total"].tap(); waitForTotal("2")

        // The receipt reflects the count and who took it.
        app.buttons["Report"].tap()
        XCTAssertTrue(app.staticTexts["Head count report"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Usher Test"].exists)
        // The receipt feeds out in 34 steps of 68 ms after a 180 ms pause (about 2.5 s); wait it out so the
        // screenshot shows the whole paper.
        sleep(3)
        snapshot("Receipt")
        app.buttons["Done"].tap()
        // Guards the bug where the offset paper swallowed the Done tap: the receipt must really close.
        XCTAssertTrue(app.staticTexts["Head count report"].waitForNonExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["totalAttendance"].waitForExistence(timeout: 5))

        // Locking disables counting.
        app.buttons["Lock count"].tap()
        XCTAssertTrue(app.buttons["Unlock count"].waitForExistence(timeout: 5))
        XCTAssertFalse(plus.isEnabled)
        // The shutter drops in 0.52 s and the time stamp lands 0.4 s later; wait for both before the screenshot.
        sleep(1)
        snapshot("Locked")
        app.buttons["Unlock count"].tap()
        XCTAssertTrue(app.buttons["Lock count"].waitForExistence(timeout: 5))
        XCTAssertTrue(plus.isEnabled)

        // The count shows up in the venue's history and can be resumed.
        app.navigationBars[venueName].buttons["Venues"].tap()
        XCTAssertTrue(venueCell.waitForExistence(timeout: 5))
        venueCell.press(forDuration: 1.0)
        app.buttons["History"].tap()
        let historyRow = app.staticTexts[venueName]
        XCTAssertTrue(historyRow.waitForExistence(timeout: 5))
        snapshot("History")

        // Each history entry opens its own receipt.
        historyRow.swipeRight()
        app.buttons["Report"].tap()
        XCTAssertTrue(app.staticTexts["Head count report"].waitForExistence(timeout: 5))
        app.buttons["Done"].tap()
        // Same Done guard as above, for the History copy of the receipt.
        XCTAssertTrue(app.staticTexts["Head count report"].waitForNonExistence(timeout: 5))

        // Then: tapping the row resumes the same count, so its total is still 2.
        XCTAssertTrue(historyRow.waitForExistence(timeout: 5))
        historyRow.tap()
        waitForTotal("2")
    }

    /// Keeps a screenshot in the result bundle for visual review.
    private func snapshot(_ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }

    private func type(_ text: String, into field: XCUIElement) {
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText(text)
    }

    private func waitForTotal(_ expected: String, file: StaticString = #filePath, line: UInt = #line) {
        let total = app.staticTexts["totalAttendance"]
        let predicate = NSPredicate(format: "label == %@", expected)
        let result = XCTWaiter.wait(for: [expectation(for: predicate, evaluatedWith: total)], timeout: 5)
        XCTAssertEqual(result, .completed, "total was \(total.label), expected \(expected)", file: file, line: line)
    }
}
