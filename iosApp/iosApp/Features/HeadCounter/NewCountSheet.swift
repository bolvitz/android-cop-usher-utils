import SwiftUI
import shared

/// Intake form shown before a new count opens: event type, auto timestamp, and who is counting.
struct NewCountSheet: View {
    let eventTypes: [EventTypeDto]
    let onCancel: () -> Void
    let onStart: (EventTypeDto?, String) -> Void

    /// nil selects the built-in generic "Head Count" type.
    @State private var selectedTypeId: String?
    @State private var countedBy = ""
    @FocusState private var nameFocused: Bool
    private let openedAt = Date()

    private var filingNumber: String {
        String(format: "%03d", Int(openedAt.timeIntervalSince1970) % 1000)
    }

    private var trimmedName: String {
        countedBy.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Filing № \(filingNumber)")
                        .font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundStyle(.secondary)
                    Text("New count").font(.system(size: 22, weight: .semibold))
                }
                Spacer()
                Button("Cancel", action: onCancel)
                    .buttonStyle(.bordered)
                    .tint(.primary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    section(eyebrow: "01 · TYPE", title: "Type", hint: "What's on the card")
                    VStack(spacing: 6) {
                        typeRow(index: 1, name: "Head Count", detail: "GENERAL", id: nil)
                        ForEach(Array(eventTypes.enumerated()), id: \.element.id) { offset, type in
                            typeRow(
                                index: offset + 2,
                                name: type.name,
                                detail: type.description_.isEmpty ? "EVENT TYPE" : type.description_.uppercased(),
                                id: type.id
                            )
                        }
                    }

                    section(eyebrow: "02 · STAMP", title: "Timestamp", hint: "Filed at (auto)")
                        .padding(.top, 6)
                    HStack(spacing: 10) {
                        RoundedRectangle(cornerRadius: 2).fill(Color.primary).frame(width: 6, height: 6)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(openedAt.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).hour(.twoDigits(amPM: .omitted)).minute()).uppercased())
                                .font(.headline)
                            Text("LIVE · CAPTURED AT OPEN")
                                .font(.system(size: 11, weight: .medium, design: .monospaced))
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text("NOW").font(.system(size: 11, weight: .bold, design: .monospaced))
                    }
                    .padding(16)
                    .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 14))

                    section(eyebrow: "03 · COUNTER", title: "Counter", hint: "Who's on the clicker")
                        .padding(.top, 6)
                    VStack(alignment: .leading, spacing: 6) {
                        TextField("Your name", text: $countedBy)
                            .font(.title2)
                            .textInputAutocapitalization(.words)
                            .submitLabel(.go)
                            .focused($nameFocused)
                            .onSubmit(start)
                            .padding(.vertical, 10)
                        Divider()
                        HStack {
                            Text("SIGN OFF · REQUIRED")
                            Spacer()
                            Text(trimmedName.isEmpty ? "—" : "\(countedBy.count) CHAR")
                        }
                        .font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 48)
            }
            .scrollDismissesKeyboard(.interactively)

            HStack(spacing: 10) {
                Button(action: onCancel) {
                    Text("Cancel").frame(maxWidth: .infinity).padding(.vertical, 14)
                }
                .buttonStyle(.bordered)
                .tint(.primary)
                Button(action: start) {
                    VStack(spacing: 2) {
                        Text("START COUNT").font(.system(size: 17, weight: .bold, design: .monospaced))
                        Text("BEGIN").font(.system(size: 10, design: .monospaced)).opacity(0.7)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                }
                .buttonStyle(.borderedProminent)
                .tint(.primary)
                .foregroundStyle(Color(.systemBackground))
                .disabled(trimmedName.isEmpty)
                .layoutPriority(1)
                .accessibilityLabel("Start Count")
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(.bar)
        }
        .background(Color(.systemBackground))
        .onAppear { nameFocused = true }
    }

    private func start() {
        guard !trimmedName.isEmpty else { return }
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        onStart(eventTypes.first { $0.id == selectedTypeId }, trimmedName)
    }

    private func section(eyebrow: String, title: String, hint: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(eyebrow)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .tracking(1.4)
                .foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline) {
                Text(title).font(.title3.weight(.semibold))
                Spacer()
                Text(hint).font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private func typeRow(index: Int, name: String, detail: String, id: String?) -> some View {
        let selected = selectedTypeId == id
        return Button {
            UISelectionFeedbackGenerator().selectionChanged()
            selectedTypeId = id
        } label: {
            HStack {
                Text(String(format: "%02d", index))
                    .font(.system(size: 11, design: .monospaced))
                    .opacity(0.6)
                    .frame(width: 28, alignment: .leading)
                VStack(alignment: .leading, spacing: 2) {
                    Text(name).font(.headline)
                    Text(detail)
                        .font(.system(size: 11, design: .monospaced))
                        .opacity(0.6)
                        .lineLimit(1)
                }
                Spacer()
                Image(systemName: selected ? "largecircle.fill.circle" : "circle")
            }
            .foregroundStyle(selected ? Color(.systemBackground) : Color.primary)
            .padding(14)
            .background(selected ? Color.primary : Color(.systemBackground))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
