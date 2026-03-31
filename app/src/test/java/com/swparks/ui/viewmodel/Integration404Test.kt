package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.swparks.R
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Event
import com.swparks.data.model.Park
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.exception.NotFoundException
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.DeleteEventUseCase
import com.swparks.domain.usecase.DeleteParkUseCase
import com.swparks.domain.usecase.IGetFutureEventsFlowUseCase
import com.swparks.domain.usecase.IGetPastEventsFlowUseCase
import com.swparks.domain.usecase.ISyncFutureEventsUseCase
import com.swparks.domain.usecase.ISyncPastEventsUseCase
import com.swparks.ui.state.EventsUIState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Integration404Test {

    private lateinit var swRepository: SWRepository
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var userNotifier: UserNotifier
    private lateinit var deleteParkUseCase: DeleteParkUseCase
    private lateinit var deleteEventUseCase: DeleteEventUseCase
    private lateinit var resourcesProvider: ResourcesProvider
    private val logger: Logger = NoOpLogger()

    private val testUser = User(
        id = 1L,
        name = "Test User",
        fullName = "Test User",
        email = "test@example.com",
        image = null,
        genderCode = 0,
        countryID = 1,
        cityID = 1,
        friendsCount = 10,
        friendRequestCount = "5",
        parksCount = "2",
        addedParks = emptyList(),
        journalCount = 2
    )

    private val testPark1 = Park(
        id = 100L,
        name = "Park 1",
        sizeID = 1,
        typeID = 1,
        longitude = "37.618423",
        latitude = "55.751244",
        address = "Moscow",
        cityID = 1,
        countryID = 1,
        commentsCount = 0,
        preview = "",
        trainingUsersCount = 0,
        createDate = "2026-03-13 12:00:00",
        modifyDate = null,
        author = testUser,
        photos = emptyList(),
        comments = emptyList(),
        trainHere = false,
        equipmentIDS = null,
        mine = true,
        canEdit = true,
        trainingUsers = emptyList()
    )

    private val testPark2 = Park(
        id = 200L,
        name = "Park 2",
        sizeID = 1,
        typeID = 1,
        longitude = "37.618423",
        latitude = "55.751244",
        address = "Moscow",
        cityID = 1,
        countryID = 1,
        commentsCount = 0,
        preview = "",
        trainingUsersCount = 0,
        createDate = "2026-03-13 12:00:00",
        modifyDate = null,
        author = testUser,
        photos = emptyList(),
        comments = emptyList(),
        trainHere = false,
        equipmentIDS = null,
        mine = true,
        canEdit = true,
        trainingUsers = emptyList()
    )

    private val testEvent1 = Event(
        id = 100L,
        title = "Event 1",
        description = "Description",
        beginDate = "2026-01-01",
        countryID = 1,
        cityID = 1,
        preview = "",
        latitude = "0.0",
        longitude = "0.0",
        isCurrent = true,
        photos = emptyList(),
        author = testUser,
        name = "Event 1"
    )

    private val testEvent2 = Event(
        id = 200L,
        title = "Event 2",
        description = "Description",
        beginDate = "2026-01-02",
        countryID = 1,
        cityID = 1,
        preview = "",
        latitude = "0.0",
        longitude = "0.0",
        isCurrent = true,
        photos = emptyList(),
        author = testUser,
        name = "Event 2"
    )

    @Before
    fun setUp() {
        swRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        deleteParkUseCase = mockk(relaxed = true)
        deleteEventUseCase = mockk(relaxed = true)
        resourcesProvider = mockk(relaxed = true)

        every { userPreferencesRepository.isAuthorized } returns flowOf(true)
        every { userPreferencesRepository.currentUserId } returns flowOf(1L)

        coEvery { countriesRepository.getCountryById(any()) } returns null
        coEvery { countriesRepository.getCityById(any()) } returns null

        every { resourcesProvider.getString(R.string.error_server_not_found) } returns "Ресурс не найден на сервере"
    }

    // ==================== Park 404 Integration Test ====================

    /**
     * Integration test: when ParkDetailViewModel receives 404 for a park:
     * 1. deleteParkUseCase is called
     * 2. userNotifier receives ResourceNotFound error
     * 3. NavigateBack event is emitted
     * 4. ProfileViewModel's currentUser flow reflects the park removal
     */
    @Test
    fun parkDetail_404_whenParkNotFound_deletesAndUpdatesParksList() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val userParksFlow =
            MutableStateFlow(testUser.copy(addedParks = listOf(testPark1, testPark2)))

        every { swRepository.getCurrentUserFlow() } returns userParksFlow
        coEvery { swRepository.getPark(testPark1.id) } returns Result.failure(
            NotFoundException.ParkNotFound(testPark1.id)
        )
        coEvery { deleteParkUseCase.invoke(testPark1.id) } coAnswers {
            userParksFlow.value = testUser.copy(addedParks = listOf(testPark2))
            Result.success(Unit)
        }

        val parksViewModel = ProfileViewModel(
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier
        )

        advanceUntilIdle()

        val savedStateHandle = SavedStateHandle(mapOf("parkId" to testPark1.id))
        val parkDetailViewModel = ParkDetailViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            userPreferencesRepository = userPreferencesRepository,
            savedStateHandle = savedStateHandle,
            userNotifier = userNotifier,
            logger = logger,
            deleteParkUseCase = deleteParkUseCase,
            resourcesProvider = resourcesProvider
        )

        parkDetailViewModel.events.test {
            runCurrent()
            val event = awaitItem()
            assertTrue("Expected NavigateBack event", event is ParkDetailEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { deleteParkUseCase.invoke(testPark1.id) }
        verify {
            userNotifier.handleError(
                AppError.ResourceNotFound(
                    message = "Ресурс не найден на сервере",
                    resourceType = AppError.ResourceType.PARK
                )
            )
        }

        advanceUntilIdle()

        val currentUser = parksViewModel.currentUser.value
        val addedParks = currentUser?.addedParks ?: emptyList()
        assertEquals("Parks list should have only 1 park after deletion", 1, addedParks.size)
        assertEquals("Remaining park should be testPark2", testPark2.id, addedParks.first().id)
    }

    // ==================== Event 404 Integration Test ====================

    /**
     * Integration test: when EventDetailViewModel receives 404 for an event:
     * 1. deleteEventUseCase is called
     * 2. userNotifier receives ResourceNotFound error
     * 3. NavigateBack event is emitted
     * 4. EventsViewModel's events list reflects the event removal
     */
    @Test
    fun eventDetail_404_whenEventNotFound_deletesAndUpdatesEventsList() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val futureEventsFlow = MutableStateFlow(listOf(testEvent1, testEvent2))

        val mockGetFutureEventsFlowUseCase = mockk<IGetFutureEventsFlowUseCase>(relaxed = true)
        val mockSyncFutureEventsUseCase = mockk<ISyncFutureEventsUseCase>(relaxed = true)
        val mockGetPastEventsFlowUseCase = mockk<IGetPastEventsFlowUseCase>(relaxed = true)
        val mockSyncPastEventsUseCase = mockk<ISyncPastEventsUseCase>(relaxed = true)

        every { mockGetFutureEventsFlowUseCase() } returns futureEventsFlow
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        coEvery { mockSyncFutureEventsUseCase() } returns Result.success(Unit)
        coEvery { mockSyncPastEventsUseCase() } returns Result.success(Unit)
        every { userPreferencesRepository.isAuthorized } returns flowOf(false)

        coEvery { swRepository.getEvent(testEvent1.id) } returns Result.failure(
            NotFoundException.EventNotFound(testEvent1.id)
        )
        coEvery { deleteEventUseCase.invoke(testEvent1.id) } coAnswers {
            futureEventsFlow.value = futureEventsFlow.value.filter { it.id != testEvent1.id }
            Result.success(Unit)
        }

        val eventsViewModel = EventsViewModel(
            getFutureEventsFlowUseCase = mockGetFutureEventsFlowUseCase,
            syncFutureEventsUseCase = mockSyncFutureEventsUseCase,
            getPastEventsFlowUseCase = mockGetPastEventsFlowUseCase,
            syncPastEventsUseCase = mockSyncPastEventsUseCase,
            userPreferencesRepository = userPreferencesRepository,
            countriesRepository = countriesRepository,
            userNotifier = userNotifier,
            logger = logger,
            swRepository = swRepository
        )

        advanceUntilIdle()

        val eventsStateBefore = eventsViewModel.eventsUIState.value
        assertTrue(
            "EventsViewModel should be Content before deletion",
            eventsStateBefore is EventsUIState.Content
        )
        val contentBefore = eventsStateBefore as EventsUIState.Content
        assertEquals("Should have 2 events before deletion", 2, contentBefore.events.size)

        val savedStateHandle = SavedStateHandle(mapOf("eventId" to testEvent1.id))
        val eventDetailViewModel = EventDetailViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            userPreferencesRepository = userPreferencesRepository,
            savedStateHandle = savedStateHandle,
            userNotifier = userNotifier,
            logger = logger,
            deleteEventUseCase = deleteEventUseCase,
            resourcesProvider = resourcesProvider
        )

        eventDetailViewModel.events.test {
            runCurrent()
            val event = awaitItem()
            assertTrue("Expected NavigateBack event", event is EventDetailEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()

        coVerify { deleteEventUseCase.invoke(testEvent1.id) }
        verify {
            userNotifier.handleError(
                AppError.ResourceNotFound(
                    message = "Ресурс не найден на сервере",
                    resourceType = AppError.ResourceType.EVENT
                )
            )
        }

        val eventsStateAfter = eventsViewModel.eventsUIState.value
        assertTrue(
            "EventsViewModel should still be Content after deletion",
            eventsStateAfter is EventsUIState.Content
        )
        val contentAfter = eventsStateAfter as EventsUIState.Content
        assertEquals("Should have only 1 event after deletion", 1, contentAfter.events.size)
        assertEquals(
            "Remaining event should be testEvent2",
            testEvent2.id,
            contentAfter.events.first().id
        )
    }
}
