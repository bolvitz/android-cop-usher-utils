import SwiftUI
import UIKit
import shared

@MainActor
final class CountingModel: ObservableObject {
    let viewModel: CountingViewModel
    private let lifetime: ViewModelLifetime
    @Published var state: CountingUiState
    @Published var eventTypes: [EventTypeDto] = []
    @Published var canUndo = false
    @Published var canRedo = false

    init(venueId: String, eventId: String? = nil) {
        viewModel = KoinIosKt.countingViewModel(venueId: venueId, eventId: eventId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! CountingUiState
        lifetime.observe(viewModel.uiState, as: CountingUiState.self) { [weak self] in
            self?.state = $0
        }
        lifetime.observe(viewModel.eventTypes, as: [EventTypeDto].self) { [weak self] in
            self?.eventTypes = $0
        }
        lifetime.observe(viewModel.canUndo, as: KotlinBoolean.self) { [weak self] in
            self?.canUndo = $0.boolValue
        }
        lifetime.observe(viewModel.canRedo, as: KotlinBoolean.self) { [weak self] in
            self?.canRedo = $0.boolValue
        }
    }

    func startCount(eventType: EventTypeDto?) {
        viewModel.createNewEvent(
            eventTypeId: eventType?.id,
            eventTypeName: eventType?.name ?? "Head Count",
            date: nowEpochMillis(),
            countedBy: "iOS"
        )
    }

    func increment(_ id: String) { viewModel.incrementCount(areaCountId: id, amount: 1) }
    func decrement(_ id: String) { viewModel.decrementCount(areaCountId: id, amount: 1) }
    func toggleInclusion(_ id: String) { viewModel.toggleAreaInclusion(areaCountId: id) }
    func undo() { viewModel.undo() }
    func redo() { viewModel.redo() }

    func toggleLock() {
        state.isLocked ? viewModel.unlockEvent() : viewModel.lockEvent()
    }

    func clearError() {
        viewModel.clearError()
    }

    /// Observes an area's seat layout and per-event statuses; cancel the returned handles to stop.
    func observeSeats(
        areaTemplateId: String,
        onRows: @escaping ([SeatRowWithSeats]) -> Void,
        onStatuses: @escaping ([String: String]) -> Void
    ) -> [Cancellable] {
        var handles = [
            lifetime.observe(viewModel.observeSeatLayout(areaTemplateId: areaTemplateId), as: [SeatRowWithSeats].self, onRows)
        ]
        if let statuses = viewModel.observeSeatStatuses(areaTemplateId: areaTemplateId) {
            handles.append(lifetime.observe(statuses, as: [SeatStatusEntity].self) { list in
                onStatuses(Dictionary(list.map {
                    ($0.seatId, $0.status)
                }, uniquingKeysWith: { _, last in last }))
            })
        }
        return handles
    }

    func cycleSeat(_ seatId: String, current: String) {
        viewModel.cycleSeatStatus(seatId: seatId, currentStatus: current)
    }
}

private struct SeatSheetTarget: Identifiable {
    let id: String
    let name: String
}

struct HeadCounterView: View {
    let venueName: String
    @StateObject private var model: CountingModel
    @State private var seatSheet: SeatSheetTarget?

    /// Pass `eventId` to resume an existing count; otherwise the user starts a new one.
    init(venueId: String, venueName: String, eventId: String? = nil) {
        self.venueName = venueName
        _model = StateObject(wrappedValue: CountingModel(venueId: venueId, eventId: eventId))
    }

    var body: some View {
        Group {
            if model.state.isLoading {
                ProgressView("Loading…")
            } else if model.state.eventId == nil {
                startPrompt
            } else {
                counter
            }
        }
        .navigationTitle(model.state.venueName.isEmpty ? venueName : model.state.venueName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if model.state.eventId != nil {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { model.undo() } label: { Image(systemName: "arrow.uturn.backward") }
                        .disabled(!model.canUndo)
                    .accessibilityLabel("Undo")
                    Button { model.redo() } label: { Image(systemName: "arrow.uturn.forward") }
                        .disabled(!model.canRedo)
                    .accessibilityLabel("Redo")
                    Button {
                        model.toggleLock()
                    } label: {
                        Image(systemName: model.state.isLocked ? "lock.fill" : "lock.open")
                    }
                    .accessibilityLabel(model.state.isLocked ? "Unlock count" : "Lock count")
                }
            }
        }
        .sheet(item: $seatSheet) { target in
            SeatMapSheet(model: model, areaTemplateId: target.id, areaName: target.name)
        }
        .alert(
            "Something went wrong",
            isPresented: Binding(get: { model.state.error != nil }, set: { if !$0 {
                model.clearError()
            } }),
            actions: { Button("OK", role: .cancel) {
            } },
            message: { Text(model.state.error ?? "") }
        )
        // Counting can take a while; keep the screen on so ushers don't lose their place.
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
    }

