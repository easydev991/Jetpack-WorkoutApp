package com.swparks.data.database.entity

import com.swparks.data.model.Comment
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParkEntityTest {
    private val testUser =
        User(
            id = 1L,
            name = "testuser",
            image = "https://example.com/avatar.jpg"
        )

    private val testPhoto = Photo(id = 10L, photo = "https://example.com/photo.jpg")

    private val testComment =
        Comment(
            id = 100L,
            body = "Test comment",
            date = "2024-01-15",
            user = testUser
        )

    @Suppress("LongParameterList")
    private fun createFullPark(
        id: Long = 1L,
        commentsCount: Int? = 3,
        trainingUsersCount: Int? = 5
    ) = Park(
        id = id,
        name = "Full Park",
        sizeID = 2,
        typeID = 1,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "Moscow",
        cityID = 1,
        countryID = 1,
        commentsCount = commentsCount,
        preview = "https://example.com/preview.jpg",
        trainingUsersCount = trainingUsersCount,
        createDate = "2024-01-01",
        modifyDate = "2024-06-15",
        author = testUser,
        photos = listOf(testPhoto, Photo(id = 11L, photo = "https://example.com/photo2.jpg")),
        comments = listOf(testComment),
        trainHere = true,
        equipmentIDS = listOf(1, 2, 3),
        mine = false,
        canEdit = true,
        trainingUsers = listOf(testUser, User(id = 2L, name = "user2", image = null))
    )

    @Test
    fun toEntity_whenFullPark_thenPreservesAllDetailFields() {
        val park = createFullPark()

        val entity = park.toEntity()

        assertEquals(park.id, entity.id)
        assertEquals(park.name, entity.name)
        assertEquals(park.sizeID, entity.sizeID)
        assertEquals(park.typeID, entity.typeID)
        assertEquals(park.longitude, entity.longitude)
        assertEquals(park.latitude, entity.latitude)
        assertEquals(park.address, entity.address)
        assertEquals(park.cityID, entity.cityID)
        assertEquals(park.countryID, entity.countryID)
        assertEquals(park.preview, entity.preview)
        assertEquals(park.commentsCount, entity.commentsCount)
        assertEquals(park.trainingUsersCount, entity.trainingUsersCount)
        assertEquals(park.createDate, entity.createDate)
        assertEquals(park.modifyDate, entity.modifyDate)
        assertEquals(park.author, entity.author)
        assertEquals(park.photos, entity.photos)
        assertEquals(park.comments, entity.comments)
        assertEquals(park.trainHere, entity.trainHere)
        assertEquals(park.equipmentIDS, entity.equipmentIDS)
        assertEquals(park.mine, entity.mine)
        assertEquals(park.canEdit, entity.canEdit)
        assertEquals(park.trainingUsers, entity.trainingUsers)
    }

    @Test
    fun toPark_whenFullParkEntity_thenRestoresAllDetailFields() {
        val entity = createFullPark().toEntity()

        val park = entity.toPark()

        assertEquals(entity.id, park.id)
        assertEquals(entity.name, park.name)
        assertEquals(entity.sizeID, park.sizeID)
        assertEquals(entity.typeID, park.typeID)
        assertEquals(entity.longitude, park.longitude)
        assertEquals(entity.latitude, park.latitude)
        assertEquals(entity.address, park.address)
        assertEquals(entity.cityID, park.cityID)
        assertEquals(entity.countryID, park.countryID)
        assertEquals(entity.preview, park.preview)
        assertEquals(entity.commentsCount, park.commentsCount)
        assertEquals(entity.trainingUsersCount, park.trainingUsersCount)
        assertEquals(entity.createDate, park.createDate)
        assertEquals(entity.modifyDate, park.modifyDate)
        assertEquals(entity.author, park.author)
        assertEquals(entity.photos, park.photos)
        assertEquals(entity.comments, park.comments)
        assertEquals(entity.trainHere, park.trainHere)
        assertEquals(entity.equipmentIDS, park.equipmentIDS)
        assertEquals(entity.mine, park.mine)
        assertEquals(entity.canEdit, park.canEdit)
        assertEquals(entity.trainingUsers, park.trainingUsers)
    }

    @Test
    fun toEntity_whenNullComplexFields_thenPreservesNullValues() {
        val park =
            Park(
                id = 1L,
                name = "Short Park",
                sizeID = 1,
                typeID = 1,
                longitude = "0",
                latitude = "0",
                address = "",
                cityID = 1,
                countryID = 1,
                preview = "",
                commentsCount = null,
                trainingUsersCount = null,
                createDate = null,
                modifyDate = null,
                author = null,
                photos = null,
                comments = null,
                trainHere = null,
                equipmentIDS = null,
                mine = null,
                canEdit = null,
                trainingUsers = null
            )

        val entity = park.toEntity()

        assertNull(entity.commentsCount)
        assertNull(entity.trainingUsersCount)
        assertNull(entity.createDate)
        assertNull(entity.modifyDate)
        assertNull(entity.author)
        assertNull(entity.photos)
        assertNull(entity.comments)
        assertNull(entity.trainHere)
        assertNull(entity.equipmentIDS)
        assertNull(entity.mine)
        assertNull(entity.canEdit)
        assertNull(entity.trainingUsers)
    }

    @Test
    fun toPark_whenEntityWithNullComplexFields_thenPreservesNullValues() {
        val entity =
            ParkEntity(
                id = 1L,
                name = "Short Park",
                sizeID = 1,
                typeID = 1,
                longitude = "0",
                latitude = "0",
                address = "",
                cityID = 1,
                countryID = 1,
                preview = ""
            )

        val park = entity.toPark()

        assertNull(park.commentsCount)
        assertNull(park.trainingUsersCount)
        assertNull(park.createDate)
        assertNull(park.modifyDate)
        assertNull(park.author)
        assertNull(park.photos)
        assertNull(park.comments)
        assertNull(park.trainHere)
        assertNull(park.equipmentIDS)
        assertNull(park.mine)
        assertNull(park.canEdit)
        assertNull(park.trainingUsers)
    }

    @Test
    fun roundTrip_whenToEntityToPark_thenPreservesNullableCountersSemantics() {
        val parkWithNullCounters =
            Park(
                id = 1L,
                name = "Park",
                sizeID = 1,
                typeID = 1,
                longitude = "0",
                latitude = "0",
                address = "",
                cityID = 1,
                countryID = 1,
                preview = "",
                commentsCount = null,
                trainingUsersCount = null
            )

        val restored = parkWithNullCounters.toEntity().toPark()

        assertNull(restored.commentsCount)
        assertNull(restored.trainingUsersCount)

        val parkWithZeroCounters =
            Park(
                id = 1L,
                name = "Park",
                sizeID = 1,
                typeID = 1,
                longitude = "0",
                latitude = "0",
                address = "",
                cityID = 1,
                countryID = 1,
                preview = "",
                commentsCount = 0,
                trainingUsersCount = 0
            )

        val restoredZero = parkWithZeroCounters.toEntity().toPark()

        assertNotNull(restoredZero.commentsCount)
        assertNotNull(restoredZero.trainingUsersCount)
        assertEquals(0, restoredZero.commentsCount)
        assertEquals(0, restoredZero.trainingUsersCount)
    }
}
