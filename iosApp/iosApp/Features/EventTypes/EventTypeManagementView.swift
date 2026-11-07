import SwiftUI
import shared

@MainActor
final class EventTypeModel: ObservableObject {
    private let viewModel = KoinIosKt.eventTypeManagementViewModel()
    @Published var state: EventTypeManagementUiState
    private let lifetime: ViewModelLifetime

    init() {
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! EventTypeManagementUiState
        lifetime.observe(viewModel.uiState, as: EventTypeManagementUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func create(name: String, description: String) {
        viewModel.createEventType(name: name, description: description)
    }
    func delete(_ id: String) { viewModel.deleteEventType(id: id) }
    func toggle(_ dto: EventTypeDto, isActive: Bool) {
        viewModel.toggleStatus(eventType: dto, isActive: isActive)
    }
    func clearMessage() { viewModel.clearMessage() }
}

struct EventTypeManagementView: View {
    @StateObject private var model = EventTypeModel()
    @State private var showingAdd = false
    @State private var newName = ""
    @State private var newDescription = ""

    var body: some View {
        Group {
            if model.state.eventTypes.isEmpty {
                EmptyMessage(text: "No event types yet.")
            } else {
                CardList {
                    ForEach(model.state.eventTypes, id: \.id) { type in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(type.name).font(.headline).foregroundStyle(AppColor.text)
                                if !type.description_.isEmpty {
                                    Text(type.description_)
                                        .font(.caption)
                                        .foregroundStyle(AppColor.secondaryText)
                                }
                            }
                            Spacer()
                            Toggle(type.name, isOn: Binding(
                                get: { type.isActive },
                                set: { model.toggle(type, isActive: $0) }
                            ))
                            .labelsHidden()
                            TextAction(title: "Delete") { model.delete(type.id) }
                        }
                        .appCard()
                    }
                }
            }
        }
        .appBackground()
        .appNavigationTitle("Event Types")
        .fab("Add event type") { showingAdd = true }
        .sheet(isPresented: $showingAdd) { addSheet }
    }

    private var addSheet: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 8) {
                    OutlinedField(label: "Name", text: $newName)
                    OutlinedField(label: "Description", text: $newDescription)
                }
                .padding(16)
            }
            .appBackground()
            .appNavigationTitle("New Event Type")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { resetSheet() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        model.create(name: newName, description: newDescription)
                        resetSheet()
                    }
                    .disabled(newName.isEmpty)
                }
            }
        }
    }

    private func resetSheet() {
        newName = ""
        newDescription = ""
        showingAdd = false
    }
}
