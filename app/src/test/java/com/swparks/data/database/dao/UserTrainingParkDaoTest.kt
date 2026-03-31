package com.swparks.data.database.dao

import android.content.Context
import androidx.room.Room
import com.swparks.data.database.SWDatabase
import com.swparks.data.database.entity.ParkEntity
import com.swparks.data.database.entity.UserTrainingParkEntity
import com.swparks.data.model.Comment
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UserTrainingParkDaoTest {

    private lateinit var database: SWDatabase
    private lateinit var userTrainingParkDao: UserTrainingParkDao
    private lateinit var parkDao: ParkDao

    private val testUser = User(
        id = 1L,
        name = "testuser",
        image = "https://example.com/avatar.jpg"
    )

    private val testPhoto = Photo(
        id = 10L,
        photo = "https://example.com/photo.jpg"
    )

    private val testComment = Comment(
        id = 100L,
        body = "Test comment",
        date = "2024-01-15",
        user = testUser
    )

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        database = Room.inMemoryDatabaseBuilder(context, SWDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userTrainingParkDao = database.userTrainingParkDao()
        parkDao = database.parkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertForUser_whenNewRelations_thenStoresUserParkIds() = runTest {
        val relations = listOf(
            UserTrainingParkEntity(userId = 1L, parkId = 100L),
            UserTrainingParkEntity(userId = 1L, parkId = 200L),
            UserTrainingParkEntity(userId = 1L, parkId = 300L)
        )

        userTrainingParkDao.insertForUser(relations)

        val storedIds = userTrainingParkDao.getParkIdsForUser(1L)
        assertEquals(3, storedIds.size)
        assertTrue(storedIds.contains(100L))
        assertTrue(storedIds.contains(200L))
        assertTrue(storedIds.contains(300L))
    }

    @Test
    fun replaceForUser_whenExistingRelations_thenOldRelationsRemoved() = runTest {
        val initialRelations = listOf(
            UserTrainingParkEntity(userId = 1L, parkId = 100L),
            UserTrainingParkEntity(userId = 1L, parkId = 200L)
        )
        userTrainingParkDao.insertForUser(initialRelations)

        val newRelations = listOf(
            UserTrainingParkEntity(userId = 1L, parkId = 300L),
            UserTrainingParkEntity(userId = 1L, parkId = 400L)
        )
        userTrainingParkDao.replaceForUser(1L, newRelations)

        val storedIds = userTrainingParkDao.getParkIdsForUser(1L)
        assertEquals(2, storedIds.size)
        assertTrue(storedIds.contains(300L))
        assertTrue(storedIds.contains(400L))
        assertFalse(storedIds.contains(100L))
        assertFalse(storedIds.contains(200L))
    }

    @Test
    fun getParkIdsForUser_whenNoRelations_thenReturnsEmptyList() = runTest {
        val result = userTrainingParkDao.getParkIdsForUser(999L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun hasCachedParks_whenCacheInitialized_thenReturnsTrue() = runTest {
        val relations = listOf(
            UserTrainingParkEntity(userId = 1L, parkId = 100L)
        )
        userTrainingParkDao.replaceForUser(1L, relations)

        val hasCache = userTrainingParkDao.hasCachedParksForUser(1L)

        assertTrue(hasCache)
    }

    @Test
    fun hasCachedParks_whenNoRelations_thenReturnsFalse() = runTest {
        val hasCache = userTrainingParkDao.hasCachedParksForUser(999L)

        assertFalse(hasCache)
    }

    @Test
    fun hasCachedParks_whenCacheInitializedWithEmptyList_thenReturnsTrue() = runTest {
        userTrainingParkDao.replaceForUser(1L, emptyList())

        val hasCache = userTrainingParkDao.hasCachedParksForUser(1L)

        assertTrue(hasCache)
    }

    @Test
    fun clearForUser_whenCalled_thenRemovesOnlySpecifiedUserRelations() = runTest {
        val user1Relations = listOf(
            UserTrainingParkEntity(userId = 1L, parkId = 100L),
            UserTrainingParkEntity(userId = 1L, parkId = 200L)
        )
        val user2Relations = listOf(
            UserTrainingParkEntity(userId = 2L, parkId = 300L)
        )
        userTrainingParkDao.insertForUser(user1Relations)
        userTrainingParkDao.insertForUser(user2Relations)

        userTrainingParkDao.clearForUser(1L)

        val user1Parks = userTrainingParkDao.getParkIdsForUser(1L)
        val user2Parks = userTrainingParkDao.getParkIdsForUser(2L)
        assertTrue(user1Parks.isEmpty())
        assertEquals(1, user2Parks.size)
        assertTrue(user2Parks.contains(300L))
    }

    @Test
    fun getParksForUserFromCache_whenRelationsAndParksExist_thenReturnsJoinedParks() = runTest {
        val park1 = createParkEntity(id = 100L, name = "Park One")
        val park2 = createParkEntity(id = 200L, name = "Park Two")
        parkDao.upsertPark(park1)
        parkDao.upsertPark(park2)

        val relations = listOf(
            UserTrainingParkEntity(userId = 1L, parkId = 100L),
            UserTrainingParkEntity(userId = 1L, parkId = 200L)
        )
        userTrainingParkDao.insertForUser(relations)

        val parks = userTrainingParkDao.getParksForUserFromCache(1L)

        assertEquals(2, parks.size)
    }

    @Test
    fun getParksForUserFromCache_whenRelationsMissing_thenReturnsEmptyList() = runTest {
        val parks = userTrainingParkDao.getParksForUserFromCache(999L)

        assertTrue(parks.isEmpty())
    }

    private fun createParkEntity(
        id: Long = 1L,
        name: String = "Test Park"
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
        commentsCount = 3,
        trainingUsersCount = 5,
        author = testUser,
        photos = listOf(testPhoto),
        comments = listOf(testComment),
        trainingUsers = listOf(testUser),
        trainHere = true,
        mine = false,
        canEdit = true,
        equipmentIDS = listOf(1, 2, 3),
        createDate = "2024-01-01",
        modifyDate = "2024-06-15"
    )
}
