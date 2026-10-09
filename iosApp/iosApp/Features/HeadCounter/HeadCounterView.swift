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

    func startCount(eventType: EventTypeDto?, countedBy: String) {
        viewModel.createNewEvent(
            eventTypeId: eventType?.id,
            eventTypeName: eventType?.name ?? "Head Count",
            date: nowEpochMillis(),
            countedBy: countedBy
        )
    }

    func bump(_ id: String, by delta: Int) {
        if delta >= 0 {
            viewModel.incrementCount(areaCountId: id, amount: Int32(delta))
        } else {
            viewModel.decrementCount(areaCountId: id, amount: Int32(-delta))
        }
    }

    func toggleInclusion(_ id: String) { viewModel.toggleAreaInclusion(areaCountId: id) }
    func undo() { viewModel.undo() }
    func redo() { viewModel.redo() }
    func generateReport() { viewModel.generateReport() }
    func clearReport() { viewModel.clearReport() }

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

// MARK: - Palette (mirrors the Android counter kit)

enum CounterPalette {
    /// Live / destructive / subtract accent.
    static let signal = Color(red: 0xFD / 255, green: 0x34 / 255, blue: 0x36 / 255)
    static let muted = Color(red: 0x77 / 255, green: 0x70 / 255, blue: 0x69 / 255)
    static let slab = LinearGradient(
        colors: [Color(red: 0x1A / 255, green: 0x1A / 255, blue: 0x1A / 255), Color(red: 0x07 / 255, green: 0x1A / 255, blue: 0x55 / 255)],
        startPoint: .top,
        endPoint: .bottom
    )
}

private struct SnackMessage: Equatable {
    let id = UUID()
    let text: String
    /// Area whose inclusion the snackbar's Undo flips back; nil hides the Undo action.
    let undoInclusionOf: String?
}

private struct FloatToken: Identifiable {
    let id: Int
    let value: Int
}

struct HeadCounterView: View {
    let venueName: String
    @StateObject private var model: CountingModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedAreaId: String?
    @State private var snack: SnackMessage?
    @State private var tokens: [FloatToken] = []
    @State private var tokenSeq = 0
    @State private var cancelledNewCount = false

    /// Pass `eventId` to resume an existing count; otherwise the user fills in a new-count sheet first.
    init(venueId: String, venueName: String, eventId: String? = nil) {
        self.venueName = venueName
        _model = StateObject(wrappedValue: CountingModel(venueId: venueId, eventId: eventId))
    }

    private var areas: [AreaCountState] { model.state.areaCounts }

    /// Last-tapped zone, else the first included one, else the first.
    private var activeArea: AreaCountState? {
        areas.first { $0.id == selectedAreaId } ?? areas.first { $0.isIncluded } ?? areas.first
    }

    private var title: String {
        model.state.venueName.isEmpty ? venueName : model.state.venueName
    }

