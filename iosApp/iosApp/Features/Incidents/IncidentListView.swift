import SwiftUI
import shared

@MainActor
final class IncidentListModel: ObservableObject {
    private let viewModel: IncidentListViewModel
    @Published var state: IncidentListUiState
    private let lifetime: ViewModelLifetime

    init(venueId: String? = nil) {
        viewModel = KoinIosKt.incidentListViewModel(venueId: venueId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! IncidentListUiState
        lifetime.observe(viewModel.uiState, as: IncidentListUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func filter(_ status: String?) { viewModel.filterByStatus(status: status) }
    func advance(_ id: String, to status: String) { viewModel.updateStatus(incidentId: id, status: status) }
    func delete(_ id: String) { viewModel.delete(incidentId: id) }
    func clearError() { viewModel.clearError() }

    func create(_ form: NewIncidentForm) {
        viewModel.createIncident(
            title: form.title.trimmed,
            description: form.description.trimmed,
            severity: form.severity,
            category: form.category.trimmed,
            location: form.location.trimmed,
            reportedBy: form.reportedBy.trimmed
        )
    }
}

/// Status order, driven by name strings (Kotlin enum entries don't bridge to
/// Swift `switch`, so we key everything off the canonical names).
struct StatusOption: Identifiable {
    let name: String
    let label: String
    var id: String { name }
}

let incidentStatusFlow: [StatusOption] = [
    StatusOption(name: "REPORTED", label: "Reported"),
    StatusOption(name: "INVESTIGATING", label: "Investigating"),
    StatusOption(name: "IN_PROGRESS", label: "In Progress"),
    StatusOption(name: "RESOLVED", label: "Resolved"),
    StatusOption(name: "CLOSED", label: "Closed")
]

let incidentSeverities: [StatusOption] = [
    StatusOption(name: "LOW", label: "Low"),
    StatusOption(name: "MEDIUM", label: "Medium"),
    StatusOption(name: "HIGH", label: "High"),
    StatusOption(name: "CRITICAL", label: "Critical")
]

func nextIncidentStatus(after name: String) -> StatusOption? {
    guard let idx = incidentStatusFlow.firstIndex(where: { $0.name == name }),
          idx + 1 < incidentStatusFlow.count else {
        return nil
    }
    return incidentStatusFlow[idx + 1]
}

struct NewIncidentForm {
    var title = ""
    var description = ""
    var severity = "MEDIUM"
    var category = ""
    var location = ""
    var reportedBy = ""

    var isValid: Bool { !title.trimmed.isEmpty }
}

struct IncidentListView: View {
    @StateObject private var model: IncidentListModel
    private let venueId: String?
    private let venues: [VenueScopeRow]
    private let onPickVenue: (String) -> Void
    /// Set by the shell (e.g. a Live-tab shortcut, or picking a venue from "+"
    /// while viewing all venues) to open the report sheet once this view appears.
    @Binding private var startAdding: Bool
    @State private var showingAdd = false

    init(
        venueId: String?,
        venues: [VenueScopeRow] = [],
        onPickVenue: @escaping (String) -> Void = { _ in },
        startAdding: Binding<Bool> = .constant(false)
    ) {
        self.venueId = venueId
        self.venues = venues
        self.onPickVenue = onPickVenue
        _startAdding = startAdding
        _model = StateObject(wrappedValue: IncidentListModel(venueId: venueId))
    }

    var body: some View {
        VStack(spacing: 0) {
            filterBar
            content
        }
        .navigationTitle("Incidents")
        .toolbar {
            ToolbarItem(placement: .primaryAction) { addButton }
        }
        .sheet(isPresented: $showingAdd) {
            NewIncidentSheet { model.create($0) }
        }
        .onAppear(perform: consumeStartAdding)
        .onChange(of: startAdding) { consumeStartAdding() }
        .alert(
            "Error",
            isPresented: Binding(get: { model.state.error != nil }, set: { if !$0 { model.clearError() } }),
            actions: { Button("OK", role: .cancel) {} },
            message: { Text(model.state.error ?? "") }
        )
    }

    private func consumeStartAdding() {
        // Without a venue, leave the request for the view rebuilt once one is picked.
        guard startAdding, venueId != nil else { return }
        startAdding = false
        showingAdd = true
    }

    /// Reporting needs a venue: in "All venues" scope "+" asks which one first.
    @ViewBuilder private var addButton: some View {
        if venueId != nil {
            Button { showingAdd = true } label: {
                Label("Report Incident", systemImage: "plus")
            }
        } else {
            Menu {
                Section("Report at which venue?") {
                    ForEach(venues, id: \.id) { venue in
                        Button(venue.name) {
                            startAdding = true
                            onPickVenue(venue.id)
                        }
                    }
                }
            } label: {
                Label("Report Incident", systemImage: "plus")
            }
            .disabled(venues.isEmpty)
        }
    }

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                FilterChip(title: "All", active: model.state.filters.status == nil) { model.filter(nil) }
                ForEach(incidentStatusFlow.prefix(3)) { s in
                    FilterChip(title: s.label, active: model.state.filters.status == s.name) { model.filter(s.name) }
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
            ContentUnavailableView {
                Label("No Incidents", systemImage: "exclamationmark.triangle")
            } description: {
                Text(venueId == nil
                     ? "Pick a venue with + to report an incident."
                     : "Incidents reported at this venue appear here.")
            } actions: {
                if venueId != nil {
                    Button("Report Incident") { showingAdd = true }
                        .buttonStyle(.borderedProminent)
                }
            }
        } else {
            List {
                ForEach(model.state.incidents, id: \.id) { incident in
                    NavigationLink(value: AppRoute.incident(id: incident.id)) {
                        IncidentRow(incident: incident) { next in
                            model.advance(incident.id, to: next)
                        }
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
}

struct FilterChip: View {
    let title: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(active ? Color.accentColor : Color(.secondarySystemBackground))
                .foregroundStyle(active ? .white : .primary)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(active ? .isSelected : [])
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
                Text(incident.description_).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
            }
            HStack {
                Text(status.displayName)
                    .font(.caption)
                    .foregroundStyle(Color(hex: status.color))
                Spacer()
                if let next = nextIncidentStatus(after: incident.status) {
                    // Borderless so it stays tappable inside the NavigationLink row.
                    Button("→ \(next.label)") { onAdvance(next.name) }
                        .font(.caption)
                        .buttonStyle(.borderless)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private struct NewIncidentSheet: View {
    let onReport: (NewIncidentForm) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var form = NewIncidentForm()

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Title", text: $form.title)
                    TextField("What happened?", text: $form.description, axis: .vertical)
                        .lineLimit(3...6)
                }
                Section("Severity") {
                    Picker("Severity", selection: $form.severity) {
                        ForEach(incidentSeverities) { Text($0.label).tag($0.name) }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                }
                Section("Details") {
                    TextField("Category (e.g. Medical)", text: $form.category)
                    TextField("Location (e.g. Lobby)", text: $form.location)
                    TextField("Reported by", text: $form.reportedBy)
                        .textContentType(.name)
                }
            }
            .navigationTitle("Report Incident")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Report") {
                        onReport(form)
                        dismiss()
                    }
                    .disabled(!form.isValid)
                }
            }
        }
    }
}

extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}
