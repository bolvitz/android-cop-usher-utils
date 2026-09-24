import SwiftUI
import shared

@MainActor
final class AreaManagementModel: ObservableObject {
    private let viewModel: AreaManagementViewModel
    private let lifetime: ViewModelLifetime
    @Published var state: AreaManagementUiState

    init(venueId: String) {
        viewModel = KoinIosKt.areaManagementViewModel(venueId: venueId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! AreaManagementUiState
        lifetime.observe(viewModel.uiState, as: AreaManagementUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func create(name: String, capacity: Int) {
        viewModel.createArea(name: name, capacity: Int32(clamping: capacity), type: "OTHER")
    }

    func delete(_ id: String) {
        viewModel.deleteArea(templateId: id)
    }

    func clearError() {
        viewModel.clearError()
    }
}

struct AreaManagementView: View {
    let onSeatMap: (String) -> Void
    @StateObject private var model: AreaManagementModel
    @State private var showingAdd = false
    @State private var pendingDelete: AreaTemplateDto?

    init(venueId: String, onSeatMap: @escaping (String) -> Void) {
        self.onSeatMap = onSeatMap
        _model = StateObject(wrappedValue: AreaManagementModel(venueId: venueId))
    }

    var body: some View {
        Group {
            if model.state.isLoading {
                ProgressView()
            } else if model.state.isEmpty {
                ContentUnavailableView {
                    Label("No Areas", systemImage: "square.grid.2x2")
                } description: {
                    Text("Add an area so it can be counted.")
                } actions: {
                    Button("Add Area") {
                        showingAdd = true
                    }
                    .buttonStyle(.borderedProminent)
                }
            } else {
                List {
                    ForEach(model.state.areas, id: \.id) { area in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(area.name).font(.headline)
                                Text(area.hasSeatMap ? "Capacity \(area.capacity) • Seat map" : "Capacity \(area.capacity)")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button("Seats") {
                                onSeatMap(area.id)
                            }
                            .buttonStyle(.bordered)
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                pendingDelete = area
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Areas")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showingAdd = true
                } label: {
                    Label("Add Area", systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: $showingAdd) {
            AddAreaSheet { name, capacity in
                model.create(name: name, capacity: capacity)
            }
        }
        .confirmationDialog(
            "Delete \(pendingDelete?.name ?? "area")?",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 {
                pendingDelete = nil
            } }),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let area = pendingDelete {
                    model.delete(area.id)
                }
                pendingDelete = nil
            }
        }
        .alert(
            "Error",
            isPresented: Binding(get: { model.state.error != nil }, set: { if !$0 {
                model.clearError()
            } }),
            actions: { Button("OK", role: .cancel) {
            } },
            message: { Text(model.state.error ?? "") }
        )
    }
}

private struct AddAreaSheet: View {
    let onAdd: (_ name: String, _ capacity: Int) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var capacity = 100

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name", text: $name)
                LabeledContent("Capacity") {
                    TextField("Capacity", value: $capacity, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                }
            }
            .navigationTitle("New Area")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(name.trimmingCharacters(in: .whitespaces), max(0, capacity))
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