    var body: some View {
        Group {
            if model.state.isLoading {
                ProgressView("Loading…")
            } else if model.state.eventId == nil {
                Color(.systemBackground)
            } else if let area = activeArea {
                countBody(area)
            } else {
                ContentUnavailableView(
                    "No zones configured",
                    systemImage: "square.grid.2x2",
                    description: Text("Add a zone to this venue from Manage Areas to begin counting.")
                )
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .overlay(alignment: .bottom) { snackbar }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 0) {
                    Text(title).font(.headline)
                    Text(model.state.eventName.isEmpty ? "Live count" : model.state.eventName)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            if model.state.eventId != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        model.toggleLock()
                    } label: {
                        Image(systemName: model.state.isLocked ? "lock.fill" : "lock.open")
                    }
                    .accessibilityLabel(model.state.isLocked ? "Unlock count" : "Lock count")
                }
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { !model.state.isLoading && model.state.eventId == nil && !cancelledNewCount },
            set: { _ in }
        )) {
            NewCountSheet(
                eventTypes: model.eventTypes,
                onCancel: {
                    cancelledNewCount = true
                    dismiss()
                },
                onStart: { type, countedBy in model.startCount(eventType: type, countedBy: countedBy) }
            )
        }
        .sheet(isPresented: Binding(get: { model.state.report != nil }, set: { if !$0 { model.clearReport() } })) {
            if let report = model.state.report {
                HeadCountReceiptView(report: report)
            }
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
        .task(id: snack?.id) {
            guard snack != nil else { return }
            try? await Task.sleep(for: .seconds(2.4))
            withAnimation { snack = nil }
        }
        // Counting can take a while; keep the screen on so ushers don't lose their place.
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
    }

    // MARK: Body

    private func countBody(_ area: AreaCountState) -> some View {
        let locked = model.state.isLocked
        let total = Int(model.state.totalAttendance)
        let capacity = Int(model.state.totalCapacity)
        let pct = capacity > 0 ? min(Double(total) / Double(capacity), 1) : 0

        return VStack(spacing: 0) {
            VStack(spacing: 14) {
                StatStrip(
                    date: Date(epochMillis: model.state.eventDate),
                    total: total,
                    capacity: capacity,
                    pct: pct
                )
                CapacityBar(pct: pct, knownCapacity: capacity > 0)
                ZoneRail(areas: areas, activeId: area.id) { select($0) }
                    .padding(.top, 10)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)

            Group {
                if area.template_.hasSeatMap {
                    SeatTallyPanel(model: model, area: area, onToggleInclusion: { toggleInclusion(area) })
                } else {
                    TallyPanel(area: area, areas: areas, onSelect: { select($0) }, onToggleInclusion: { toggleInclusion(area) })
                }
            }
            .frame(maxHeight: .infinity)
            .padding(.horizontal, 20)
            .padding(.vertical, 16)

            VStack(spacing: 12) {
                if locked {
                    Banner(dot: CounterPalette.signal, text: "Count locked. Unlock from the lock button to resume.", tint: CounterPalette.signal.opacity(0.12))
                }
                if area.template_.hasSeatMap {
                    Banner(dot: nil, text: "Tap a seat to cycle: Available → Occupied → Reserved → Blocked.", tint: Color.primary.opacity(0.06))
                } else {
                    HStack(spacing: 12) {
                        KeypadKey(
                            label: "−1",
                            caption: "Subtract",
                            isAdd: false,
                            accessibilityName: "Decrease \(area.template_.name)",
                            action: { bump(area, by: -1) }
                        )
                        .disabled(locked || area.count <= 0)
                        KeypadKey(
                            label: "+1",
                            caption: "Tap or hold",
                            isAdd: true,
                            accessibilityName: "Increase \(area.template_.name)",
                            action: { bump(area, by: 1) }
                        )
                        .disabled(locked)
                    }
                    .overlay(alignment: .top) {
                        FloatingTokens(tokens: tokens) { done in tokens.removeAll { $0.id == done } }
                    }
                }
                HStack(spacing: 8) {
                    ToolButton(label: "Undo", glyph: "arrow.uturn.backward") {
                        model.undo()
                        showSnack("Undone.")
                    }
                    .disabled(!model.canUndo || locked)
                    ToolButton(label: "Redo", glyph: "arrow.uturn.forward") { model.redo() }
                        .disabled(!model.canRedo || locked)
                    ToolButton(label: "Report", glyph: "doc.plaintext") { model.generateReport() }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0),
                        .init(color: Color(.systemBackground).opacity(0.92), location: 0.28),
                        .init(color: Color(.systemBackground), location: 1)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
        }
    }

    @ViewBuilder
    private var snackbar: some View {
        if let snack {
            HStack {
                Text(snack.text)
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let areaId = snack.undoInclusionOf {
                    Button("Undo") {
                        model.toggleInclusion(areaId)
                        showSnack("Undone.")
                    }
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color(.systemBackground).opacity(0.14), in: RoundedRectangle(cornerRadius: 10))
                }
            }
            .foregroundStyle(Color(.systemBackground))
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.primary, in: RoundedRectangle(cornerRadius: 16))
            .padding(16)
            .transition(.move(edge: .bottom).combined(with: .opacity))
            .id(snack.id)
        }
    }

    // MARK: Actions

    private func select(_ area: AreaCountState) {
        selectedAreaId = area.id
    }

    private func bump(_ area: AreaCountState, by delta: Int) {
        model.bump(area.id, by: delta)
        tokenSeq += 1
        tokens.append(FloatToken(id: tokenSeq, value: delta))
    }

    private func toggleInclusion(_ area: AreaCountState) {
        UISelectionFeedbackGenerator().selectionChanged()
        selectedAreaId = area.id
        model.toggleInclusion(area.id)
        let name = area.template_.name
        withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
            snack = SnackMessage(
                text: area.isIncluded ? "\(name) excluded from total." : "\(name) included in total.",
                undoInclusionOf: area.id
            )
        }
    }

    private func showSnack(_ text: String) {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
            snack = SnackMessage(text: text, undoInclusionOf: nil)
        }
    }
}

