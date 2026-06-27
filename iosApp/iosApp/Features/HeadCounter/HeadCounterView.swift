import SwiftUI
import shared

@MainActor
final class CountingModel: ObservableObject {
    private let viewModel: CountingViewModel
    @Published var state: CountingUiState
    @Published var eventTypes: [EventTypeDto] = []
    @Published var canUndo = false
    @Published var canRedo = false

    private var handles: [Cancellable] = []

    init(venueId: String, eventId: String? = nil) {
        viewModel = KoinIosKt.countingViewModel(venueId: venueId, eventId: eventId)
        state = viewModel.uiState.value as! CountingUiState
        handles.append(viewModel.observe(flow: viewModel.uiState) { [weak self] v in
            if let s = v as? CountingUiState { self?.state = s }
        })
        handles.append(viewModel.observe(flow: viewModel.eventTypes) { [weak self] v in
            self?.eventTypes = (v as? [EventTypeDto]) ?? []
        })
        handles.append(viewModel.observe(flow: viewModel.canUndo) { [weak self] v in
            self?.canUndo = (v as? KotlinBoolean)?.boolValue ?? false
        })
        handles.append(viewModel.observe(flow: viewModel.canRedo) { [weak self] v in
            self?.canRedo = (v as? KotlinBoolean)?.boolValue ?? false
        })
    }

    deinit {
        handles.forEach { $0.cancel() }
    }

    func startCount() {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        viewModel.createNewEvent(
            eventTypeId: eventTypes.first?.id,
            eventTypeName: eventTypes.first?.name ?? "Head Count",
            date: now,
            countedBy: "iOS"
        )
    }

    func increment(_ id: String) { viewModel.incrementCount(areaCountId: id, amount: 1) }
    func decrement(_ id: String) { viewModel.decrementCount(areaCountId: id, amount: 1) }
    func toggleInclusion(_ id: String) { viewModel.toggleAreaInclusion(areaCountId: id) }
    func undo() { viewModel.undo() }
    func redo() { viewModel.redo() }
    func lock() { viewModel.lockEvent() }
    func unlock() { viewModel.unlockEvent() }
}

struct HeadCounterView: View {
    let venueId: String
    let venueName: String
    @StateObject private var model: CountingModel

    init(venueId: String, venueName: String) {
        self.venueId = venueId
        self.venueName = venueName
        _model = StateObject(wrappedValue: CountingModel(venueId: venueId))
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
        .toolbar {
            if model.state.eventId != nil {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { model.undo() } label: { Image(systemName: "arrow.uturn.backward") }
                        .disabled(!model.canUndo)
                    Button { model.redo() } label: { Image(systemName: "arrow.uturn.forward") }
                        .disabled(!model.canRedo)
                    Button {
                        model.state.isLocked ? model.unlock() : model.lock()
                    } label: {
                        Image(systemName: model.state.isLocked ? "lock.fill" : "lock.open")
                    }
                }
            }
        }
    }

    private var startPrompt: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.3.sequence").font(.system(size: 48)).foregroundStyle(.secondary)
            Text("No active count for this venue.").foregroundStyle(.secondary)
            Button("Start Head Count", action: model.startCount)
                .buttonStyle(.borderedProminent)
        }
        .padding()
    }

    private var counter: some View {
        VStack(spacing: 0) {
            totalBanner
            List {
                ForEach(model.state.areaCounts, id: \.id) { area in
                    AreaCounterRow(
                        area: area,
                        locked: model.state.isLocked,
                        onIncrement: { model.increment(area.id) },
                        onDecrement: { model.decrement(area.id) },
                        onToggle: { model.toggleInclusion(area.id) }
                    )
                }
            }
        }
    }

    private var totalBanner: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("Total Attendance").font(.caption).foregroundStyle(.secondary)
                Text("\(model.state.totalAttendance)").font(.largeTitle.bold())
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("Capacity").font(.caption).foregroundStyle(.secondary)
                Text("\(model.state.totalCapacity)").font(.title2)
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

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Toggle(isOn: Binding(get: { area.isIncluded }, set: { _ in onToggle() })) {
                    Text(area.template.name).font(.headline)
                }
                .toggleStyle(.button)
            }
            HStack {
                Text("\(area.count) / \(area.capacity)").font(.title3.monospacedDigit())
                Text("\(area.percentage)%").font(.caption).foregroundStyle(.secondary)
                Spacer()
                Button { onDecrement() } label: { Image(systemName: "minus.circle.fill") }
                    .font(.title)
                    .disabled(locked)
                Button { onIncrement() } label: { Image(systemName: "plus.circle.fill") }
                    .font(.title)
                    .disabled(locked)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 4)
        .opacity(area.isIncluded ? 1 : 0.5)
    }
}
