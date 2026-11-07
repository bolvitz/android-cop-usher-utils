import SwiftUI
import shared

// Building blocks that mirror the Android Compose screens (cards on the shared
// ground colour, FAB, chips), so both apps lay out every screen the same way.

/// Shared palette roles, named after the Material roles Android maps them to.
enum AppColor {
    static let background = CounterPalette.ground
    static let card = CounterPalette.plate
    static let cardHigh = CounterPalette.plateHigh
    static let primary = CounterPalette.brass
    static let onPrimary = CounterPalette.brassInk
    static let text = CounterPalette.chalk
    static let secondaryText = CounterPalette.pew
    static let outline = CounterPalette.line
}

/// Android's `FieldShapes`: small 14, medium (cards) 18, large (FAB) 24.
enum AppRadius {
    static let small: CGFloat = 14
    static let card: CGFloat = 18
    static let large: CGFloat = 24
}

extension View {
    /// Material `Card`: plate fill, 18pt corners.
    func appCard(padding: CGFloat = 16) -> some View {
        self.padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColor.card, in: RoundedRectangle(cornerRadius: AppRadius.card))
    }

    /// Ground colour behind the whole screen, as Android's `surface`.
    func appBackground() -> some View {
        self.background(AppColor.background.ignoresSafeArea())
    }

    /// Bottom-trailing floating action button, as Android's `FloatingActionButton`.
    func fab(
        _ label: String,
        systemImage: String = "plus",
        text: String? = nil,
        isShown: Bool = true,
        action: @escaping () -> Void
    ) -> some View {
        self.overlay(alignment: .bottomTrailing) {
            if isShown { fabButton(label, systemImage: systemImage, text: text, action: action) }
        }
    }

    private func fabButton(_ label: String, systemImage: String, text: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage).font(.title3.weight(.semibold))
                if let text { Text(text).font(.subheadline.weight(.semibold)) }
            }
            .foregroundStyle(AppColor.primary)
            .frame(minWidth: 56, minHeight: 56)
            .padding(.horizontal, text == nil ? 0 : 16)
            .background(AppColor.cardHigh, in: RoundedRectangle(cornerRadius: AppRadius.large))
            .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
        }
        .accessibilityLabel(label)
        .padding(16)
    }
}

/// Vertical card list with 16pt gutters, as Android's `LazyColumn(contentPadding = 16.dp)`.
struct CardList<Content: View>: View {
    var spacing: CGFloat = 8
    @ViewBuilder let content: Content

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: spacing) { content }
                .padding(16)
                // Room for the FAB so the last card's buttons stay reachable.
                .padding(.bottom, 72)
        }
        .appBackground()
    }
}

/// Centered grey message for empty and loading states.
struct EmptyMessage: View {
    let text: String

    var body: some View {
        Text(text)
            .foregroundStyle(AppColor.secondaryText)
            .multilineTextAlignment(.center)
            .padding(32)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .appBackground()
    }
}

struct LoadingView: View {
    var body: some View {
        ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity).appBackground()
    }
}

/// In-content screen title on tab roots, as Android's `headlineSmall` bold title.
struct TabTitle: View {
    let text: String
    var body: some View {
        Text(text).font(.title2.bold()).foregroundStyle(AppColor.text)
            .accessibilityAddTraits(.isHeader)
    }
}

/// Small grey label above a group of rows.
struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text).font(.subheadline.weight(.medium)).foregroundStyle(AppColor.secondaryText)
            .padding(.top, 8)
    }
}

/// Rounded tappable row with a chevron, as Android's `ListRow`.
struct ListRow: View {
    let title: String
    var subtitle: String? = nil
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).foregroundStyle(AppColor.text)
                    if let subtitle {
                        Text(subtitle).font(.caption).foregroundStyle(AppColor.secondaryText)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right").font(.footnote.weight(.semibold))
                    .foregroundStyle(AppColor.secondaryText)
            }
            .padding(.horizontal, 16).padding(.vertical, 10)
            .frame(minHeight: 56)
            .background(AppColor.card, in: RoundedRectangle(cornerRadius: AppRadius.small))
            .contentShape(Rectangle())
            .opacity(enabled ? 1 : 0.5)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// Label over a big number; tappable when given an action.
struct StatCard: View {
    let label: String
    let value: String
    var large = true
    var action: (() -> Void)? = nil