// MARK: - Stat strip & capacity bar

private struct StatStrip: View {
    let date: Date
    let total: Int
    let capacity: Int
    let pct: Double
    @State private var live = false

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(CounterPalette.signal.opacity(live ? 1 : 0.35))
                .frame(width: 10, height: 10)
                .onAppear {
                    withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { live = true }
                }
                .accessibilityHidden(true)
            Text(date.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).hour(.twoDigits(amPM: .omitted)).minute()).uppercased())
                .font(.system(size: 13, weight: .medium, design: .monospaced))
                .tracking(1.4)
                .foregroundStyle(.secondary)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(String(format: "%03d", total))
                .font(.system(size: 48, weight: .bold, design: .monospaced))
                .contentTransition(.numericText(value: Double(total)))
                .animation(.snappy, value: total)
                .accessibilityLabel("\(total)")
                .accessibilityIdentifier("totalAttendance")
            VStack(alignment: .trailing, spacing: 2) {
                Text("TOTAL")
                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                    .tracking(2)
                Text(capacity > 0 ? "/\(capacity) · \(Int((pct * 100).rounded()))%" : "/ —")
                    .font(.system(size: 14, weight: .medium, design: .monospaced))
            }
            .foregroundStyle(.secondary)
        }
    }
}

private struct CapacityBar: View {
    let pct: Double
    let knownCapacity: Bool
    var segments = 18
    @State private var pulse = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<segments, id: \.self) { i in
                let high = Double(i + 1) / Double(segments)
                let active = knownCapacity && pct >= high - 1 / Double(segments)
                let base: Color = high >= 0.93 ? CounterPalette.signal : high >= 0.78 ? CounterPalette.muted : .primary
                RoundedRectangle(cornerRadius: 3)
                    .fill(active ? base.opacity(pct >= 0.95 && high > 0.78 && pulse ? 0.55 : 1) : Color.primary.opacity(0.10))
                    .frame(height: 14)
            }
        }
        .animation(.easeOut(duration: 0.22), value: pct)
        .onAppear {
            withAnimation(.easeInOut(duration: 0.72).repeatForever(autoreverses: true)) { pulse = true }
        }
        .accessibilityElement()
        .accessibilityLabel("Capacity")
        .accessibilityValue(knownCapacity ? "\(Int((pct * 100).rounded())) percent" : "Unknown")
    }
}

// MARK: - Zone rail

private struct ZoneRail: View {
    let areas: [AreaCountState]
    let activeId: String
    let onSelect: (AreaCountState) -> Void

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(areas, id: \.id) { area in
                        ZoneChip(area: area, selected: area.id == activeId) { onSelect(area) }
                            .id(area.id)
                    }
                }
            }
            .onChange(of: activeId, initial: true) { _, id in
                withAnimation(.snappy) { proxy.scrollTo(id, anchor: .center) }
            }
        }
    }
}

