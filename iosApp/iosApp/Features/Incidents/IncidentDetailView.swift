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

    func save(_ f: IncidentForm) {
        viewModel.save(
            title: f.title, description: f.description, severity: f.severity,
            category: f.category, location: f.location, assignedTo: f.assignedTo
        )
    }

    func resolve(_ actions: String) { viewModel.resolve(actionsTaken: actions) }
    func delete() { viewModel.delete() }
}

struct IncidentForm {
    var title = "", description = "", severity = "MEDIUM", category = "", location = "", assignedTo = ""

    init() {}

    init(_ i: IncidentDto) {
        title = i.title
        description = i.description_
        severity = i.severity
        category = i.category
        location = i.location
        assignedTo = i.assignedTo
    }
}

struct IncidentDetailView: View {
    @StateObject private var model: IncidentDetailModel
    @Environment(\.dismiss) private var dismiss
    @State private var form = IncidentForm()
    @State private var loadedId: String?
    @State private var showResolve = false
    @State private var actionsTaken = ""

    init(incidentId: String) {
        _model = StateObject(wrappedValue: IncidentDetailModel(incidentId: incidentId))
    }

    var body: some View {
        Group {
            if let incident = model.incident {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        StatusChip(text: IncidentStatus.companion.fromString(value: incident.status).displayName)
                        OutlinedField(label: "Title", text: $form.title)
                        OutlinedField(label: "Description", text: $form.description, axis: .vertical)
                        Text("Severity").font(.caption.weight(.medium)).foregroundStyle(AppColor.text)
                        SeverityPicker(selected: $form.severity)
                        OutlinedField(label: "Category", text: $form.category)
                        OutlinedField(label: "Location", text: $form.location)
                        OutlinedField(label: "Assigned to", text: $form.assignedTo)
                        HStack(spacing: 8) {
                            WideButton(title: "Save") { model.save(form) }
                            WideButton(title: "Resolve", filled: false) { showResolve = true }
                        }
                    }
                    .padding(16)
                }
            } else {
                LoadingView()
            }
        }
        .appBackground()
        .appNavigationTitle("Incident")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Delete") {
                    model.delete()
                    dismiss()
                }
            }
        }
        .alert("Resolve Incident", isPresented: $showResolve) {
            TextField("Actions taken", text: $actionsTaken)
            Button("Cancel", role: .cancel) {}
            Button("Resolve") { model.resolve(actionsTaken) }
        }
        .onReceive(model.$incident) { incident in
            // Seed the form once; later emissions shouldn't clobber in-progress edits.
            guard let incident, loadedId != incident.id else { return }
            form = IncidentForm(incident)
            loadedId = incident.id
        }
    }
}
