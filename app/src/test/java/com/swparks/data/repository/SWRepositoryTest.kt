package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.cash.turbine.test
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.Event
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.Park
import com.swparks.data.model.User
import com.swparks.network.SWApi
import com.swparks.ui.model.EventType
import com.swparks.util.NoOpCrashReporter
import com.swparks.util.NoOpLogger
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
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val currentUserIdKey = longPreferencesKey("currentUserId")
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val mockJournalDao = mockk<JournalDao>(relaxed = true)
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)
    private val mockDialogDao = mockk<DialogDao>(relaxed = true)
    private val mockEventDao = mockk<EventDao>(relaxed = true)
    private val mockParkDao = mockk<ParkDao>(relaxed = true)
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockEvent(id: Long = 1L): Event =
        Event(
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

    private fun createMockUser(id: Long = 1L): User = User(id = id, name = "testuser", image = "")

    private fun createMockPark(id: Long = 1L): Park =
        Park(
            id = id,
            name = "Test Park",
            sizeID = 1,
            typeID = 1,
            longitude = "0.0",
            latitude = "0.0",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = ""
        )

    @Test
    fun getPastEvents_whenApiReturnsData_thenReturnsEvents() =
        runTest {
            // Given
            val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getPastEvents() } returns mockEventsList

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When
            val result = repository.getPastEvents()

            // Then
            assertEquals(mockEventsList, result)
            coVerify { mockApi.getPastEvents() }
        }

    @Test
    fun getPastEvents_whenApiThrowsIOException_thenThrowsIOException() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getPastEvents() } throws IOException("Network error")

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When & Then
            try {
                repository.getPastEvents()
                throw AssertionError("Expected IOException was not thrown")
            } catch (e: IOException) {
                assertEquals("Network error", e.message)
            }
        }

    @Test
    fun getPastEvents_whenApiThrowsHttpException_thenThrowsHttpException() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            val mockResponse = mockk<Response<*>>(relaxed = true)
            every { mockResponse.code() } returns 404
            every { mockResponse.message() } returns "HTTP 404"
            coEvery { mockApi.getPastEvents() } throws HttpException(mockResponse)

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When & Then
            try {
                repository.getPastEvents()
                throw AssertionError("Expected HttpException was not thrown")
            } catch (e: HttpException) {
                assertEquals("HTTP 404", e.message())
            }
        }

    @Test
    fun isAuthorized_whenUserIdExists_thenReturnsTrue() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            val mockDataStore = mockk<DataStore<Preferences>>()
            val preferences = mutablePreferencesOf(currentUserIdKey to 123L)
            every { mockDataStore.data } returns flowOf(preferences)

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When & Then
            repository.isAuthorized.test {
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun isAuthorized_whenUserIdNotExists_thenReturnsFalse() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            val mockDataStore = mockk<DataStore<Preferences>>()
            val preferences = mutablePreferencesOf()
            every { mockDataStore.data } returns flowOf(preferences)

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When & Then
            repository.isAuthorized.test {
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun login_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val mockSuccess = LoginSuccess(userId = 123L)
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.login() } returns mockSuccess

            val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When
            val result = repository.login("test_token")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockSuccess, result.getOrNull())
            coVerify { mockApi.login() }
        }

    @Test
    fun getUser_whenApiReturnsUser_thenReturnsUser() =
        runTest {
            // Given
            val mockUser = createMockUser(123L)
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getUser(123L) } returns mockUser

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            // Mock userDao to return null (no existing cached user)
            every { mockUserDao.getUserByIdFlow(any()) } returns flowOf(null)

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When
            val result = repository.getUser(123L)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockUser, result.getOrNull())
            coVerify { mockApi.getUser(123L) }
        }

    @Test
    fun getAllParks_whenApiReturnsParks_thenReturnsParks() =
        runTest {
            // Given
            val mockParksList = listOf(createMockPark(1L), createMockPark(2L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getAllParks() } returns mockParksList

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When
            val result = repository.getAllParks()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockParksList, result.getOrNull())
            coVerify { mockApi.getAllParks() }
        }

    @Test
    fun getEvents_whenTypeIsPast_thenReturnsPastEvents() =
        runTest {
            // Given
            val mockEventsList = listOf(createMockEvent(1L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getPastEvents() } returns mockEventsList

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When
            val result = repository.getEvents(EventType.PAST)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockEventsList, result.getOrNull())
            coVerify { mockApi.getPastEvents() }
        }

    @Test
    fun getEvents_whenTypeIsFuture_thenReturnsFutureEvents() =
        runTest {
            // Given
            val mockEventsList = listOf(createMockEvent(2L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getFutureEvents() } returns mockEventsList

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository =
                SWRepositoryImp(
                    mockApi,
                    mockDataStore,
                    mockUserDao,
                    mockJournalDao,
                    mockJournalEntryDao,
                    mockDialogDao,
                    mockEventDao,
                    mockParkDao,
                    crashReporter,
                    logger
                )

            // When
            val result = repository.getEvents(EventType.FUTURE)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockEventsList, result.getOrNull())
            coVerify { mockApi.getFutureEvents() }
        }
}