private struct ZoneChip: View {
    let area: AreaCountState
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Text(area.template_.name)
                    .font(.subheadline.weight(.semibold))
                    .opacity(selected ? 1 : 0.78)
                Text("\(area.count)")
                    .font(.system(.title3, design: .monospaced).weight(.bold))
                    .opacity(area.count == 0 ? 0.32 : 1)
                    .contentTransition(.numericText(value: Double(area.count)))
            }
            .foregroundStyle(selected ? Color(.systemBackground) : Color.primary)
            .padding(.horizontal, 16)
            .frame(minHeight: 68)
            .background(selected ? Color.primary : Color.primary.opacity(0.06), in: RoundedRectangle(cornerRadius: 8))
            .animation(.easeInOut(duration: 0.22), value: selected)
        }
        .buttonStyle(PressScaleStyle())
        .opacity(area.isIncluded ? 1 : 0.42)
        .accessibilityLabel("\(area.template_.name), \(area.count)")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

private struct PressScaleStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// MARK: - Tally panel (swipe between zones)

private func caption(for area: AreaCountState) -> String {
    guard area.capacity > 0 else { return "\(area.count) counted" }
    let pct = min(Double(area.count) / Double(area.capacity), 1) * 100
    return "\(area.count) of \(area.capacity) · \(Int(pct.rounded()))%"
}

private struct TallyPanel: View {
    let area: AreaCountState
    let areas: [AreaCountState]
    let onSelect: (AreaCountState) -> Void
    let onToggleInclusion: () -> Void

    @State private var dragOffset: CGFloat = 0
    @State private var pulse: CGFloat = 1

    private let commitThreshold: CGFloat = 96
    private let travelLimit: CGFloat = 260
    private let flingOffset: CGFloat = 360

    private var index: Int? { areas.firstIndex { $0.id == area.id } }
    private var prevArea: AreaCountState? { index.flatMap { $0 > 0 ? areas[$0 - 1] : nil } }
    private var nextArea: AreaCountState? { index.flatMap { $0 < areas.count - 1 ? areas[$0 + 1] : nil } }

    private var pct: Double {
        area.capacity > 0 ? min(Double(area.count) / Double(area.capacity), 1) : 0
    }

    private var progressColor: Color {
        pct >= 0.95 ? CounterPalette.signal : pct >= 0.80 ? CounterPalette.muted : .white
    }

    var body: some View {
        let p = dragOffset
        let activeAlpha = 1 - min(abs(p) / commitThreshold, 1) * 0.55
        let prevAlpha = min(max(p / commitThreshold, 0), 1) * 0.45
        let nextAlpha = min(max(-p / commitThreshold, 0), 1) * 0.45

        VStack(spacing: 14) {
            HStack {
                Text(area.template_.name)
                    .font(.system(size: 22, weight: .semibold))
                    .lineLimit(1)
                Spacer()
                InclusionPill(included: area.isIncluded, action: onToggleInclusion)
            }

            ZStack {
                if let prevArea {
                    digit(prevArea.count).opacity(0.7 * prevAlpha).offset(x: -flingOffset + p)
                        .accessibilityHidden(true)
                }
                if let nextArea {
                    digit(nextArea.count).opacity(0.7 * nextAlpha).offset(x: flingOffset + p)
                        .accessibilityHidden(true)
                }
                digit(area.count)
                    .contentTransition(.numericText(value: Double(area.count)))
                    .animation(.snappy, value: area.count)
                    .scaleEffect(pulse)
                    .opacity(activeAlpha)
                    .offset(x: p)
                    .accessibilityLabel("\(area.template_.name) count")
                    .accessibilityValue("\(area.count)")
            }
            .frame(maxWidth: .infinity)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(.white.opacity(0.18))
                    Capsule().fill(progressColor).frame(width: geo.size.width * pct)
                }
            }
            .frame(height: 8)
            .animation(.easeOut(duration: 0.52), value: pct)

