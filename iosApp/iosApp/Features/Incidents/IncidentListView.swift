import SwiftUI
import shared

@MainActor
final class IncidentListModel: ObservableObject {
    private let viewModel: IncidentListViewModel
    @Published var state: IncidentListUiState
    private let lifetime: ViewModelLifetime

    init(venueId: String? = nil) {
        self.venueId = venueId
        viewModel = KoinIosKt.incidentListViewModel(venueId: venueId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! IncidentListUiState
        lifetime.observe(viewModel.uiState, as: IncidentListUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    /// The scoped venue; nil when the list shows all venues.
    let venueId: String?

    func filter(_ status: String?) { viewModel.filterByStatus(status: status) }
    func create(title: String, description: String, severity: String, venueId: String) {
        viewModel.createIncident(
            title: title, description: description, severity: severity,
            category: "", location: "", reportedBy: "", venueId: venueId
        )
    }
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
    @StateObject private var model: IncidentListModel
    /// True as a top-level tab, where the nav bar is hidden and the title sits in content.
    let isTab: Bool
    let onOpen: (String) -> Void
    /// Choices for a new incident when the list shows all venues.
    let venues: [VenueScopeRow]
    /// Set while the add sheet is open: the venue the new incident goes to.
    @State private var addVenueId: String?
    @State private var pickVenue = false

    init(venueId: String? = nil, isTab: Bool = false, venues: [VenueScopeRow] = [], onOpen: @escaping (String) -> Void) {
        self.isTab = isTab
        self.venues = venues
        self.onOpen = onOpen
        _model = StateObject(wrappedValue: IncidentListModel(venueId: venueId))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if isTab { ScreenHeader(title: "Incidents") }
            ChipRow {
                FilterChip(title: "All", selected: model.state.filters.status == nil) { model.filter(nil) }
                ForEach(statusFlow.prefix(3)) { s in
                    FilterChip(title: s.label, selected: model.state.filters.status == s.name) { model.filter(s.name) }
                }
            }
            content
        }
        .appBackground()
        .appNavigationTitle("Incidents")
        .fab("Report incident", isShown: model.venueId != nil || !venues.isEmpty) {
            if let id = model.venueId { addVenueId = id } else { pickVenue = true }
        }
        .venuePicker(isPresented: $pickVenue, venues: venues) { addVenueId = $0 }
        .sheet(isPresented: Binding(get: { addVenueId != nil }, set: { if !$0 { addVenueId = nil } })) {
            if let target = addVenueId {
                AddIncidentSheet { title, description, severity in
                    model.create(title: title, description: description, severity: severity, venueId: target)
                }
            }
        }
    }

    @ViewBuilder private var content: some View {
        if model.state.isLoading {
            LoadingView()
        } else if model.state.isEmpty {
            EmptyMessage(text: "No incidents reported.")
        } else {
            CardList {
                ForEach(model.state.incidents, id: \.id) { incident in
                    IncidentCard(
                        incident: incident,
                        onOpen: { onOpen(incident.id) },
                        onAdvance: { model.advance(incident.id, to: $0) },
                        onDelete: { model.delete(incident.id) }
                    )
                }
            }
        }
    }
}

private struct IncidentCard: View {
    let incident: IncidentDto
    let onOpen: () -> Void
    let onAdvance: (String) -> Void
    let onDelete: () -> Void

    var body: some View {
        // Kotlin enums expose displayName/color via fromString lookups.
        let severity = IncidentSeverity.companion.fromString(value: incident.severity)
        let status = IncidentStatus.companion.fromString(value: incident.status)
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(incident.title).font(.headline).foregroundStyle(AppColor.text)
                Spacer()
                ColorBadge(text: severity.displayName, hex: severity.color)
            }
            if !incident.description_.isEmpty {
                Text(incident.description_).font(.subheadline).foregroundStyle(AppColor.secondaryText)
                    .padding(.top, 4)
            }
            HStack {
                StatusChip(text: status.displayName, color: Color(hex: status.color))
                Spacer()
                if let next = nextStatus(after: incident.status) {
                    TextAction(title: "→ \(next.label)") { onAdvance(next.name) }
                }
                TextAction(title: "Delete", action: onDelete)
            }
            .padding(.top, 8)
        }
        .appCard()
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
        .accessibilityAction(named: "Open", onOpen)
    }
}

private struct AddIncidentSheet: View {
    let onAdd: (_ title: String, _ description: String, _ severity: String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var description = ""
    @State private var severity = "MEDIUM"

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    OutlinedField(label: "Title", text: $title)
                    OutlinedField(label: "Description", text: $description)
                    SeverityPicker(selected: $severity)
                }
                .padding(16)
            }
            .appBackground()
            .appNavigationTitle("Report Incident")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Report") {
                        onAdd(title.trimmingCharacters(in: .whitespacesAndNewlines), description.trimmingCharacters(in: .whitespacesAndNewlines), severity)
                        dismiss()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}

/// Low / Medium / High / Critical chips, keyed by the canonical names.
struct SeverityPicker: View {
    @Binding var selected: String
    private let options = [("LOW", "Low"), ("MEDIUM", "Medium"), ("HIGH", "High"), ("CRITICAL", "Critical")]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(options, id: \.0) { name, label in
                    FilterChip(title: label, selected: selected == name) { selected = name }
                }
            }
        }
    }
}
