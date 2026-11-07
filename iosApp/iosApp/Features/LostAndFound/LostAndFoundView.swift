import SwiftUI
import shared

@MainActor
final class LostAndFoundModel: ObservableObject {
    private let viewModel: LostAndFoundViewModel
    @Published var state: LostAndFoundUiState
    private let lifetime: ViewModelLifetime

    init(locationId: String? = nil) {
        self.locationId = locationId
        viewModel = KoinIosKt.lostAndFoundViewModel(locationId: locationId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! LostAndFoundUiState
        lifetime.observe(viewModel.uiState, as: LostAndFoundUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    /// The scoped venue; nil when the list shows all venues.
    let locationId: String?

    func filter(_ status: String?) { viewModel.filterByStatus(status: status) }
    func create(description: String, category: String, zone: String, locationId: String) {
        viewModel.createItem(
            description: description, category: category, foundZone: zone,
            color: "", brand: "", reportedBy: "", locationId: locationId
        )
    }
    /// Quick claim from the list, as Android does; the detail screen records the real claimant.
    func claim(_ id: String) { viewModel.claim(itemId: id, claimedBy: "Claimant", contact: "", notes: "") }
    func updateStatus(_ id: String, to status: String) { viewModel.updateStatus(itemId: id, status: status) }
    func delete(_ id: String) { viewModel.delete(itemId: id) }
}

private struct ItemStatusOption: Identifiable {
    let name: String
    let label: String
    var id: String { name }
}

private let itemStatuses: [ItemStatusOption] = [
    ItemStatusOption(name: "PENDING", label: "Pending"),
    ItemStatusOption(name: "CLAIMED", label: "Claimed"),
    ItemStatusOption(name: "DONATED", label: "Donated"),
    ItemStatusOption(name: "DISPOSED", label: "Disposed")
]

struct LostAndFoundView: View {
    @StateObject private var model: LostAndFoundModel
    /// True as a top-level tab, where the nav bar is hidden and the title sits in content.
    let isTab: Bool
    let onOpen: (String) -> Void
    /// Choices for a new item when the list shows all venues.
    let venues: [VenueScopeRow]
    /// Set while the add sheet is open: the venue the new item goes to.
    @State private var addVenueId: String?
    @State private var pickVenue = false

    init(locationId: String? = nil, isTab: Bool = false, venues: [VenueScopeRow] = [], onOpen: @escaping (String) -> Void) {
        self.isTab = isTab
        self.venues = venues
        self.onOpen = onOpen
        _model = StateObject(wrappedValue: LostAndFoundModel(locationId: locationId))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if isTab { ScreenHeader(title: "Lost & Found") }
            ChipRow {
                FilterChip(title: "All", selected: model.state.filters.status == nil) { model.filter(nil) }
                ForEach(itemStatuses) { s in
                    FilterChip(title: s.label, selected: model.state.filters.status == s.name) { model.filter(s.name) }
                }
            }
            content
        }
        .appBackground()
        .appNavigationTitle("Lost & Found")
        .fab("Add found item", isShown: model.locationId != nil || !venues.isEmpty) {
            if let id = model.locationId { addVenueId = id } else { pickVenue = true }
        }
        .venuePicker(isPresented: $pickVenue, venues: venues) { addVenueId = $0 }
        .sheet(isPresented: Binding(get: { addVenueId != nil }, set: { if !$0 { addVenueId = nil } })) {
            if let target = addVenueId {
                AddItemSheet { description, category, zone in
                    model.create(description: description, category: category, zone: zone, locationId: target)
                }
            }
        }
    }

    @ViewBuilder private var content: some View {
        if model.state.isLoading {
            LoadingView()
        } else if model.state.isEmpty {
            EmptyMessage(text: "No items recorded.")
        } else {
            CardList {
                ForEach(model.state.items, id: \.id) { item in
                    LostItemCard(
                        item: item,
                        onOpen: { onOpen(item.id) },
                        onClaim: { model.claim(item.id) },
                        onDonate: { model.updateStatus(item.id, to: "DONATED") },
                        onDelete: { model.delete(item.id) }
                    )
                }
            }
        }
    }
}

private struct LostItemCard: View {
    let item: LostItemDto
    let onOpen: () -> Void
    let onClaim: () -> Void
    let onDonate: () -> Void
    let onDelete: () -> Void

    var body: some View {
        let category = ItemCategory.companion.fromString(value: item.category)
        let status = ItemStatus.companion.fromString(value: item.status)
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(item.description_).font(.headline).foregroundStyle(AppColor.text)
                Spacer()
                ColorBadge(text: status.displayName, hex: status.color)
            }
            Text("\(category.displayName) • \(item.foundZone)")
                .font(.subheadline).foregroundStyle(AppColor.secondaryText)
                .padding(.top, 4)
            HStack {
                if item.status == "PENDING" {
                    TextAction(title: "Claim", action: onClaim)
                    TextAction(title: "Donate", action: onDonate)
                }
                Spacer()
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

private struct AddItemSheet: View {
    let onAdd: (_ description: String, _ category: String, _ zone: String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var description = ""
    @State private var zone = ""
    @State private var category = "OTHER"

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    OutlinedField(label: "Description", text: $description)
                    OutlinedField(label: "Found zone", text: $zone)
                    Text("Category").font(.caption.weight(.medium)).foregroundStyle(AppColor.text)
                    CategoryPicker(selected: $category)
                }
                .padding(16)
            }
            .appBackground()
            .appNavigationTitle("Found Item")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(description.trimmingCharacters(in: .whitespacesAndNewlines), category, zone.trimmingCharacters(in: .whitespacesAndNewlines))
                        dismiss()
                    }
                    .disabled(description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}

/// The first six item categories as chips, as on Android.
struct CategoryPicker: View {
    @Binding var selected: String
    private let options = [
        ("ELECTRONICS", "Electronics"), ("CLOTHING", "Clothing"), ("DOCUMENTS", "Documents"),
        ("ACCESSORIES", "Accessories"), ("BAGS", "Bags & Luggage"), ("PERSONAL_ITEMS", "Personal Items")
    ]

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
