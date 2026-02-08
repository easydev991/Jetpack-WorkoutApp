package com.swparks.model

import com.swparks.data.model.Comment
import com.swparks.data.model.Event
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTest {
    // Вспомогательные методы для создания тестовых данных
    private fun createTestUser(id: Long = 1L) = User(
        id = id,
        name = "testuser",
        image = "https://example.com/image.jpg"
    )

    @Suppress("LongParameterList")
    private fun createTestEvent(
        id: Long = 1L,
        description: String = "Test description",
        comments: List<Comment>? = null,
        photos: List<Photo> = emptyList(),
        trainingUsers: List<User>? = null,
        commentsCount: Int? = null,
        trainingUsersCount: Int? = null
    ) = Event(
        id = id,
        title = "Test Event",
        description = description,
        beginDate = "2024-01-01",
        countryID = 1,
        cityID = 1,
        commentsCount = commentsCount,
        preview = "https://example.com/preview.jpg",
        parkID = null,
        latitude = "55.7558",
        longitude = "37.6173",
        trainingUsersCount = trainingUsersCount,
        isCurrent = true,
        address = null,
        photos = photos,
        trainingUsers = trainingUsers,
        author = createTestUser(),
        name = "Test Event Name",
        comments = comments,
        isOrganizer = null,
        canEdit = null,
        trainHere = null
    )

    // Тесты для computed properties

    @Test
    fun hasDescription_whenDescriptionIsNotEmpty_thenReturnsTrue() {
        // Given
        val event = createTestEvent(description = "Test description")

        // When & Then
        assertTrue(event.hasDescription)
    }

    @Test
    fun hasDescription_whenDescriptionIsEmpty_thenReturnsFalse() {
        // Given
        val event = createTestEvent(description = "")

        // When & Then
        assertFalse(event.hasDescription)
    }

    @Test
    fun hasDescription_whenDescriptionIsBlank_thenReturnsFalse() {
        // Given
        val event = createTestEvent(description = "   ")

        // When & Then
        assertFalse(event.hasDescription)
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
        val event = createTestEvent(comments = listOf(comment))

        // When & Then
        assertTrue(event.hasComments)
    }

    @Test
    fun hasComments_whenCommentsIsNull_thenReturnsFalse() {
        // Given
        val event = createTestEvent(comments = null)

        // When & Then
        assertFalse(event.hasComments)
    }

    @Test
    fun hasComments_whenCommentsListIsEmpty_thenReturnsFalse() {
        // Given
        val event = createTestEvent(comments = emptyList())

        // When & Then
        assertFalse(event.hasComments)
    }

    @Test
    fun hasPhotos_whenPhotosListIsNotEmpty_thenReturnsTrue() {
        // Given
        val photo = Photo(id = 1L, photo = "https://example.com/photo.jpg")
        val event = createTestEvent(photos = listOf(photo))

        // When & Then
        assertTrue(event.hasPhotos)
    }

    @Test
    fun hasPhotos_whenPhotosListIsEmpty_thenReturnsFalse() {
        // Given
        val event = createTestEvent(photos = emptyList())

        // When & Then
        assertFalse(event.hasPhotos)
    }

    @Test
    fun hasParticipants_whenTrainingUsersListIsNotEmpty_thenReturnsTrue() {
        // Given
        val user = createTestUser()
        val event = createTestEvent(trainingUsers = listOf(user))

        // When & Then
        assertTrue(event.hasParticipants)
    }

    @Test
    fun hasParticipants_whenTrainingUsersIsNull_thenReturnsFalse() {
        // Given
        val event = createTestEvent(trainingUsers = null)

        // When & Then
        assertFalse(event.hasParticipants)
    }

    @Test
    fun hasParticipants_whenTrainingUsersListIsEmpty_thenReturnsFalse() {
        // Given
        val event = createTestEvent(trainingUsers = emptyList())

        // When & Then
        assertFalse(event.hasParticipants)
    }

    @Test
    fun shareLinkStringURL_whenEventHasId_thenReturnsCorrectUrl() {
        // Given
        val event = createTestEvent(id = 123L)

        // When & Then
        assertEquals("https://workout.su/trainings/123", event.shareLinkStringURL)
    }

    @Test
    fun shareLinkStringURL_whenEventHasDifferentId_thenReturnsCorrectUrl() {
        // Given
        val event = createTestEvent(id = 456L)

        // When & Then
        assertEquals("https://workout.su/trainings/456", event.shareLinkStringURL)
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
        val user = createTestUser()
        val event = createTestEvent(
            comments = listOf(comment),
            commentsCount = 1,
            trainingUsers = listOf(user),
            trainingUsersCount = 1
        )

        // When & Then
        assertTrue(event.isFull)
    }

    @Test
    fun isFull_whenCommentsNeedUpdate_thenReturnsFalse() {
        // Given
        val event = createTestEvent(
            comments = null,
            commentsCount = 5
        )

        // When & Then
        assertFalse(event.isFull)
    }

    @Test
    fun isFull_whenParticipantsNeedUpdate_thenReturnsFalse() {
        // Given
        val event = createTestEvent(
            trainingUsers = null,
            trainingUsersCount = 3
        )

        // When & Then
        assertFalse(event.isFull)
    }

    @Test
    fun isFull_whenBothNeedUpdate_thenReturnsFalse() {
        // Given
        val event = createTestEvent(
            comments = null,
            commentsCount = 5,
            trainingUsers = null,
            trainingUsersCount = 3
        )

        // When & Then
        assertFalse(event.isFull)
    }
}
