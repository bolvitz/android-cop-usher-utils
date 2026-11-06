import SwiftUI
import UIKit
import shared

private enum SeatStatus: String {
    case available = "AVAILABLE"
    case occupied = "OCCUPIED"
    case reserved = "RESERVED"
    case blocked = "BLOCKED"

    var name: String {
        switch self {
        case .available: return "Available"
        case .occupied: return "Taken"
        case .reserved: return "Reserved"
        case .blocked: return "Blocked"
        }
    }
}

/// Seat-by-seat counter for a seat-mapped zone: tap a seat to cycle its status.
struct SeatMapView: View {
    @ObservedObject var model: CountingModel
    let areaTemplateId: String
    let zoneName: String

    @State private var rows: [SeatRowWithSeats] = []
    @State private var statusBySeat: [String: String] = [:]
    @State private var handles: [Cancellable] = []
    @State private var popped: String?

    private var allSeats: [SeatEntity] { rows.flatMap { $0.seats } }

    private func count(_ status: SeatStatus) -> Int {
        allSeats.filter { (statusBySeat[$0.id] ?? SeatStatus.available.rawValue) == status.rawValue }.count
    }

    var body: some View {
        let taken = count(.occupied)
        let usable = allSeats.count - count(.blocked)
        let pct = percent(taken, of: usable)

        VStack(spacing: 0) {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Seated").font(.footnote).foregroundStyle(CounterPalette.pew)
                    Odometer(value: taken, size: 72, color: CounterPalette.brass)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel("Seated")
                        .accessibilityValue("\(taken)")
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(pct)% full").font(.headline).foregroundStyle(CounterPalette.level(pct))
                    Text("of \(usable) usable seats").font(.footnote).foregroundStyle(CounterPalette.pew)
                }
                .padding(.bottom, 10)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)

            if rows.isEmpty {
                ContentUnavailableView(
                    "No seats yet",
                    systemImage: "square.grid.3x3",
                    description: Text("Add rows in this zone's seat map editor, then come back to count.")
                )
                .frame(maxHeight: .infinity)
            } else {
                GeometryReader { geo in
                    ScrollView([.horizontal, .vertical]) {
                        grid
                            .padding(.vertical, 20)
                            .padding(.horizontal, 12)
                            .frame(minWidth: geo.size.width, minHeight: geo.size.height, alignment: .top)
                    }
                    .scrollBounceBehavior(.basedOnSize, axes: [.horizontal, .vertical])
                }
                legend
            }
        }
        .foregroundStyle(CounterPalette.chalk)
        .background(CounterPalette.ground.ignoresSafeArea())
        .navigationTitle(zoneName)
        .navigationBarTitleDisplayMode(.inline)
        .preferredColorScheme(.dark)
        .task(id: areaTemplateId) {
            handles.forEach { $0.cancel() }
            handles = model.observeSeats(
                areaTemplateId: areaTemplateId,
                onRows: { rows = $0 },
                onStatuses: { statusBySeat = $0 }
            )
        }
        // Pushing this view fires the counter's onDisappear, which lets the screen sleep; keep it on here too.
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
            handles.forEach { $0.cancel() }
            handles = []
        }
    }

    /// Stage and rows share one centre line; labels on both ends keep each row centred.
    private var grid: some View {
        VStack(spacing: 5) {
            VStack(spacing: 6) {
                UnevenRoundedRectangle(bottomLeadingRadius: 40, bottomTrailingRadius: 40)
                    .fill(CounterPalette.chalk.opacity(0.5))
                    .frame(height: 6)
                Text("Stage").font(.caption).foregroundStyle(CounterPalette.pew)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 14)
            .accessibilityHidden(true)

            ForEach(rows, id: \.row.id) { row in
                HStack(spacing: 4) {
                    rowLabel(row.row.label)
                    ForEach(row.seats.sorted { $0.number < $1.number }, id: \.id) { seat in
                        seatButton(seat)
                    }
                    rowLabel(row.row.label)
                }
            }
        }
        .fixedSize()
    }

    private func seatButton(_ seat: SeatEntity) -> some View {
        let status = SeatStatus(rawValue: statusBySeat[seat.id] ?? "") ?? .available
        return Button {
            CounterHaptics.tick()
            popped = seat.id
            model.cycleSeat(seat.id, current: status.rawValue)
        } label: {
            Text("\(seat.number)")
                .font(.system(size: 10, weight: .semibold).monospacedDigit())
                .foregroundStyle(seatInk(status))
                .frame(width: 24, height: 26)
                .background { seatFace(status) }
                .phaseAnimator([1.0, 0.7, 1.0], trigger: popped == seat.id ? statusBySeat[seat.id] : nil) { view, scale in
                    view.scaleEffect(scale)
                } animation: { scale in
                    scale < 1 ? .easeOut(duration: 0.06) : .spring(response: 0.28, dampingFraction: 0.45)
                }
        }
        .buttonStyle(.plain)
        .disabled(model.state.isLocked)
        .accessibilityLabel("Seat \(seat.label)")
        .accessibilityValue(status.name)
    }

    @ViewBuilder
    private func seatFace(_ status: SeatStatus) -> some View {
        let shape = UnevenRoundedRectangle(topLeadingRadius: 6, bottomLeadingRadius: 3, bottomTrailingRadius: 3, topTrailingRadius: 6)
        switch status {
        case .available: shape.fill(CounterPalette.plateHigh)
        case .occupied: shape.fill(CounterPalette.brass)
        case .reserved: shape.strokeBorder(CounterPalette.brass, lineWidth: 1.5)
        case .blocked:
            shape.fill(CounterPalette.plate)
                .overlay {
                    Canvas { context, size in
                        var x: CGFloat = -size.height
                        while x < size.width {
                            var path = Path()
                            path.move(to: CGPoint(x: x, y: size.height))
                            path.addLine(to: CGPoint(x: x + size.height, y: 0))
                            context.stroke(path, with: .color(CounterPalette.plateHigh), lineWidth: 2)
                            x += 6
                        }
                    }
                    .clipShape(shape)
                }
        }
    }

    private func seatInk(_ status: SeatStatus) -> Color {
        switch status {
        case .available: return CounterPalette.pew
        case .occupied: return CounterPalette.brassInk
        case .reserved: return CounterPalette.brass
        case .blocked: return .clear
        }
    }

    private func rowLabel(_ label: String) -> some View {
        Text(label)
            .font(.caption.weight(.semibold))
            .foregroundStyle(CounterPalette.pew)
            .frame(width: 18)
            .accessibilityHidden(true)
    }

    private var legend: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Tap a seat to change it: available, taken, reserved, blocked.")
                .font(.footnote)
                .foregroundStyle(CounterPalette.pew)
            Grid(horizontalSpacing: 20, verticalSpacing: 10) {
                GridRow { legendItem(.available); legendItem(.occupied) }
                GridRow { legendItem(.reserved); legendItem(.blocked) }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .counterGlass(Rectangle())
    }

    private func legendItem(_ status: SeatStatus) -> some View {
        let value = count(status)
        return HStack(spacing: 8) {
            seatFace(status).frame(width: 14, height: 16)
            Text(status.name).font(.subheadline)
            Spacer(minLength: 8)
            Text("\(value)")
                .font(CounterFont.digits(19))
                .contentTransition(.numericText(value: Double(value)))
                .animation(.snappy, value: value)
        }
        .accessibilityElement(children: .combine)
    }
}
