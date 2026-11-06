import SwiftUI
import shared

@MainActor
final class HistoryModel: ObservableObject {
    private let viewModel: HistoryViewModel
    private let lifetime: ViewModelLifetime
    @Published var state: HistoryUiState
    @Published var report: HeadCountReport?

    init(venueId: String?) {
        viewModel = KoinIosKt.historyViewModel(venueId: venueId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! HistoryUiState
        lifetime.observe(viewModel.uiState, as: HistoryUiState.self) { [weak self] in
            self?.state = $0
        }
        // A null emission never reaches Swift, so closing the receipt clears `report` locally.
        lifetime.observe(viewModel.report, as: HeadCountReport.self) { [weak self] in
            self?.report = $0
        }
    }

    func showReport(_ eventId: String) {
        viewModel.showReport(eventId: eventId)
    }

    func clearReport() {
        report = nil
        viewModel.clearReport()
    }

    func unlock(_ eventId: String) {
        viewModel.unlockEvent(eventId: eventId)
    }

    func delete(_ eventId: String) {
        viewModel.deleteEvent(eventId: eventId)
    }
}

struct HistoryView: View {
    let onOpenTrends: () -> Void
    @StateObject private var model: HistoryModel
    @State private var pendingDelete: EventWithDetails_?
    @State private var showReceipt = false

    init(venueId: String?, onOpenTrends: @escaping () -> Void) {
        self.onOpenTrends = onOpenTrends
        _model = StateObject(wrappedValue: HistoryModel(venueId: venueId))
    }

    /// Best turnout per venue, so each bar compares like with like.
    private var bestByVenue: [String: Int32] {
        Dictionary(model.state.events.map { ($0.event.venueId, $0.event.totalAttendance) }, uniquingKeysWith: max)
    }

    var body: some View {
        Group {
            if model.state.isLoading {
                ProgressView()
            } else if model.state.isEmpty {
                ContentUnavailableView(
                    "No counts yet",
                    systemImage: "clock",
                    description: Text("Counts you take appear here, open or locked.")
                )
            } else {
                list
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(CounterPalette.ground.ignoresSafeArea())
        .navigationTitle("History")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: onOpenTrends) {
                    Label("Trends", systemImage: "chart.xyaxis.line")
                }
            }
        }
        .fullScreenCover(isPresented: $showReceipt) {
            if let report = model.report {
                HeadCountReceiptView(report: report, onClose: closeReceipt)
                    .presentationBackground(.clear)
            }
        }
        .onChange(of: model.report != nil) { _, hasReport in
            var instant = Transaction()
            instant.disablesAnimations = true
            withTransaction(instant) { showReceipt = hasReport }
        }
        .confirmationDialog(
            "Delete this count?",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let item = pendingDelete {
                    model.delete(item.event.id)
                }
                pendingDelete = nil
            }
        } message: {
            Text("Its zone counts are removed for good.")
        }
    }

    private var list: some View {
        List {
            Section {
                let best = bestByVenue
                ForEach(Array(model.state.events.enumerated()), id: \.element.event.id) { index, item in
                    NavigationLink {
                        HeadCounterView(venueId: item.event.venueId, venueName: item.venue.name, eventId: item.event.id)
                    } label: {
                        HistoryRow(item: item, best: Int(best[item.event.venueId] ?? 0), index: index)
                    }
                    .listRowBackground(CounterPalette.plate.opacity(0.6))
                    .swipeActions(edge: .leading) {
                        Button {
                            model.showReport(item.event.id)
                        } label: {
                            Label("Report", systemImage: "scroll")
                        }
                        .tint(CounterPalette.brass)
                    }
                    .swipeActions {
                        Button(role: .destructive) {
                            pendingDelete = item
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        if item.event.isLocked {
                            Button {
                                CounterHaptics.soft()
                                model.unlock(item.event.id)
                            } label: {
                                Label("Unlock", systemImage: "lock.open")
                            }
                            .tint(CounterPalette.pew)
                        }
                    }
                    .contextMenu {
                        Button {
                            model.showReport(item.event.id)
                        } label: {
                            Label("View report", systemImage: "scroll")
                        }
                    }
                }
            } header: {
                Text("Bars compare each count with its venue's best turnout. Swipe a count for its report or more actions.")
                    .textCase(nil)
                    .font(.footnote)
                    .foregroundStyle(CounterPalette.pew)
            }
        }
        .scrollContentBackground(.hidden)
    }

    private func closeReceipt() {
        var instant = Transaction()
        instant.disablesAnimations = true
        withTransaction(instant) { showReceipt = false }
        model.clearReport()
    }
}

private struct HistoryRow: View {
    let item: EventWithDetails_
    let best: Int
    let index: Int
    @State private var grown = false

    private var title: String {
        if !item.event.eventName.isEmpty {
            return item.event.eventName
        }
        return item.eventType?.name ?? "Count"
    }

    private var total: Int { Int(item.event.totalAttendance) }
    private var isBest: Bool { best > 0 && total == best }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title).font(.headline)
                    Text(item.venue.name).font(.subheadline)
                    Text(Date(epochMillis: item.event.date), format: .dateTime.weekday(.abbreviated).month().day().hour().minute())
                        .font(.footnote)
                        .foregroundStyle(CounterPalette.pew)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text("\(total)")
                        .font(CounterFont.digits(34))
                        .foregroundStyle(CounterPalette.chalk)
                    statusTag
                }
            }
            HStack(spacing: 10) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(CounterPalette.plateHigh)
                        Capsule()
                            .fill(isBest ? CounterPalette.brass : CounterPalette.pew)
                            .frame(width: grown && best > 0 ? geo.size.width * CGFloat(total) / CGFloat(best) : 0)
                    }
                }
                .frame(height: 4)
                Text(isBest ? "Best turnout" : best > 0 ? "\(percent(total, of: best))% of best" : "")
                    .font(.caption)
                    .foregroundStyle(CounterPalette.pew)
                    .fixedSize()
            }
            .accessibilityHidden(true)
        }
        .padding(.vertical, 4)
        .onAppear {
            withAnimation(.spring(response: 0.7, dampingFraction: 0.85).delay(Double(min(index, 10)) * 0.05)) { grown = true }
        }
    }

    private var statusTag: some View {
        Text(item.event.isLocked ? "Locked" : "Open")
            .font(.caption.weight(.semibold))
            .foregroundStyle(item.event.isLocked ? CounterPalette.chalk : CounterPalette.brassInk)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(item.event.isLocked ? CounterPalette.plateHigh : CounterPalette.brass, in: Capsule())
    }
}
