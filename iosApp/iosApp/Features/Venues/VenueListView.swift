import SwiftUI
import shared

@MainActor
final class VenueListModel: ObservableObject {
    private let viewModel = KoinIosKt.venueListViewModel()
    private let lifetime: ViewModelLifetime
    @Published var state: VenueListUiState

    init() {
        lifetime = ViewModelLifetime(viewModel)
        // Seed from the current StateFlow value, then observe updates.
        state = viewModel.uiState.value as! VenueListUiState
        lifetime.observe(viewModel.uiState, as: VenueListUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func create(name: String, location: String, code: String) {
        viewModel.createVenue(name: name, location: location, code: code)
    }
    func delete(_ id: String) { viewModel.deleteVenue(venueId: id) }
    func clearError() { viewModel.clearError() }
}

/// Pushed screens, shared by every tab's NavigationStack.
enum AppRoute: Hashable {
    case count(venueId: String, venueName: String)
    case areas(venueId: String)
    case seatMap(areaTemplateId: String)
    case history(venueId: String?)
    case trends(venueId: String?)
    case edit(venueId: String)
    case venues
    case eventTypes
}

struct VenueListView: View {
    @StateObject private var model = VenueListModel()
    /// The owning tab's stack; this view pushes routes onto it.
    @Binding var path: NavigationPath
    @State private var showingAdd = false
    @State private var pendingDelete: VenueDto?

    var body: some View {
        Group {
            if model.state.isLoading {
                ProgressView("Loading venues…")
            } else if model.state.isEmpty {
                ContentUnavailableView {
                    Label("No Venues", systemImage: "building.2")
                } description: {
                    Text("Add a venue, then add the areas you want to count.")
                } actions: {
                    Button("Add Venue") {
                        showingAdd = true
                    }
                    .buttonStyle(.borderedProminent)
                }
            } else {
                List {
                    ForEach(model.state.venues, id: \.id) { venue in
                        NavigationLink(value: AppRoute.count(venueId: venue.id, venueName: venue.name)) {
                            VenueRow(venue: venue)
                        }
                        .contextMenu {
                            menuItems(for: venue)
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                pendingDelete = venue
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                            Button {
                                path.append(AppRoute.edit(venueId: venue.id))
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            .tint(.blue)
                        }
                        .swipeActions(edge: .leading) {
                            Button {
                                path.append(AppRoute.areas(venueId: venue.id))
                            } label: {
                                Label("Areas", systemImage: "square.grid.2x2")
                            }
                            .tint(.indigo)
                        }
                    }
                }
            }
        }
        .navigationTitle("Venues")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    path.append(AppRoute.history(venueId: nil))
                } label: {
                    Label("All History", systemImage: "clock.arrow.circlepath")
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showingAdd = true
                } label: {
                    Label("Add Venue", systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: $showingAdd) {
            AddVenueSheet { name, location, code in
                model.create(name: name, location: location, code: code)
            }
        }
        .confirmationDialog(
            "Delete \(pendingDelete?.name ?? "venue")?",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 {
                pendingDelete = nil
            } }),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let venue = pendingDelete {
                    model.delete(venue.id)
                }
                pendingDelete = nil
            }
        } message: {
            Text("This permanently deletes the venue, its areas and its count history.")
        }
        .alert(
            "Error",
            isPresented: Binding(get: { model.state.errorMessage != nil }, set: { if !$0 {
                model.clearError()
            } }),
            actions: { Button("OK", role: .cancel) {} },
            message: { Text(model.state.errorMessage ?? "") }
        )
    }

    @ViewBuilder
    private func menuItems(for venue: VenueDto) -> some View {
        Button {
            path.append(AppRoute.count(venueId: venue.id, venueName: venue.name))
        } label: {
            Label("Head Count", systemImage: "person.3.sequence")
        }
        Button {
            path.append(AppRoute.areas(venueId: venue.id))
        } label: {
            Label("Manage Areas", systemImage: "square.grid.2x2")
        }
        Button {
            path.append(AppRoute.history(venueId: venue.id))
        } label: {
            Label("History", systemImage: "clock.arrow.circlepath")
        }
        Divider()
        Button {
            path.append(AppRoute.edit(venueId: venue.id))
        } label: {
            Label("Edit Venue", systemImage: "pencil")
        }
        Button(role: .destructive) {
            pendingDelete = venue
        } label: {
            Label("Delete", systemImage: "trash")
        }
    }
}

/// Resolves an [AppRoute]; register once per tab stack with `.navigationDestination`.
struct AppRouteDestination: View {
    let route: AppRoute
    @Binding var path: NavigationPath

    var body: some View {
        switch route {
        case let .count(venueId, venueName):
            HeadCounterView(venueId: venueId, venueName: venueName)
        case let .areas(venueId):
            AreaManagementView(venueId: venueId) {
                path.append(AppRoute.seatMap(areaTemplateId: $0))
            }
        case let .seatMap(areaTemplateId):
            SeatMapEditorView(areaTemplateId: areaTemplateId)
        case let .history(venueId):
            HistoryView(venueId: venueId) {
                path.append(AppRoute.trends(venueId: venueId))
            }
        case let .trends(venueId):
            TrendsView(venueId: venueId)
        case let .edit(venueId):
            VenueEditView(venueId: venueId)
        case .venues:
            VenueListView(path: $path)
        case .eventTypes:
            EventTypeManagementView()
        }
    }
}

private struct VenueRow: View {
    let venue: VenueDto

    var body: some View {
        HStack(spacing: 12) {
            Text(venue.code.prefix(2).uppercased())
                .font(.subheadline.bold())
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(Color(hex: venue.color), in: RoundedRectangle(cornerRadius: 8))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(venue.name).font(.headline)
                if !venue.location.isEmpty {
                    Text(venue.location).font(.subheadline).foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private struct AddVenueSheet: View {
    let onAdd: (_ name: String, _ location: String, _ code: String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var location = ""
    @State private var code = ""

    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty && !code.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name", text: $name)
                TextField("Location", text: $location)
                TextField("Code (e.g. MC)", text: $code)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
            }
            .navigationTitle("New Venue")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(
                            name.trimmingCharacters(in: .whitespaces),
                            location.trimmingCharacters(in: .whitespaces),
                            code.trimmingCharacters(in: .whitespaces).uppercased()
                        )
                        dismiss()
                    }
                    .disabled(!isValid)
                }
            }
        }
        .presentationDetents([.medium])
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
