import SwiftUI
import shared

@MainActor
final class IncidentDetailModel: ObservableObject {
    private let viewModel: IncidentDetailViewModel
    private let lifetime: ViewModelLifetime
    @Published var incident: IncidentDto?

    init(incidentId: String) {
        viewModel = KoinIosKt.incidentDetailViewModel(incidentId: incidentId)
        lifetime = ViewModelLifetime(viewModel)
        incident = viewModel.incident.value as? IncidentDto
        lifetime.observe(viewModel.incident, as: IncidentDto.self) { [weak self] in
            self?.incident = $0
        }
    }

    func save(_ form: IncidentEditForm) {
        viewModel.saveDetails(
            title: form.title.trimmed,
            description: form.description.trimmed,
            severity: form.severity,
            category: form.category.trimmed,
            location: form.location.trimmed,
            assignedTo: form.assignedTo.trimmed,
            notes: form.notes.trimmed
        )
    }

    func updateStatus(_ status: String) { viewModel.updateStatus(status: status) }
    func resolve(actionsTaken: String) { viewModel.resolve(actionsTaken: actionsTaken.trimmed) }
    func delete() { viewModel.delete() }
}

struct IncidentEditForm: Equatable {
    var title = ""
    var description = ""
    var severity = "LOW"
    var category = ""
    var location = ""
    var assignedTo = ""
    var notes = ""

    init() {}

    init(_ incident: IncidentDto) {
        title = incident.title
        description = incident.description_
        severity = incident.severity
        category = incident.category
        location = incident.location
        assignedTo = incident.assignedTo
        notes = incident.notes
    }
}

struct IncidentDetailView: View {
    @StateObject private var model: IncidentDetailModel
    @Environment(\.dismiss) private var dismiss
    @State private var form = IncidentEditForm()
    @State private var loadedId: String?
    @State private var showingResolve = false
    @State private var actionsTaken = ""
    @State private var confirmingDelete = false

    init(incidentId: String) {
        _model = StateObject(wrappedValue: IncidentDetailModel(incidentId: incidentId))
    }

    private var isDirty: Bool {
        guard let incident = model.incident else { return false }
        return form != IncidentEditForm(incident)
    }

    var body: some View {
        Group {
            if let incident = model.incident {
                content(incident)
            } else {
                ProgressView()
            }
        }
        .navigationTitle("Incident")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: load)
        .onChange(of: model.incident?.id) { load() }
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { model.save(form) }
                    .disabled(!isDirty || form.title.trimmed.isEmpty)
            }
        }
        .alert("Resolve Incident", isPresented: $showingResolve) {
            TextField("Actions taken", text: $actionsTaken)
            Button("Cancel", role: .cancel) {}
            Button("Resolve") {
                if isDirty { model.save(form) }
                model.resolve(actionsTaken: actionsTaken)
            }
        } message: {
            Text("Describe what was done to resolve it.")
        }
        .confirmationDialog("Delete this incident?", isPresented: $confirmingDelete, titleVisibility: .visible) {
            Button("Delete", role: .destructive) {
                model.delete()
                dismiss()
            }
        }
    }

    private func load() {
        guard let incident = model.incident, loadedId != incident.id else { return }
        form = IncidentEditForm(incident)
        loadedId = incident.id
    }

    private func content(_ incident: IncidentDto) -> some View {
        let status = IncidentStatus.companion.fromString(value: incident.status)
        return Form {
            Section("Status") {
                Picker("Status", selection: Binding(get: { incident.status }, set: { model.updateStatus($0) })) {
                    ForEach(incidentStatusFlow) { Text($0.label).tag($0.name) }
                }
                .tint(Color(hex: status.color))
                if incident.status != "RESOLVED" && incident.status != "CLOSED" {
                    Button("Resolve…") {
                        actionsTaken = incident.actionsTaken
                        showingResolve = true
                    }
                }
                if !incident.actionsTaken.isEmpty {
                    LabeledContent("Actions taken", value: incident.actionsTaken)
                }
            }
            Section("Incident") {
                TextField("Title", text: $form.title)
                TextField("Description", text: $form.description, axis: .vertical)
                    .lineLimit(2...6)
                Picker("Severity", selection: $form.severity) {
                    ForEach(incidentSeverities) { Text($0.label).tag($0.name) }
                }
            }
            Section("Details") {
                TextField("Category", text: $form.category)
                TextField("Location", text: $form.location)
                TextField("Assigned to", text: $form.assignedTo)
                TextField("Notes", text: $form.notes, axis: .vertical)
                    .lineLimit(1...4)
            }
            Section {
                if !incident.reportedBy.isEmpty {
                    LabeledContent("Reported by", value: incident.reportedBy)
                }
                if incident.reportedAt > 0 {
                    LabeledContent("Reported") {
                        Text(Date(epochMillis: incident.reportedAt), format: .dateTime.day().month().hour().minute())
                    }
                }
                if let resolvedAt = incident.resolvedAt?.int64Value, resolvedAt > 0 {
                    LabeledContent("Resolved") {
                        Text(Date(epochMillis: resolvedAt), format: .dateTime.day().month().hour().minute())
                    }
                }
            }
            Section {
                Button("Delete Incident", role: .destructive) { confirmingDelete = true }
            }
        }
    }
}
