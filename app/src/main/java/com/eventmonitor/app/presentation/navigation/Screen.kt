package com.eventmonitor.app.presentation.navigation

sealed class Screen(val route: String) {
    object VenueList : Screen("venue_list")
    object VenueSetup : Screen("venue_setup/{venueId}") {
        fun createRoute(venueId: String = "new") = "venue_setup/$venueId"
    }
    object Counting : Screen("counting/{venueId}?serviceId={serviceId}") {
        fun createRoute(venueId: String, serviceId: String? = null) =
            if (serviceId != null) "counting/$venueId?serviceId=$serviceId"
            else "counting/$venueId"
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
    object Settings : Screen("settings")
    object AreaManagement : Screen("area_management/{venueId}") {
        fun createRoute(venueId: String) = "area_management/$venueId"
    }
    object ServiceTypeManagement : Screen("service_type_management")
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
}
