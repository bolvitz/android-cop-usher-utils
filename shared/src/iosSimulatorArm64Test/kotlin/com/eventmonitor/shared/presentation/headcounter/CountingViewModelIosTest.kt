package com.eventmonitor.shared.presentation.headcounter

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.eventmonitor.shared.data.local.database.AppDatabase
import com.eventmonitor.shared.data.models.AreaTemplateDto
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.data.repository.*
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.*

/** Runs the head counter against a real in-memory Room database on the iOS target. */
@OptIn(ExperimentalCoroutinesApi::class)
class CountingViewModelIosTest {
    private lateinit var db: AppDatabase
    private lateinit var vm: CountingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default.limitedParallelism(1))
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @AfterTest
    fun tearDown() {
        if (::vm.isInitialized) vm.dispose()
        db.close()
        Dispatchers.resetMain()
    }

    /** Creates a venue with one area, starts a count and returns the area-count id. */
    private suspend fun startCount(): String {
        val venues = VenueRepositoryImpl(db.venueDao(), db.areaTemplateDao())
        val venueId =
            (venues.createVenue(VenueDto(name = "Venue", code = "V")) as Result.Success).data
        venues.createAreaTemplate(AreaTemplateDto(venueId = venueId, name = "Hall", capacity = 100))
        vm = CountingViewModel(
            venueRepository = venues,
            eventRepository = EventRepositoryImpl(db.eventDao()),
            eventTypeRepository = EventTypeRepositoryImpl(db.eventTypeDao()),
            areaCountRepository = AreaCountRepositoryImpl(db.areaCountDao(), db.eventDao()),
            seatMapRepository = SeatMapRepositoryImpl(
                db.seatRowDao(),
                db.seatStatusDao(),
                db.seatDao(),
                db.areaTemplateDao()
            ),
            venueId = venueId
        )
        awaitState("venue loaded") { !it.isLoading && it.eventId == null }
        vm.createNewEvent(null, "Head Count", 1L, "test")
        return awaitState("count started") { it.eventId != null && it.areaCounts.isNotEmpty() }.areaCounts.single().id
    }

    @Test
    fun incrementDecrementUndoRedo() = runBlocking<Unit> {
        val area = startCount()
        vm.incrementCount(area, 1); awaitState("+1") { it.totalAttendance == 1 }
        vm.incrementCount(area, 1); awaitState("+2") { it.totalAttendance == 2 }
        vm.decrementCount(area, 1); awaitState("-1") { it.totalAttendance == 1 }
        vm.undo(); awaitState("undo") { it.totalAttendance == 2 }
        vm.redo(); awaitState("redo") { it.totalAttendance == 1 }
    }

    @Test
    fun undoImmediatelyAfterTapRevertsThatTap() = runBlocking<Unit> {
        val area = startCount()
        vm.incrementCount(area, 1); awaitState("+1") { it.totalAttendance == 1 }
        // No wait between the tap and undo: undo must revert this tap, not the previous one.
        vm.incrementCount(area, 1)
        vm.undo()
        delay(500)
        assertEquals(1, awaitState("undo") { it.totalAttendance == 1 }.totalAttendance)
        vm.redo(); awaitState("redo") { it.totalAttendance == 2 }
    }

    @Test
    fun lockedCountIgnoresChangesAndUndo() = runBlocking<Unit> {
        val area = startCount()
        vm.incrementCount(area, 1); awaitState("+1") { it.totalAttendance == 1 }
        vm.lockEvent(); awaitState("locked") { it.isLocked }
        vm.incrementCount(area, 1)
        vm.undo()
        delay(500)
        assertEquals(1, vm.uiState.value.totalAttendance)
    }

    @Test
    fun excludedAreaDropsOutOfTotal() = runBlocking<Unit> {
        val area = startCount()
        vm.incrementCount(area, 3); awaitState("+3") { it.totalAttendance == 3 }
        vm.toggleAreaInclusion(area); awaitState("excluded") { it.totalAttendance == 0 && it.totalCapacity == 0 }
        vm.toggleAreaInclusion(area); awaitState("included") { it.totalAttendance == 3 }
    }

    private suspend fun awaitState(
        step: String,
        predicate: (CountingUiState) -> Boolean
    ): CountingUiState =
        try {
            withTimeout(5_000) { vm.uiState.first(predicate) }
        } catch (e: TimeoutCancellationException) {
            fail("$step timed out; last state = ${vm.uiState.value}")
        }
}
