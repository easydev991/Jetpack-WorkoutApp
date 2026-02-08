package com.swparks.model

import com.swparks.data.model.Comment
import com.swparks.data.model.Park
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkTest {
    // Вспомогательные методы для создания тестовых данных
    private fun createTestUser(id: Long = 1L) = User(
        id = id,
        name = "testuser",
        image = "https://example.com/image.jpg"
    )

    @Suppress("LongParameterList")
    private fun createTestPark(
        id: Long = 1L,
        sizeID: Int = 1,
        typeID: Int = 1,
        comments: List<Comment>? = null,
        photos: List<Photo>? = null,
        trainingUsers: List<User>? = null,
        commentsCount: Int? = null,
        trainingUsersCount: Int? = null,
        createDate: String? = null,
        author: User? = createTestUser()
    ) = Park(
        id = id,
        name = "Test Park",
        sizeID = sizeID,
        typeID = typeID,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "Test Address",
        cityID = 1,
        countryID = 1,
        commentsCount = commentsCount,
        preview = "https://example.com/preview.jpg",
        trainingUsersCount = trainingUsersCount,
        createDate = createDate,
        modifyDate = null,
        author = author,
        photos = photos,
        comments = comments,
        trainHere = null,
        equipmentIDS = null,
        mine = null,
        canEdit = null,
        trainingUsers = trainingUsers
    )

    // Тесты для computed properties

    @Test
    fun size_whenSizeIDIs1_thenReturnsSmall() {
        // Given
        val park = createTestPark(sizeID = 1)

        // When & Then
        assertEquals(ParkSize.SMALL, park.size)
    }

    @Test
    fun size_whenSizeIDIs2_thenReturnsMedium() {
        // Given
        val park = createTestPark(sizeID = 2)

        // When & Then
        assertEquals(ParkSize.MEDIUM, park.size)
    }

    @Test
    fun size_whenSizeIDIs3_thenReturnsLarge() {
        // Given
        val park = createTestPark(sizeID = 3)

        // When & Then
        assertEquals(ParkSize.LARGE, park.size)
    }

    @Test
    fun size_whenSizeIDIsInvalid_thenReturnsNull() {
        // Given
        val park = createTestPark(sizeID = 999)

        // When & Then
        assertNull(park.size)
    }

    @Test
    fun type_whenTypeIDIs1_thenReturnsSoviet() {
        // Given
        val park = createTestPark(typeID = 1)

        // When & Then
        assertEquals(ParkType.SOVIET, park.type)
    }

    @Test
    fun type_whenTypeIDIs2_thenReturnsModern() {
        // Given
        val park = createTestPark(typeID = 2)

        // When & Then
        assertEquals(ParkType.MODERN, park.type)
    }

    @Test
    fun type_whenTypeIDIs3_thenReturnsCollars() {
        // Given
        val park = createTestPark(typeID = 3)

        // When & Then
        assertEquals(ParkType.COLLARS, park.type)
    }

    @Test
    fun type_whenTypeIDIs6_thenReturnsLegendary() {
        // Given
        val park = createTestPark(typeID = 6)

        // When & Then
        assertEquals(ParkType.LEGENDARY, park.type)
    }

    @Test
    fun type_whenTypeIDIsInvalid_thenReturnsNull() {
        // Given
        val park = createTestPark(typeID = 999)

        // When & Then
        assertNull(park.type)
    }

    @Test
    fun hasComments_whenCommentsListIsNotEmpty_thenReturnsTrue() {
        // Given
        val comment = Comment(
            id = 1L,
            body = "Test comment",
            date = "2024-01-01",
            user = createTestUser()
        )
        val park = createTestPark(comments = listOf(comment))

        // When & Then
        assertTrue(park.hasComments)
    }

    @Test
    fun hasComments_whenCommentsIsNull_thenReturnsFalse() {
        // Given
        val park = createTestPark(comments = null)

        // When & Then
        assertFalse(park.hasComments)
    }

    @Test
    fun hasComments_whenCommentsListIsEmpty_thenReturnsFalse() {
        // Given
        val park = createTestPark(comments = emptyList())

        // When & Then
        assertFalse(park.hasComments)
    }

    @Test
    fun hasPhotos_whenPhotosListIsNotEmpty_thenReturnsTrue() {
        // Given
        val photo = Photo(id = 1L, photo = "https://example.com/photo.jpg")
        val park = createTestPark(photos = listOf(photo))

        // When & Then
        assertTrue(park.hasPhotos)
    }

    @Test
    fun hasPhotos_whenPhotosIsNull_thenReturnsFalse() {
        // Given
        val park = createTestPark(photos = null)

        // When & Then
        assertFalse(park.hasPhotos)
    }

    @Test
    fun hasPhotos_whenPhotosListIsEmpty_thenReturnsFalse() {
        // Given
        val park = createTestPark(photos = emptyList())

        // When & Then
        assertFalse(park.hasPhotos)
    }

    @Test
    fun hasParticipants_whenTrainingUsersListIsNotEmpty_thenReturnsTrue() {
        // Given
        val user = createTestUser()
        val park = createTestPark(trainingUsers = listOf(user))

        // When & Then
        assertTrue(park.hasParticipants)
    }

    @Test
    fun hasParticipants_whenTrainingUsersIsNull_thenReturnsFalse() {
        // Given
        val park = createTestPark(trainingUsers = null)

        // When & Then
        assertFalse(park.hasParticipants)
    }

    @Test
    fun hasParticipants_whenTrainingUsersListIsEmpty_thenReturnsFalse() {
        // Given
        val park = createTestPark(trainingUsers = emptyList())

        // When & Then
        assertFalse(park.hasParticipants)
    }

    @Test
    fun shareLinkStringURL_whenParkHasId_thenReturnsCorrectUrl() {
        // Given
        val park = createTestPark(id = 123L)

        // When & Then
        assertEquals("https://workout.su/areas/123", park.shareLinkStringURL)
    }

    @Test
    fun shareLinkStringURL_whenParkHasDifferentId_thenReturnsCorrectUrl() {
        // Given
        val park = createTestPark(id = 456L)

        // When & Then
        assertEquals("https://workout.su/areas/456", park.shareLinkStringURL)
    }

    @Test
    fun isFull_whenAllDataIsPresent_thenReturnsTrue() {
        // Given
        val comment = Comment(
            id = 1L,
            body = "Test comment",
            date = "2024-01-01",
            user = createTestUser()
        )
        val photo = Photo(id = 1L, photo = "https://example.com/photo.jpg")
        val user = createTestUser()
        val park = createTestPark(
            createDate = "2024-01-01",
            author = createTestUser(),
            photos = listOf(photo),
            comments = listOf(comment),
            commentsCount = 1,
            trainingUsers = listOf(user),
            trainingUsersCount = 1
        )

        // When & Then
        assertTrue(park.isFull)
    }

    @Test
    fun isFull_whenCreateDateIsNull_thenReturnsFalse() {
        // Given
        val park = createTestPark(
            createDate = null,
            author = createTestUser(),
            photos = listOf(Photo(id = 1L, photo = "https://example.com/photo.jpg"))
        )

        // When & Then
        assertFalse(park.isFull)
    }

    @Test
    fun isFull_whenAuthorIsNull_thenReturnsFalse() {
        // Given
        val park = createTestPark(
            createDate = "2024-01-01",
            author = null,
            photos = listOf(Photo(id = 1L, photo = "https://example.com/photo.jpg"))
        )

        // When & Then
        assertFalse(park.isFull)
    }

    @Test
    fun isFull_whenHasNoPhotos_thenReturnsFalse() {
        // Given
        val park = createTestPark(
            createDate = "2024-01-01",
            author = createTestUser(),
            photos = null
        )

        // When & Then
        assertFalse(park.isFull)
    }

    @Test
    fun isFull_whenCommentsNeedUpdate_thenReturnsFalse() {
        // Given
        val park = createTestPark(
            createDate = "2024-01-01",
            author = createTestUser(),
            photos = listOf(Photo(id = 1L, photo = "https://example.com/photo.jpg")),
            comments = null,
            commentsCount = 5
        )

        // When & Then
        assertFalse(park.isFull)
    }

    @Test
    fun isFull_whenParticipantsNeedUpdate_thenReturnsFalse() {
        // Given
        val park = createTestPark(
            createDate = "2024-01-01",
            author = createTestUser(),
            photos = listOf(Photo(id = 1L, photo = "https://example.com/photo.jpg")),
            trainingUsers = null,
            trainingUsersCount = 3
        )

        // When & Then
        assertFalse(park.isFull)
    }

    @Test
    fun isFull_whenMultipleConditionsFail_thenReturnsFalse() {
        // Given
        val park = createTestPark(
            createDate = null,
            author = null,
            photos = listOf(Photo(id = 1L, photo = "https://example.com/photo.jpg"))
        )

        // When & Then
        assertFalse(park.isFull)
    }
}
