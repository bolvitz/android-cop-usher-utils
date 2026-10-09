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

    init(venueId: String?, onOpenTrends: @escaping () -> Void) {
        self.onOpenTrends = onOpenTrends
        _model = StateObject(wrappedValue: HistoryModel(venueId: venueId))
    }

    var body: some View {
        Group {
            if model.state.isLoading {
                ProgressView()
            } else if model.state.isEmpty {
                ContentUnavailableView(
                    "No Events",
                    systemImage: "clock",
                    description: Text("Completed and in-progress head counts appear here.")
                )
            } else {
                List {
                    ForEach(model.state.events, id: \.event.id) { item in
                        NavigationLink {
                            HeadCounterView(venueId: item.event.venueId, venueName: item.venue.name, eventId: item.event.id)
                        } label: {
                            HistoryRow(item: item)
                        }
                        .swipeActions(edge: .leading) {
                            Button {
                                model.showReport(item.event.id)
                            } label: {
                                Label("Report", systemImage: "doc.plaintext")
                            }
                            .tint(.accentColor)
                        }
                        .contextMenu {
                            Button {
                                model.showReport(item.event.id)
                            } label: {
                                Label("View Report", systemImage: "doc.plaintext")
                            }
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                pendingDelete = item
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                            if item.event.isLocked {
                                Button {
                                    model.unlock(item.event.id)
                                } label: {
                                    Label("Unlock", systemImage: "lock.open")
                                }
                                .tint(.orange)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("History")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: onOpenTrends) {
                    Label("Trends", systemImage: "chart.xyaxis.line")
                }
            }
        }
        .sheet(isPresented: Binding(get: { model.report != nil }, set: { if !$0 {
            model.clearReport()
        } })) {
            if let report = model.report {
                HeadCountReceiptView(report: report)
            }
        }
        .confirmationDialog(
            "Delete this event?",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 {
                pendingDelete = nil
            } }),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let item = pendingDelete {
                    model.delete(item.event.id)
                }
                pendingDelete = nil
            }
        } message: {
            Text("Its head counts will be permanently removed.")
        }
    }
}

private struct HistoryRow: View {
    let item: EventWithDetails_

    private var title: String {
        if !item.event.eventName.isEmpty {
            return item.event.eventName
        }
        return item.eventType?.name ?? "Event"
    }

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(item.venue.name).font(.subheadline)
                Text(Date(epochMillis: item.event.date), format: .dateTime.weekday(.abbreviated).month().day().hour().minute())
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Label(item.event.isLocked ? "Locked" : "Open", systemImage: item.event.isLocked ? "lock.fill" : "circle.dotted")
                    .font(.caption)
                    .foregroundStyle(item.event.isLocked ? Color.secondary : Color.accentColor)
            }
            Spacer()
            Text("\(item.event.totalAttendance)")
                .font(.title2.bold().monospacedDigit())
        }
        .padding(.vertical, 2)
    }
}
