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
                LoadingView()
            } else if model.state.isEmpty {
                EmptyMessage(text: "No areas yet. Add one so it can be counted.")
            } else {
                CardList {
                    ForEach(model.state.areas, id: \.id) { area in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(area.name).font(.headline).foregroundStyle(AppColor.text)
                                Text(area.hasSeatMap ? "Capacity \(area.capacity) • Seat map" : "Capacity \(area.capacity)")
                                    .font(.caption)
                                    .foregroundStyle(AppColor.secondaryText)
                            }
                            Spacer()
                            TextAction(title: "Seats") { onSeatMap(area.id) }
                            TextAction(title: "Delete") { pendingDelete = area }
                        }
                        .appCard()
                    }
                }
            }
        }
        .appBackground()
        .appNavigationTitle("Areas")
        .fab("Add area") { showingAdd = true }
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
    @State private var capacity = "100"

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 8) {
                    OutlinedField(label: "Name", text: $name)
                    OutlinedField(label: "Capacity", text: $capacity, keyboard: .numberPad)
                        .onChange(of: capacity) { _, new in capacity = new.filter(\.isNumber) }
                }
                .padding(16)
            }
            .appBackground()
            .appNavigationTitle("New Area")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(name.trimmingCharacters(in: .whitespaces), Int(capacity) ?? 0)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