    var body: some View {
        let card = VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption).foregroundStyle(AppColor.secondaryText)
            Text(value).font(large ? .title.bold() : .title3.bold()).foregroundStyle(AppColor.text)
        }
        .appCard(padding: large ? 14 : 12)
        .accessibilityElement(children: .combine)

        if let action {
            Button(action: action) { card }.buttonStyle(.plain)
        } else {
            card
        }
    }
}

/// Material `FilterChip`: outlined when off, filled with a check when on.
struct FilterChip: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if selected { Image(systemName: "checkmark").font(.caption.weight(.bold)) }
                Text(title).font(.subheadline.weight(.medium))
            }
            .padding(.horizontal, 12)
            .frame(minHeight: 32)
            .foregroundStyle(selected ? AppColor.primary : AppColor.text)
            .background(selected ? AppColor.cardHigh : .clear, in: RoundedRectangle(cornerRadius: 8))
            .overlay {
                if !selected { RoundedRectangle(cornerRadius: 8).stroke(AppColor.outline) }
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

/// Row of filter chips with 16pt gutters; scrolls when it overflows.
struct ChipRow<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) { content }
                .padding(.horizontal, 16).padding(.vertical, 8)
        }
    }
}

/// Solid coloured label, as Android's severity / status badge.
struct ColorBadge: View {
    let text: String
    let hex: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.medium))
            .foregroundStyle(.white)
            .padding(.horizontal, 8).padding(.vertical, 2)
            .background(Color(hex: hex), in: RoundedRectangle(cornerRadius: AppRadius.small))
    }
}

/// Tonal pill showing a status, as Android's `AssistChip`.
struct StatusChip: View {
    let text: String
    var color: Color = AppColor.text

    var body: some View {
        Text(text)
            .font(.subheadline.weight(.medium))
            .foregroundStyle(color)
            .padding(.horizontal, 12)
            .frame(minHeight: 32)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(AppColor.outline))
    }
}

/// Material `TextButton`: brass text, no fill.
struct TextAction: View {
    let title: String
    var role: ButtonRole? = nil
    let action: () -> Void

    var body: some View {
        // Padding sits inside the label so a near miss doesn't fall through to the card's tap.
        Button(role: role, action: action) {
            Text(title)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(AppColor.primary)
                .padding(.horizontal, 8)
                .frame(minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Material `OutlinedTextField`: label above a bordered field.
struct OutlinedField: View {
    let label: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var axis: Axis = .horizontal

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(AppColor.secondaryText)
                .accessibilityHidden(true)
            TextField(label, text: $text, axis: axis)
                .keyboardType(keyboard)
                .foregroundStyle(AppColor.text)
                .padding(.horizontal, 14)
                .frame(minHeight: 52)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(AppColor.secondaryText.opacity(0.6)))
        }
    }
}

/// Full-width filled or outlined button, as Android's `Button` / `OutlinedButton`.
struct WideButton: View {
    let title: String
    var filled = true
    let action: () -> Void
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        Button(action: action) {
            Text(title).font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: 48)
                .foregroundStyle(filled ? AppColor.onPrimary : AppColor.primary)
                .background(filled ? AppColor.primary : .clear, in: Capsule())
                .overlay { if !filled { Capsule().stroke(AppColor.outline) } }
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .opacity(isEnabled ? 1 : 0.5)
    }
}

/// Small title row standing in for Android's `TopAppBar` on tab roots, where the nav bar is hidden.
struct ScreenHeader: View {
    let title: String

    var body: some View {
        Text(title).font(.title2).foregroundStyle(AppColor.text)
            .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
            .padding(.horizontal, 16)
            .accessibilityAddTraits(.isHeader)
    }
}

extension View {
    /// Pushed screens use the small inline title Android's `TopAppBar` shows.
    func appNavigationTitle(_ title: String) -> some View {
        self.navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(AppColor.background, for: .navigationBar)
    }
}

extension View {
    /// "Which venue?" action sheet, used before adding a record while the list shows all venues.
    func venuePicker(isPresented: Binding<Bool>, venues: [VenueScopeRow], onPick: @escaping (String) -> Void) -> some View {
        self.confirmationDialog("Which venue?", isPresented: isPresented, titleVisibility: .visible) {
            ForEach(venues, id: \.id) { venue in
                Button(venue.name) { onPick(venue.id) }
            }
            Button("Cancel", role: .cancel) {}
        }
    }
}
