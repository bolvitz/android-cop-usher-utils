package com.eventmonitor.app.presentation.navigation

/**
 * App navigation routes. Every screen is backed by the shared KMP module
 * (Koin-resolved ViewModels over the single shared Room database).
 */
sealed class Screen(val route: String) {
    object SharedVenueList : Screen("shared_venue_list")

    object SharedAreaManagement : Screen("shared_area_management/{venueId}") {
        fun createRoute(venueId: String) = "shared_area_management/$venueId"
    }

    object SharedSeatMapEditor : Screen("shared_seat_map/{areaId}") {
        fun createRoute(areaId: String) = "shared_seat_map/$areaId"
    }

    object SharedVenueEdit : Screen("shared_venue_edit/{venueId}") {
        fun createRoute(venueId: String) = "shared_venue_edit/$venueId"
    }

    object SharedEventTypes : Screen("shared_event_types")

    object SharedReports : Screen("shared_reports")

    object SharedCounting : Screen("shared_counting/{venueId}") {
        fun createRoute(venueId: String) = "shared_counting/$venueId"
    }

    object SharedHistory : Screen("shared_history?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "shared_history?venueId=$venueId" else "shared_history"
    }

    object SharedTrends : Screen("shared_trends?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "shared_trends?venueId=$venueId" else "shared_trends"
    }

    object SharedIncidents : Screen("shared_incidents?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "shared_incidents?venueId=$venueId" else "shared_incidents"
    }

    object SharedIncidentDetail : Screen("shared_incident/{incidentId}") {
        fun createRoute(incidentId: String) = "shared_incident/$incidentId"
    }

    object SharedLostAndFound : Screen("shared_lost_and_found?locationId={locationId}") {
        fun createRoute(locationId: String? = null) =
            if (locationId != null) "shared_lost_and_found?locationId=$locationId" else "shared_lost_and_found"
    }

    object SharedLostItemDetail : Screen("shared_lost_item/{itemId}") {
        fun createRoute(itemId: String) = "shared_lost_item/$itemId"
    }
}