    private var startPrompt: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.3.sequence")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("No active count for this venue.")
                .foregroundStyle(.secondary)
            if model.eventTypes.isEmpty {
                Button("Start Head Count") {
                    model.startCount(eventType: nil)
                }
                .buttonStyle(.borderedProminent)
            } else {
                Menu {
                    Button("Head Count") {
                        model.startCount(eventType: nil)
                    }
                    ForEach(model.eventTypes, id: \.id) { type in
                        Button(type.name) {
                            model.startCount(eventType: type)
                        }
                    }
                } label: {
                    Label("Start Head Count", systemImage: "play.fill")
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
    }

    private var counter: some View {
        VStack(spacing: 0) {
            TotalBanner(
                total: Int(model.state.totalAttendance),
                capacity: Int(model.state.totalCapacity),
                locked: model.state.isLocked
            )
            if model.state.areaCounts.isEmpty {
                ContentUnavailableView(
                    "No Areas",
                    systemImage: "square.grid.2x2",
                    description: Text("Add areas to this venue from Manage Areas, then start a new count.")
                )
            } else {
                List {
                    ForEach(model.state.areaCounts, id: \.id) { area in
                        AreaCounterRow(
                            area: area,
                            locked: model.state.isLocked,
                            onIncrement: { model.increment(area.id) },
                            onDecrement: { model.decrement(area.id) },
                            onToggle: { model.toggleInclusion(area.id) },
                            onOpenSeats: { seatSheet = SeatSheetTarget(id: area.template_.id, name: area.template_.name) }
                        )
                    }
                }
                .listStyle(.insetGrouped)
            }
        }
    }
}

private struct TotalBanner: View {
    let total: Int
    let capacity: Int
    let locked: Bool

    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("Total Attendance").font(.caption).foregroundStyle(.secondary)
                Text("\(total)")
                    .font(.largeTitle.bold().monospacedDigit())
                    .accessibilityIdentifier("totalAttendance")
                    .contentTransition(.numericText())
                    .animation(.snappy, value: total)
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("Capacity").font(.caption).foregroundStyle(.secondary)
                Text("\(capacity)").font(.title2.monospacedDigit())
                if locked {
                    Label("Locked", systemImage: "lock.fill")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding()
        .background(.thinMaterial)
    }
}

private struct AreaCounterRow: View {
    let area: AreaCountState
    let locked: Bool
    let onIncrement: () -> Void
    let onDecrement: () -> Void
    let onToggle: () -> Void
    let onOpenSeats: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(area.template_.name).font(.headline)
                Spacer()
                Button(area.isIncluded ? "Included" : "Excluded", action: onToggle)
                    .buttonStyle(.bordered)
                    .tint(area.isIncluded ? .accentColor : .secondary)
                    .controlSize(.small)
                    .accessibilityHint("Toggles whether this area counts toward the total")
            }
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(area.count) / \(area.capacity)")
                        .font(.title2.monospacedDigit())
                        .contentTransition(.numericText())
                        .animation(.snappy, value: area.count)
                    Text("\(area.percentage)% full").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                if area.template_.hasSeatMap {
                    Button("View Seats", action: onOpenSeats)
                        .buttonStyle(.bordered)
                } else {
                    CounterButton(systemImage: "minus", label: "Decrease \(area.template_.name)", disabled: locked || area.count <= 0, action: onDecrement)
                    CounterButton(systemImage: "plus", label: "Increase \(area.template_.name)", disabled: locked, action: onIncrement)
                }
            }
        }
        .padding(.vertical, 6)
        .opacity(area.isIncluded ? 1 : 0.5)
    }
}

private struct CounterButton: View {
    let systemImage: String
    let label: String
    let disabled: Bool
    let action: () -> Void

    var body: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            action()
        } label: {
            Image(systemName: systemImage)
                .font(.title2.bold())
                .frame(width: 56, height: 56)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.circle)
        .disabled(disabled)
        .accessibilityLabel(label)
    }
}

