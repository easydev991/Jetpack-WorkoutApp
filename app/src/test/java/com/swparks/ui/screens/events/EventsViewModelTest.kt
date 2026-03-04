package com.swparks.ui.screens.events

import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.IGetFutureEventsUseCase
import com.swparks.domain.usecase.IGetPastEventsFlowUseCase
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
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockGetFutureEventsUseCase = mockk<IGetFutureEventsUseCase>(relaxed = true)
    private val mockGetPastEventsFlowUseCase = mockk<IGetPastEventsFlowUseCase>(relaxed = true)
    private val mockSyncPastEventsUseCase = mockk<ISyncPastEventsUseCase>(relaxed = true)
    private val mockUserPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val mockCountriesRepository = mockk<CountriesRepository>(relaxed = true)
    private val mockUserNotifier = mockk<UserNotifier>(relaxed = true)
    private val mockLogger = mockk<Logger>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mockSyncPastEventsUseCase() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        // resetMain не доступен в kotlinx-coroutines-test
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
            getFutureEventsUseCase = mockGetFutureEventsUseCase,
            getPastEventsFlowUseCase = mockGetPastEventsFlowUseCase,
            syncPastEventsUseCase = mockSyncPastEventsUseCase,
            userPreferencesRepository = mockUserPreferencesRepository,
            countriesRepository = mockCountriesRepository,
            userNotifier = mockUserNotifier,
            logger = mockLogger
        )
    }

    // =====================
    // Test: init loads future events by default
    // =====================
    @Test
    fun init_loadsFutureEventsByDefault() = runTest {
        // Given
        val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(mockEventsList)
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value
        assertTrue("State should be Content but was $state", state is EventsUIState.Content)
        val contentState = state as EventsUIState.Content
        assertEquals(EventKind.FUTURE, contentState.selectedTab)
        assertEquals(mockEventsList, contentState.events)
        assertFalse(contentState.isLoading)
    }

    // =====================
    // Test: onTabSelected past loads past events from cache
    // =====================
    @Test
    fun onTabSelected_past_loadsPastEventsFromCache() = runTest {
        // Given
        val mockPastEventsList = listOf(createMockEvent(1L))
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(mockPastEventsList)
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // Then - selectedTab should be PAST
        assertEquals(EventKind.PAST, viewModel.selectedTab.value)
    }

    // =====================
    // Test: onTabSelected past does not call syncPastEvents (already loaded at init)
    // =====================
    @Test
    fun onTabSelected_past_afterInit_doesNotCallSyncPastEvents() = runTest {
        // Given
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        coEvery { mockSyncPastEventsUseCase() } returns Result.success(Unit)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - switch to PAST (already loaded at init)
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // Then - syncPastEvents was called only once at init
        coVerify(exactly = 1) { mockSyncPastEventsUseCase() }
    }

    // =====================
    // Test: onTabSelected past second time does not call syncPastEvents again
    // =====================
    @Test
    fun onTabSelected_pastSecondTime_doesNotCallSyncPastEventsAgain() = runTest {
        // Given
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        coEvery { mockSyncPastEventsUseCase() } returns Result.success(Unit)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // First switch to PAST (already loaded at init, so no new call)
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // Switch to FUTURE
        viewModel.onTabSelected(EventKind.FUTURE)
        advanceUntilIdle()

        // When - second switch to PAST
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // Then - syncPastEvents should be called only once at init
        coVerify(exactly = 1) { mockSyncPastEventsUseCase() }
    }

    // =====================
    // Test: onTabSelected future uses memory cache
    // =====================
    @Test
    fun onTabSelected_future_usesMemoryCache() = runTest {
        // Given
        val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(mockEventsList)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // First switch to PAST
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // Then switch back to FUTURE (should use cache)
        viewModel.onTabSelected(EventKind.FUTURE)
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value
        assertTrue("State should be Content but was $state", state is EventsUIState.Content)
        val contentState = state as EventsUIState.Content
        assertEquals(EventKind.FUTURE, contentState.selectedTab)
        assertEquals(mockEventsList, contentState.events)

        // Verify getFutureEventsUseCase was called only once (cache used on second switch)
        coVerify(exactly = 1) { mockGetFutureEventsUseCase() }
    }

    // =====================
    // Test: refresh future updates memory cache
    // =====================
    @Test
    fun refresh_future_updatesMemoryCache() = runTest {
        // Given
        val initialEvents = listOf(createMockEvent(1L), createMockEvent(2L))
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(initialEvents)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then - verify getFutureEventsUseCase was called twice (init + refresh)
        coVerify(exactly = 2) { mockGetFutureEventsUseCase() }
    }

    // =====================
    // Test: refresh past syncs with API and cache
    // =====================
    @Test
    fun refresh_past_syncsWithApiAndCache() = runTest {
        // Given
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        coEvery { mockSyncPastEventsUseCase() } returns Result.success(Unit)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Switch to PAST first
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        coVerify(atLeast = 1) { mockSyncPastEventsUseCase() }
    }

    // =====================
    // Test: onEventClick logs event
    // =====================
    @Test
    fun onEventClick_logsEvent() = runTest {
        // Given
        val mockEvent = createMockEvent(1L)
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEventClick(mockEvent)
        advanceUntilIdle()

        // Then
        verify { mockLogger.d("EventsViewModel", "Нажато мероприятие: Test Event 1 (id=1)") }
    }

    // =====================
    // Test: onFabClick logs action
    // =====================
    @Test
    fun onFabClick_logsAction() = runTest {
        // Given
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onFabClick()
        advanceUntilIdle()

        // Then
        verify { mockLogger.d("EventsViewModel", "Нажатие FAB: создание мероприятия") }
    }

    // =====================
    // Test: isAuthorized reflects user preferences
    // =====================
    @Test
    fun isAuthorized_reflectsUserPreferences() = runTest {
        // Given
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(true)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.isAuthorized.value)
    }

    // =====================
    // Test: error handling network error shows error
    // =====================
    @Test
    fun errorHandling_networkError_showsError() = runTest {
        // Given
        val ioException = IOException("Network error")
        coEvery { mockGetFutureEventsUseCase() } returns Result.failure(ioException)
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value
        assertTrue("State should be Error but was $state", state is EventsUIState.Error)
        val errorState = state as EventsUIState.Error
        assertEquals("Network error", errorState.message)
    }

    // =====================
    // Test: error handling server error shows error
    // =====================
    @Test
    fun errorHandling_serverError_showsError() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 500
        every { mockResponse.message() } returns "Server Error"
        val httpException = HttpException(mockResponse)
        coEvery { mockGetFutureEventsUseCase() } returns Result.failure(httpException)
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value
        assertTrue("State should be Error but was $state", state is EventsUIState.Error)
        val errorState = state as EventsUIState.Error
        // HttpException.message() returns formatted message like "HTTP 500 Server Error"
        assertTrue(
            "Error message should contain 'Server Error'",
            errorState.message?.contains("Server Error") == true
        )
    }

    // =====================
    // Test: sync past events error shows error
    // =====================
    @Test
    fun syncPastEvents_error_showsError() = runTest {
        // Given
        val ioException = IOException("Sync error")
        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        coEvery { mockSyncPastEventsUseCase() } returns Result.failure(ioException)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Switch to PAST
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value
        assertTrue("State should be Error but was $state", state is EventsUIState.Error)
    }

    // =====================
    // Test: load addresses when events exist
    // =====================
    @Test
    fun loadAddresses_whenEventsExist_shouldLoadUniqueAddresses() = runTest {
        // Given
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

        coEvery { mockGetFutureEventsUseCase() } returns Result.success(events)
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry1
        coEvery { mockCountriesRepository.getCountryById("2") } returns mockCountry2
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity1
        coEvery { mockCountriesRepository.getCityById("2") } returns mockCity2
        coEvery { mockCountriesRepository.getCityById("3") } returns mockCity3
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия, Москва", state.addresses[1 to 1])
        assertEquals("Россия, Санкт-Петербург", state.addresses[1 to 2])
        assertEquals("США, Нью-Йорк", state.addresses[2 to 3])
    }

    @Test
    fun loadAddresses_whenCountryAndCityExist_shouldReturnFormattedAddress() = runTest {
        // Given
        val events = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val mockCountry = mockk<Country> { every { name } returns "Россия" }
        val mockCity = mockk<City> { every { name } returns "Москва" }

        coEvery { mockGetFutureEventsUseCase() } returns Result.success(events)
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия, Москва", state.addresses[1 to 1])
    }

    @Test
    fun loadAddresses_whenOnlyCountryExists_shouldReturnCountryName() = runTest {
        // Given
        val events = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val mockCountry = mockk<Country> { every { name } returns "Россия" }

        coEvery { mockGetFutureEventsUseCase() } returns Result.success(events)
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry
        coEvery { mockCountriesRepository.getCityById("1") } returns null
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия", state.addresses[1 to 1])
    }

    @Test
    fun loadAddresses_whenOnlyCityExists_shouldReturnCityName() = runTest {
        // Given
        val events = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val mockCity = mockk<City> { every { name } returns "Москва" }

        coEvery { mockGetFutureEventsUseCase() } returns Result.success(events)
        coEvery { mockCountriesRepository.getCountryById("1") } returns null
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Москва", state.addresses[1 to 1])
    }

    @Test
    fun loadAddresses_whenErrorOccurs_shouldFallbackToIds() = runTest {
        // Given
        val events = listOf(createMockEvent(1L).copy(countryID = 99, cityID = 88))

        coEvery { mockGetFutureEventsUseCase() } returns Result.success(events)
        coEvery { mockCountriesRepository.getCountryById("99") } throws IOException("Network error")
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("99, 88", state.addresses[99 to 88])
    }

    // =====================
    // Test: first PAST tab selection loads addresses before showing list
    // =====================
    @Test
    fun onTabSelected_pastFirstTime_loadsAddressesBeforeShowingList() = runTest {
        // Given
        val pastEvents = listOf(
            createMockEvent(1L).copy(countryID = 1, cityID = 1),
            createMockEvent(2L).copy(countryID = 2, cityID = 2)
        )
        val mockCountry1 = mockk<Country> { every { name } returns "Россия" }
        val mockCountry2 = mockk<Country> { every { name } returns "США" }
        val mockCity1 = mockk<City> { every { name } returns "Москва" }
        val mockCity2 = mockk<City> { every { name } returns "Нью-Йорк" }

        coEvery { mockGetFutureEventsUseCase() } returns Result.success(emptyList())
        every { mockGetPastEventsFlowUseCase() } returns flowOf(pastEvents)
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry1
        coEvery { mockCountriesRepository.getCountryById("2") } returns mockCountry2
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity1
        coEvery { mockCountriesRepository.getCityById("2") } returns mockCity2

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - first switch to PAST
        viewModel.onTabSelected(EventKind.PAST)
        advanceUntilIdle()

        // Then - addresses should be loaded before showing list
        val state = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals(2, state.addresses.size)
        assertEquals("Россия, Москва", state.addresses[1 to 1])
        assertEquals("США, Нью-Йорк", state.addresses[2 to 2])
        assertFalse(state.isLoading)
    }

    @Test
    fun loadAddresses_whenCacheExists_shouldUseCache() = runTest {
        // Given
        val events1 = listOf(createMockEvent(1L).copy(countryID = 1, cityID = 1))
        val events2 = listOf(
            createMockEvent(1L).copy(countryID = 1, cityID = 1),
            createMockEvent(2L).copy(countryID = 2, cityID = 2)
        )
        val mockCountry1 = mockk<Country> { every { name } returns "Россия" }
        val mockCountry2 = mockk<Country> { every { name } returns "США" }
        val mockCity1 = mockk<City> { every { name } returns "Москва" }
        val mockCity2 = mockk<City> { every { name } returns "Нью-Йорк" }

        coEvery { mockGetFutureEventsUseCase() } returns Result.success(events1) andThen Result.success(
            events2
        )
        coEvery { mockCountriesRepository.getCountryById("1") } returns mockCountry1
        coEvery { mockCountriesRepository.getCountryById("2") } returns mockCountry2
        coEvery { mockCountriesRepository.getCityById("1") } returns mockCity1
        coEvery { mockCountriesRepository.getCityById("2") } returns mockCity2
        every { mockUserPreferencesRepository.isAuthorized } returns flowOf(false)
        every { mockGetPastEventsFlowUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // First load - address for (1, 1) should be loaded
        val state1 = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия, Москва", state1.addresses[1 to 1])
        assertEquals(1, state1.addresses.size)

        // Refresh - should load new address for (2, 2) but keep cached (1, 1)
        viewModel.refresh()
        advanceUntilIdle()

        val state2 = viewModel.eventsUIState.value as EventsUIState.Content
        assertEquals("Россия, Москва", state2.addresses[1 to 1])
        assertEquals("США, Нью-Йорк", state2.addresses[2 to 2])
        assertEquals(2, state2.addresses.size)

        // Verify country (1, 1) was loaded only once (cached on second load)
        coVerify(exactly = 1) { mockCountriesRepository.getCountryById("1") }
        coVerify(exactly = 1) { mockCountriesRepository.getCityById("1") }
    }
}

