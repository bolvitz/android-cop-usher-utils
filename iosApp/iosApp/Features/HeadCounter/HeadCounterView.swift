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
    func updateNotes(_ notes: String) { viewModel.updateNotes(notes: notes) }

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

private struct SnackMessage: Equatable {
    let id = UUID()
    let text: String
    /// Area whose inclusion the snackbar's Undo flips back; nil hides the Undo action.
    let undoInclusionOf: String?
}

private struct FloatToken: Identifiable {
    let id: Int
    let value: Int
    /// Horizontal position as a fraction of the pad's width.
    let x: CGFloat
}

/// A zone's number travelling between the zone and the total when it is included or left out.
private struct FlyToken: Equatable {
    let id: Int
    let text: String
    let joining: Bool
}

private let counterSpace = "counter"

struct HeadCounterView: View {
    let venueName: String
    @StateObject private var model: CountingModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var selectedAreaId: String?
    @State private var snack: SnackMessage?
    @State private var tokens: [FloatToken] = []
    @State private var seq = 0
    @State private var cancelledNewCount = false
    @State private var showSeatMap = false
    @State private var showReceipt = false
    @State private var lockedAt: Date?
    @State private var entryEdge: Edge = .trailing
    @State private var dragOffset: CGFloat = 0
    @State private var fly: FlyToken?
    @State private var flyLanded = false
    @State private var totalFrame: CGRect = .zero
    @State private var zoneFrame: CGRect = .zero
    @State private var lastPercents: [String: Int] = [:]

    /// Pass `eventId` to resume an existing count; otherwise the user fills in a new-count sheet first.
    init(venueId: String, venueName: String, eventId: String? = nil) {
        self.venueName = venueName
        _model = StateObject(wrappedValue: CountingModel(venueId: venueId, eventId: eventId))
    }

    private var areas: [AreaCountState] { model.state.areaCounts }

    /// Last-picked zone, else the first included one, else the first.
    private var activeArea: AreaCountState? {
        areas.first { $0.id == selectedAreaId } ?? areas.first { $0.isIncluded } ?? areas.first
    }

    private var title: String {
        model.state.venueName.isEmpty ? venueName : model.state.venueName
    }

    private var subtitle: String {
        let name = model.state.eventName.isEmpty ? "Live count" : model.state.eventName
        guard model.state.eventDate > 0 else { return name }
        let time = Date(epochMillis: model.state.eventDate).formatted(date: .omitted, time: .shortened)
        return "\(name), started \(time)"
    }

