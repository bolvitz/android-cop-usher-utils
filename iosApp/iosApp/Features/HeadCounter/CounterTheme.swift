import SwiftUI
import UIKit
import shared

// MARK: - Palette

/// Head counter palette: dark-first for dim halls, with a tuned light variant.
enum CounterPalette {
    /// Values come from the shared `DesignTokens`, so Android reads the same ones.
    private static let tokens = DesignTokens.shared

    static let ground = dynamic(tokens.ground)
    static let plate = dynamic(tokens.plate)
    static let plateHigh = dynamic(tokens.plateHigh)
    static let brass = dynamic(tokens.brass)
    static let brassInk = dynamic(tokens.brassInk)
    static let chalk = dynamic(tokens.chalk)
    static let pew = dynamic(tokens.pew)
    /// Over capacity only.
    static let signal = dynamic(tokens.signal)
    static let line = chalk.opacity(tokens.LINE_ALPHA)

    static let paper = Color(hex: tokens.PAPER)
    static let paperInk = Color(hex: tokens.PAPER_INK)
    static let paperAccent = Color(hex: tokens.PAPER_ACCENT)

    /// Bar and status colour for a fill percentage; the status text carries the same meaning.
    static func level(_ percent: Int) -> Color {
        percent >= 100 ? signal : percent >= 90 ? brass : chalk
    }

    private static func dynamic(_ color: ThemedColor) -> Color {
        let dark = UIColor(hex: color.dark), light = UIColor(hex: color.light)
        return Color(UIColor { $0.userInterfaceStyle == .dark ? dark : light })
    }
}

extension UIColor {
    convenience init(hex: Int64) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

extension Color {
    init(hex: Int64) { self.init(uiColor: UIColor(hex: hex)) }
}

extension Animation {
    /// A shared spring token as a SwiftUI spring.
    static func token(_ spring: SpringToken) -> Animation {
        .spring(response: spring.response, dampingFraction: spring.damping)
    }
}

// MARK: - Type

enum CounterFont {
    /// Condensed figures for every count; bundled so both apps match.
    static func digits(_ size: CGFloat, bold: Bool = false) -> Font {
        .custom(bold ? "BarlowCondensed-Bold" : "BarlowCondensed-SemiBold", fixedSize: size)
    }
}

// MARK: - Liquid Glass

extension View {
    /// Liquid Glass on iOS 26+, a material of the same shape on earlier systems.
    @ViewBuilder
    func counterGlass<S: Shape>(_ shape: S, tint: Color? = nil, interactive: Bool = false) -> some View {
        if #available(iOS 26, *) {
            self.glassEffect(Glass.regular.tint(tint).interactive(interactive), in: shape)
        } else {
            self.background {
                ZStack {
                    shape.fill(.ultraThinMaterial)
                    if let tint { shape.fill(tint) }
                }
            }
        }
    }

    @ViewBuilder
    func counterGlassID(_ id: String, in namespace: Namespace.ID) -> some View {
        if #available(iOS 26, *) {
            self.glassEffectID(id, in: namespace)
        } else {
            self
        }
    }
}

/// Groups glass shapes so they blend and morph together on iOS 26+.
struct CounterGlassGroup<Content: View>: View {
    var spacing: CGFloat = 8
    @ViewBuilder let content: Content

    var body: some View {
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: spacing) { content }
        } else {
            content
        }
    }
}

/// Glass capsule button for secondary actions (tool dock, receipt controls).
struct GlassPillStyle: ButtonStyle {
    var tint: Color? = nil
    var foreground: Color = CounterPalette.chalk
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(foreground)
            .padding(.horizontal, 16)
            .frame(minHeight: CGFloat(DesignTokens.shared.MIN_TOUCH))
            .contentShape(Capsule())
            .counterGlass(Capsule(), tint: tint, interactive: isEnabled)
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .opacity(isEnabled ? 1 : 0.38)
            .animation(.token(DesignTokens.shared.pressSpring), value: configuration.isPressed)
    }
}

// MARK: - Odometer

/// Mechanical counter: each digit wheel rolls to its new value. No leading zeros.
struct Odometer: View {
    let value: Int
    let size: CGFloat
    var color: Color = CounterPalette.chalk
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// Places keyed from the units up, so the units wheel stays put when a digit is added.
    private var places: [Int] {
        let count = String(max(0, value)).count
        return Array((0..<count).reversed())
    }

    var body: some View {
        HStack(spacing: -size * 0.02) {
            ForEach(places, id: \.self) { place in
                DigitWheel(digit: (max(0, value) / Int(pow(10, Double(place)))) % 10, size: size)
                    .transition(.asymmetric(insertion: .push(from: .trailing), removal: .opacity))
            }
        }
        .foregroundStyle(color)
        .animation(reduceMotion ? nil : .token(DesignTokens.shared.rollSpring), value: value)
        .accessibilityHidden(true)
    }
}

private struct DigitWheel: View {
    let digit: Int
    let size: CGFloat

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<10, id: \.self) { n in
                Text("\(n)")
                    .font(CounterFont.digits(size))
                    .frame(height: size)
            }
        }
        .frame(width: size * 0.5, height: size, alignment: .top)
        .offset(y: -CGFloat(digit) * size)
        .frame(width: size * 0.5, height: size, alignment: .top)
        .clipped()
    }
}

// MARK: - Haptics

@MainActor
enum CounterHaptics {
    static func tick() { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
    static func soft() { UIImpactFeedbackGenerator(style: .soft).impactOccurred(intensity: 0.6) }
    static func select() { UISelectionFeedbackGenerator().selectionChanged() }
    static func warning() { UINotificationFeedbackGenerator().notificationOccurred(.warning) }
    static func success() { UINotificationFeedbackGenerator().notificationOccurred(.success) }
    static func heavy() { UIImpactFeedbackGenerator(style: .rigid).impactOccurred() }
}

/// Percent with no capacity treated as 0, so empty zones never divide by zero.
func percent(_ count: Int, of capacity: Int) -> Int {
    capacity > 0 ? Int((Double(count) * 100 / Double(capacity)).rounded()) : 0
}

/// Fires when a fill level crosses 90% or 100% on the way up.
func crossedWarning(from old: Int, to new: Int) -> Bool {
    (old < 90 && new >= 90) || (old < 100 && new >= 100)
}
