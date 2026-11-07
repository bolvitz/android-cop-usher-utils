import SwiftUI
import UIKit
import shared

/// Receipt-style summary of one head count. It feeds out of a printer slot line by line,
/// and can be shared as plain text. Pass `onNotesChange` to make the event notes editable.
struct HeadCountReceiptView: View {
    let report: HeadCountReport
    var notes: String? = nil
    var onNotesChange: ((String) -> Void)? = nil
    let onClose: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var fed: Int = 0
    @State private var printing = true
    @State private var shown = false
    @State private var paperHeight: CGFloat = 0
    @State private var noteText = ""
    @State private var closing = false
    @FocusState private var noteFocused: Bool

    private let feedSteps = 34

    var body: some View {
        ZStack(alignment: .top) {
            Color.black.opacity(shown ? 0.72 : 0)
                .ignoresSafeArea()
                .onTapGesture { noteFocused = false }

            VStack(spacing: 0) {
                CounterGlassGroup(spacing: 12) {
                    HStack {
                        ShareLink(item: shareText, subject: Text("Head count, \(report.venueName)")) {
                            Label("Share", systemImage: "square.and.arrow.up")
                        }
                        .buttonStyle(GlassPillStyle())
                        Spacer()
                        Button("Done", action: close)
                            .buttonStyle(GlassPillStyle())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .opacity(shown ? 1 : 0)
                // The paper slides up behind the slot while printing; keep these controls on top of it.
                .zIndex(2)

                printer
                    .padding(.top, 16)
                    .zIndex(1)
            }
        }
        .preferredColorScheme(.dark)
        .onAppear {
            noteText = notes ?? report.eventNotes
            withAnimation(.easeOut(duration: 0.22)) { shown = true }
        }
        .task { await feed() }
        .task(id: noteText) {
            // Saves once typing pauses rather than on every keystroke.
            guard let onNotesChange, noteText != (notes ?? report.eventNotes) else { return }
            try? await Task.sleep(for: .milliseconds(450))
            guard !Task.isCancelled else { return }
            onNotesChange(noteText)
        }
    }

    private var shareText: String {
        guard onNotesChange != nil, noteText != report.eventNotes else { return report.text }
        return report.text + "\n\nEvent notes: \(noteText)"
    }

    // MARK: Printer

    private var printer: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .trailing) {
                Capsule()
                    .fill(CounterPalette.plateHigh)
                    .overlay(Capsule().strokeBorder(.black.opacity(0.4), lineWidth: 2))
                    .frame(height: 12)
                Circle()
                    .fill(printing && fed % 4 < 2 ? CounterPalette.brass : CounterPalette.plate)
                    .frame(width: 6, height: 6)
                    .padding(.trailing, 10)
            }
            .padding(.horizontal, 22)
            .zIndex(1)
            .accessibilityHidden(true)

            GeometryReader { _ in
                let remaining = 1 - CGFloat(fed) / CGFloat(feedSteps)
                ScrollView {
                    VStack(spacing: 0) {
                        paper
                        TornEdge()
                            .fill(CounterPalette.paper)
                            .frame(height: 10)
                    }
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { paperHeight = $0 }
                }
                .scrollDisabled(printing)
                .scrollIndicators(.hidden)
                // A receipt taller than the viewport feeds its full length, torn edge first; the outer clip hides the rest.
                .scrollClipDisabled(printing)
                .offset(x: printing && fed % 2 == 1 ? 0.6 : 0, y: -paperHeight * remaining)
                .opacity(paperHeight > 0 ? 1 : 0)
                // Clipping hides the unprinted paper but not its touch area, so ignore touches while it feeds.
                .allowsHitTesting(!printing)
                .clipped()
            }
            .padding(.horizontal, 34)
            .padding(.top, -6)
            .padding(.bottom, 16)
        }
    }

    private func feed() async {
        if reduceMotion {
            fed = feedSteps
            printing = false
            return
        }
        let haptic = UIImpactFeedbackGenerator(style: .soft)
        haptic.prepare()
        try? await Task.sleep(for: .milliseconds(180))
        for step in 1...feedSteps {
            guard !Task.isCancelled else { break }
            withAnimation(.easeOut(duration: 0.05)) { fed = step }
            haptic.impactOccurred(intensity: 0.35)
            try? await Task.sleep(for: .milliseconds(68))
        }
        fed = feedSteps
        printing = false
    }

    private func close() {
        // A double tap on Done must not call onClose twice.
        guard !closing else { return }
        closing = true
        noteFocused = false
        if let onNotesChange, noteText != (notes ?? report.eventNotes) { onNotesChange(noteText) }
        withAnimation(.easeIn(duration: 0.18)) { shown = false }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) { onClose() }
    }

    // MARK: Paper

    private var paper: some View {
        VStack(alignment: .leading, spacing: 4) {
            VStack(spacing: 2) {
                mono("Head count report", 16, .bold)
                mono(report.venueName, 14, .bold)
                if !report.venueLocation.isEmpty {
                    mono(report.venueLocation, 12).opacity(0.7)
                }
            }
            .frame(maxWidth: .infinity)
            .multilineTextAlignment(.center)
            rule

            heading("Area breakdown")
            ForEach(Array(report.lines.enumerated()), id: \.offset) { _, line in
                HStack(alignment: .firstTextBaseline) {
                    mono(line.isIncluded ? line.name : "\(line.name) (excluded)", 13)
                    Spacer(minLength: 12)
                    mono("\(line.count)", 17, .bold)
                        .foregroundStyle(line.isIncluded ? CounterPalette.paperAccent : CounterPalette.paperInk)
                }
                .opacity(line.isIncluded ? 1 : 0.55)
                .accessibilityElement(children: .combine)
                if !line.notes.isEmpty {
                    field("Note", line.notes)
                }
            }
            rule

            field("Total count", "\(report.totalCount)", bold: true)
            field("Total capacity", "\(report.totalCapacity)")
            field("Utilization", "\(report.utilizationPercent)%")
            rule

            notesSection
            if !report.weather.isEmpty {
                mono("Weather: \(report.weather)", 12).opacity(0.7)
            }
            rule

            field("Event", report.eventName)
            field("Date", report.dateText)
            field("Time", report.timeText)
            field("Counted by", report.countedBy)
            mono("Generated \(report.generatedText)", 11).opacity(0.6).padding(.top, 8)
            mono("ID \(report.reference)", 11).opacity(0.6)
        }
        .foregroundStyle(CounterPalette.paperInk)
        .padding(.horizontal, 18)
        .padding(.top, 18)
        .padding(.bottom, 8)
        .background(CounterPalette.paper)
        .environment(\.colorScheme, .light)
    }

    @ViewBuilder
    private var notesSection: some View {
        if onNotesChange != nil {
            heading("Event notes")
            TextField("Tap to add notes for this count", text: $noteText, axis: .vertical)
                .font(.system(size: 13, design: .monospaced))
                .lineLimit(2...6)
                .focused($noteFocused)
                .padding(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .strokeBorder(CounterPalette.paperInk.opacity(0.4), style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
                )
                .accessibilityLabel("Event notes")
        } else if !report.eventNotes.isEmpty {
            heading("Event notes")
            mono(report.eventNotes, 13)
        }
    }

    private func mono(_ text: String, _ size: CGFloat, _ weight: Font.Weight = .regular) -> Text {
        Text(text).font(.system(size: size, weight: weight, design: .monospaced))
    }

    private func heading(_ text: String) -> some View {
        mono(text, 13, .bold)
            .padding(.top, 4)
            .padding(.bottom, 2)
            .accessibilityAddTraits(.isHeader)
    }

    private func field(_ label: String, _ value: String, bold: Bool = false) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            mono(label, 13).opacity(0.75)
            Spacer(minLength: 8)
            mono(value, 13, bold ? .bold : .regular).multilineTextAlignment(.trailing)
        }
    }

    private var rule: some View {
        Line()
            .stroke(CounterPalette.paperInk.opacity(0.35), style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
            .frame(height: 1)
            .padding(.vertical, 8)
            .accessibilityHidden(true)
    }
}

private struct Line: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.midY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        return path
    }
}

/// Zigzag bottom edge of torn receipt paper.
private struct TornEdge: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let tooth: CGFloat = 14
        path.move(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        var x = rect.maxX
        var down = true
        while x > rect.minX {
            x -= tooth / 2
            path.addLine(to: CGPoint(x: max(x, rect.minX), y: down ? rect.maxY : rect.minY))
            down.toggle()
        }
        path.closeSubpath()
        return path
    }
}
