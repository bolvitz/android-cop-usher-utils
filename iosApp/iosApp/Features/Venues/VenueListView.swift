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
    case reports
    case incidents(venueId: String)
    case incidentDetail(incidentId: String)
    case lostItems(venueId: String)
    case lostItemDetail(itemId: String)
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
                LoadingView()
            } else if model.state.isEmpty {
                EmptyMessage(text: "No venues yet. Tap + to add one.")
            } else {
                CardList {
                    ForEach(model.state.venues, id: \.id) { venue in
                        VenueCard(venue: venue, open: { path.append($0) }, onDelete: { pendingDelete = venue })
                    }
                }
            }
        }
        .appBackground()
        .appNavigationTitle("Venues")
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button("Reports") { path.append(AppRoute.reports) }
                Button("Event Types") { path.append(AppRoute.eventTypes) }
            }
        }
        .fab("Add venue") { showingAdd = true }
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
        case .reports:
            ReportsView()
        case let .incidents(venueId):
            IncidentListView(venueId: venueId) { path.append(AppRoute.incidentDetail(incidentId: $0)) }
        case let .incidentDetail(incidentId):
            IncidentDetailView(incidentId: incidentId)
        case let .lostItems(venueId):
            LostAndFoundView(locationId: venueId) { path.append(AppRoute.lostItemDetail(itemId: $0)) }
        case let .lostItemDetail(itemId):
            LostItemDetailView(itemId: itemId)
        }
    }
}

/// Avatar, name and location; tap counts, the ⋮ menu holds the rest, as on Android.
private struct VenueCard: View {
    let venue: VenueDto
    let open: (AppRoute) -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(venue.code.prefix(2).uppercased())
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(Color(hex: venue.color), in: RoundedRectangle(cornerRadius: AppRadius.small))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(venue.name).font(.headline).foregroundStyle(AppColor.text)
                if !venue.location.isEmpty {
                    Text(venue.location).font(.caption).foregroundStyle(AppColor.secondaryText)
                }
            }
            Spacer()
            Menu {
                Button("Head Count") { open(.count(venueId: venue.id, venueName: venue.name)) }
                Button("Manage Areas") { open(.areas(venueId: venue.id)) }
                Button("History") { open(.history(venueId: venue.id)) }
                Button("Incidents") { open(.incidents(venueId: venue.id)) }
                Button("Lost & Found") { open(.lostItems(venueId: venue.id)) }
                Divider()
                Button("Edit Venue") { open(.edit(venueId: venue.id)) }
                Button("Delete", role: .destructive, action: onDelete)
            } label: {
                Image(systemName: "ellipsis").rotationEffect(.degrees(90))
                    .foregroundStyle(AppColor.text)
                    .frame(width: 48, height: 48)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Actions")
        }
        .appCard()
        .contentShape(Rectangle())
        .onTapGesture { open(.count(venueId: venue.id, venueName: venue.name)) }
        .accessibilityAction(named: "Head Count") { open(.count(venueId: venue.id, venueName: venue.name)) }
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
            ScrollView {
                VStack(spacing: 8) {
                    OutlinedField(label: "Name", text: $name)
                    OutlinedField(label: "Location", text: $location)
                    OutlinedField(label: "Code (e.g. MC)", text: $code)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                }
                .padding(16)
            }
            .appBackground()
            .appNavigationTitle("New Venue")
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
