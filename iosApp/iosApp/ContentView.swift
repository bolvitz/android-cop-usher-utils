import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            VenueListView()
                .tabItem { Label("Venues", systemImage: "building.2") }

            EventTypeManagementView()
                .tabItem { Label("Event Types", systemImage: "calendar") }

            IncidentListView()
                .tabItem { Label("Incidents", systemImage: "exclamationmark.triangle") }

            LostAndFoundView()
                .tabItem { Label("Lost & Found", systemImage: "bag") }
        }
    }
}

#Preview {
    ContentView()
}
