package com.eventmonitor.app.presentation.navigation

sealed class Screen(val route: String) {
    object BranchList : Screen("branch_list")
    object BranchSetup : Screen("branch_setup/{branchId}") {
        fun createRoute(branchId: String = "new") = "branch_setup/$branchId"
    }
    object Counting : Screen("counting/{branchId}?serviceId={serviceId}") {
        fun createRoute(branchId: String, serviceId: String? = null) =
            if (serviceId != null) "counting/$branchId?serviceId=$serviceId"
            else "counting/$branchId"
    }
    object History : Screen("history?branchId={branchId}") {
        fun createRoute(branchId: String? = null) =
            if (branchId != null) "history?branchId=$branchId"
            else "history"
    }
    object HistoryDetail : Screen("history/{serviceId}") {
        fun createRoute(serviceId: String) = "history/$serviceId"
    }
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object AreaManagement : Screen("area_management/{branchId}") {
        fun createRoute(branchId: String) = "area_management/$branchId"
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
    object IncidentList : Screen("incidents?branchId={branchId}") {
        fun createRoute(branchId: String? = null) =
            if (branchId != null) "incidents?branchId=$branchId"
            else "incidents"
    }
    object AddEditIncident : Screen("add_edit_incident/{branchId}?incidentId={incidentId}") {
        fun createRoute(branchId: String, incidentId: String? = null) =
            if (incidentId != null) "add_edit_incident/$branchId?incidentId=$incidentId"
            else "add_edit_incident/$branchId"
    }
    object IncidentDetail : Screen("incident/{incidentId}") {
        fun createRoute(incidentId: String) = "incident/$incidentId"
    }
}