    var body: some View {
        Group {
            if model.state.isLoading {
                ProgressView("Loading…").tint(CounterPalette.chalk)
            } else if model.state.eventId == nil {
                Color.clear
            } else if let area = activeArea {
                counter(area)
            } else {
                ContentUnavailableView(
                    "No zones yet",
                    systemImage: "square.grid.2x2",
                    description: Text("Add a zone to this venue from Manage Areas, then come back to count.")
                )
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(CounterPalette.ground.ignoresSafeArea())
        .foregroundStyle(CounterPalette.chalk)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 1) {
                    Text(title).font(.headline)
                    Text(subtitle).font(.caption).foregroundStyle(CounterPalette.pew)
                }
            }
            if model.state.eventId != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        toggleLock()
                    } label: {
                        Image(systemName: model.state.isLocked ? "lock.fill" : "lock.open")
                            .foregroundStyle(model.state.isLocked ? CounterPalette.brass : CounterPalette.chalk)
                            .contentTransition(.symbolEffect(.replace))
                    }
                    .accessibilityLabel(model.state.isLocked ? "Unlock count" : "Lock count")
                }
            }
        }
        .preferredColorScheme(.dark)
        // The pad and dock own the bottom of the screen while counting.
        .toolbar(.hidden, for: .tabBar)
        .fullScreenCover(isPresented: Binding(
            get: { !model.state.isLoading && model.state.eventId == nil && !cancelledNewCount },
            set: { _ in }
        )) {
            NewCountSheet(
                venueName: title,
                eventTypes: model.eventTypes,
                onCancel: {
                    cancelledNewCount = true
                    dismiss()
                },
                onStart: { type, countedBy in model.startCount(eventType: type, countedBy: countedBy) }
            )
        }
        .navigationDestination(isPresented: $showSeatMap) {
            if let area = activeArea {
                SeatMapView(model: model, areaTemplateId: area.template_.id, zoneName: area.template_.name)
            }
        }
        .fullScreenCover(isPresented: $showReceipt) {
            if let report = model.state.report {
                HeadCountReceiptView(
                    report: report,
                    notes: model.state.notes,
                    onNotesChange: { model.updateNotes($0) },
                    onClose: closeReceipt
                )
                .presentationBackground(.clear)
            }
        }
        .onChange(of: model.state.report != nil) { _, hasReport in
            // The receipt brings its own printing animation; skip the cover's slide.
            var instant = Transaction()
            instant.disablesAnimations = true
            withTransaction(instant) { showReceipt = hasReport }
        }
        .onChange(of: areas.map { "\($0.id):\($0.count):\($0.isIncluded)" }, initial: true) { _, _ in
            warnOnCapacityCrossing()
        }
        .alert(
            "Something went wrong",
            isPresented: Binding(get: { model.state.error != nil }, set: { if !$0 { model.clearError() } }),
            actions: { Button("OK", role: .cancel) {} },
            message: { Text(model.state.error ?? "") }
        )
        .task(id: snack?.id) {
            guard snack != nil else { return }
            // A newer snack cancels this task; leave it on screen instead of clearing it early.
            guard (try? await Task.sleep(for: .seconds(3.2))) != nil else { return }
            withAnimation { snack = nil }
        }
        // Counting can take a while; keep the screen on so ushers don't lose their place.
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }

    // MARK: Counter

    private func counter(_ area: AreaCountState) -> some View {
        let locked = model.state.isLocked
        let zonePct = percent(Int(area.count), of: Int(area.capacity))

        return ZStack(alignment: .top) {
            VStack(spacing: 0) {
                totalHeader
                    .padding(.horizontal, 20)
                    .padding(.top, 4)
                ZoneRail(areas: areas, activeId: area.id) { select($0) }
                    .padding(.top, 10)
                zoneStage(area)
                    .frame(maxHeight: .infinity)
                pad(area, locked: locked)
                    .padding(.horizontal, 16)
                if !area.template_.hasSeatMap {
                    Text("Swipe up or down on the pad to count without looking.")
                        .font(.footnote)
                        .foregroundStyle(CounterPalette.pew)
                        .multilineTextAlignment(.center)
                        .padding(.top, 10)
                        .padding(.horizontal, 24)
                }
                dock(locked: locked)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 8)
            }

            flyOverlay
            snackbar
        }
        // A background, not a ZStack child: its safe-area bleed must not resize the layout.
        .background { RoomLevel(percent: zonePct, included: area.isIncluded) }
        .coordinateSpace(.named(counterSpace))
    }

    private var totalHeader: some View {
        let total = Int(model.state.totalAttendance)
        let capacity = Int(model.state.totalCapacity)
        let pct = percent(total, of: capacity)
        let status = capacity > 0 && total > capacity ? "\(total - capacity) over" : "\(pct)% full"

        return VStack(alignment: .leading, spacing: 8) {
            Text("In the room")
                .font(.footnote)
                .foregroundStyle(CounterPalette.pew)
            HStack(alignment: .bottom) {
                Odometer(value: total, size: 64)
                    .onGeometryChange(for: CGRect.self) { $0.frame(in: .named(counterSpace)) } action: { totalFrame = $0 }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("\(total)")
                    .accessibilityAddTraits(.isStaticText)
                    .accessibilityIdentifier("totalAttendance")
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(capacity > 0 ? status : "No capacity set")
                        .font(.headline)
                        .foregroundStyle(CounterPalette.level(pct))
                        .contentTransition(.numericText(value: Double(pct)))
                    if capacity > 0 {
                        Text("of \(capacity) capacity")
                            .font(.footnote)
                            .foregroundStyle(CounterPalette.pew)
                    }
                }
                .padding(.bottom, 8)
                .animation(.snappy, value: pct)
            }
            LevelBar(percent: pct, height: 8, color: CounterPalette.level(pct))
                .accessibilityElement()
                .accessibilityLabel("Capacity")
                .accessibilityValue(capacity > 0 ? "\(pct) percent" : "Unknown")
        }
    }

    private func zoneStage(_ area: AreaCountState) -> some View {
        let index = areas.firstIndex { $0.id == area.id } ?? 0
        let count = Int(area.count)
        let capacity = Int(area.capacity)
        let pct = percent(count, of: capacity)
        let status: (String, Color)? = {
            if capacity > 0 && count > capacity { return ("\(count - capacity) over capacity", CounterPalette.signal) }
            if capacity > 0 && pct >= 100 { return ("Full", CounterPalette.signal) }
            if pct >= 90 { return ("Nearly full", CounterPalette.brass) }
            return nil
        }()
        let caption = area.template_.hasSeatMap
            ? "\(count) of \(capacity) seats taken"
            : (capacity > 0 ? "\(pct)% of \(capacity)" : "\(count) counted")

        return VStack(spacing: 6) {
            HStack {
                Button { step(-1) } label: { Image(systemName: "chevron.left").frame(width: 44, height: 44).contentShape(Rectangle()) }
                    .disabled(index == 0)
                    .accessibilityLabel("Previous zone")
                Spacer()
                Text(area.template_.name)
                    .font(.title2.weight(.semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Spacer()
                Button { step(1) } label: { Image(systemName: "chevron.right").frame(width: 44, height: 44).contentShape(Rectangle()) }
                    .disabled(index >= areas.count - 1)
                    .accessibilityLabel("Next zone")
            }
            .buttonStyle(.plain)
            .foregroundStyle(CounterPalette.chalk)

            Odometer(value: count, size: 150, color: area.isIncluded ? CounterPalette.brass : CounterPalette.pew)
                .onGeometryChange(for: CGRect.self) { $0.frame(in: .named(counterSpace)) } action: { zoneFrame = $0 }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(area.template_.name) count")
                .accessibilityValue("\(count)")

            HStack(spacing: 8) {
                Text(caption).foregroundStyle(CounterPalette.pew)
                if let status {
                    Text(status.0).fontWeight(.semibold).foregroundStyle(status.1)
                }
            }
            .font(.subheadline)

            LevelBar(percent: pct, height: 6, color: area.isIncluded ? CounterPalette.level(pct) : CounterPalette.pew)
                .frame(width: 220)
                .padding(.top, 6)
                .accessibilityHidden(true)

            InclusionToggle(included: area.isIncluded) { toggleInclusion(area) }
                .disabled(model.state.isLocked)
                .padding(.top, 8)
        }
        .padding(.horizontal, 12)
        .id(area.id)
        .transition(reduceMotion ? .opacity : .push(from: entryEdge))
        .offset(x: dragOffset)
        .contentShape(Rectangle())
        .gesture(zoneSwipe(index: index))
        .accessibilityAction(named: "Next zone") { step(1) }
        .accessibilityAction(named: "Previous zone") { step(-1) }
    }

    private func zoneSwipe(index: Int) -> some Gesture {
        DragGesture(minimumDistance: 20)
            .onChanged { value in
                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                let atEdge = (value.translation.width > 0 && index == 0)
                    || (value.translation.width < 0 && index >= areas.count - 1)
                dragOffset = value.translation.width * (atEdge ? 0.2 : 0.6)
            }
            .onEnded { value in
                let width = value.translation.width
                if width < -80 && index < areas.count - 1 {
                    dragOffset = 0
                    step(1)
                } else if width > 80 && index > 0 {
                    dragOffset = 0
                    step(-1)
                } else {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) { dragOffset = 0 }
                }
            }
    }

    @ViewBuilder
    private func pad(_ area: AreaCountState, locked: Bool) -> some View {
        let name = area.template_.name
        ZStack {
            if area.template_.hasSeatMap {
                Button { showSeatMap = true } label: {
                    VStack(spacing: 10) {
                        Image(systemName: "square.grid.3x3.square")
                            .font(.system(size: 36))
                            .foregroundStyle(CounterPalette.brass)
                        Text("Open seat map").font(.title3.weight(.semibold))
                        Text("\(name) is counted seat by seat")
                            .font(.footnote)
                            .foregroundStyle(CounterPalette.pew)
                    }
                    .foregroundStyle(CounterPalette.chalk)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(RoundedRectangle(cornerRadius: 24))
                    .counterGlass(RoundedRectangle(cornerRadius: 24), interactive: true)
                }
                .buttonStyle(.plain)
            } else {
                GeometryReader { geo in
                    CounterGlassGroup(spacing: 8) {
                        HStack(spacing: 8) {
                            PadKey(sign: -1, title: "−1", caption: nil, accessibilityName: "Subtract one from \(name)") { bump(area, by: $0) }
                                .frame(width: (geo.size.width - 8) / 3)
                                .disabled(locked || area.count <= 0)
                            PadKey(sign: 1, title: "+1", caption: "Tap or hold", accessibilityName: "Add one to \(name)") { bump(area, by: $0) }
                                .disabled(locked)
                        }
                    }
                }
            }
            Shutter(locked: locked, lockedAt: lockedAt, onUnlock: toggleLock)
        }
        .frame(height: 184)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(alignment: .top) {
            FloatingTokens(tokens: tokens) { done in tokens.removeAll { $0.id == done } }
        }
    }

    private func dock(locked: Bool) -> some View {
        CounterGlassGroup(spacing: 8) {
            HStack(spacing: 8) {
                Button { model.undo() } label: {
                    Label("Undo", systemImage: "arrow.uturn.backward").frame(maxWidth: .infinity)
                }
                .disabled(!model.canUndo || locked)
                Button { model.redo() } label: {
                    Label("Redo", systemImage: "arrow.uturn.forward").frame(maxWidth: .infinity)
                }
                .disabled(!model.canRedo || locked)
                Button { model.generateReport() } label: {
                    Label("Report", systemImage: "scroll").frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(GlassPillStyle())
        }
    }

    // MARK: Overlays

    @ViewBuilder
    private var flyOverlay: some View {
        if let fly, totalFrame != .zero, zoneFrame != .zero {
            let from = fly.joining ? zoneFrame : totalFrame
            let to = fly.joining ? totalFrame : zoneFrame
            let at = flyLanded ? to : from
            let scale: CGFloat = fly.joining ? (flyLanded ? 0.42 : 1) : (flyLanded ? 1 : 0.42)
            Text(fly.text)
                .font(CounterFont.digits(56, bold: true))
                .foregroundStyle(fly.joining ? CounterPalette.brass : CounterPalette.pew)
                .scaleEffect(scale)
                .opacity(flyLanded ? 0 : 1)
                .position(x: at.midX, y: at.midY)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private var snackbar: some View {
        if let snack {
            HStack(spacing: 12) {
                Text(snack.text)
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let areaId = snack.undoInclusionOf, let area = areas.first(where: { $0.id == areaId }) {
                    Button("Undo") { toggleInclusion(area) }
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(CounterPalette.brass)
                }
            }
            .foregroundStyle(CounterPalette.chalk)
            .padding(.horizontal, 18)
            .frame(minHeight: 52)
            .counterGlass(Capsule())
            .padding(.horizontal, 16)
            .padding(.top, 4)
            .transition(.move(edge: .top).combined(with: .opacity))
            .id(snack.id)
        }
    }

    // MARK: Actions

    private func select(_ area: AreaCountState) {
        guard area.id != activeArea?.id else { return }
        let from = areas.firstIndex { $0.id == activeArea?.id } ?? 0
        let to = areas.firstIndex { $0.id == area.id } ?? 0
        entryEdge = to > from ? .trailing : .leading
        CounterHaptics.select()
        withAnimation(reduceMotion ? .easeInOut(duration: 0.15) : .spring(response: 0.38, dampingFraction: 0.86)) {
            selectedAreaId = area.id
        }
    }

    private func step(_ delta: Int) {
        guard let current = activeArea, let index = areas.firstIndex(where: { $0.id == current.id }) else { return }
        let target = index + delta
        guard areas.indices.contains(target) else { return }
        select(areas[target])
    }

    private func bump(_ area: AreaCountState, by delta: Int) {
        guard !model.state.isLocked, delta > 0 || area.count > 0 else { return }
        CounterHaptics.tick()
        model.bump(area.id, by: delta)
        seq += 1
        let x: CGFloat = delta > 0 ? .random(in: 0.5...0.85) : .random(in: 0.1...0.22)
        tokens.append(FloatToken(id: seq, value: delta, x: x))
    }

    private func toggleInclusion(_ area: AreaCountState) {
        // The snackbar's Undo can outlive a lock; a locked count must not change.
        guard !model.state.isLocked else { return }
        CounterHaptics.select()
        selectedAreaId = area.id
        let joining = !area.isIncluded
        model.toggleInclusion(area.id)

        if area.count > 0 && !reduceMotion {
            seq += 1
            flyLanded = false
            fly = FlyToken(id: seq, text: "\(joining ? "+" : "−")\(area.count)", joining: joining)
            let id = seq
            DispatchQueue.main.async {
                withAnimation(.timingCurve(0.5, 0, 0.3, 1, duration: 0.76)) { flyLanded = true }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                if fly?.id == id { fly = nil }
            }
        }

        let name = area.template_.name
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            snack = SnackMessage(
                text: joining ? "\(name) counted in the total." : "\(name) left out of the total.",
                undoInclusionOf: area.id
            )
        }
    }

    private func toggleLock() {
        if model.state.isLocked {
            CounterHaptics.soft()
        } else {
            lockedAt = Date()
            CounterHaptics.heavy()
        }
        model.toggleLock()
    }

    private func closeReceipt() {
        var instant = Transaction()
        instant.disablesAnimations = true
        withTransaction(instant) { showReceipt = false }
        model.clearReport()
    }

    /// One warning buzz when a zone or the total crosses 90% or 100% on the way up.
    private func warnOnCapacityCrossing() {
        var next: [String: Int] = [:]
        var warn = false
        for area in areas {
            let pct = percent(Int(area.count), of: Int(area.capacity))
            next[area.id] = pct
            if let old = lastPercents[area.id], crossedWarning(from: old, to: pct) { warn = true }
        }
        let totalPct = percent(Int(model.state.totalAttendance), of: Int(model.state.totalCapacity))
        next["total"] = totalPct
        if let old = lastPercents["total"], crossedWarning(from: old, to: totalPct) { warn = true }
        if warn { CounterHaptics.warning() }
        lastPercents = next
    }
}

// MARK: - Room level

/// Brass "water line" behind the counter showing how full the selected zone is.
private struct RoomLevel: View {
    let percent: Int
    let included: Bool

    var body: some View {
        let color = !included ? CounterPalette.pew : percent >= 100 ? CounterPalette.signal : CounterPalette.brass
        GeometryReader { geo in
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                Rectangle().fill(color.opacity(0.45)).frame(height: 1.5)
                Rectangle().fill(color.opacity(0.10))
                    .frame(height: geo.size.height * CGFloat(min(percent, 100)) / 100)
            }
        }
        .ignoresSafeArea(edges: .bottom)
        .animation(.spring(response: 0.6, dampingFraction: 0.8), value: percent)
        .animation(.easeInOut(duration: 0.3), value: included)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

private struct LevelBar: View {
    let percent: Int
    let height: CGFloat
    let color: Color

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(CounterPalette.plate)
                Capsule().fill(color)
                    .frame(width: geo.size.width * CGFloat(min(percent, 100)) / 100)
                Rectangle()
                    .fill(CounterPalette.pew.opacity(0.7))
                    .frame(width: 2, height: height + 6)
                    .offset(x: geo.size.width * 0.9 - 1)
            }
        }
        .frame(height: height)
        .animation(.spring(response: 0.45, dampingFraction: 0.85), value: percent)
    }
}

// MARK: - Zone rail

private struct ZoneRail: View {
    let areas: [AreaCountState]
    let activeId: String
    let onSelect: (AreaCountState) -> Void
    @Namespace private var glass

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                CounterGlassGroup(spacing: 8) {
                    HStack(spacing: 8) {
                        ForEach(areas, id: \.id) { area in
                            chip(area).id(area.id)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 6)
                }
            }
            .onChange(of: activeId, initial: true) { _, id in
                withAnimation(.snappy) { proxy.scrollTo(id, anchor: .center) }
            }
        }
    }

    private func chip(_ area: AreaCountState) -> some View {
        let selected = area.id == activeId
        let fg = selected ? CounterPalette.ground : area.isIncluded ? CounterPalette.chalk : CounterPalette.pew
        return Button { onSelect(area) } label: {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                // Name and count take different tones so the number reads first, like the stage odometer.
                Text(area.template_.name).font(.subheadline.weight(.medium))
                    .foregroundStyle(selected ? fg.opacity(0.7) : fg)
                Text("\(area.count)")
                    .font(CounterFont.digits(19))
                    .foregroundStyle(!selected && area.isIncluded ? CounterPalette.brass : fg)
                    .strikethrough(!area.isIncluded)
                    .contentTransition(.numericText(value: Double(area.count)))
            }
            .padding(.horizontal, 14)
            .frame(height: 40)
            .contentShape(Capsule())
            .counterGlass(Capsule(), tint: selected ? CounterPalette.chalk : nil, interactive: true)
            .counterGlassID(area.id, in: glass)
            .animation(.easeInOut(duration: 0.22), value: selected)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(area.template_.name), \(area.count)\(area.isIncluded ? "" : ", left out of total")")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

// MARK: - Inclusion toggle

private struct InclusionToggle: View {
    let included: Bool
    let action: () -> Void
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                ZStack(alignment: included ? .trailing : .leading) {
                    Capsule().fill(included ? CounterPalette.brass : CounterPalette.plateHigh)
                    Circle()
                        .fill(included ? CounterPalette.brassInk : CounterPalette.chalk)
                        .padding(3)
                }
                .frame(width: 40, height: 24)
                Text(included ? "Counted in total" : "Left out of total")
                    .font(.subheadline)
                    .foregroundStyle(included ? CounterPalette.chalk : CounterPalette.pew)
            }
            .frame(minHeight: 44)
            .opacity(isEnabled ? 1 : 0.4)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: included)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(included ? "Counted in total" : "Left out of total")
        .accessibilityHint("Changes whether this zone counts toward the total")
    }
}

