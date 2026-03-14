package com.swparks.ui.screens.events

import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.IGetFutureEventsFlowUseCase
import com.swparks.domain.usecase.IGetPastEventsFlowUseCase
import com.swparks.domain.usecase.ISyncFutureEventsUseCase
import com.swparks.domain.usecase.ISyncPastEventsUseCase
import com.swparks.ui.model.EventKind
import com.swparks.ui.state.EventsUIState
import com.swparks.ui.viewmodel.EventsViewModel
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockGetFutureEventsFlowUseCase = mockk<IGetFutureEventsFlowUseCase>(relaxed = true)
    private val mockSyncFutureEventsUseCase = mockk<ISyncFutureEventsUseCase>(relaxed = true)
    private val mockGetPastEventsFlowUseCase = mockk<IGetPastEventsFlowUseCase>(relaxed = true)
    private val mockSyncPastEventsUseCase = mockk<ISyncPastEventsUseCase>(relaxed = true)
    private val mockUserPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val mockCountriesRepository = mockk<CountriesRepository>(relaxed = true)
    private val mockUserNotifier = mockk<UserNotifier>(relaxed = true)
    private val mockLogger = mockk<Logger>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mockSyncFutureEventsUseCase() } returns Result.success(Unit)
        coEvery { mockSyncPastEventsUseCase() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
    }

    private fun createMockEvent(id: Long = 1L): Event {
        return Event(
            id = id,
            title = "Test Event $id",
            description = "Test Description",
            beginDate = "2024-01-01",
            countryID = 1,
            cityID = 1,
            preview = "",
            latitude = "0.0",
            longitude = "0.0",
            isCurrent = false,
            photos = emptyList(),
            author = createMockUser(),
            name = "Test Event $id"
        )
    }

    private fun createMockUser(id: Long = 1L): User {
        return User(id = id, name = "testuser", image = "")
    }

    private fun createViewModel(): EventsViewModel {
        return EventsViewModel(
            getFutureEventsFlowUseCase = mockGetFutureEventsFlowUseCase,
            syncFutureEventsUseCase = mockSyncFutureEventsUseCase,
            getPastEventsFlowUseCase = mockGetPastEventsFlowUseCase,
            syncPastEventsUseCase = mockSyncPastEventsUseCase,
            userPreferencesRepository = mockUserPreferencesRepository,
            countriesRepository = mockCountriesRepository,
            userNotifier = mockUserNotifier,
            logger = mockLogger
        )
    }

    @Test
    fun init_syncsFutureEvents() = runTest {
        val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(mockEventsList)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        coVerify { mockSyncFutureEventsUseCase() }
        val state = viewModel.eventsUIState.value
        assertTrue("State should be Content but was $state", state is EventsUIState.Content)
        val contentState = state as EventsUIState.Content
        assertEquals(EventKind.FUTURE, contentState.selectedTab)
        assertEquals(mockEventsList, contentState.events)
        assertFalse(contentState.isLoading)
    }

    @Test
    fun onTabSelected_past_loadsPastEventsFromCache() = runTest {
        val mockPastEventsList = listOf(createMockEvent(1L))
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(mockPastEventsList)
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        assertEquals(EventKind.PAST, viewModel.selectedTab.value)
    }

    @Test
    fun onTabSelected_future_showsCachedEvents() = runTest {
        val mockFutureEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(mockFutureEventsList)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        viewModel.onTabSelected(EventKind.FUTURE)
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals(EventKind.FUTURE, state.selectedTab)
        assertEquals(mockFutureEventsList, state.events)
    }

    @Test
    fun refresh_future_callsSync() = runTest {
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(atLeast = 2) { mockSyncFutureEventsUseCase() }
    }

    @Test
    fun refresh_past_callsSync() = runTest {
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(atLeast = 2) { mockSyncPastEventsUseCase() }
    }

    @Test
    fun onEventClick_logsEvent() = runTest {
        val mockEvent = createMockEvent(1L)
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEventClick(mockEvent)
        advanceUntilIdle()

        verify { mockLogger.d("EventsViewModel", "Нажато мероприятие: Test Event 1 (id=1)") }
    }

    @Test
    fun onFabClick_logsAction() = runTest {
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onFabClick()
        advanceUntilIdle()

        verify { mockLogger.d("EventsViewModel", "Нажатие FAB: создание мероприятия") }
    }

    @Test
    fun isAuthorized_reflectsUserPreferences() = runTest {
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.isAuthorized.value)
    }

    @Test
    fun errorHandling_syncFutureError_showsError() = runTest {
        val ioException = IOException("Network error")
        coEvery { mockSyncFutureEventsUseCase() } returns Result.failure(ioException)
        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value
        assertTrue("State should be Error but was $state", state is EventsUIState.Error)
        val errorState = state as EventsUIState.Error
        assertEquals("Network error", errorState.message)
    }

    @Test
    fun loadAddresses_whenEventsExist_shouldLoadUniqueAddresses() = runTest {
        val events = listOf(
            createMockEvent(1L).copy(countryID = 1, cityID = 1),
            createMockEvent(2L).copy(countryID = 1, cityID = 2),
            createMockEvent(3L).copy(countryID = 2, cityID = 3)
        )
        val mockCountry1 = mockk<Country> { every { name } returns "Россия" }
        val mockCountry2 = mockk<Country> { every { name } returns "США" }
        val mockCity1 = mockk<City> { every { name } returns "Москва" }
        val mockCity2 = mockk<City> { every { name } returns "Санкт-Петербург" }
        val mockCity3 = mockk<City> { every { name } returns "Нью-Йорк" }

        every { mockGetFutureEventsFlowUseCase() } returns flowOf(events)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry1
        coEvery { mockCountriesRepository.getCountryById("2") } returns mockCountry2
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity1
        coEvery { mockCountriesRepository.getCityById("2") } returns mockCity2
        coEvery { mockCountriesRepository.getCityById("3") } returns mockCity3
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия, Москва", state.addresses[1 to 1])
        assertEquals("Россия, Санкт-Петербург", state.addresses[1 to 2])
        assertEquals("США, Нью-Йорк", state.addresses[2 to 3])
    }

    @Test
    fun loadAddresses_whenCountryAndCityExist_shouldReturnFormattedAddress() = runTest {
        val events = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val mockCountry = mockk<Country> { every { name } returns "Россия" }
        val mockCity = mockk<City> { every { name } returns "Москва" }

        every { mockGetFutureEventsFlowUseCase() } returns flowOf(events)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия, Москва", state.addresses[1 to 1])
    }

    @Test
    fun loadAddresses_whenOnlyCountryExists_shouldReturnCountryName() = runTest {
        val events = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val mockCountry = mockk<Country> { every { name } returns "Россия" }

        every { mockGetFutureEventsFlowUseCase() } returns flowOf(events)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry
        coEvery { mockCountriesRepository.getCityById("1") } returns null
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия", state.addresses[1 to 1])
    }

    @Test
    fun loadAddresses_whenOnlyCityExists_shouldReturnCityName() = runTest {
        val events = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val mockCity = mockk<City> { every { name } returns "Москва" }

        every { mockGetFutureEventsFlowUseCase() } returns flowOf(events)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        coEvery { mockCountriesRepository.getCountryById("1") } returns null
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Москва", state.addresses[1 to 1])
    }

    @Test
    fun loadAddresses_whenErrorOccurs_shouldFallbackToIds() = runTest {
        val events = listOf(createMockEvent(1L).copy(countryID = 99, cityID = 88))

        every { mockGetFutureEventsFlowUseCase() } returns flowOf(events)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        coEvery { mockCountriesRepository.getCountryById("99") } throws IOException("Network error")
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("99, 88", state.addresses[99 to 88])
    }

    @Test
    fun onTabSelected_pastFirstTime_loadsAddressesBeforeShowingList() = runTest {
        val pastEvents = listOf(
            createMockEvent(1L).copy(countryID = 1, cityID = 1),
            createMockEvent(2L).copy(countryID = 2, cityID = 2)
        )
        val mockCountry1 = mockk<Country> { every { name } returns "Россия" }
        val mockCountry2 = mockk<Country> { every { name } returns "США" }
        val mockCity1 = mockk<City> { every { name } returns "Москва" }
        val mockCity2 = mockk<City> { every { name } returns "Нью-Йорк" }

        every { mockGetFutureEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(pastEvents)
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry1
        coEvery { mockCountriesRepository.getCountryById("2") } returns mockCountry2
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity1
        coEvery { mockCountriesRepository.getCityById("2") } returns mockCity2

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals(2, state.addresses.size)
        assertEquals("Россия, Москва", state.addresses[1 to 1])
        assertEquals("США, Нью-Йорк", state.addresses[2 to 2])
        assertFalse(state.isLoading)
    }

    @Test
    fun loadAddresses_whenCacheExists_shouldUseCache() = runTest {
        val events1 = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val events2 = listOf(
            createMockEvent(1L).copy(countryID = 1, cityID = 1),
            createMockEvent(2L).copy(countryID = 2, cityID = 2)
        )
        val mockCountry1 = mockk<Country> { every { name } returns "Россия" }
        val mockCountry2 = mockk<Country> { every { name } returns "США" }
        val mockCity1 = mockk<City> { every { name } returns "Москва" }
        val mockCity2 = mockk<City> { every { name } returns "Нью-Йорк" }

        every { mockGetFutureEventsFlowUseCase() } returns flowOf(events1) andThen flowOf(events2)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry1
        coEvery { mockCountriesRepository.getCountryById("2") } returns mockCountry2
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity1
        coEvery { mockCountriesRepository.getCityById("2") } returns mockCity2

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state1 = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия, Москва", state1.addresses[1 to 1])
        assertEquals(1, state1.addresses.size)

        coVerify(exactly = 1) { mockCountriesRepository.getCountryById("1") }
        coVerify(exactly = 1) { mockCountriesRepository.getCityById("1") }
    }
}
