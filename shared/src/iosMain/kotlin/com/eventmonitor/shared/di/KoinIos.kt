package com.eventmonitor.shared.di

import com.eventmonitor.shared.presentation.areas.AreaManagementViewModel
import com.eventmonitor.shared.presentation.areas.SeatMapEditorViewModel
import com.eventmonitor.shared.presentation.eventtypes.EventTypeManagementViewModel
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import com.eventmonitor.shared.presentation.headcounter.HistoryViewModel
import com.eventmonitor.shared.presentation.headcounter.TrendsViewModel
import com.eventmonitor.shared.presentation.incidents.IncidentDetailViewModel
import com.eventmonitor.shared.presentation.incidents.IncidentListViewModel
import com.eventmonitor.shared.presentation.lostandfound.LostAndFoundViewModel
import com.eventmonitor.shared.presentation.lostandfound.LostItemDetailViewModel
import com.eventmonitor.shared.presentation.scope.AppShellViewModel
import com.eventmonitor.shared.presentation.venues.VenueDetailViewModel
import com.eventmonitor.shared.presentation.venues.VenueListViewModel
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/**
 * Entry points used by the SwiftUI app. Swift cannot call Koin's generic
 * `get<T>()`, so concrete factory helpers are exposed here.
 */
fun startKoinIos() {
    startKoin {
        modules(sharedModules)
    }
}

fun appShellViewModel(): AppShellViewModel = KoinPlatform.getKoin().get()

fun venueListViewModel(): VenueListViewModel = KoinPlatform.getKoin().get()

fun venueDetailViewModel(venueId: String): VenueDetailViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId) }

fun eventTypeManagementViewModel(): EventTypeManagementViewModel = KoinPlatform.getKoin().get()

fun areaManagementViewModel(venueId: String): AreaManagementViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId) }

fun seatMapEditorViewModel(areaTemplateId: String): SeatMapEditorViewModel =
    KoinPlatform.getKoin().get { parametersOf(areaTemplateId) }

fun countingViewModel(venueId: String, eventId: String?): CountingViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId, eventId) }

fun historyViewModel(venueId: String?): HistoryViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId) }

fun trendsViewModel(venueId: String?): TrendsViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId) }

fun incidentListViewModel(venueId: String?): IncidentListViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId) }

fun lostAndFoundViewModel(locationId: String?): LostAndFoundViewModel =
    KoinPlatform.getKoin().get { parametersOf(locationId) }

fun incidentDetailViewModel(incidentId: String): IncidentDetailViewModel =
    KoinPlatform.getKoin().get { parametersOf(incidentId) }

fun lostItemDetailViewModel(itemId: String): LostItemDetailViewModel =
    KoinPlatform.getKoin().get { parametersOf(itemId) }
