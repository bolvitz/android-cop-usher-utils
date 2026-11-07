import SwiftUI
import shared

@MainActor
final class AppShellModel: ObservableObject {
    private let viewModel = KoinIosKt.appShellViewModel()
    private let lifetime: ViewModelLifetime
    @Published var state: AppShellUiState

    init() {
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! AppShellUiState
        lifetime.observe(viewModel.uiState, as: AppShellUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    /// Pass nil for "All venues".
    func select(_ venueId: String?) { viewModel.selectVenue(venueId: venueId) }

    /// Changes whenever the scope changes, so scoped screens rebuild their ViewModel.
    var scopeKey: String { state.selectedVenueId ?? "all" }
}

enum AppTab: Hashable {
    case live, count, incidents, lostItems, more
}

/// Top-level shell: venue scope switcher in each tab's toolbar, five tabs at the bottom.
/// Each tab owns its NavigationStack; pushed screens (counter, setup) go on that stack.
struct ContentView: View {
    @StateObject private var shell = AppShellModel()
    @State private var tab: AppTab = .live
    @State private var livePath = NavigationPath()
    @State private var countPath = NavigationPath()
    @State private var incidentsPath = NavigationPath()
    @State private var lostPath = NavigationPath()
    /// One-shot requests to open a tab's "new" sheet (from Live shortcuts, or
    /// from picking a venue under "+" while viewing all venues).
    @State private var startIncident = false
    @State private var startLostItem = false
    @State private var morePath = NavigationPath()

    var body: some View {
        let state = shell.state
        // Tapping the tab you are on pops it back to its root, as in COP Member.
        let selection = Binding<AppTab>(get: { tab }, set: { new in
            if new == tab { popToRoot(new) } else { tab = new }
        })
        TabView(selection: selection) {
            stack($livePath) {
                LiveTab(
                    shell: shell,
                    path: $livePath,
                    tab: $tab,
                    onReportIncident: { startIncident = true; tab = .incidents },
                    onLogFoundItem: { startLostItem = true; tab = .lostItems }
                )
                .navigationTitle("Live")
            }
            .tabItem { Label("Live", systemImage: "waveform.path.ecg") }
            .tag(AppTab.live)

            stack($countPath) {
                CountTab(shell: shell, path: $countPath).navigationTitle("Count")
            }
            .tabItem { Label("Count", systemImage: "person.badge.plus") }
            .tag(AppTab.count)

            stack($incidentsPath) {
                if !state.isLoading {
                    IncidentListView(
                        venueId: state.selectedVenueId,
                        venues: state.venues,
                        onPickVenue: { shell.select($0) },
                        startAdding: $startIncident
                    )
                    .id("incidents_\(shell.scopeKey)")
                }
            }
            .tabItem { Label("Incidents", systemImage: "exclamationmark.triangle") }
            .badge(Int(state.openIncidents))
            .tag(AppTab.incidents)

            stack($lostPath) {
                if !state.isLoading {
                    LostAndFoundView(
                        locationId: state.selectedVenueId,
                        venues: state.venues,
                        onPickVenue: { shell.select($0) },
                        startAdding: $startLostItem
                    )
                    .id("lost_\(shell.scopeKey)")
                }
            }
            // Matches Android, where "Lost & Found" truncates in a 5-item bar.
            .tabItem { Label("Lost Items", systemImage: "bag") }
            .badge(Int(state.unclaimedItems))
            .tag(AppTab.lostItems)

            stack($morePath) {
                MoreTab(shell: shell, path: $morePath).navigationTitle("More")
            }
            .tabItem { Label("More", systemImage: "line.3.horizontal") }
            .tag(AppTab.more)
        }
    }

    private func popToRoot(_ tab: AppTab) {
        switch tab {
        case .live: livePath = NavigationPath()
        case .count: countPath = NavigationPath()
        case .more: morePath = NavigationPath()
        case .incidents: incidentsPath = NavigationPath()
        case .lostItems: lostPath = NavigationPath()
        }
    }

    private func stack<Root: View>(_ path: Binding<NavigationPath>, @ViewBuilder root: () -> Root) -> some View {
        NavigationStack(path: path) {
            root()
                .toolbar { ToolbarItem(placement: .topBarLeading) { ScopeMenu(shell: shell) } }
                .navigationDestination(for: AppRoute.self) { AppRouteDestination(route: $0, path: path) }
        }
    }
}

/// "Viewing: <scope>" menu; picking a venue scopes every tab.
private struct ScopeMenu: View {
    @ObservedObject var shell: AppShellModel

    var body: some View {
        let state = shell.state
        Menu {
            Picker("Show", selection: Binding(get: { state.selectedVenueId }, set: { shell.select($0) })) {
                Text(AppShellUiState.companion.ALL_VENUES).tag(String?.none)
                ForEach(state.venues, id: \.id) { venue in
                    Text(venue.name).tag(Optional(venue.id))
                }
            }
        } label: {
            HStack(spacing: 2) {
                Text(state.scopeName).font(.subheadline.weight(.semibold)).lineLimit(1)
                Image(systemName: "chevron.down").font(.caption2.weight(.bold))
            }
        }
        .accessibilityLabel("Viewing \(state.scopeName). Change venue")
    }
}

private struct LiveTab: View {
    @ObservedObject var shell: AppShellModel
    @Binding var path: NavigationPath
    @Binding var tab: AppTab
    let onReportIncident: () -> Void
    let onLogFoundItem: () -> Void

    var body: some View {
        let state = shell.state
        List {
            if state.isLoading {
                ProgressView()
            } else if state.venues.isEmpty {
                EmptyVenues { path.append(AppRoute.venues) }
            } else if let venue = state.selectedVenue {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(venue.name).font(.title2.bold())
                        if !venue.location.isEmpty {
                            Text(venue.location).foregroundStyle(.secondary)
                        }
                        Button {
                            path.append(AppRoute.count(venueId: venue.id, venueName: venue.name))
                        } label: {
                            Text("Count").frame(maxWidth: .infinity, minHeight: 36)
                        }
                        .buttonStyle(.borderedProminent)
                        HStack {
                            Button { path.append(AppRoute.areas(venueId: venue.id)) } label: {
                                Text("Areas").frame(maxWidth: .infinity, minHeight: 32)
                            }
                            Button { path.append(AppRoute.history(venueId: venue.id)) } label: {
                                Text("History").frame(maxWidth: .infinity, minHeight: 32)
                            }
                        }
                        .buttonStyle(.bordered)
                        HStack {
                            Button(action: onReportIncident) {
                                Label("Report incident", systemImage: "exclamationmark.triangle")
                                    .frame(maxWidth: .infinity, minHeight: 32)
                            }
                            Button(action: onLogFoundItem) {
                                Label("Found item", systemImage: "bag.badge.plus")
                                    .frame(maxWidth: .infinity, minHeight: 32)
                            }
                        }
                        .buttonStyle(.bordered)
                        .font(.subheadline)
                    }
                    .padding(.vertical, 4)
                }
                stats(open: venue.openIncidents, unclaimed: venue.unclaimedItems)
            } else {
                stats(open: state.openIncidents, unclaimed: state.unclaimedItems)
                Section("Venues") {
                    ForEach(state.venues, id: \.id) { row in
                        Button { shell.select(row.id) } label: { VenueScopeRowView(row: row) }
                            .foregroundStyle(.primary)
                    }
                }
            }
        }
    }

