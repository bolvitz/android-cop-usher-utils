package com.eventmonitor.app.presentation.navigation

sealed class Screen(val route: String) {
    object VenueList : Screen("venue_list")
    // Shared-DB venue hub (app entry point) + venue-scoped management on :shared.
    object SharedVenueList : Screen("shared_venue_list")
    object SharedAreaManagement : Screen("shared_area_management/{venueId}") {
        fun createRoute(venueId: String) = "shared_area_management/$venueId"
    }
    object SharedEventTypes : Screen("shared_event_types")
    object VenueSetup : Screen("venue_setup/{venueId}") {
        fun createRoute(venueId: String = "new") = "venue_setup/$venueId"
    }
    object Counting : Screen("counting/{venueId}?serviceId={serviceId}") {
        fun createRoute(venueId: String, serviceId: String? = null) =
            if (serviceId != null) "counting/$venueId?serviceId=$serviceId"
            else "counting/$venueId"
    }
    // Head counter backed by the shared KMP CountingViewModel (resolved via Koin).
    object SharedCounting : Screen("shared_counting/{venueId}") {
        fun createRoute(venueId: String) = "shared_counting/$venueId"
    }
    // Incidents backed by the shared KMP IncidentListViewModel (resolved via Koin).
    object SharedIncidents : Screen("shared_incidents?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "shared_incidents?venueId=$venueId" else "shared_incidents"
    }
    // Lost & found backed by the shared KMP LostAndFoundViewModel (resolved via Koin).
    object SharedLostAndFound : Screen("shared_lost_and_found?locationId={locationId}") {
        fun createRoute(locationId: String? = null) =
            if (locationId != null) "shared_lost_and_found?locationId=$locationId" else "shared_lost_and_found"
    }
    // History & trends backed by the shared KMP ViewModels (resolved via Koin).
    object SharedHistory : Screen("shared_history?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "shared_history?venueId=$venueId" else "shared_history"
    }
    object SharedTrends : Screen("shared_trends?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "shared_trends?venueId=$venueId" else "shared_trends"
    }
    object History : Screen("history?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "history?venueId=$venueId"
            else "history"
    }
    object HistoryDetail : Screen("history/{serviceId}") {
        fun createRoute(serviceId: String) = "history/$serviceId"
    }
    object Reports : Screen("reports")
    object AreaManagement : Screen("area_management/{venueId}") {
        fun createRoute(venueId: String) = "area_management/$venueId"
    }
    object ZoneEditor : Screen("zone_editor/{venueId}?zoneId={zoneId}&mode={mode}") {
        const val MODE_SOLO = "solo"
        const val MODE_BATCH = "batch"
        const val MODE_EDIT = "edit"
        fun createRoute(venueId: String, zoneId: String? = null, mode: String = MODE_SOLO): String {
            val resolvedMode = if (zoneId != null) MODE_EDIT else mode
            return if (zoneId != null) "zone_editor/$venueId?zoneId=$zoneId&mode=$resolvedMode"
            else "zone_editor/$venueId?mode=$resolvedMode"
        }
    }
    object ServiceTypeManagement : Screen("service_type_management")
    object VenueManagement : Screen("venue_management")
    object LostAndFound : Screen("lost_and_found?locationId={locationId}") {
        fun createRoute(locationId: String? = null) =
            if (locationId != null) "lost_and_found?locationId=$locationId"
            else "lost_and_found"
    }
    object AddEditLostItem : Screen("add_edit_lost_item/{locationId}?itemId={itemId}") {
        fun createRoute(locationId: String, itemId: String? = null) =
            if (itemId != null) "add_edit_lost_item/$locationId?itemId=$itemId"
            else "add_edit_lost_item/$locationId"
    }
    object LostItemDetail : Screen("lost_item/{itemId}") {
        fun createRoute(itemId: String) = "lost_item/$itemId"
    }
    object IncidentList : Screen("incidents?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "incidents?venueId=$venueId"
            else "incidents"
    }
    object AddEditIncident : Screen("add_edit_incident/{venueId}?incidentId={incidentId}") {
        fun createRoute(venueId: String, incidentId: String? = null) =
            if (incidentId != null) "add_edit_incident/$venueId?incidentId=$incidentId"
            else "add_edit_incident/$venueId"
    }
    object IncidentDetail : Screen("incident/{incidentId}") {
        fun createRoute(incidentId: String) = "incident/$incidentId"
    }
    object Trends : Screen("trends?venueId={venueId}") {
        fun createRoute(venueId: String? = null) =
            if (venueId != null) "trends?venueId=$venueId"
            else "trends"
    }
    object SeatMapDemo : Screen("seat_map_demo")
}
