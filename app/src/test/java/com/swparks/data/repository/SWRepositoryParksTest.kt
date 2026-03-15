package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.Park
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
import com.swparks.ui.model.ParkForm
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
import retrofit2.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
        )

        // When
        val result = repository.getPark(123L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
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
            mockEventDao
        )

        // When
        val result = repository.getUpdatedParks("2024-01-01")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
