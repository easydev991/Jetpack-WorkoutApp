package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.UserEntity
import com.swparks.data.model.Park
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
import com.swparks.ui.model.ParkForm
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для методов площадок в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryParksTest {
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
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockPark(id: Long = 1L): Park {
        return Park(
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
    }

    @Test
    fun getAllParks_whenApiReturnsParks_thenReturnsParks() = runTest {
        // Given
        val mockParksList = listOf(createMockPark(1L), createMockPark(2L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getAllParks() } returns mockParksList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
    fun getAllParks_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getAllParks() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getPark_whenApiReturnsPark_thenReturnsPark() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getPark(123L) } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.getPark(123L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockPark, result.getOrNull())
        coVerify { mockApi.getPark(123L) }
    }

    @Test
    fun getPark_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getPark(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.getPark(123L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getPark_whenApiReturns404_thenReturnsParkNotFound() = runTest {
        // Given
        val parkId = 123L
        val mockApi = mockk<SWApi>()
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 404
        every { mockResponse.message() } returns "HTTP 404"
        coEvery { mockApi.getPark(parkId) } throws HttpException(mockResponse)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.getPark(parkId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(
            "Expected NotFoundException.ParkNotFound but got $exception",
            exception is com.swparks.domain.exception.NotFoundException.ParkNotFound
        )
        assertEquals(
            parkId,
            (exception as com.swparks.domain.exception.NotFoundException.ParkNotFound).resourceId
        )
    }

    @Test
    fun savePark_whenIdIsNotNull_thenCallsEditPark() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editPark(
                parkId = any(),
                address = any(),
                latitude = any(),
                longitude = any(),
                cityId = any(),
                typeId = any(),
                sizeId = any(),
                photos = any()
            )
        } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "Test Address",
            latitude = "0.0",
            longitude = "0.0",
            cityId = 1,
            typeId = 1,
            sizeId = 1
        )

        // When
        val result = repository.savePark(123L, form, null)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.editPark(
                parkId = 123L,
                address = any(),
                latitude = any(),
                longitude = any(),
                cityId = any(),
                typeId = any(),
                sizeId = any(),
                photos = any()
            )
        }
    }

    @Test
    fun savePark_whenIdIsNull_thenCallsCreatePark() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.createPark(
                address = any(),
                latitude = any(),
                longitude = any(),
                cityId = any(),
                typeId = any(),
                sizeId = any(),
                photos = any()
            )
        } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "Test Address",
            latitude = "0.0",
            longitude = "0.0",
            cityId = 1,
            typeId = 1,
            sizeId = 1
        )

        // When
        val result = repository.savePark(null, form, null)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockApi.createPark(
                address = any(),
                latitude = any(),
                longitude = any(),
                cityId = any(),
                typeId = any(),
                sizeId = any(),
                photos = any()
            )
        }
    }

    @Test
    fun savePark_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.createPark(
                address = any(),
                latitude = any(),
                longitude = any(),
                cityId = any(),
                typeId = any(),
                sizeId = any(),
                photos = any()
            )
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "Test Address",
            latitude = "0.0",
            longitude = "0.0",
            cityId = 1,
            typeId = 1,
            sizeId = 1
        )

        // When
        val result = repository.savePark(null, form, null)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun savePark_withPhotos_createsPhotoPartsWithCorrectNames() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()
        val capturedPhotos = mutableListOf<List<MultipartBody.Part>?>()
        coEvery {
            mockApi.createPark(
                address = any(),
                latitude = any(),
                longitude = any(),
                cityId = any(),
                typeId = any(),
                sizeId = any(),
                photos = captureNullable(capturedPhotos)
            )
        } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "Test Address",
            latitude = "0.0",
            longitude = "0.0",
            cityId = 1,
            typeId = 1,
            sizeId = 1
        )
        val photos = listOf(
            ByteArray(10) { 0xFF.toByte() },
            ByteArray(10) { 0xFE.toByte() },
            ByteArray(10) { 0xFD.toByte() }
        )

        // When
        val result = repository.savePark(null, form, photos)

        // Then
        assertTrue(result.isSuccess)
        val capturedList = capturedPhotos.firstOrNull()
        assertNotNull(capturedList)
        assertEquals(3, capturedList?.size)

        val photoNames = capturedList?.map { part ->
            val contentDisposition = part.headers?.get("Content-Disposition") ?: ""
            val nameMatch = Regex("""name="([^"]+)"""").find(contentDisposition)
            nameMatch?.groupValues?.get(1)
        }
        assertEquals(listOf("photo1", "photo2", "photo3"), photoNames)
    }

    @Test
    fun savePark_withPhotos_usesJpegMimeType() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()
        val capturedPhotos = mutableListOf<List<MultipartBody.Part>?>()
        coEvery {
            mockApi.editPark(
                parkId = any(),
                address = any(),
                latitude = any(),
                longitude = any(),
                cityId = any(),
                typeId = any(),
                sizeId = any(),
                photos = captureNullable(capturedPhotos)
            )
        } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "Test Address",
            latitude = "0.0",
            longitude = "0.0",
            cityId = 1,
            typeId = 1,
            sizeId = 1
        )
        val photos = listOf(ByteArray(10) { 0xFF.toByte() })

        // When
        val result = repository.savePark(123L, form, photos)

        // Then
        assertTrue(result.isSuccess)
        val capturedList = capturedPhotos.firstOrNull()
        assertNotNull(capturedList)
        assertEquals(1, capturedList?.size)

        val contentType = capturedList?.first()?.body?.contentType()
        assertNotNull(contentType)
        assertEquals("image/jpeg", contentType.toString())
    }

    @Test
    fun savePark_createMode_sendsCorrectFormParameters() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()

        val addressList = mutableListOf<RequestBody>()
        val latitudeList = mutableListOf<RequestBody>()
        val longitudeList = mutableListOf<RequestBody>()
        val typeIdList = mutableListOf<RequestBody>()
        val sizeIdList = mutableListOf<RequestBody>()

        coEvery {
            mockApi.createPark(
                address = capture(addressList),
                latitude = capture(latitudeList),
                longitude = capture(longitudeList),
                cityId = any(),
                typeId = capture(typeIdList),
                sizeId = capture(sizeIdList),
                photos = any()
            )
        } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "123 Main Street",
            latitude = "55.7558",
            longitude = "37.6173",
            cityId = 42,
            typeId = 2,
            sizeId = 3
        )

        // When
        val result = repository.savePark(null, form, null)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("123 Main Street", addressList.first().readToString())
        assertEquals("55.7558", latitudeList.first().readToString())
        assertEquals("37.6173", longitudeList.first().readToString())
        assertEquals("2", typeIdList.first().readToString())
        assertEquals("3", sizeIdList.first().readToString())
    }

    private fun RequestBody.readToString(): String {
        val buffer = okio.Buffer()
        this.writeTo(buffer)
        return buffer.readUtf8()
    }

    @Test
    fun savePark_editMode_sendsCorrectFormParameters() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()

        val parkIdSlot = slot<Long>()
        val addressList = mutableListOf<RequestBody>()
        val latitudeList = mutableListOf<RequestBody>()
        val longitudeList = mutableListOf<RequestBody>()
        val typeIdList = mutableListOf<RequestBody>()
        val sizeIdList = mutableListOf<RequestBody>()

        coEvery {
            mockApi.editPark(
                parkId = capture(parkIdSlot),
                address = capture(addressList),
                latitude = capture(latitudeList),
                longitude = capture(longitudeList),
                cityId = any(),
                typeId = capture(typeIdList),
                sizeId = capture(sizeIdList),
                photos = any()
            )
        } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "456 Park Avenue",
            latitude = "40.7128",
            longitude = "-74.0060",
            cityId = 99,
            typeId = 6,
            sizeId = 2
        )

        // When
        val result = repository.savePark(123L, form, null)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(123L, parkIdSlot.captured)
        assertEquals("456 Park Avenue", addressList.first().readToString())
        assertEquals("40.7128", latitudeList.first().readToString())
        assertEquals("-74.0060", longitudeList.first().readToString())
        assertEquals("6", typeIdList.first().readToString())
        assertEquals("2", sizeIdList.first().readToString())
    }

    @Test
    fun savePark_withPhotosAndFormData_sendsAllParametersCorrectly() = runTest {
        // Given
        val mockPark = createMockPark(123L)
        val mockApi = mockk<SWApi>()

        val addressList = mutableListOf<RequestBody>()
        val latitudeList = mutableListOf<RequestBody>()
        val longitudeList = mutableListOf<RequestBody>()
        val typeIdList = mutableListOf<RequestBody>()
        val sizeIdList = mutableListOf<RequestBody>()

        coEvery {
            mockApi.createPark(
                address = capture(addressList),
                latitude = capture(latitudeList),
                longitude = capture(longitudeList),
                cityId = any(),
                typeId = capture(typeIdList),
                sizeId = capture(sizeIdList),
                photos = any()
            )
        } returns mockPark

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val form = ParkForm(
            address = "789 Sports Street",
            latitude = "48.8566",
            longitude = "2.3522",
            cityId = 15,
            typeId = 3,
            sizeId = 1
        )
        val photos = listOf(
            ByteArray(5) { 0x11.toByte() },
            ByteArray(5) { 0x22.toByte() }
        )

        // When
        val result = repository.savePark(null, form, photos)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("789 Sports Street", addressList.first().readToString())
        assertEquals("48.8566", latitudeList.first().readToString())
        assertEquals("2.3522", longitudeList.first().readToString())
        assertEquals("3", typeIdList.first().readToString())
        assertEquals("1", sizeIdList.first().readToString())
    }

    @Test
    fun deletePark_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deletePark(1L) } returns Response.success(Unit)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.deletePark(1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deletePark(1L) }
    }

    @Test
    fun deletePark_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deletePark(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.deletePark(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getParksForUser_whenApiReturnsParks_thenReturnsParks() = runTest {
        // Given
        val mockParksList = listOf(createMockPark(1L), createMockPark(2L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getParksForUser(1L) } returns mockParksList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.getParksForUser(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockParksList, result.getOrNull())
        coVerify { mockApi.getParksForUser(1L) }
    }

    @Test
    fun getParksForUser_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getParksForUser(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.getParksForUser(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun changeTrainHereStatus_whenTrainHereTrue_thenCallsPostTrainHere() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.postTrainHere(1L) } returns Response.success(Unit)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.changeTrainHereStatus(true, 1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.postTrainHere(1L) }
        coVerify(exactly = 0) { mockApi.deleteTrainHere(any()) }
    }

    @Test
    fun changeTrainHereStatus_whenTrainHereFalse_thenCallsDeleteTrainHere() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteTrainHere(1L) } returns Response.success(Unit)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.changeTrainHereStatus(false, 1L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteTrainHere(1L) }
        coVerify(exactly = 0) { mockApi.postTrainHere(any()) }
    }

    @Test
    fun changeTrainHereStatus_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.postTrainHere(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.changeTrainHereStatus(true, 1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun getUpdatedParks_whenApiReturnsParks_thenReturnsParks() = runTest {
        // Given
        val mockParksList = listOf(createMockPark(1L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getUpdatedParks("2024-01-01") } returns mockParksList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.getUpdatedParks("2024-01-01")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockParksList, result.getOrNull())
        coVerify { mockApi.getUpdatedParks("2024-01-01") }
    }

    @Test
    fun getUpdatedParks_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getUpdatedParks(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.getUpdatedParks("2024-01-01")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun deleteParkPhoto_whenApiSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteParkPhoto(1L, 100L) } returns Response.success(Unit)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.deleteParkPhoto(1L, 100L)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.deleteParkPhoto(1L, 100L) }
    }

    @Test
    fun deleteParkPhoto_whenApiError_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deleteParkPhoto(any(), any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
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
        val result = repository.deleteParkPhoto(1L, 100L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun savePark_createMode_withCurrentUser_updatesAddedParksCache() = runTest {
        // Given
        val currentUserId = 1L
        val mockPark = createMockPark(123L)
        val mockUser = UserEntity(id = currentUserId, name = "test", addedParks = emptyList())

        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.createPark(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockPark

        val currentUserIdKey = longPreferencesKey("currentUserId")
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(preferencesOf(currentUserIdKey to currentUserId))

        val repository = SWRepositoryImp(
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

        // Mock getUserByIdFlow to return the user
        coEvery { mockUserDao.getUserByIdFlow(currentUserId) } returns flowOf(mockUser)
        coEvery { mockUserDao.insert(any()) } returns Unit

        val form = ParkForm(
            address = "Test Address",
            latitude = "0.0",
            longitude = "0.0",
            cityId = 1,
            typeId = 1,
            sizeId = 1
        )

        // When - savePark with null id (create mode)
        repository.savePark(null, form, null)

        // Then - verify userDao.insert was called with updated addedParks
        coVerify {
            mockUserDao.insert(withArg { user ->
                assertTrue(user.addedParks?.any { it.id == 123L } == true)
            })
        }
    }

    @Test
    fun deletePark_withCurrentUser_updatesAddedParksCache() = runTest {
        // Given
        val currentUserId = 1L
        val parkIdToDelete = 123L
        val mockPark = createMockPark(parkIdToDelete)
        val mockUser = UserEntity(id = currentUserId, name = "test", addedParks = listOf(mockPark))

        val mockApi = mockk<SWApi>()
        coEvery { mockApi.deletePark(parkIdToDelete) } returns Response.success(Unit)

        val currentUserIdKey = longPreferencesKey("currentUserId")
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(preferencesOf(currentUserIdKey to currentUserId))

        val repository = SWRepositoryImp(
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

        coEvery { mockUserDao.getUserByIdFlow(currentUserId) } returns flowOf(mockUser)
        coEvery { mockUserDao.insert(any()) } returns Unit

        // When
        val result = repository.deletePark(parkIdToDelete)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockUserDao.insert(withArg { user ->
                assertTrue(user.addedParks?.none { it.id == parkIdToDelete } == true)
            })
        }
    }

    @Test
    fun savePark_editMode_withCurrentUser_removesOldAndAddsNewPark() = runTest {
        // Given - editing same park (same id), new content
        val currentUserId = 1L
        val parkId = 100L
        val updatedPark = createMockPark(parkId).copy(address = "Updated Address")
        val existingPark = createMockPark(parkId)
        val mockUser =
            UserEntity(id = currentUserId, name = "test", addedParks = listOf(existingPark))

        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.editPark(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns updatedPark

        val currentUserIdKey = longPreferencesKey("currentUserId")
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(preferencesOf(currentUserIdKey to currentUserId))

        val repository = SWRepositoryImp(
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

        coEvery { mockUserDao.getUserByIdFlow(currentUserId) } returns flowOf(mockUser)
        coEvery { mockUserDao.insert(any()) } returns Unit

        val form = ParkForm(
            address = "Updated Address",
            latitude = "1.0",
            longitude = "1.0",
            cityId = 1,
            typeId = 1,
            sizeId = 1
        )

        // When - edit mode with same park id
        repository.savePark(parkId, form, null)

        // Then - verify the park was replaced (only one park with same id, updated content)
        coVerify {
            mockUserDao.insert(withArg { user ->
                assertTrue(user.addedParks?.size == 1)
                assertTrue(user.addedParks?.first()?.id == parkId)
                assertTrue(user.addedParks?.first()?.address == "Updated Address")
            })
        }
    }
}
