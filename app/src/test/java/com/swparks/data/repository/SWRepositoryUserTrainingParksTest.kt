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
import com.swparks.data.database.dao.UserTrainingParkDao
import com.swparks.data.database.entity.ParkEntity
import com.swparks.data.model.Park
import com.swparks.network.SWApi
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryUserTrainingParksTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val mockJournalDao = mockk<JournalDao>(relaxed = true)
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)
    private val mockDialogDao = mockk<DialogDao>(relaxed = true)
    private val mockEventDao = mockk<EventDao>(relaxed = true)
    private val mockParkDao = mockk<ParkDao>(relaxed = true)
    private val mockUserTrainingParkDao = mockk<UserTrainingParkDao>(relaxed = true)
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
            name = "Test Park $id",
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

    private fun createRepository(
        mockApi: SWApi,
        mockDataStore: DataStore<Preferences>,
        userTrainingParkDao: UserTrainingParkDao = mockUserTrainingParkDao
    ): SWRepositoryImp {
        return SWRepositoryImp(
            swApi = mockApi,
            dataStore = mockDataStore,
            userDao = mockUserDao,
            journalDao = mockJournalDao,
            journalEntryDao = mockJournalEntryDao,
            dialogDao = mockDialogDao,
            eventDao = mockEventDao,
            parkDao = mockParkDao,
            userTrainingParkDao = userTrainingParkDao,
            crashReporter = crashReporter,
            logger = logger
        )
    }

    @Test
    fun getParksForUser_whenNetworkSucceeds_thenUpsertsParksIntoCommonParkTable() = runTest {
        val userId = 1L
        val mockParksList = listOf(createMockPark(1L), createMockPark(2L), createMockPark(3L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getParksForUser(userId) } returns mockParksList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        repository.getParksForUser(userId)

        coVerify {
            mockParkDao.insertAll(match { entities ->
                entities.size == 3 &&
                    entities.map { it.id }.toSet() == setOf(1L, 2L, 3L)
            })
        }
    }

    @Test
    fun getParksForUser_whenNetworkSucceeds_thenReplacesUserParkRelations() = runTest {
        val userId = 1L
        val mockParksList = listOf(createMockPark(10L), createMockPark(20L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getParksForUser(userId) } returns mockParksList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        repository.getParksForUser(userId)

        coVerify {
            mockUserTrainingParkDao.replaceForUser(
                userId = userId,
                relations = match { relations ->
                    relations.map { it.parkId }.toSet() == setOf(10L, 20L)
                }
            )
        }
    }

    @Test
    fun getParksForUser_whenNetworkReturnsEmptyList_thenClearsUserRelations() = runTest {
        val userId = 1L
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getParksForUser(userId) } returns emptyList()

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.getParksForUser(userId)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
        coVerify { mockUserTrainingParkDao.replaceForUser(userId, emptyList()) }
        coVerify(exactly = 0) { mockParkDao.insertAll(any()) }
    }

    @Test
    fun getCachedParksForUser_whenRelationsExist_thenReturnsParksFromRoom() = runTest {
        val userId = 1L
        val cachedParks = listOf(
            createParkEntity(10L, "Cached Park 1"),
            createParkEntity(20L, "Cached Park 2")
        )

        coEvery { mockUserTrainingParkDao.hasCachedParksForUser(userId) } returns true
        coEvery { mockUserTrainingParkDao.getParksForUserFromCache(userId) } returns cachedParks

        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.getCachedParksForUser(userId)

        assertNotNull(result)
        assertEquals(2, result?.size)
        assertEquals(10L, result?.get(0)?.id)
        assertEquals(20L, result?.get(1)?.id)
    }

    @Test
    fun getCachedParksForUser_whenNoCache_thenReturnsNull() = runTest {
        val userId = 1L

        coEvery { mockUserTrainingParkDao.hasCachedParksForUser(userId) } returns false

        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.getCachedParksForUser(userId)

        assertNull(result)
    }

    @Test
    fun getCachedParksForUser_whenNeverLoaded_thenReturnsCacheMissInsteadOfEmptyList() = runTest {
        val userId = 1L

        coEvery { mockUserTrainingParkDao.hasCachedParksForUser(userId) } returns false

        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.getCachedParksForUser(userId)

        assertNull(result)
        coVerify(exactly = 0) { mockUserTrainingParkDao.getParksForUserFromCache(any()) }
    }

    @Test
    fun getCachedParksForUser_whenCacheInitializedButEmpty_thenReturnsEmptyList() = runTest {
        val userId = 1L

        coEvery { mockUserTrainingParkDao.hasCachedParksForUser(userId) } returns true
        coEvery { mockUserTrainingParkDao.getParksForUserFromCache(userId) } returns emptyList()

        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.getCachedParksForUser(userId)

        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun hasCachedParksForUser_whenRelationsExist_thenReturnsTrue() = runTest {
        val userId = 1L

        coEvery { mockUserTrainingParkDao.hasCachedParksForUser(userId) } returns true

        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.hasCachedParksForUser(userId)

        assertTrue(result)
    }

    @Test
    fun hasCachedParksForUser_whenNoRelations_thenReturnsFalse() = runTest {
        val userId = 1L

        coEvery { mockUserTrainingParkDao.hasCachedParksForUser(userId) } returns false

        val mockApi = mockk<SWApi>()
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.hasCachedParksForUser(userId)

        assertFalse(result)
    }

    @Test
    fun getParksForUser_whenCacheExistsAndNetworkSucceeds_thenUpdatesBothCacheAndRelations() =
        runTest {
            val userId = 1L
            val mockParksList = listOf(createMockPark(100L), createMockPark(200L))

            coEvery { mockUserTrainingParkDao.hasCachedParksForUser(userId) } returns true
            coEvery { mockUserTrainingParkDao.getParksForUserFromCache(userId) } returns listOf(
                createParkEntity(50L, "Old Cached Park")
            )

            val mockApi = mockk<SWApi>()
            coEvery { mockApi.getParksForUser(userId) } returns mockParksList

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository = createRepository(mockApi, mockDataStore)

            val result = repository.getParksForUser(userId)

            assertTrue(result.isSuccess)
            assertEquals(mockParksList, result.getOrNull())
            coVerify {
                mockParkDao.insertAll(match {
                    it.map { e -> e.id }.toSet() == setOf(
                        100L,
                        200L
                    )
                })
            }
            coVerify {
                mockUserTrainingParkDao.replaceForUser(
                    userId = userId,
                    relations = match { relations ->
                        relations.map { it.parkId }.toSet() == setOf(100L, 200L)
                    }
                )
            }
        }

    @Test
    fun getParksForUser_whenNetworkFails_thenDoesNotClearExistingRelations() = runTest {
        val userId = 1L
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getParksForUser(userId) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = createRepository(mockApi, mockDataStore)

        val result = repository.getParksForUser(userId)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { mockUserTrainingParkDao.clearForUser(any()) }
        coVerify(exactly = 0) { mockUserTrainingParkDao.replaceForUser(any(), any()) }
    }

    private fun createParkEntity(id: Long, name: String) = ParkEntity(
        id = id,
        name = name,
        sizeID = 1,
        typeID = 1,
        longitude = "0.0",
        latitude = "0.0",
        address = "Test Address",
        cityID = 1,
        countryID = 1,
        preview = "",
        commentsCount = 0,
        trainingUsersCount = 0
    )
}