// MARK: - Pad

/// Press bookkeeping in a reference type, so hold and swipe updates never re-render the key mid-press.
@MainActor
private final class PressTracker {
    var active = false
    /// Set when a hold or swipe already counted, so the release doesn't add one more.
    var consumed = false
    var lastY: CGFloat = 0
    var task: Task<Void, Never>?
}

/// Count key: tap for one, hold to repeat faster, or swipe up/down on it to add or subtract.
private struct PadKey: View {
    let sign: Int
    let title: String
    let caption: String?
    let accessibilityName: String
    let onBump: (Int) -> Void

    @Environment(\.isEnabled) private var isEnabled
    @State private var tracker = PressTracker()
    /// Resets on release and on cancellation, which `onEnded` never sees.
    @GestureState private var pressing = false

    var body: some View {
        Button {
            if !tracker.consumed { onBump(sign) }
        } label: {
            VStack(spacing: 6) {
                Text(title).font(CounterFont.digits(sign > 0 ? 72 : 44))
                if let caption {
                    Text(caption).font(.footnote.weight(.medium)).opacity(0.8)
                }
            }
        }
        .buttonStyle(PadKeyStyle(isAdd: sign > 0))
        .simultaneousGesture(press)
        .onChange(of: pressing) { _, isPressing in
            guard !isPressing else { return }
            tracker.task?.cancel()
            tracker.active = false
        }
        .onChange(of: isEnabled) { _, enabled in
            if !enabled { tracker.task?.cancel() }
        }
        .onDisappear { tracker.task?.cancel() }
        .accessibilityLabel(accessibilityName)
    }

