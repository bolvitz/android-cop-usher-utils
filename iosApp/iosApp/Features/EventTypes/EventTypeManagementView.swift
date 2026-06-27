import SwiftUI
import shared

@MainActor
final class EventTypeModel: ObservableObject {
    private let viewModel = KoinIosKt.eventTypeManagementViewModel()
    @Published var state: EventTypeManagementUiState
    private var handle: Cancellable?

    init() {
        state = viewModel.uiState.value as! EventTypeManagementUiState
        handle = viewModel.observe(flow: viewModel.uiState) { [weak self] value in
            guard let next = value as? EventTypeManagementUiState else { return }
            self?.state = next
        }
    }

    deinit {
        handle?.cancel()
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
        NavigationStack {
            List {
                ForEach(model.state.eventTypes, id: \.id) { type in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(type.name).font(.headline)
                            if !type.description_.isEmpty {
                                Text(type.description_)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Toggle("", isOn: Binding(
                            get: { type.isActive },
                            set: { model.toggle(type, isActive: $0) }
                        ))
                        .labelsHidden()
                    }
                    .swipeActions {
                        Button(role: .destructive) { model.delete(type.id) } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
            .navigationTitle("Event Types")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button { showingAdd = true } label: { Image(systemName: "plus") }
                }
            }
            .sheet(isPresented: $showingAdd) { addSheet }
        }
    }

    private var addSheet: some View {
        NavigationStack {
            Form {
                TextField("Name", text: $newName)
                TextField("Description", text: $newDescription)
            }
            .navigationTitle("New Event Type")
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