            ZStack {
                if let prevArea {
                    captionText(prevArea).opacity(prevAlpha).offset(x: (-flingOffset + p) * 0.55)
                }
                if let nextArea {
                    captionText(nextArea).opacity(nextAlpha).offset(x: (flingOffset + p) * 0.55)
                }
                captionText(area).opacity(activeAlpha).offset(x: p * 0.55)
            }
        }
        .foregroundStyle(.white)
        .padding(.horizontal, 24)
        .padding(.vertical, 20)
        .background(CounterPalette.slab, in: RoundedRectangle(cornerRadius: 28))
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .contentShape(Rectangle())
        .gesture(swipe)
        .onChange(of: area.count) { old, new in
            guard old != new else { return }
            pulse = 1.06
            withAnimation(.spring(response: 0.35, dampingFraction: 0.55)) { pulse = 1 }
        }
        .accessibilityAction(named: "Next zone") { if let nextArea { onSelect(nextArea) } }
        .accessibilityAction(named: "Previous zone") { if let prevArea { onSelect(prevArea) } }
    }

    private func digit(_ value: Int32) -> some View {
        Text("\(value)")
            .font(.system(size: 112, weight: .light))
            .monospacedDigit()
            .tracking(-6)
            .lineLimit(1)
            .minimumScaleFactor(0.5)
    }

    private func captionText(_ area: AreaCountState) -> some View {
        Text(caption(for: area))
            .font(.system(size: 15, weight: .medium, design: .monospaced))
            .foregroundStyle(.white.opacity(0.85))
            .lineLimit(1)
    }

    private var swipe: some Gesture {
        DragGesture(minimumDistance: 12)
            .onChanged { value in
                let target = value.translation.width
                let resisted: CGFloat
                if (target < 0 && nextArea == nil) || (target > 0 && prevArea == nil) {
                    resisted = target * 0.28
                } else {
                    resisted = target
                }
                dragOffset = min(max(resisted, -travelLimit), travelLimit)
            }
            .onEnded { _ in
                let off = dragOffset
                if off <= -commitThreshold, let nextArea {
                    commit(to: nextArea, continuingFrom: flingOffset + off)
                } else if off >= commitThreshold, let prevArea {
                    commit(to: prevArea, continuingFrom: -flingOffset + off)
                } else {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.62)) { dragOffset = 0 }
                }
            }
    }

    /// Swap zones without animation so the new zone starts where its ghost was, then settle it.
    private func commit(to target: AreaCountState, continuingFrom position: CGFloat) {
        UISelectionFeedbackGenerator().selectionChanged()
        var swap = Transaction()
        swap.disablesAnimations = true
        withTransaction(swap) {
            onSelect(target)
            dragOffset = position
        }
        DispatchQueue.main.async {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.82)) { dragOffset = 0 }
        }
    }
}

private struct InclusionPill: View {
    let included: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Circle()
                    .fill(included ? Color.white : Color.white.opacity(0.45))
                    .frame(width: 8, height: 8)
                Text(included ? "IN TOTAL" : "EXCLUDED")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .tracking(1.4)
                    .foregroundStyle(.white.opacity(included ? 0.95 : 0.65))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.white.opacity(included ? 0.16 : 0.06), in: Capsule())
            .overlay(Capsule().strokeBorder(Color.white.opacity(included ? 0.45 : 0.22), lineWidth: 1))
            .animation(.easeInOut(duration: 0.22), value: included)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(included ? "In total" : "Excluded")
        .accessibilityHint("Toggles whether this zone counts toward the total")
    }
}

// MARK: - Keypad

/// Hold-to-repeat bookkeeping. A reference type on purpose: writing view state mid-press
/// re-renders the button and can leave it stuck pressed without ever firing its action.
@MainActor
private final class KeyRepeater {
    var task: Task<Void, Never>?
    var didRepeat = false
}

/// Arcade key: tap to fire once, hold to repeat with accelerating cadence.
private struct KeypadKey: View {
    let label: String
    let caption: String
    let isAdd: Bool
    let accessibilityName: String
    let action: () -> Void

    @Environment(\.isEnabled) private var isEnabled
    @State private var repeater = KeyRepeater()

    var body: some View {
        Button {
            // A hold already fired its own repeats; the release shouldn't add one more.
            if repeater.didRepeat {
                repeater.didRepeat = false
            } else {
                fire()
            }
        } label: {
            VStack(spacing: 2) {
                Text(label)
                    .font(.system(size: 40, weight: .bold, design: .monospaced))
                Text(caption.uppercased())
                    .font(.system(size: 10, weight: .medium, design: .monospaced))
                    .tracking(1.6)
                    .opacity(0.7)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(KeyStyle(isAdd: isAdd, onPressChange: pressChanged))
        .accessibilityLabel(accessibilityName)
    }

    private func fire() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        action()
    }

    private func pressChanged(_ pressed: Bool) {
        repeater.task?.cancel()
        guard pressed, isEnabled else { return }
        let repeater = repeater
        repeater.task = Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(380))
            var interval = 180.0
            while !Task.isCancelled {
                repeater.didRepeat = true
                fire()
                try? await Task.sleep(for: .milliseconds(Int(interval)))
                interval = max(50, interval * 0.85)
            }
        }
    }
}