    private var press: some Gesture {
        DragGesture(minimumDistance: 0, coordinateSpace: .global)
            .updating($pressing) { _, state, _ in state = true }
            .onChanged { value in
                if !tracker.active {
                    tracker.active = true
                    tracker.consumed = false
                    tracker.lastY = value.location.y
                    startHold()
                    return
                }
                let dy = tracker.lastY - value.location.y
                if abs(dy) >= 28 {
                    tracker.task?.cancel()
                    tracker.consumed = true
                    tracker.lastY = value.location.y
                    onBump(dy > 0 ? 1 : -1)
                }
            }
            .onEnded { _ in
                tracker.task?.cancel()
                tracker.active = false
                // The button's own action runs on release; clear the flag after it has read it.
                let tracker = tracker
                DispatchQueue.main.async { tracker.consumed = false }
            }
    }

    private func startHold() {
        guard isEnabled else { return }
        let tracker = tracker
        let sign = sign
        let onBump = onBump
        tracker.task?.cancel()
        tracker.task = Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(420))
            var interval = 220.0
            while !Task.isCancelled {
                tracker.consumed = true
                onBump(sign)
                try? await Task.sleep(for: .milliseconds(Int(interval)))
                interval = max(70, interval * 0.86)
            }
        }
    }
}

private struct PadKeyStyle: ButtonStyle {
    let isAdd: Bool
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        let shape = RoundedRectangle(cornerRadius: 22)
        configuration.label
            .foregroundStyle(isAdd ? CounterPalette.brassInk : CounterPalette.chalk)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(shape)
            .counterGlass(shape, tint: isAdd ? CounterPalette.brass : CounterPalette.plate.opacity(0.7), interactive: isEnabled)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .opacity(isEnabled ? 1 : 0.35)
            .animation(.spring(response: 0.22, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

private struct FloatingTokens: View {
    let tokens: [FloatToken]
    let onComplete: (Int) -> Void

    var body: some View {
        GeometryReader { geo in
            ForEach(tokens) { token in
                FloatingTokenView(token: token) { onComplete(token.id) }
                    .position(x: geo.size.width * token.x, y: 44)
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
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var travel: CGFloat = 0
    @State private var fade: Double = 1

    var body: some View {
        let adding = token.value > 0
        Text(adding ? "+1" : "−1")
            .font(CounterFont.digits(34, bold: true))
            .foregroundStyle(adding ? CounterPalette.brass : CounterPalette.pew)
            .scaleEffect(adding ? 0.85 + 0.35 * (1 - fade) : 1)
            .opacity(fade)
            .offset(y: travel)
            .task {
                let distance: CGFloat = reduceMotion ? 0 : (adding ? -170 : 90)
                withAnimation(.easeOut(duration: 0.72)) { travel = distance }
                withAnimation(.easeIn(duration: 0.5).delay(0.22)) { fade = 0 }
                try? await Task.sleep(for: .milliseconds(740))
                onComplete()
            }
    }
}

// MARK: - Lock shutter

/// Roller shutter that drops over the pad when the count is locked, with a stamped time.
private struct Shutter: View {
    let locked: Bool
    let lockedAt: Date?
    let onUnlock: () -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var stamped = false

    private var stampText: String {
        guard let lockedAt else { return "Locked" }
        return "Locked at \(lockedAt.formatted(date: .omitted, time: .shortened))"
    }

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Canvas { context, size in
                    var y: CGFloat = 0
                    while y < size.height {
                        context.fill(Path(CGRect(x: 0, y: y, width: size.width, height: 22)), with: .color(CounterPalette.plateHigh))
                        context.fill(Path(CGRect(x: 0, y: y + 22, width: size.width, height: 2)), with: .color(CounterPalette.plate))
                        y += 24
                    }
                }
                VStack(spacing: 14) {
                    Text(stampText)
                        .font(CounterFont.digits(26, bold: true))
                        .foregroundStyle(CounterPalette.brass)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(CounterPalette.ground, in: RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).strokeBorder(CounterPalette.brass, lineWidth: 2))
                        .rotationEffect(.degrees(-4))
                        .scaleEffect(stamped ? 1 : 1.7)
                        .opacity(stamped ? 1 : 0)
                    Button("Unlock to keep counting", action: onUnlock)
                        .buttonStyle(GlassPillStyle())
                }
            }
            .offset(y: locked ? 0 : -geo.size.height - 8)
            .animation(reduceMotion ? .easeInOut(duration: 0.15) : .timingCurve(0.65, 0, 0.25, 1, duration: 0.52), value: locked)
        }
        .allowsHitTesting(locked)
        .accessibilityHidden(!locked)
        .onChange(of: locked, initial: true) { _, isLocked in
            stamped = false
            guard isLocked else { return }
            withAnimation(.spring(response: 0.4, dampingFraction: 0.55).delay(reduceMotion ? 0 : 0.4)) { stamped = true }
        }
    }
}