// MARK: - Seat map

private let seatStatuses = ["AVAILABLE", "OCCUPIED", "RESERVED", "BLOCKED"]

private func seatColor(_ status: String) -> Color {
    switch status {
    case "OCCUPIED": return .accentColor
    case "RESERVED": return .orange
    case "BLOCKED": return .red
    default: return Color(.systemGray5)
    }
}

private struct SeatMapSheet: View {
    @ObservedObject var model: CountingModel
    let areaTemplateId: String
    let areaName: String
    @Environment(\.dismiss) private var dismiss
    @State private var rows: [SeatRowWithSeats] = []
    @State private var statusBySeat: [String: String] = [:]
    @State private var handles: [Cancellable] = []

    private var locked: Bool {
        model.state.isLocked
    }
    private var occupied: Int {
        statusBySeat.values.filter {
            $0 == "OCCUPIED"
        }
        .count
    }

    var body: some View {
        NavigationStack {
            Group {
                if rows.isEmpty {
                    ContentUnavailableView(
                        "No Seats",
                        systemImage: "chair",
                        description: Text("No seats are defined for this area yet.")
                    )
                } else {
                    VStack(spacing: 12) {
                        Text("\(occupied) occupied")
                            .font(.headline.monospacedDigit())
                        // Cinema layout: the screen and every row share one center line; wide maps scroll sideways
                        GeometryReader { geo in
                            ScrollView([.horizontal, .vertical]) {
                                VStack(spacing: 6) {
                                    cinemaScreen(width: widestRowWidth)
                                    ForEach(rows, id: \.row.id) { row in
                                        // Labels on both sides keep the seats centered under the screen
                                        HStack(spacing: 4) {
                                            rowLabel(row.row.label)
                                            ForEach(row.seats.sorted {
                                                $0.number < $1.number
                                            }, id: \.id) { seat in
                                                seatButton(seat)
                                            }
                                            rowLabel(row.row.label)
                                        }
                                    }
                                }
                                .padding()
                                .frame(minWidth: geo.size.width)
                            }
                        }
                        legend
                    }
                    .padding(.bottom)
                }
            }
            .navigationTitle(areaName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            handles = model.observeSeats(
                areaTemplateId: areaTemplateId,
                onRows: { rows = $0 },
                onStatuses: { statusBySeat = $0 }
            )
        }
        .onDisappear {
            handles.forEach {
                $0.cancel()
            }
            handles = []
        }
    }

    private func seatButton(_ seat: SeatEntity) -> some View {
        let status = statusBySeat[seat.id] ?? "AVAILABLE"
        return Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            model.cycleSeat(seat.id, current: status)
        } label: {
            Text("\(seat.number)")
                .font(.caption2.monospacedDigit())
                .foregroundStyle(status == "AVAILABLE" ? Color.primary : Color.white)
                .frame(width: 34, height: 34)
                .background(seatColor(status), in: RoundedRectangle(cornerRadius: 6))
        }
        .buttonStyle(.plain)
        .disabled(locked)
        .accessibilityLabel("Seat \(seat.label)")
        .accessibilityValue(status.capitalized)
    }

    private func rowLabel(_ label: String) -> some View {
        Text(label)
            .font(.caption.bold())
            .frame(width: 24)
    }

    /// Width of the longest row: two labels plus its seats, with 4pt spacing between each.
    private var widestRowWidth: CGFloat {
        let seats = CGFloat(rows.map {
            $0.seats.count
        }
                            .max() ?? 0)
        return 2 * 24 + seats * 34 + (seats + 1) * 4
    }

    private func cinemaScreen(width: CGFloat) -> some View {
        VStack(spacing: 4) {
            UnevenRoundedRectangle(bottomLeadingRadius: 50, bottomTrailingRadius: 50)
                .fill(Color.accentColor.opacity(0.6))
                .frame(width: max(width - 56, 40), height: 6)
            Text("SCREEN")
                .font(.caption2)
                .tracking(4)
                .foregroundStyle(.secondary)
        }
        .padding(.bottom, 12)
        .accessibilityHidden(true)
    }

    private var legend: some View {
        HStack(spacing: 12) {
            ForEach(seatStatuses, id: \.self) { status in
                HStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(seatColor(status))
                        .frame(width: 12, height: 12)
                    Text(status.capitalized).font(.caption2)
                }
            }
        }
        .accessibilityHidden(true)
    }
}
