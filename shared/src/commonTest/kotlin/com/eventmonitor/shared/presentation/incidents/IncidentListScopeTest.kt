package com.eventmonitor.shared.presentation.incidents

import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.data.repository.IncidentRepository
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit under test: [IncidentListViewModel], the shared list behind the Incidents tab on Android and iOS.
 *
 * The Incidents tab keeps one [IncidentListViewModel] and calls [IncidentListViewModel.setVenue]
 * on a scope switch, instead of building a new ViewModel per venue (which leaked the old ones).
 * With "All venues" in scope, the add button asks which venue first and passes it to
 * [IncidentListViewModel.createIncident].
 *
 * The repository is a hand-written fake ([FakeIncidents]) rather than a mock: it filters a fixed
 * in-memory list the way the Room queries do, and records which venue each create targets.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentListScopeTest {
    // One incident per venue, so every scope maps to a distinct, predictable id list.
    private val incidents = listOf(
        IncidentDto(id = "1", venueId = "a"),
        IncidentDto(id = "2", venueId = "b")
    )

    // viewModelScope runs on Dispatchers.Main; an unconfined test dispatcher makes each launch run eagerly.
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    /**
     * Scenario: the user switches the venue scope while a search filter is active.
     * Expected: the same ViewModel re-filters to the new venue, clears the old filters, and
     * "All venues" (null) shows every incident.
     * Why: guards the leak fix; one ViewModel must serve every scope correctly.
     */
    @Test
    fun setVenue_refiltersInPlaceAndResetsFilters() = runTest(UnconfinedTestDispatcher()) {
        // Given: a list scoped to venue "a", collected so the WhileSubscribed state stays live.
        val vm = IncidentListViewModel(FakeIncidents(incidents), venueId = "a")
        val job = vm.uiState.launchIn(backgroundScope)

        // Then: only venue "a"'s incident shows.
        assertEquals(listOf("1"), vm.uiState.value.incidents.map { it.id })

        // When: a search is active and the scope moves to venue "b".
        vm.search("x")
        vm.setVenue("b")
        // Then: venue "b"'s incident shows and the search is gone, as on a fresh screen.
        assertEquals(listOf("2"), vm.uiState.value.incidents.map { it.id })
        assertEquals(IncidentFilters(), vm.uiState.value.filters)

        // When / Then: "All venues" lists both incidents.
        vm.setVenue(null)
        assertEquals(listOf("1", "2"), vm.uiState.value.incidents.map { it.id })
        job.cancel()
    }

    /**
     * Scenario: an incident is added from the "All venues" list and from a single-venue list.
     * Expected: with all venues in scope, the incident goes to the venue picked in the dialog, and
     * nothing is created without one; with a venue in scope, it goes to that venue.
     * Why: the venue picker is the only way to add while "All venues" is selected, so a lost or
     * wrong venue id would file the report under the wrong venue or drop it.
     */
    @Test
    fun createIncident_allVenuesUsesPickedVenue_scopedUsesScope() = runTest(UnconfinedTestDispatcher()) {
        // Given: one fake repository shared by an "All venues" list and a list scoped to venue "a".
        val repo = FakeIncidents(incidents)
        val all = IncidentListViewModel(repo, venueId = null)

        // When: "All venues" creates with no venue picked.
        all.createIncident(title = "t", description = "", severity = "LOW")
        // Then: nothing reaches the repository, since there is no venue to file it under.
        assertEquals(emptyList(), repo.createdFor, "no venue picked, nothing created")

        // When: "All venues" creates after the user picks venue "b", then the venue "a" list creates without one.
        all.createIncident(title = "t", description = "", severity = "LOW", venueId = "b")
        IncidentListViewModel(repo, venueId = "a").createIncident(title = "t", description = "", severity = "LOW")

        // Then: the picked venue wins on the first create; the scope supplies it on the second.
        assertEquals(listOf("b", "a"), repo.createdFor)
    }

    private class FakeIncidents(private val all: List<IncidentDto>) : IncidentRepository {
        /** Venue id of every create call, in order, so tests can assert where reports were filed. */
        val createdFor = mutableListOf<String>()
        override fun getAllIncidents(): Flow<List<IncidentDto>> = flowOf(all)
        override fun getIncidentsByVenue(venueId: String) = flowOf(all.filter { it.venueId == venueId })
        override fun getIncidentsByStatus(status: String) = flowOf(all.filter { it.status == status })
        override fun getIncidentsBySeverity(severity: String) = flowOf(all.filter { it.severity == severity })
        override fun getIncidentsByVenueAndStatus(venueId: String, status: String) =
            flowOf(all.filter { it.venueId == venueId && it.status == status })
        override fun getIncidentsByVenueAndSeverity(venueId: String, severity: String) =
            flowOf(all.filter { it.venueId == venueId && it.severity == severity })
        override fun searchIncidents(query: String) = flowOf(emptyList<IncidentDto>())
        override fun getIncidentById(incidentId: String) = flowOf(all.find { it.id == incidentId })
        override suspend fun createIncident(
            venueId: String, title: String, description: String, severity: String, category: String,
            location: String, reportedBy: String, notes: String, eventId: String?
        ): Result<String> {
            createdFor += venueId
            // The new id is never read; success keeps the ViewModel off its error path.
            return Result.Success("new")
        }
        override suspend fun updateIncident(incident: IncidentDto): Result<Unit> = error("unused")
        override suspend fun deleteIncident(incidentId: String): Result<Unit> = error("unused")
        override suspend fun updateIncidentStatus(incidentId: String, status: String): Result<Unit> = error("unused")
        override suspend fun assignIncident(incidentId: String, assignedTo: String, status: String): Result<Unit> =
            error("unused")
        override suspend fun resolveIncident(incidentId: String, status: String, actionsTaken: String): Result<Unit> =
            error("unused")
    }
}