private struct KeyStyle: ButtonStyle {
    let isAdd: Bool
    let onPressChange: (Bool) -> Void
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed && isEnabled
        let accent = isAdd ? Color.primary : CounterPalette.signal
        let face: Color = !isEnabled ? Color.primary.opacity(0.05)
            : isAdd ? (pressed ? Color.primary.opacity(0.8) : .primary)
            : (pressed ? CounterPalette.signal.opacity(0.18) : Color.primary.opacity(0.06))
        let fg: Color = !isEnabled ? Color.secondary.opacity(0.45) : isAdd ? Color(.systemBackground) : .primary

        return ZStack(alignment: .top) {
            RoundedRectangle(cornerRadius: 22)
                .fill(accent.opacity(0.55 * (isEnabled ? 1 : 0.18)))
                .padding(.top, 8)
            configuration.label
                .foregroundStyle(fg)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(face, in: RoundedRectangle(cornerRadius: 22))
                .padding(.bottom, 8)
                .offset(y: pressed ? 6 : 0)
                .animation(.spring(response: 0.25, dampingFraction: 0.5), value: pressed)
        }
        .frame(height: 112)
        .onChange(of: configuration.isPressed) { _, isPressed in onPressChange(isPressed) }
    }
}

private struct FloatingTokens: View {
    let tokens: [FloatToken]
    let onComplete: (Int) -> Void

    var body: some View {
        ZStack {
            ForEach(tokens) { token in
                FloatingTokenView(token: token) { onComplete(token.id) }
            }
        }
        .frame(height: 0)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

private struct FloatingTokenView: View {
    let token: FloatToken
    let onComplete: () -> Void
    @State private var rise: CGFloat = 0
    @State private var fade: Double = 1

    var body: some View {
        let fromAdd = token.value >= 0
        Text(fromAdd ? "+\(token.value)" : "\(token.value)")
            .font(.system(size: 30, weight: .bold, design: .monospaced))
            .foregroundStyle(fromAdd ? Color.primary.opacity(0.75) : CounterPalette.signal)
            .opacity(fade)
            .scaleEffect(0.85 + 0.25 * fade)
            // Tokens drift slightly inward as they rise, toward the readout.
            .offset(x: fromAdd ? -rise * 0.08 : rise * 0.08, y: rise)
            .frame(maxWidth: .infinity, alignment: fromAdd ? .trailing : .leading)
            .padding(.horizontal, 36)
            .task {
                withAnimation(.easeOut(duration: 0.72)) { rise = -180 }
                withAnimation(.easeIn(duration: 0.48).delay(0.22)) { fade = 0 }
                try? await Task.sleep(for: .milliseconds(720))
                onComplete()
            }
    }
}

// MARK: - Dock pieces

private struct ToolButton: View {
    let label: String
    let glyph: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(label, systemImage: glyph)
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .buttonStyle(.bordered)
        .tint(.primary)
    }
}

private struct Banner: View {
    let dot: Color?
    let text: String
    let tint: Color

