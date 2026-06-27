import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            VenueListView()
                .tabItem { Label("Venues", systemImage: "building.2") }

            EventTypeManagementView()
                .tabItem { Label("Event Types", systemImage: "calendar") }
        }
    }
}

#Preview {
    ContentView()
}
