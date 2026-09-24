import XCTest

/// End-to-end smoke test for the head counter: venue -> area -> count -> undo/redo -> lock.
final class HeadCounterUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    func testCountUndoRedoAndLock() {
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
        let start = app.buttons["Start Head Count"]
        XCTAssertTrue(start.waitForExistence(timeout: 5))
        start.tap()
        // With event types configured, "Start Head Count" is a menu.
        let plainHeadCount = app.buttons["Head Count"]
        if plainHeadCount.waitForExistence(timeout: 1) {
            plainHeadCount.tap()
        }

        let total = app.staticTexts["totalAttendance"]
        XCTAssertTrue(total.waitForExistence(timeout: 5))
        XCTAssertEqual(total.label, "0")

        let plus = app.buttons["Increase \(areaName)"]
        let minus = app.buttons["Decrease \(areaName)"]
        plus.tap(); waitForTotal("1")
        plus.tap(); waitForTotal("2")
        plus.tap(); waitForTotal("3")
        minus.tap(); waitForTotal("2")

        app.buttons["Undo"].tap(); waitForTotal("3")
        app.buttons["Redo"].tap(); waitForTotal("2")

        // Excluding the area removes it from the total.
        app.buttons["Included"].tap(); waitForTotal("0")
        app.buttons["Excluded"].tap(); waitForTotal("2")

        // Locking disables counting.
        app.buttons["Lock count"].tap()
        XCTAssertTrue(app.buttons["Unlock count"].waitForExistence(timeout: 5))
        XCTAssertFalse(plus.isEnabled)
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
        historyRow.tap()
        waitForTotal("2")
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
