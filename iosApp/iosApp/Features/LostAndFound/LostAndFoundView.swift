import SwiftUI
import shared

@MainActor
final class LostAndFoundModel: ObservableObject {
    private let viewModel: LostAndFoundViewModel
    @Published var state: LostAndFoundUiState
    private var handle: Cancellable?

    init(locationId: String? = nil) {
        viewModel = KoinIosKt.lostAndFoundViewModel(locationId: locationId)
        state = viewModel.uiState.value as! LostAndFoundUiState
        handle = viewModel.observe(flow: viewModel.uiState) { [weak self] value in
            if let next = value as? LostAndFoundUiState { self?.state = next }
        }
    }

    deinit { handle?.cancel() }

    func filter(_ status: String?) { viewModel.filterByStatus(status: status) }
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
    @StateObject private var model = LostAndFoundModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                filterBar
                content
            }
            .navigationTitle("Lost & Found")
        }
    }

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                chip("All", active: model.state.filters.status == nil) { model.filter(nil) }
                ForEach(itemStatuses) { s in
                    chip(s.label, active: model.state.filters.status == s.name) { model.filter(s.name) }
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
            ContentUnavailableView("No Items", systemImage: "bag")
        } else {
            List {
                ForEach(model.state.items, id: \.id) { item in
                    LostItemRow(item: item) { status in
                        model.updateStatus(item.id, to: status)
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

    private func chip(_ title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(active ? Color.accentColor : Color(.secondarySystemBackground))
                .foregroundStyle(active ? .white : .primary)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
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
            Text("\(category.displayName) • \(item.foundZone)")
                .font(.subheadline).foregroundStyle(.secondary)
            if item.status == "PENDING" {
                HStack {
                    Button("Claim") { onStatus("CLAIMED") }.font(.caption)
                    Button("Donate") { onStatus("DONATED") }.font(.caption)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
