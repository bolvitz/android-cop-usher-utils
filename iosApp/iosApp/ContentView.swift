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

/// Top-level shell: venue scope bar on top of each tab root, five tabs at the bottom.
/// Each tab owns its NavigationStack; pushed screens (counter, setup) go on that stack
/// and show their own inline title instead of the scope bar, as on Android.
struct ContentView: View {
    @StateObject private var shell = AppShellModel()
    @State private var tab: AppTab = .live
    @State private var livePath = NavigationPath()
    @State private var countPath = NavigationPath()
    @State private var incidentsPath = NavigationPath()
    @State private var lostPath = NavigationPath()
    @State private var morePath = NavigationPath()
    @State private var showScopeSheet = false

    var body: some View {
        let state = shell.state
        // Tapping the tab you are on pops it back to its root, as in COP Member.
        let selection = Binding<AppTab>(get: { tab }, set: { new in
            if new == tab { popToRoot(new) } else { tab = new }
        })
        TabView(selection: selection) {
            stack($livePath) {
                LiveTab(shell: shell, path: $livePath, tab: $tab)
            }
            .tabItem { Label("Live", systemImage: "waveform.path.ecg") }
            .tag(AppTab.live)

            stack($countPath) {
                CountTab(shell: shell, path: $countPath)
            }
            .tabItem { Label("Count", systemImage: "person.badge.plus") }
            .tag(AppTab.count)

            stack($incidentsPath) {
                if !state.isLoading {
                    IncidentListView(venueId: state.selectedVenueId, isTab: true, venues: state.venues) {
                        incidentsPath.append(AppRoute.incidentDetail(incidentId: $0))
                    }
                    .id("incidents_\(shell.scopeKey)")
                } else {
                    LoadingView()
                }
            }
            .tabItem { Label("Incidents", systemImage: "exclamationmark.triangle") }
            .badge(Int(state.openIncidents))
            .tag(AppTab.incidents)

            stack($lostPath) {
                if !state.isLoading {
                    LostAndFoundView(locationId: state.selectedVenueId, isTab: true, venues: state.venues) {
                        lostPath.append(AppRoute.lostItemDetail(itemId: $0))
                    }
                    .id("lost_\(shell.scopeKey)")
                } else {
                    LoadingView()
                }
            }
            // Matches Android, where "Lost & Found" truncates in a 5-item bar.
            .tabItem { Label("Lost Items", systemImage: "bag") }
            .badge(Int(state.unclaimedItems))
            .tag(AppTab.lostItems)

            stack($morePath) {
                MoreTab(shell: shell, path: $morePath)
            }
            .tabItem { Label("More", systemImage: "line.3.horizontal") }
            .tag(AppTab.more)
        }
        .tint(AppColor.primary)
        .sheet(isPresented: $showScopeSheet) {
            ScopeSheet(state: state) { venueId in
                shell.select(venueId)
                showScopeSheet = false
            }
            .presentationDetents([.medium, .large])
        }
    }

    private func popToRoot(_ tab: AppTab) {
        switch tab {
        case .live: livePath = NavigationPath()
        case .count: countPath = NavigationPath()
        case .incidents: incidentsPath = NavigationPath()
        case .lostItems: lostPath = NavigationPath()
        case .more: morePath = NavigationPath()
        }
    }

    private func stack<Root: View>(_ path: Binding<NavigationPath>, @ViewBuilder root: () -> Root) -> some View {
        NavigationStack(path: path) {
            root()
                .toolbar(.hidden, for: .navigationBar)
                .safeAreaInset(edge: .top, spacing: 0) {
                    ScopeBar(scopeName: shell.state.scopeName) { showScopeSheet = true }
                }
                .navigationDestination(for: AppRoute.self) { AppRouteDestination(route: $0, path: path) }
        }
    }
}

/// "Viewing / <scope> ▾" bar; tapping opens the scope sheet.
private struct ScopeBar: View {
    let scopeName: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                Text("Viewing").font(.caption2).foregroundStyle(AppColor.secondaryText)
                HStack(spacing: 2) {
                    Text(scopeName).font(.headline).foregroundStyle(AppColor.text).lineLimit(1)
                    Image(systemName: "arrowtriangle.down.fill").font(.system(size: 9))
                        .foregroundStyle(AppColor.primary)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
            .padding(.horizontal, 16)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(AppColor.background)
        .accessibilityLabel("Viewing \(scopeName). Change venue")
    }
}

