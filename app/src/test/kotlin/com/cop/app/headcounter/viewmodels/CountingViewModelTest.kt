package com.cop.app.headcounter.viewmodels

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.cop.app.headcounter.data.local.entities.*
import com.cop.app.headcounter.domain.models.AreaType
import com.cop.app.headcounter.domain.models.ServiceType
import com.cop.app.headcounter.domain.repository.BranchRepository
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
import com.cop.app.headcounter.presentation.screens.counting.CountingViewModel
import com.cop.app.headcounter.repositories.FakeAreaCountRepository
import com.cop.app.headcounter.repositories.FakeServiceRepository
import com.cop.app.headcounter.utils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountingViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: CountingViewModel
    private lateinit var fakeServiceRepository: FakeServiceRepository
    private lateinit var fakeAreaCountRepository: FakeAreaCountRepository
    private lateinit var mockBranchRepository: BranchRepository
    private lateinit var mockServiceTypeRepository: ServiceTypeRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testBranchId = "test-branch-id"
    private val testServiceId = "test-service-id"

    @Before
    fun setup() {
        fakeServiceRepository = FakeServiceRepository()
        fakeAreaCountRepository = FakeAreaCountRepository()
        mockBranchRepository = mockk()
        mockServiceTypeRepository = mockk()

        // Create test data
        val testBranch = BranchEntity(
            id = testBranchId,
            name = "Test Branch",
            location = "Test Location",
            code = "TB"
        )

        val testArea = AreaTemplateEntity(
            id = "area-1",
            branchId = testBranchId,
            name = "Main Hall",
            type = "BAY",
            capacity = 100,
            displayOrder = 0
        )

        val branchWithAreas = BranchWithAreas(
            branch = testBranch,
            areas = listOf(testArea)
        )

        // Mock repositories
        coEvery { mockBranchRepository.getBranchById(testBranchId) } returns flowOf(branchWithAreas)
        coEvery { mockServiceTypeRepository.getAllServiceTypes() } returns flowOf(emptyList())

        savedStateHandle = SavedStateHandle(mapOf("branchId" to testBranchId))
    }

    private fun createViewModel(): CountingViewModel {
        return CountingViewModel(
            branchRepository = mockBranchRepository,
            serviceRepository = fakeServiceRepository,
            serviceTypeRepository = mockServiceTypeRepository,
            areaCountRepository = fakeAreaCountRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `initial state should load branch details`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Test Branch", state.branchName)
            assertEquals("TB", state.branchCode)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `createNewService should update state with service details`() = runTest {
        viewModel = createViewModel()

        // Setup test service
        val testService = ServiceEntity(
            id = testServiceId,
            branchId = testBranchId,
            date = System.currentTimeMillis(),
            countedBy = "Test Counter"
        )

        val serviceWithDetails = ServiceWithDetails(
            service = testService,
            branch = BranchEntity(
                id = testBranchId,
                name = "Test Branch",
                location = "Test Location",
                code = "TB"
            ),
        )

        fakeServiceRepository.addService(serviceWithDetails)

        viewModel.createNewService(
            serviceTypeId = "type-1",
            serviceTypeName = "Sunday Morning",
            date = testService.date,
            countedBy = "Test Counter"
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.serviceId)
            assertEquals("Sunday Morning", state.serviceName)
            assertEquals("Test Counter", state.counterName)
        }
    }

    @Test
    fun `incrementCount should update count and add to undo stack`() = runTest {
        savedStateHandle = SavedStateHandle(
            mapOf(
                "branchId" to testBranchId,
                "serviceId" to testServiceId
            )
        )
        viewModel = createViewModel()

        val testAreaCount = AreaCountEntity(
            id = "area-count-1",
            serviceId = testServiceId,
            areaTemplateId = "area-1",
            count = 5,
            capacity = 100
        )

        val testAreaTemplate = AreaTemplateEntity(
            id = "area-1",
            branchId = testBranchId,
            name = "Main Hall",
            type = "BAY",
            capacity = 100,
            displayOrder = 0
        )

        val areaCountWithTemplate = AreaCountWithTemplate(
            areaCount = testAreaCount,
            template = testAreaTemplate
        )

        fakeAreaCountRepository.setAreaCountsWithTemplates(listOf(areaCountWithTemplate))

        val testService = ServiceEntity(
            id = testServiceId,
            branchId = testBranchId,
            date = System.currentTimeMillis(),
            totalAttendance = 5,
            totalCapacity = 100
        )

        val serviceWithDetails = ServiceWithDetails(
            service = testService,
            branch = BranchEntity(
                id = testBranchId,
                name = "Test Branch",
                location = "Test Location",
                code = "TB"
            ),
        )

        fakeServiceRepository.addService(serviceWithDetails)

        viewModel.incrementCount("area-count-1", 2)

        viewModel.canUndo.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `undo should revert count change`() = runTest {
        savedStateHandle = SavedStateHandle(
            mapOf(
                "branchId" to testBranchId,
                "serviceId" to testServiceId
            )
        )
        viewModel = createViewModel()

        // Setup area count
        val testAreaCount = AreaCountEntity(
            id = "area-count-1",
            serviceId = testServiceId,
            areaTemplateId = "area-1",
            count = 5,
            capacity = 100
        )

        fakeServiceRepository.setAreaCount("area-count-1", 5)

        // Increment count
        viewModel.incrementCount("area-count-1", 2)

        // Undo
        viewModel.undo()

        viewModel.canUndo.test {
            assertFalse(awaitItem())
        }

        viewModel.canRedo.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `redo should reapply count change`() = runTest {
        savedStateHandle = SavedStateHandle(
            mapOf(
                "branchId" to testBranchId,
                "serviceId" to testServiceId
            )
        )
        viewModel = createViewModel()

        fakeServiceRepository.setAreaCount("area-count-1", 5)

        // Increment, then undo
        viewModel.incrementCount("area-count-1", 2)
        viewModel.undo()

        // Redo
        viewModel.redo()

        viewModel.canRedo.test {
            assertFalse(awaitItem())
        }

        viewModel.canUndo.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `lockService should prevent count changes`() = runTest {
        savedStateHandle = SavedStateHandle(
            mapOf(
                "branchId" to testBranchId,
                "serviceId" to testServiceId
            )
        )
        viewModel = createViewModel()

        val testService = ServiceEntity(
            id = testServiceId,
            branchId = testBranchId,
            date = System.currentTimeMillis(),
            isLocked = false
        )

        val serviceWithDetails = ServiceWithDetails(
            service = testService,
            branch = BranchEntity(
                id = testBranchId,
                name = "Test Branch",
                location = "Test Location",
                code = "TB"
            ),
        )

        fakeServiceRepository.addService(serviceWithDetails)

        viewModel.lockService()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isLocked)
        }
    }

    @Test
    fun `unlockService should allow count changes`() = runTest {
        savedStateHandle = SavedStateHandle(
            mapOf(
                "branchId" to testBranchId,
                "serviceId" to testServiceId
            )
        )
        viewModel = createViewModel()

        val testService = ServiceEntity(
            id = testServiceId,
            branchId = testBranchId,
            date = System.currentTimeMillis(),
            isLocked = true
        )

        val serviceWithDetails = ServiceWithDetails(
            service = testService,
            branch = BranchEntity(
                id = testBranchId,
                name = "Test Branch",
                location = "Test Location",
                code = "TB"
            ),
        )

        fakeServiceRepository.addService(serviceWithDetails)

        viewModel.unlockService()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLocked)
        }
    }

    @Test
    fun `incrementCount on locked service should not change count`() = runTest {
        savedStateHandle = SavedStateHandle(
            mapOf(
                "branchId" to testBranchId,
                "serviceId" to testServiceId
            )
        )
        viewModel = createViewModel()

        val testService = ServiceEntity(
            id = testServiceId,
            branchId = testBranchId,
            date = System.currentTimeMillis(),
            isLocked = true
        )

        val serviceWithDetails = ServiceWithDetails(
            service = testService,
            branch = BranchEntity(
                id = testBranchId,
                name = "Test Branch",
                location = "Test Location",
                code = "TB"
            ),
        )

        fakeServiceRepository.addService(serviceWithDetails)

        viewModel.incrementCount("area-count-1", 1)

        viewModel.canUndo.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `setCount should update count with custom value`() = runTest {
        savedStateHandle = SavedStateHandle(
            mapOf(
                "branchId" to testBranchId,
                "serviceId" to testServiceId
            )
        )
        viewModel = createViewModel()

        val testAreaCount = AreaCountEntity(
            id = "area-count-1",
            serviceId = testServiceId,
            areaTemplateId = "area-1",
            count = 5,
            capacity = 100
        )

        val testAreaTemplate = AreaTemplateEntity(
            id = "area-1",
            branchId = testBranchId,
            name = "Main Hall",
            type = "BAY",
            capacity = 100,
            displayOrder = 0
        )

        val areaCountWithTemplate = AreaCountWithTemplate(
            areaCount = testAreaCount,
            template = testAreaTemplate
        )

        fakeAreaCountRepository.setAreaCountsWithTemplates(listOf(areaCountWithTemplate))
        fakeServiceRepository.setAreaCount("area-count-1", 5)

        viewModel.setCount("area-count-1", 42)

        viewModel.canUndo.test {
            assertTrue(awaitItem())
        }
    }
}