    private func stats(open: Int32, unclaimed: Int32) -> some View {
        Section {
            HStack(spacing: 8) {
                StatCard(label: "Open incidents", value: open) { tab = .incidents }
                StatCard(label: "Unclaimed items", value: unclaimed) { tab = .lostItems }
            }
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
        }
    }
}

private struct CountTab: View {
    @ObservedObject var shell: AppShellModel
    @Binding var path: NavigationPath

    var body: some View {
        let state = shell.state
        let rows = state.selectedVenue.map { [$0] } ?? state.venues
        List {
            if state.isLoading {
                ProgressView()
            } else if rows.isEmpty {
                EmptyVenues { path.append(AppRoute.venues) }
            } else {
                Section {
                    ForEach(rows, id: \.id) { row in
                        NavigationLink(value: AppRoute.count(venueId: row.id, venueName: row.name)) {
                            VStack(alignment: .leading) {
                                Text(row.name)
                                if !row.location.isEmpty {
                                    Text(row.location).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                } header: {
                    if state.isAllVenues { Text("Pick a venue") }
                }
            }
        }
    }
}

private struct MoreTab: View {
    @ObservedObject var shell: AppShellModel
    @Binding var path: NavigationPath

    var body: some View {
        let venue = shell.state.selectedVenue
        List {
            Section("Records") {
                NavigationLink("History", value: AppRoute.history(venueId: venue?.id))
                NavigationLink("Trends", value: AppRoute.trends(venueId: venue?.id))
            }
            Section("Setup") {
                NavigationLink("Venues", value: AppRoute.venues)
                if let venue {
                    NavigationLink(value: AppRoute.areas(venueId: venue.id)) {
                        VStack(alignment: .leading) {
                            Text("Areas & seat maps")
                            Text(venue.name).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                } else {
                    VStack(alignment: .leading) {
                        Text("Areas & seat maps")
                        Text("Pick a venue at the top first").font(.caption)
                    }
                    .foregroundStyle(.secondary)
                }
                NavigationLink("Event types", value: AppRoute.eventTypes)
            }
        }
    }
}

private struct StatCard: View {
    let label: String
    let value: Int32
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption).foregroundStyle(.secondary)
                Text("\(value)").font(.title.bold()).foregroundStyle(.primary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}

private struct VenueScopeRowView: View {
    let row: VenueScopeRow

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(row.name).font(.headline)
            if !row.location.isEmpty {
                Text(row.location).font(.caption).foregroundStyle(.secondary)
            }
            HStack {
                chip(row.openIncidents, one: "open incident", many: "open incidents")
                chip(row.unclaimedItems, one: "unclaimed item", many: "unclaimed items")
            }
        }
        .padding(.vertical, 4)
    }

    private func chip(_ count: Int32, one: String, many: String) -> some View {
        Text("\(count) \(count == 1 ? one : many)")
            .font(.caption)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(Color(.tertiarySystemFill), in: Capsule())
    }
}

private struct EmptyVenues: View {
    let onSetUp: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label("No venues yet", systemImage: "building.2")
        } description: {
            Text("Add a venue, then its areas, to start counting.")
        } actions: {
            Button("Set up your first venue", action: onSetUp).buttonStyle(.borderedProminent)
        }
    }
}

#Preview {
    ContentView()
}