/// Bottom sheet listing "All venues" and every venue, the current one checked.
private struct ScopeSheet: View {
    let state: AppShellUiState
    let onPick: (String?) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("Show").font(.headline).foregroundStyle(AppColor.text)
                    .padding(.horizontal, 24).padding(.vertical, 8)
                    .accessibilityAddTraits(.isHeader)
                option(AppShellUiState.companion.ALL_VENUES, "\(state.venues.count) venues", state.isAllVenues) { onPick(nil) }
                ForEach(state.venues, id: \.id) { venue in
                    option(venue.name, venue.location.isEmpty ? nil : venue.location, venue.id == state.selectedVenueId) {
                        onPick(venue.id)
                    }
                }
            }
            .padding(.top, 16).padding(.bottom, 24)
        }
        .presentationBackground(AppColor.card)
    }

    private func option(_ title: String, _ subtitle: String?, _ selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).fontWeight(selected ? .semibold : .regular).foregroundStyle(AppColor.text)
                    if let subtitle { Text(subtitle).font(.subheadline).foregroundStyle(AppColor.secondaryText) }
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark").foregroundStyle(AppColor.primary).accessibilityLabel("Selected")
                }
            }
            .padding(.horizontal, 16)
            .frame(minHeight: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Live tab: all-venues overview, or the selected venue's hub.
private struct LiveTab: View {
    @ObservedObject var shell: AppShellModel
    @Binding var path: NavigationPath
    @Binding var tab: AppTab

    var body: some View {
        let state = shell.state
        CardList(spacing: 12) {
            TabTitle(text: "Live")
            if state.isLoading {
                ProgressView().frame(maxWidth: .infinity)
            } else if state.venues.isEmpty {
                EmptyVenues { path.append(AppRoute.venues) }
            } else if let venue = state.selectedVenue {
                VStack(alignment: .leading, spacing: 12) {
                    Text(venue.name).font(.title2.weight(.semibold)).foregroundStyle(AppColor.text)
                    if !venue.location.isEmpty {
                        Text(venue.location).foregroundStyle(AppColor.secondaryText)
                    }
                    WideButton(title: "Count") {
                        path.append(AppRoute.count(venueId: venue.id, venueName: venue.name))
                    }
                    HStack(spacing: 8) {
                        WideButton(title: "Areas", filled: false) { path.append(AppRoute.areas(venueId: venue.id)) }
                        WideButton(title: "History", filled: false) { path.append(AppRoute.history(venueId: venue.id)) }
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(AppColor.card, in: RoundedRectangle(cornerRadius: 20))
                stats(open: venue.openIncidents, unclaimed: venue.unclaimedItems)
            } else {
                stats(open: state.openIncidents, unclaimed: state.unclaimedItems)
                SectionLabel(text: "Venues")
                ForEach(state.venues, id: \.id) { row in
                    Button { shell.select(row.id) } label: { VenueScopeCard(row: row) }
                        .buttonStyle(.plain)
                }
            }
        }
    }

    private func stats(open: Int32, unclaimed: Int32) -> some View {
        HStack(spacing: 8) {
            StatCard(label: "Open incidents", value: "\(open)") { tab = .incidents }
            StatCard(label: "Unclaimed items", value: "\(unclaimed)") { tab = .lostItems }
        }
    }
}

/// Count tab: pick the venue to count (only the scoped one when a venue is selected).
private struct CountTab: View {
    @ObservedObject var shell: AppShellModel
    @Binding var path: NavigationPath

    var body: some View {
        let state = shell.state
        let rows = state.selectedVenue.map { [$0] } ?? state.venues
        CardList(spacing: 12) {
            TabTitle(text: "Count")
            if state.isLoading {
                ProgressView().frame(maxWidth: .infinity)
            } else if rows.isEmpty {
                EmptyVenues { path.append(AppRoute.venues) }
            } else {
                if state.isAllVenues { SectionLabel(text: "Pick a venue") }
                ForEach(rows, id: \.id) { row in
                    ListRow(title: row.name, subtitle: row.location.isEmpty ? nil : row.location) {
                        path.append(AppRoute.count(venueId: row.id, venueName: row.name))
                    }
                }
            }
        }
    }
}

/// More tab: records, coordinator setup and app settings.
private struct MoreTab: View {
    @ObservedObject var shell: AppShellModel
    @Binding var path: NavigationPath

    var body: some View {
        let venue = shell.state.selectedVenue
        CardList(spacing: 8) {
            TabTitle(text: "More")
            SectionLabel(text: "Records")
            ListRow(title: "History") { path.append(AppRoute.history(venueId: venue?.id)) }
            ListRow(title: "Trends") { path.append(AppRoute.trends(venueId: venue?.id)) }
            ListRow(title: "Reports") { path.append(AppRoute.reports) }
            SectionLabel(text: "Setup")
            ListRow(title: "Venues") { path.append(AppRoute.venues) }
            ListRow(
                title: "Areas & seat maps",
                subtitle: venue?.name ?? "Pick a venue at the top first",
                enabled: venue != nil
            ) {
                if let venue { path.append(AppRoute.areas(venueId: venue.id)) }
            }
            ListRow(title: "Event types") { path.append(AppRoute.eventTypes) }
        }
    }
}

private struct VenueScopeCard: View {
    let row: VenueScopeRow

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(row.name).font(.headline).foregroundStyle(AppColor.text)
            if !row.location.isEmpty {
                Text(row.location).font(.caption).foregroundStyle(AppColor.secondaryText)
            }
            HStack(spacing: 8) {
                chip(row.openIncidents, one: "open incident", many: "open incidents")
                chip(row.unclaimedItems, one: "unclaimed item", many: "unclaimed items")
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColor.card, in: RoundedRectangle(cornerRadius: 16))
    }

    private func chip(_ count: Int32, one: String, many: String) -> some View {
        Text("\(count) \(count == 1 ? one : many)")
            .font(.caption.weight(.medium))
            .foregroundStyle(AppColor.text)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(AppColor.cardHigh, in: Capsule())
    }
}

private struct EmptyVenues: View {
    let onSetUp: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("No venues yet").font(.headline).foregroundStyle(AppColor.text)
            Text("Add a venue, then its areas, to start counting.").foregroundStyle(AppColor.secondaryText)
            Button(action: onSetUp) {
                Text("Set up your first venue").font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppColor.onPrimary)
                    .padding(.horizontal, 24).frame(minHeight: 48)
                    .background(AppColor.primary, in: Capsule())
            }
            .buttonStyle(.plain)
        }
    }
}

#Preview {
    ContentView()
}
