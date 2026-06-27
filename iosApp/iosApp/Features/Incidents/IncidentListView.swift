import SwiftUI
import shared

@MainActor
final class IncidentListModel: ObservableObject {
    private let viewModel: IncidentListViewModel
    @Published var state: IncidentListUiState
    private var handle: Cancellable?

    init(venueId: String? = nil) {
        viewModel = KoinIosKt.incidentListViewModel(venueId: venueId)
        state = viewModel.uiState.value as! IncidentListUiState
        handle = viewModel.observe(flow: viewModel.uiState) { [weak self] value in
            if let next = value as? IncidentListUiState { self?.state = next }
        }
    }

    deinit { handle?.cancel() }

    func filter(_ status: String?) { viewModel.filterByStatus(status: status) }
    func advance(_ id: String, to status: String) { viewModel.updateStatus(incidentId: id, status: status) }
    func delete(_ id: String) { viewModel.delete(incidentId: id) }
}

/// Status order, driven by name strings (Kotlin enum entries don't bridge to
/// Swift `switch`, so we key everything off the canonical names).
private struct StatusOption: Identifiable {
    let name: String
    let label: String
    var id: String { name }
}

private let statusFlow: [StatusOption] = [
    StatusOption(name: "REPORTED", label: "Reported"),
    StatusOption(name: "INVESTIGATING", label: "Investigating"),
    StatusOption(name: "IN_PROGRESS", label: "In Progress"),
    StatusOption(name: "RESOLVED", label: "Resolved"),
    StatusOption(name: "CLOSED", label: "Closed")
]

private func nextStatus(after name: String) -> StatusOption? {
    guard let idx = statusFlow.firstIndex(where: { $0.name == name }), idx + 1 < statusFlow.count else {
        return nil
    }
    return statusFlow[idx + 1]
}

struct IncidentListView: View {
    @StateObject private var model = IncidentListModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                filterBar
                content
            }
            .navigationTitle("Incidents")
        }
    }

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                chip("All", active: model.state.filters.status == nil) { model.filter(nil) }
                ForEach(statusFlow.prefix(3)) { s in
                    chip(s.label, active: model.state.filters.status == s.name) { model.filter(s.name) }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }

    @ViewBuilder private var content: some View {
        if model.state.isLoading {
            Spacer(); ProgressView(); Spacer()
        } else if model.state.isEmpty {
            ContentUnavailableView("No Incidents", systemImage: "exclamationmark.triangle")
        } else {
            List {
                ForEach(model.state.incidents, id: \.id) { incident in
                    IncidentRow(incident: incident) { next in
                        model.advance(incident.id, to: next)
                    }
                    .swipeActions {
                        Button(role: .destructive) { model.delete(incident.id) } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }

    private func chip(_ title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(active ? Color.accentColor : Color(.secondarySystemBackground))
                .foregroundStyle(active ? .white : .primary)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct IncidentRow: View {
    let incident: IncidentDto
    let onAdvance: (String) -> Void

    var body: some View {
        // Kotlin enums expose displayName/color via fromString lookups.
        let severity = IncidentSeverity.companion.fromString(value: incident.severity)
        let status = IncidentStatus.companion.fromString(value: incident.status)
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(incident.title).font(.headline)
                Spacer()
                Text(severity.displayName)
                    .font(.caption2).bold()
                    .padding(.horizontal, 8).padding(.vertical, 2)
                    .background(Color(hex: severity.color))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
            }
            if !incident.description_.isEmpty {
                Text(incident.description_).font(.subheadline).foregroundStyle(.secondary)
            }
            HStack {
                Text(status.displayName)
                    .font(.caption)
                    .foregroundStyle(Color(hex: status.color))
                Spacer()
                if let next = nextStatus(after: incident.status) {
                    Button("→ \(next.label)") { onAdvance(next.name) }
                        .font(.caption)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
