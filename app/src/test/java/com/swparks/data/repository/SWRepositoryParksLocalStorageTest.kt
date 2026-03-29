package com.swparks.data.repository

import android.content.Context
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
import com.swparks.data.model.Park
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryParksLocalStorageTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val mockJournalDao = mockk<JournalDao>(relaxed = true)
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)
    private val mockDialogDao = mockk<DialogDao>(relaxed = true)
    private val mockEventDao = mockk<EventDao>(relaxed = true)
    private val mockParkDao = mockk<ParkDao>(relaxed = true)
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

    private lateinit var mockContext: Context
    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var repository: SWRepositoryImp

    private val parksJson = """
        [
            {
                "id": 1,
                "name": "Парк Горького",
                "class_id": 1,
                "type_id": 1,
                "longitude": "37.6176",
                "latitude": "55.7558",
                "address": "Москва, Крымский вал, 9",
                "city_id": 1,
                "country_id": 1,
                "preview": "",
                "comments_count": 10,
                "trainings": 5
            },
            {
                "id": 2,
                "name": "Сокольники",
                "class_id": 2,
                "type_id": 2,
                "longitude": "37.6786",
                "latitude": "55.7892",
                "address": "Москва, Сокольнический Вал, 1",
                "city_id": 1,
                "country_id": 1,
                "preview": "",
                "comments_count": 3,
                "trainings": 2
            }
        ]
    """.trimIndent()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        mockContext = mockk(relaxed = true)
        mockDataStore = mockk(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        every { mockContext.assets.open("parks.json") } returns parksJson.byteInputStream()

        repository = SWRepositoryImp(
            mockk(relaxed = true),
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

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createMockPark(id: Long = 1L, name: String = "Test Park"): Park {
        return Park(
            id = id,
            name = name,
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
    fun importSeedParks_whenRoomIsEmpty_importsParksFromAssets() = runTest {
        coEvery { mockParkDao.isEmpty() } returns true

        repository.importSeedParks(mockContext)

        coVerify { mockParkDao.insertAll(any()) }
    }

    @Test
    fun importSeedParks_whenRoomIsNotEmpty_doesNotImportAgain() = runTest {
        coEvery { mockParkDao.isEmpty() } returns false

        repository.importSeedParks(mockContext)

        coVerify(exactly = 0) { mockParkDao.insertAll(any()) }
    }

    @Test
    fun importSeedParks_whenEmpty_insertsCorrectParks() = runTest {
        coEvery { mockParkDao.isEmpty() } returns true
        val insertedEntities = slot<List<com.swparks.data.database.entity.ParkEntity>>()

        repository.importSeedParks(mockContext)

        coVerify { mockParkDao.insertAll(capture(insertedEntities)) }
        assertEquals(2, insertedEntities.captured.size)
        assertEquals(1L, insertedEntities.captured[0].id)
        assertEquals("Парк Горького", insertedEntities.captured[0].name)
        assertEquals(2L, insertedEntities.captured[1].id)
        assertEquals("Сокольники", insertedEntities.captured[1].name)
    }

    @Test
    fun upsertParks_existingPark_updatesById() = runTest {
        val updatedPark = createMockPark(id = 1L, name = "Updated Name")
        val insertedEntities = slot<List<com.swparks.data.database.entity.ParkEntity>>()

        repository.upsertParks(listOf(updatedPark))

        coVerify { mockParkDao.insertAll(capture(insertedEntities)) }
        assertEquals(1, insertedEntities.captured.size)
        assertEquals("Updated Name", insertedEntities.captured[0].name)
    }

    @Test
    fun upsertParks_newPark_insertsWithNewId() = runTest {
        val newPark = createMockPark(id = 999L, name = "New Park")
        val insertedEntities = slot<List<com.swparks.data.database.entity.ParkEntity>>()

        repository.upsertParks(listOf(newPark))

        coVerify { mockParkDao.insertAll(capture(insertedEntities)) }
        assertEquals(1, insertedEntities.captured.size)
        assertEquals(999L, insertedEntities.captured[0].id)
        assertEquals("New Park", insertedEntities.captured[0].name)
    }

    @Test
    fun upsertParks_multipleParks_insertsAll() = runTest {
        val parks = listOf(
            createMockPark(id = 1L, name = "Park 1"),
            createMockPark(id = 2L, name = "Park 2"),
            createMockPark(id = 3L, name = "Park 3")
        )
        val insertedEntities = slot<List<com.swparks.data.database.entity.ParkEntity>>()

        repository.upsertParks(parks)

        coVerify { mockParkDao.insertAll(capture(insertedEntities)) }
        assertEquals(3, insertedEntities.captured.size)
    }

    @Test
    fun getParksFlow_returnsParksFromRoom() = runTest {
        val parkEntity = com.swparks.data.database.entity.ParkEntity(
            id = 1L,
            name = "Room Park",
            sizeID = 1,
            typeID = 1,
            longitude = "37.0",
            latitude = "55.0",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = "",
            commentsCount = 0,
            trainingUsersCount = 0
        )
        every { mockParkDao.getAllParks() } returns flowOf(listOf(parkEntity))

        val parks = repository.getParksFlow().first()

        assertEquals(1, parks.size)
        assertEquals(1L, parks[0].id)
        assertEquals("Room Park", parks[0].name)
    }

    @Test
    fun getParksFlow_whenRoomIsEmpty_returnsEmptyList() = runTest {
        every { mockParkDao.getAllParks() } returns flowOf(emptyList())

        val parks = repository.getParksFlow().first()

        assertTrue(parks.isEmpty())
    }
}
