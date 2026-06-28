package com.eventmonitor.shared.di

import com.eventmonitor.shared.presentation.eventtypes.EventTypeManagementViewModel
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import com.eventmonitor.shared.presentation.incidents.IncidentListViewModel
import com.eventmonitor.shared.presentation.lostandfound.LostAndFoundViewModel
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

fun venueListViewModel(): VenueListViewModel = KoinPlatform.getKoin().get()

fun eventTypeManagementViewModel(): EventTypeManagementViewModel = KoinPlatform.getKoin().get()

fun countingViewModel(venueId: String, eventId: String?): CountingViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId, eventId) }

fun incidentListViewModel(venueId: String?): IncidentListViewModel =
    KoinPlatform.getKoin().get { parametersOf(venueId) }

fun lostAndFoundViewModel(locationId: String?): LostAndFoundViewModel =
    KoinPlatform.getKoin().get { parametersOf(locationId) }
