package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.Comment
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventType
import com.swparks.util.NoOpCrashReporter
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для методов мероприятий в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryEventsTest {
    private val testDispatcher = StandardTestDispatcher()
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
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockUser(id: Long = 1L): User =
        User(
            id = id,
            name = "testuser",
            image = "",
            cityID = 1,
            countryID = 1
        )

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

    @Test
    fun getEvents_whenTypeIsFuture_thenReturnsFutureEvents() =
        runTest {
            // Given
            val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
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

    @Test
    fun getEvents_whenTypeIsPast_thenReturnsPastEvents() =
        runTest {
            // Given
            val mockEventsList = listOf(createMockEvent(3L), createMockEvent(4L))
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
    fun getEvents_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getFutureEvents() } throws IOException("Network error")

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
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun getEvent_whenApiReturnsEvent_thenReturnsEvent() =
        runTest {
            // Given
            val mockEvent = createMockEvent(123L)
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getEvent(123L) } returns mockEvent

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
            val result = repository.getEvent(123L)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockEvent, result.getOrNull())
            coVerify { mockApi.getEvent(123L) }
        }

    @Test
    fun getEvent_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getEvent(any()) } throws IOException("Network error")

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
            val result = repository.getEvent(123L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun getEvent_whenApiReturns404_thenReturnsEventNotFound() =
        runTest {
            // Given
            val eventId = 123L
            val mockApi = mockk<SWApi>()
            val mockResponse = mockk<Response<*>>(relaxed = true)
            every { mockResponse.code() } returns 404
            every { mockResponse.message() } returns "HTTP 404"
            coEvery { mockApi.getEvent(eventId) } throws HttpException(mockResponse)

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
            val result = repository.getEvent(eventId)

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(
                "Expected NotFoundException.EventNotFound but got $exception",
                exception is com.swparks.domain.exception.NotFoundException.EventNotFound
            )
            assertEquals(
                eventId,
                (exception as com.swparks.domain.exception.NotFoundException.EventNotFound).resourceId
            )
        }

    @Test
    fun saveEvent_whenIdIsNotNull_thenCallsEditEvent() =
        runTest {
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
            val form =
                EventForm(
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
    fun saveEvent_whenIdIsNull_thenCallsCreateEvent() =
        runTest {
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
            val form =
                EventForm(
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
    fun saveEvent_whenApiThrowsException_thenReturnsFailure() =
        runTest {
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
                    crashReporter = crashReporter,
                    logger = logger
                )
            val form =
                EventForm(
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
    fun changeIsGoingToEvent_whenGoTrue_thenCallsPostGoToEvent() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.postGoToEvent(1L) } returns Response.success(Unit)

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
                    crashReporter = crashReporter,
                    logger = logger
                )

            // When
            val result = repository.changeIsGoingToEvent(true, 1L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.postGoToEvent(1L) }
            coVerify(exactly = 0) { mockApi.deleteGoToEvent(any()) }
        }

    @Test
    fun changeIsGoingToEvent_whenGoFalse_thenCallsDeleteGoToEvent() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteGoToEvent(1L) } returns Response.success(Unit)

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
                    crashReporter = crashReporter,
                    logger = logger
                )

            // When
            val result = repository.changeIsGoingToEvent(false, 1L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.deleteGoToEvent(1L) }
            coVerify(exactly = 0) { mockApi.postGoToEvent(any()) }
        }

    @Test
    fun changeIsGoingToEvent_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.postGoToEvent(any()) } throws IOException("Network error")

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
                    crashReporter = crashReporter,
                    logger = logger
                )

            // When
            val result = repository.changeIsGoingToEvent(true, 1L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    @Test
    fun saveEvent_withPhotos_createsPhotoPartsWithCorrectNames() =
        runTest {
            // Given
            val mockEvent = createMockEvent(123L)
            val mockApi = mockk<SWApi>()
            val capturedPhotos = mutableListOf<List<MultipartBody.Part>?>()

            coEvery {
                mockApi.createEvent(
                    title = any(),
                    description = any(),
                    date = any(),
                    parkId = any(),
                    photos = captureNullable(capturedPhotos)
                )
            } returns mockEvent

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
                    crashReporter = crashReporter,
                    logger = logger
                )
            val form =
                EventForm(
                    title = "Test Event",
                    description = "Test Description",
                    date = "2024-01-01",
                    parkId = 1L
                )
            val photos =
                listOf(
                    ByteArray(10) { 0xFF.toByte() },
                    ByteArray(10) { 0xFE.toByte() },
                    ByteArray(10) { 0xFD.toByte() }
                )

            // When
            val result = repository.saveEvent(null, form, photos)

            // Then
            assertTrue(result.isSuccess)
            val capturedList = capturedPhotos.firstOrNull()
            assertNotNull(capturedList)
            assertEquals(3, capturedList?.size)

            // Verify photo names: photo1, photo2, photo3 (extracted from Content-Disposition)
            val photoNames =
                capturedList?.map { part ->
                    val contentDisposition = part.headers?.get("Content-Disposition") ?: ""
                    val nameMatch = Regex("""name="([^"]+)"""").find(contentDisposition)
                    nameMatch?.groupValues?.get(1)
                }
            assertEquals(listOf("photo1", "photo2", "photo3"), photoNames)
        }

    @Test
    fun saveEvent_withPhotos_usesJpegMimeType() =
        runTest {
            // Given
            val mockEvent = createMockEvent(123L)
            val mockApi = mockk<SWApi>()
            val capturedPhotos = mutableListOf<List<MultipartBody.Part>?>()

            coEvery {
                mockApi.editEvent(
                    eventId = any(),
                    title = any(),
                    description = any(),
                    date = any(),
                    parkId = any(),
                    photos = captureNullable(capturedPhotos)
                )
            } returns mockEvent

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
                    crashReporter = crashReporter,
                    logger = logger
                )
            val form =
                EventForm(
                    title = "Test Event",
                    description = "Test Description",
                    date = "2024-01-01",
                    parkId = 1L
                )
            val photos = listOf(ByteArray(10) { 0xFF.toByte() })

            // When
            val result = repository.saveEvent(123L, form, photos)

            // Then
            assertTrue(result.isSuccess)
            val capturedList = capturedPhotos.firstOrNull()
            assertNotNull(capturedList)
            assertEquals(1, capturedList?.size)

            // Verify MIME type is image/jpeg
            val contentType = capturedList?.first()?.body?.contentType()
            assertNotNull(contentType)
            assertEquals("image/jpeg", contentType.toString())
        }

    @Test
    fun deleteEvent_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteEvent(1L) } returns Response.success(Unit)

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
                    crashReporter = crashReporter,
                    logger = logger
                )

            // When
            val result = repository.deleteEvent(1L)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApi.deleteEvent(1L) }
        }

    @Test
    fun deleteEvent_whenApiThrowsException_thenReturnsFailure() =
        runTest {
            // Given
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.deleteEvent(any()) } throws IOException("Network error")

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
                    crashReporter = crashReporter,
                    logger = logger
                )

            // When
            val result = repository.deleteEvent(1L)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NetworkException)
        }

    // ========== Past Events Room Cache Tests ==========

    @Test
    fun getPastEventsFlow_whenEventsInRoom_thenReadsFromRoom() =
        runTest {
            // Given - events already in Room
            val eventId = 1L
            val eventInRoom = createMockEventEntity(eventId)
            every { mockEventDao.getAllPastEvents() } returns flowOf(listOf(eventInRoom))

            val mockApi = mockk<SWApi>()
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
            val eventsFlow = repository.getPastEventsFlow()
            val events = eventsFlow.first()

            // Then - events read from Room, not from API
            assertTrue(events.isNotEmpty())
            assertEquals(eventId, events.first().id)
            coVerify(exactly = 0) { mockApi.getPastEvents() }
        }

    @Test
    fun syncPastEvents_whenApiReturnsEvents_thenSavesToRoom() =
        runTest {
            // Given
            val mockEvents = listOf(createMockEvent(1L), createMockEvent(2L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getPastEvents() } returns mockEvents

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
            val result = repository.syncPastEvents()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockEventDao.insertEventsPartial(any()) }
            coVerify(exactly = 0) { mockEventDao.replaceAll(any()) }
        }

    @Test
    fun eventDao_contract_exposesPastEventDetailCacheMethods() {
        val methodNames =
            EventDao::class.java.methods
                .map { it.name }
                .toSet()

        assertTrue(
            "EventDao должен иметь getEventById(id) для чтения cached past event details",
            methodNames.contains("getEventById")
        )
        assertTrue(
            "EventDao должен иметь upsertEventFull(event) для сохранения full snapshot",
            methodNames.contains("upsertEventFull")
        )
        assertTrue(
            "EventDao должен иметь insertEventsPartial(events) для merge-синхронизации past list",
            methodNames.contains("insertEventsPartial")
        )
    }

    @Test
    fun swRepository_contract_exposesPastEventCacheMethods() {
        val methodNames =
            SWRepository::class.java.methods
                .map { it.name }
                .toSet()

        assertTrue(
            "SWRepository должен иметь getEventFromCache(eventId) для cache-first details",
            methodNames.contains("getEventFromCache")
        )
        assertTrue(
            "SWRepository должен иметь saveEventFull(event) для записи full past event snapshot",
            methodNames.contains("saveEventFull")
        )
    }

    @Test
    fun saveEventFull_whenEventHasDetailData_thenSavesFullSnapshotToRoom() =
        runTest {
            val eventId = 1L
            val fullEvent = createFullMockEvent(eventId)
            val repository = createRepository(mockApi = mockk())

            val eventSlot = slot<com.swparks.data.database.entity.EventEntity>()
            coEvery { mockEventDao.upsertEventFull(capture(eventSlot)) } returns Unit

            repository.saveEventFull(fullEvent)

            coVerify(exactly = 1) { mockEventDao.upsertEventFull(any()) }
            coVerify(exactly = 0) { mockEventDao.replaceAll(any()) }
            assertTrue(eventSlot.captured.isFull)
        }

    @Test
    fun getEventFromCache_whenFullEventSaved_thenReturnsFullEvent() =
        runTest {
            val eventId = 1L
            val fullEvent = createFullMockEvent(eventId)
            val repository = createRepository(mockApi = mockk())

            var cachedEntity: com.swparks.data.database.entity.EventEntity? = null
            coEvery { mockEventDao.upsertEventFull(any()) } answers {
                cachedEntity = firstArg()
            }
            coEvery { mockEventDao.getEventById(eventId) } answers { cachedEntity }

            repository.saveEventFull(fullEvent)
            val cachedEvent = repository.getEventFromCache(eventId)

            assertNotNull("После saveEventFull() past event должен читаться из cache", cachedEvent)

            if (cachedEvent !is Event) {
                fail("getEventFromCache(eventId) должен возвращать Event")
            }

            val event = cachedEvent as Event

            assertEquals(eventId, event.id)
            assertTrue(event.photos.isNotEmpty())
            assertTrue(!event.comments.isNullOrEmpty())
            assertTrue(!event.trainingUsers.isNullOrEmpty())
            assertNotNull(event.address)
        }

    @Test
    fun syncPastEvents_whenFullSnapshotContractIsIntroduced_thenMustUseMergeInsteadOfReplaceAll() =
        runTest {
            val mockEvents = listOf(createMockEvent(1L), createMockEvent(2L))
            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getPastEvents() } returns mockEvents

            val repository = createRepository(mockApi)
            val daoMethodNames =
                EventDao::class.java.methods
                    .map { it.name }
                    .toSet()

            assertTrue(
                "Перед переходом к этапу 3 нужен insertEventsPartial(events) для merge-синхронизации",
                daoMethodNames.contains("insertEventsPartial")
            )

            repository.syncPastEvents()

            coVerify(exactly = 1) { mockEventDao.insertEventsPartial(any()) }
            coVerify(exactly = 0) {
                mockEventDao.replaceAll(any())
            }
        }

    // ========== Helper Methods ==========

    private fun createRepository(mockApi: SWApi): SWRepositoryImp {
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        return SWRepositoryImp(
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
    }

    private fun createMockEventEntity(id: Long): com.swparks.data.database.entity.EventEntity =
        com.swparks.data.database.entity.EventEntity(
            id = id,
            title = "Test Event $id",
            description = "Description",
            beginDate = "2024-01-01",
            countryId = 1,
            cityId = 1,
            preview = "",
            latitude = "0.0",
            longitude = "0.0",
            authorId = 1L,
            authorName = "Author",
            authorImage = null
        )

    private fun createFullMockEvent(id: Long): Event =
        Event(
            id = id,
            title = "Full Test Event $id",
            description = "Full Description",
            beginDate = "2024-01-01",
            countryID = 1,
            cityID = 1,
            commentsCount = 5,
            preview = "preview.jpg",
            parkID = 1L,
            latitude = "55.751244",
            longitude = "37.618423",
            trainingUsersCount = 10,
            isCurrent = false,
            address = "Moscow, Red Square",
            photos =
                listOf(
                    com.swparks.data.model
                        .Photo(id = 1L, photo = "photo1.jpg"),
                    com.swparks.data.model
                        .Photo(id = 2L, photo = "photo2.jpg")
                ),
            trainingUsers =
                listOf(
                    User(id = 1L, name = "User 1", image = null),
                    User(id = 2L, name = "User 2", image = null)
                ),
            author = User(id = 1L, name = "Full Author", image = "author.jpg"),
            name = "Full Event",
            comments =
                listOf(
                    Comment(
                        id = 1L,
                        body = "Comment 1",
                        date = "2024-01-01",
                        user = User(id = 2L, name = "Commenter", image = null)
                    )
                ),
            isOrganizer = true,
            canEdit = true,
            trainHere = false
        )
}