    var body: some View {
        HStack(spacing: 10) {
            if let dot {
                Circle().fill(dot).frame(width: 8, height: 8)
            }
            Text(text).font(.footnote)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(tint, in: RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - Seat-mapped tally panel

private let seatStatuses = ["AVAILABLE", "OCCUPIED", "RESERVED", "BLOCKED"]

/// Seat colors tuned for the dark slab: occupied seats read as solid white.
private func seatFill(_ status: String) -> Color {
    switch status {
    case "OCCUPIED": return .white
    case "RESERVED": return CounterPalette.muted
    case "BLOCKED": return CounterPalette.signal
    default: return .white.opacity(0.14)
    }
}

private struct SeatTallyPanel: View {
    @ObservedObject var model: CountingModel
    let area: AreaCountState
    let onToggleInclusion: () -> Void
    @State private var rows: [SeatRowWithSeats] = []
    @State private var statusBySeat: [String: String] = [:]
    @State private var handles: [Cancellable] = []

    private var allSeats: [SeatEntity] { rows.flatMap { $0.seats } }
    private var occupied: Int { allSeats.filter { statusBySeat[$0.id] == "OCCUPIED" }.count }
    private var usable: Int { allSeats.filter { statusBySeat[$0.id] != "BLOCKED" }.count }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(area.template_.name).font(.system(size: 22, weight: .semibold))
                    Text("\(occupied) of \(usable) seated")
                        .font(.system(.subheadline, design: .monospaced))
                        .foregroundStyle(.white.opacity(0.85))
                        .contentTransition(.numericText(value: Double(occupied)))
                }
                Spacer()
                InclusionPill(included: area.isIncluded, action: onToggleInclusion)
            }
            if rows.isEmpty {
                Text("No rows configured. Add rows in this zone's seat map editor.")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.75))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                GeometryReader { geo in
                    ScrollView([.horizontal, .vertical]) {
                        VStack(spacing: 6) {
                            screenMarker
                            ForEach(rows, id: \.row.id) { row in
                                // Labels on both sides keep the seats centered under the screen
                                HStack(spacing: 4) {
                                    rowLabel(row.row.label)
                                    ForEach(row.seats.sorted { $0.number < $1.number }, id: \.id) { seat in
                                        seatButton(seat)
                                    }
                                    rowLabel(row.row.label)
                                }
                            }
                        }
                        .padding(.vertical, 8)
                        .frame(minWidth: geo.size.width)
                    }
                }
                legend
            }
        }
        .foregroundStyle(.white)
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(CounterPalette.slab, in: RoundedRectangle(cornerRadius: 28))
        .task(id: area.template_.id) {
            handles.forEach { $0.cancel() }
            rows = []
            statusBySeat = [:]
            handles = model.observeSeats(
                areaTemplateId: area.template_.id,
                onRows: { rows = $0 },
                onStatuses: { statusBySeat = $0 }
            )
        }
        .onDisappear {
            handles.forEach { $0.cancel() }
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
                .foregroundStyle(status == "OCCUPIED" ? Color.black : Color.white)
                .frame(width: 34, height: 34)
                .background(seatFill(status), in: RoundedRectangle(cornerRadius: 6))
        }
        .buttonStyle(.plain)
        .disabled(model.state.isLocked)
        .accessibilityLabel("Seat \(seat.label)")
        .accessibilityValue(status.capitalized)
    }

    private func rowLabel(_ label: String) -> some View {
        Text(label)
            .font(.caption.bold())
            .frame(width: 24)
    }

    /// Width of the longest row: two labels plus its seats, with 4pt spacing between each.
    /// The horizontal scroll view proposes no width, so the screen marker needs it explicitly.
    private var widestRowWidth: CGFloat {
        let seats = CGFloat(rows.map { $0.seats.count }.max() ?? 0)
        return 2 * 24 + seats * 34 + (seats + 1) * 4
    }

    private var screenMarker: some View {
        VStack(spacing: 4) {
            UnevenRoundedRectangle(bottomLeadingRadius: 50, bottomTrailingRadius: 50)
                .fill(Color.white.opacity(0.6))
                .frame(width: max(widestRowWidth - 56, 40), height: 6)
            Text("SCREEN")
                .font(.caption2)
                .tracking(4)
                .foregroundStyle(.white.opacity(0.7))
        }
        .padding(.bottom, 12)
        .accessibilityHidden(true)
    }

    private var legend: some View {
        HStack(spacing: 12) {
            ForEach(seatStatuses, id: \.self) { status in
                HStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(seatFill(status))
                        .frame(width: 12, height: 12)
                    Text(status.capitalized).font(.caption2)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityHidden(true)
    }
}
