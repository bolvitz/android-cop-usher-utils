import SwiftUI
import shared

/// Intake shown before a new count opens: what is being counted, an automatic start time, and who is counting.
struct NewCountSheet: View {
    let venueName: String
    let eventTypes: [EventTypeDto]
    let onCancel: () -> Void
    let onStart: (EventTypeDto?, String) -> Void

    /// nil selects the built-in general "Head count" type.
    @State private var selectedTypeId: String?
    @State private var countedBy = ""
    @FocusState private var nameFocused: Bool
    @State private var openedAt = Date()

    private var trimmedName: String {
        countedBy.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("New count").font(.largeTitle.weight(.bold))
                    Text(venueName).font(.subheadline).foregroundStyle(CounterPalette.pew)
                }
                Spacer()
                Button("Cancel", action: onCancel)
                    .buttonStyle(GlassPillStyle())
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)

            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("What are you counting?").font(.headline)
                        VStack(spacing: 8) {
                            typeRow(name: "Head count", detail: "A general count with no event type", id: nil)
                            ForEach(eventTypes, id: \.id) { type in
                                typeRow(
                                    name: type.name,
                                    detail: type.description_.isEmpty ? "Event type" : type.description_,
                                    id: type.id
                                )
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Started").font(.headline)
                        HStack(alignment: .firstTextBaseline) {
                            Text(openedAt.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).hour().minute()))
                                .font(CounterFont.digits(30))
                            Spacer()
                            Text("Set automatically").font(.footnote).foregroundStyle(CounterPalette.pew)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(CounterPalette.plate, in: RoundedRectangle(cornerRadius: 16))
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Who is counting?").font(.headline)
                        TextField("Your name", text: $countedBy)
                            .font(.title3)
                            .textContentType(.name)
                            .textInputAutocapitalization(.words)
                            .submitLabel(.go)
                            .focused($nameFocused)
                            .onSubmit(start)
                            .padding(.horizontal, 16)
                            .frame(height: 56)
                            .background(CounterPalette.plate, in: RoundedRectangle(cornerRadius: 16))
                        Text("Your name goes on the report.")
                            .font(.footnote)
                            .foregroundStyle(CounterPalette.pew)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)
                .padding(.bottom, 32)
            }
            .scrollDismissesKeyboard(.interactively)

            Button(action: start) {
                Text("Start count")
                    .font(CounterFont.digits(26))
                    .foregroundStyle(CounterPalette.brassInk)
                    .frame(maxWidth: .infinity)
                    .frame(height: 60)
                    .contentShape(Capsule())
                    .counterGlass(Capsule(), tint: CounterPalette.brass, interactive: !trimmedName.isEmpty)
                    .opacity(trimmedName.isEmpty ? 0.4 : 1)
            }
            .buttonStyle(.plain)
            .disabled(trimmedName.isEmpty)
            .animation(.easeInOut(duration: 0.2), value: trimmedName.isEmpty)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .foregroundStyle(CounterPalette.chalk)
        .background(CounterPalette.ground.ignoresSafeArea())
        .preferredColorScheme(.dark)
        .onAppear { nameFocused = true }
    }

    private func start() {
        guard !trimmedName.isEmpty else { return }
        CounterHaptics.success()
        onStart(eventTypes.first { $0.id == selectedTypeId }, trimmedName)
    }

    private func typeRow(name: String, detail: String, id: String?) -> some View {
        let selected = selectedTypeId == id
        return Button {
            CounterHaptics.select()
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { selectedTypeId = id }
        } label: {
            HStack(spacing: 14) {
                ZStack {
                    Circle().strokeBorder(selected ? CounterPalette.brass : CounterPalette.pew, lineWidth: 2)
                    if selected {
                        Circle().fill(CounterPalette.brass).padding(5).transition(.scale)
                    }
                }
                .frame(width: 20, height: 20)
                VStack(alignment: .leading, spacing: 2) {
                    Text(name).font(.body.weight(.medium))
                    Text(detail).font(.footnote).foregroundStyle(CounterPalette.pew).lineLimit(1)
                }
                Spacer()
            }
            .foregroundStyle(CounterPalette.chalk)
            .padding(.horizontal, 16)
            .frame(minHeight: 60)
            .contentShape(RoundedRectangle(cornerRadius: 16))
            .background(selected ? CounterPalette.plate : .clear, in: RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(selected ? CounterPalette.brass : CounterPalette.line, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
