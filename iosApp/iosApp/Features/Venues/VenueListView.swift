import SwiftUI
import shared

@MainActor
final class VenueListModel: ObservableObject {
    private let viewModel = KoinIosKt.venueListViewModel()
    @Published var state: VenueListUiState
    private var handle: Cancellable?

    init() {
        // Seed from the current StateFlow value, then observe updates.
        state = viewModel.uiState.value as! VenueListUiState
        handle = viewModel.observe(flow: viewModel.uiState) { [weak self] value in
            guard let next = value as? VenueListUiState else { return }
            self?.state = next
        }
    }

    deinit {
        handle?.cancel()
    }

    func delete(_ id: String) { viewModel.deleteVenue(venueId: id) }
    func clearError() { viewModel.clearError() }
}

struct VenueListView: View {
    @StateObject private var model = VenueListModel()

    var body: some View {
        NavigationStack {
            Group {
                if model.state.isLoading {
                    ProgressView("Loading venues…")
                } else if model.state.isEmpty {
                    ContentUnavailableView(
                        "No Venues",
                        systemImage: "building.2",
                        description: Text("Add a venue to get started.")
                    )
                } else {
                    List {
                        ForEach(model.state.venues, id: \.id) { venue in
                            NavigationLink {
                                HeadCounterView(venueId: venue.id, venueName: venue.name)
                            } label: {
                                VenueRow(venue: venue)
                            }
                            .swipeActions {
                                Button(role: .destructive) {
                                    model.delete(venue.id)
                                } label: { Label("Delete", systemImage: "trash") }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Venues")
            .alert(
                "Error",
                isPresented: Binding(
                    get: { model.state.errorMessage != nil },
                    set: { _ in model.clearError() }
                ),
                actions: { Button("OK", role: .cancel) {} },
                message: { Text(model.state.errorMessage ?? "") }
            )
        }
    }
}

private struct VenueRow: View {
    let venue: VenueDto

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color(hex: venue.color))
                .frame(width: 12, height: 12)
            VStack(alignment: .leading, spacing: 2) {
                Text(venue.name).font(.headline)
                Text(venue.location).font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer()
            Text(venue.code).font(.caption).foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

extension Color {
    /// Parses "#RRGGBB" strings stored by the shared model.
    init(hex: String) {
        let cleaned = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
        var rgb: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&rgb)
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255
        )
    }
}
