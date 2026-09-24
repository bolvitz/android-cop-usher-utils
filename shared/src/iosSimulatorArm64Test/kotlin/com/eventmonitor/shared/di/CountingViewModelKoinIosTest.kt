package com.eventmonitor.shared.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.eventmonitor.shared.data.local.database.AppDatabase
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.test.*

/** Resolves CountingViewModel the way both apps do: parametersOf(venueId, null). */
@OptIn(ExperimentalCoroutinesApi::class)
class CountingViewModelKoinIosTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default.limitedParallelism(1))
        val testDb = module {
            single<AppDatabase> {
                Room.inMemoryDatabaseBuilder<AppDatabase>()
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.Default)
                    .build()
            }
        }
        startKoin { modules(testDb, dataModule, viewModelModule) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun nullEventIdIsNotResolvedAsVenueId() {
        val vm: CountingViewModel = KoinPlatform.getKoin().get { parametersOf("venue-1", null) }
        assertNull(vm.uiState.value.eventId, "a null event id must not resolve to the venue id")
        vm.dispose()
    }

    @Test
    fun explicitEventIdIsPassedThrough() {
        val vm: CountingViewModel =
            KoinPlatform.getKoin().get { parametersOf("venue-1", "event-9") }
        assertEquals("event-9", vm.uiState.value.eventId)
        vm.dispose()
    }
}
