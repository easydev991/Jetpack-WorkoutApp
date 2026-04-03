package com.swparks.data.database.dao

import android.content.Context
import androidx.room.Room
import com.swparks.data.database.SWDatabase
import com.swparks.data.database.entity.ParkEntity
import com.swparks.data.model.Comment
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ParkDaoTest {
    private lateinit var database: SWDatabase
    private lateinit var parkDao: ParkDao

    private val testUser =
        User(
            id = 1L,
            name = "testuser",
            image = "https://example.com/avatar.jpg"
        )

    private val testPhoto =
        Photo(
            id = 10L,
            photo = "https://example.com/photo.jpg"
        )

    private val testComment =
        Comment(
            id = 100L,
            body = "Test comment",
            date = "2024-01-15",
            user = testUser
        )

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        database =
            Room
                .inMemoryDatabaseBuilder(context, SWDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        parkDao = database.parkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getParkById_whenEntityExists_shouldReturnEntity() =
        runTest {
            val entity = createParkEntity()
            parkDao.upsertPark(entity)

            val result = parkDao.getParkById(entity.id)

            assertNotNull(result)
            assertEquals(entity.id, result?.id)
            assertEquals(entity.name, result?.name)
        }

    @Test
    fun getParkById_whenEntityMissing_shouldReturnNull() =
        runTest {
            val result = parkDao.getParkById(999L)

            assertNull(result)
        }

    @Test
    fun upsertPark_shouldInsertNewEntity() =
        runTest {
            val entity = createParkEntity()

            parkDao.upsertPark(entity)

            val stored = parkDao.getParkById(entity.id)
            assertNotNull(stored)
            assertEquals(entity.name, stored?.name)
            assertEquals(entity.preview, stored?.preview)
        }

    @Test
    fun upsertPark_shouldReplaceExistingEntity() =
        runTest {
            val original = createParkEntity(name = "Original Park", commentsCount = 1)
            val updated = createParkEntity(name = "Updated Park", commentsCount = 5)

            parkDao.upsertPark(original)
            parkDao.upsertPark(updated)

            val stored = parkDao.getParkById(updated.id)
            assertNotNull(stored)
            assertEquals("Updated Park", stored?.name)
            assertEquals(5, stored?.commentsCount)
        }

    @Test
    fun storedEntity_shouldPreserveDetailFieldsViaRoom() =
        runTest {
            val entity = createParkEntity()

            parkDao.upsertPark(entity)

            val stored = parkDao.getParkById(entity.id)
            assertNotNull(stored)
            assertEquals(entity.author, stored?.author)
            assertEquals(entity.photos, stored?.photos)
            assertEquals(entity.comments, stored?.comments)
            assertEquals(entity.trainingUsers, stored?.trainingUsers)
            assertEquals(entity.trainHere, stored?.trainHere)
            assertEquals(entity.mine, stored?.mine)
            assertEquals(entity.canEdit, stored?.canEdit)
            assertEquals(entity.equipmentIDS, stored?.equipmentIDS)
            assertEquals(entity.createDate, stored?.createDate)
            assertEquals(entity.modifyDate, stored?.modifyDate)
        }

    @Test
    fun deleteById_whenEntityExists_shouldRemoveEntity() =
        runTest {
            val entity = createParkEntity()
            parkDao.upsertPark(entity)

            parkDao.deleteById(entity.id)

            val stored = parkDao.getParkById(entity.id)
            assertNull(stored)
        }

    private fun createParkEntity(
        id: Long = 1L,
        name: String = "Test Park",
        commentsCount: Int? = 3
    ) = ParkEntity(
        id = id,
        name = name,
        sizeID = 2,
        typeID = 1,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "Moscow",
        cityID = 1,
        countryID = 1,
        preview = "https://example.com/preview.jpg",
        commentsCount = commentsCount,
        trainingUsersCount = 5,
        author = testUser,
        photos = listOf(testPhoto, Photo(id = 11L, photo = "https://example.com/photo2.jpg")),
        comments = listOf(testComment),
        trainingUsers = listOf(testUser, User(id = 2L, name = "user2", image = null)),
        trainHere = true,
        mine = false,
        canEdit = true,
        equipmentIDS = listOf(1, 2, 3),
        createDate = "2024-01-01",
        modifyDate = "2024-06-15"
    )
}
