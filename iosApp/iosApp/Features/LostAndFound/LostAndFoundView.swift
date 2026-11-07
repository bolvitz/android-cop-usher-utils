import SwiftUI
import shared

@MainActor
final class LostAndFoundModel: ObservableObject {
    private let viewModel: LostAndFoundViewModel
    @Published var state: LostAndFoundUiState
    private let lifetime: ViewModelLifetime

    init(locationId: String? = nil) {
        viewModel = KoinIosKt.lostAndFoundViewModel(locationId: locationId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! LostAndFoundUiState
        lifetime.observe(viewModel.uiState, as: LostAndFoundUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func filter(_ status: String?) { viewModel.filterByStatus(status: status) }
    func updateStatus(_ id: String, to status: String) { viewModel.updateStatus(itemId: id, status: status) }
    func delete(_ id: String) { viewModel.delete(itemId: id) }
    func clearError() { viewModel.clearError() }

    func create(_ form: NewLostItemForm) {
        viewModel.createItem(
            description: form.description.trimmed,
            category: form.category,
            foundZone: form.foundZone.trimmed,
            color: form.color.trimmed,
            brand: form.brand.trimmed,
            reportedBy: form.reportedBy.trimmed
        )
    }
}

let itemStatuses: [StatusOption] = [
    StatusOption(name: "PENDING", label: "Pending"),
    StatusOption(name: "CLAIMED", label: "Claimed"),
    StatusOption(name: "DONATED", label: "Donated"),
    StatusOption(name: "DISPOSED", label: "Disposed")
]

/// Mirrors the shared `ItemCategory` enum (names are what the database stores).
let itemCategories: [StatusOption] = [
    "ELECTRONICS", "CLOTHING", "DOCUMENTS", "ACCESSORIES", "BAGS", "PERSONAL_ITEMS", "KEYS",
    "WALLETS", "JEWELRY", "TOYS", "BOOKS", "SPORTS_EQUIPMENT", "OTHER"
].map { StatusOption(name: $0, label: ItemCategory.companion.fromString(value: $0).displayName) }

struct NewLostItemForm {
    var description = ""
    var category = "OTHER"
    var foundZone = ""
    var color = ""
    var brand = ""
    var reportedBy = ""

    var isValid: Bool { !description.trimmed.isEmpty }
}

struct LostAndFoundView: View {
    @StateObject private var model: LostAndFoundModel
    private let locationId: String?
    private let venues: [VenueScopeRow]
    private let onPickVenue: (String) -> Void
    /// Set by the shell to open the "found item" sheet once this view appears.
    @Binding private var startAdding: Bool
    @State private var showingAdd = false

    init(
        locationId: String?,
        venues: [VenueScopeRow] = [],
        onPickVenue: @escaping (String) -> Void = { _ in },
        startAdding: Binding<Bool> = .constant(false)
    ) {
        self.locationId = locationId
        self.venues = venues
        self.onPickVenue = onPickVenue
        _startAdding = startAdding
        _model = StateObject(wrappedValue: LostAndFoundModel(locationId: locationId))
    }

    var body: some View {
        VStack(spacing: 0) {
            filterBar
            content
        }
        .navigationTitle("Lost & Found")
        .toolbar {
            ToolbarItem(placement: .primaryAction) { addButton }
        }
        .sheet(isPresented: $showingAdd) {
            NewLostItemSheet { model.create($0) }
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
        guard startAdding, locationId != nil else { return }
        startAdding = false
        showingAdd = true
    }

    /// Logging needs a venue: in "All venues" scope "+" asks which one first.
    @ViewBuilder private var addButton: some View {
        if locationId != nil {
            Button { showingAdd = true } label: {
                Label("Log Found Item", systemImage: "plus")
            }
        } else {
            Menu {
                Section("Found at which venue?") {
                    ForEach(venues, id: \.id) { venue in
                        Button(venue.name) {
                            startAdding = true
                            onPickVenue(venue.id)
                        }
                    }
                }
            } label: {
                Label("Log Found Item", systemImage: "plus")
            }
            .disabled(venues.isEmpty)
        }
    }

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                FilterChip(title: "All", active: model.state.filters.status == nil) { model.filter(nil) }
                ForEach(itemStatuses) { s in
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
                Label("No Items", systemImage: "bag")
            } description: {
                Text(locationId == nil
                     ? "Pick a venue with + to log a found item."
                     : "Items found at this venue appear here.")
            } actions: {
                if locationId != nil {
                    Button("Log Found Item") { showingAdd = true }
                        .buttonStyle(.borderedProminent)
                }
            }
        } else {
            List {
                ForEach(model.state.items, id: \.id) { item in
                    NavigationLink(value: AppRoute.lostItem(id: item.id)) {
                        LostItemRow(item: item) { status in
                            model.updateStatus(item.id, to: status)
                        }
                    }
                    .swipeActions {
                        Button(role: .destructive) { model.delete(item.id) } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }
}

private struct LostItemRow: View {
    let item: LostItemDto
    let onStatus: (String) -> Void

    var body: some View {
        let category = ItemCategory.companion.fromString(value: item.category)
        let status = ItemStatus.companion.fromString(value: item.status)
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(item.description_).font(.headline)
                Spacer()
                Text(status.displayName)
                    .font(.caption2).bold()
                    .padding(.horizontal, 8).padding(.vertical, 2)
                    .background(Color(hex: status.color))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
            }
            Text(item.foundZone.isEmpty ? category.displayName : "\(category.displayName) • \(item.foundZone)")
                .font(.subheadline).foregroundStyle(.secondary)
            if item.status == "PENDING" {
                // Claiming needs a name and contact, so it lives on the detail screen.
                HStack {
                    Button("Donate") { onStatus("DONATED") }
                        .font(.caption)
                        .buttonStyle(.borderless)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private struct NewLostItemSheet: View {
    let onAdd: (NewLostItemForm) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var form = NewLostItemForm()

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("What was found? (e.g. Black umbrella)", text: $form.description)
                    Picker("Category", selection: $form.category) {
                        ForEach(itemCategories) { Text($0.label).tag($0.name) }
                    }
                    TextField("Found in (e.g. Row C, Lobby)", text: $form.foundZone)
                }
                Section("Details") {
                    TextField("Colour", text: $form.color)
                    TextField("Brand", text: $form.brand)
                    TextField("Logged by", text: $form.reportedBy)
                        .textContentType(.name)
                }
            }
            .navigationTitle("Found Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(form)
                        dismiss()
                    }
                    .disabled(!form.isValid)
                }
            }
        }
    }
}
