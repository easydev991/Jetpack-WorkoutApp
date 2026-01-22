package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.domain.exception.NetworkException
import com.swparks.model.Event
import com.swparks.model.EventForm
import com.swparks.model.EventType
import com.swparks.model.User
import com.swparks.network.SWApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для методов мероприятий в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryEventsTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockUser(id: Long = 1L): User {
        return User(
            id = id,
            name = "testuser",
            image = "",
            lang = "ru",
            cityID = 1,
            countryID = 1
        )
    }

    private fun createMockEvent(id: Long = 1L): Event {
        return Event(
            id = id,
            title = "Test Event",
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
            name = "Test Event"
        )
    }

    @Test
    fun getEvents_whenTypeIsFuture_thenReturnsFutureEvents() = runTest {
        // Given
        val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getFutureEvents() } returns mockEventsList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getEvents(EventType.FUTURE)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockEventsList, result.getOrNull())
        coVerify { mockApi.getFutureEvents() }
    }

    @Test
    fun getEvents_whenTypeIsPast_thenReturnsPastEvents() = runTest {
        // Given
        val mockEventsList = listOf(createMockEvent(3L), createMockEvent(4L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getPastEvents() } returns mockEventsList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getEvents(EventType.PAST)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockEventsList, result.getOrNull())
        coVerify { mockApi.getPastEvents() }
    }

    @Test
    fun getEvents_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getFutureEvents() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getEvents(EventType.FUTURE)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getEvent_whenApiReturnsEvent_thenReturnsEvent() = runTest {
        // Given
        val mockEvent = createMockEvent(123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getEvent(123L) } returns mockEvent

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getEvent(123L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockEvent, result.getOrNull())
        coVerify { mockApi.getEvent(123L) }
    }

    @Test
    fun getEvent_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getEvent(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getEvent(123L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun saveEvent_whenIdIsNotNull_thenCallsEditEvent() = runTest {
        // Given
        val mockEvent = createMockEvent(123L)
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editEvent(
                eventId = any(),
                title = any(),
                description = any(),
                date = any(),
                parkId = any(),
                photos = any()
            )
        } returns mockEvent

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val form = EventForm(
            title = "Test Event",
            description = "Test Description",
            date = "2024-01-01",
            parkId = 1L
        )

        // When
        val result = repository.saveEvent(123L, form, null)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.editEvent(
                eventId = 123L,
                title = any(),
                description = any(),
                date = any(),
                parkId = any(),
                photos = any()
            )
        }
    }

    @Test
    fun saveEvent_whenIdIsNull_thenCallsCreateEvent() = runTest {
        // Given
        val mockEvent = createMockEvent(123L)
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.createEvent(
                title = any(),
                description = any(),
                date = any(),
                parkId = any(),
                photos = any()
            )
        } returns mockEvent

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val form = EventForm(
            title = "Test Event",
            description = "Test Description",
            date = "2024-01-01",
            parkId = 1L
        )

        // When
        val result = repository.saveEvent(null, form, null)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.createEvent(
                title = any(),
                description = any(),
                date = any(),
                parkId = any(),
                photos = any()
            )
        }
    }

    @Test
    fun saveEvent_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.createEvent(
                title = any(),
                description = any(),
                date = any(),
                parkId = any(),
                photos = any()
            )
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)
        val form = EventForm(
            title = "Test Event",
            description = "Test Description",
            date = "2024-01-01",
            parkId = 1L
        )

        // When
        val result = repository.saveEvent(null, form, null)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun changeIsGoingToEvent_whenGoTrue_thenCallsPostGoToEvent() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.postGoToEvent(1L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.changeIsGoingToEvent(true, 1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.postGoToEvent(1L) }
        coVerify(exactly = 0) { mockApi.deleteGoToEvent(any()) }
    }

    @Test
    fun changeIsGoingToEvent_whenGoFalse_thenCallsDeleteGoToEvent() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteGoToEvent(1L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.changeIsGoingToEvent(false, 1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteGoToEvent(1L) }
        coVerify(exactly = 0) { mockApi.postGoToEvent(any()) }
    }

    @Test
    fun changeIsGoingToEvent_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.postGoToEvent(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.changeIsGoingToEvent(true, 1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun deleteEvent_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteEvent(1L) } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.deleteEvent(1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteEvent(1L) }
    }

    @Test
    fun deleteEvent_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteEvent(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.deleteEvent(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
