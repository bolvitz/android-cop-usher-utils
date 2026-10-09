import SwiftUI
import shared

/// Receipt-style summary of one head count, shareable as plain text.
struct HeadCountReceiptView: View {
    let report: HeadCountReport
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                receipt
                    .padding(20)
                    .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 4))
                    .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
                    .padding(16)
            }
            .background(Color(.secondarySystemBackground))
            .navigationTitle("Summary")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    ShareLink(item: report.text, subject: Text("Head count · \(report.venueName)")) {
                        Label("Share report", systemImage: "square.and.arrow.up")
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var receipt: some View {
        VStack(alignment: .leading, spacing: 4) {
            Group {
                mono("HEAD COUNT REPORT", .title3, .bold)
                mono(report.venueName.uppercased(), .headline, .bold)
                if !report.venueLocation.isEmpty {
                    mono(report.venueLocation, .subheadline).foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity)
            .multilineTextAlignment(.center)
            .accessibilityElement(children: .combine)
            rule

            heading("AREA BREAKDOWN")
            ForEach(Array(report.lines.enumerated()), id: \.offset) { _, line in
                HStack(alignment: .firstTextBaseline) {
                    mono(line.isIncluded ? line.name : "\(line.name) (excluded)", .body)
                        .foregroundStyle(.secondary)
                    Spacer(minLength: 12)
                    mono("\(line.count)", .title2, .black)
                        .foregroundStyle(line.isIncluded ? Color.primary : Color.secondary)
                }
                .accessibilityElement(children: .combine)
                if !line.notes.isEmpty {
                    field("Note", line.notes)
                }
                Divider().opacity(0.6)
            }
            rule

            heading("TOTAL")
            field("Total Count", "\(report.totalCount)")
            field("Total Capacity", "\(report.totalCapacity)")
            field("Utilization", "\(report.utilizationPercent)%")
            rule

            if !report.eventNotes.isEmpty {
                heading("EVENT NOTES")
                mono(report.eventNotes, .body)
                Spacer().frame(height: 8)
            }
            if !report.weather.isEmpty {
                meta("Weather", report.weather)
                Spacer().frame(height: 8)
            }
            field("Event", report.eventName)
            field("Date", report.dateText)
            field("Time", report.timeText)
            field("Counted By", report.countedBy)
            Spacer().frame(height: 8)
            meta("Generated", report.generatedText)
            meta("ID", report.reference)
        }
        .textSelection(.enabled)
    }

    private func mono(_ text: String, _ style: Font.TextStyle, _ weight: Font.Weight = .regular) -> Text {
        Text(text).font(.system(style, design: .monospaced).weight(weight))
    }

    private func heading(_ text: String) -> some View {
        mono(text, .headline, .bold)
            .tracking(3)
            .padding(.top, 8)
            .padding(.bottom, 4)
            .accessibilityAddTraits(.isHeader)
    }

    private func field(_ label: String, _ value: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            mono("\(label):", .subheadline)
                .foregroundStyle(.secondary)
                .frame(width: 130, alignment: .leading)
            mono(value, .subheadline)
        }
        .accessibilityElement(children: .combine)
    }

    private func meta(_ label: String, _ value: String) -> some View {
        HStack(spacing: 4) {
            mono("\(label):", .subheadline)
            mono(value, .caption).opacity(0.7)
        }
        .foregroundStyle(.secondary)
        .accessibilityElement(children: .combine)
    }

    private var rule: some View {
        Divider().padding(.vertical, 6)
    }
}
