package com.swparks.model

import com.swparks.data.model.Event
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты десериализации модели [com.swparks.data.model.Event]
 * с использованием IntStringSerializer и LongStringSerializer
 */
class EventDeserializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun deserialize_whenAllInts_thenReturnsEvent() {
        // Given
        val jsonString = """
            {
                "id": 123,
                "title": "Test Event",
                "description": "Test Description",
                "begin_date": "2024-01-15T10:30:00Z",
                "country_id": 1,
                "city_id": 2,
                "comment_count": 3,
                "preview": "preview.jpg",
                "area_id": 456,
                "latitude": "55.75",
                "longitude": "37.61",
                "user_count": 4,
                "is_current": true,
                "address": "Test Address",
                "photos": [{"id": 1, "photo": "photo1.jpg"}],
                "training_users": [],
                "author": {"id": 1, "name": "testuser", "image": "avatar.jpg", "lang": "ru"},
                "name": "Test Name",
                "comments": [],
                "is_organizer": true,
                "can_edit": true,
                "train_here": true
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Event>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals("Test Event", result.title)
        assertEquals("Test Description", result.description)
        assertEquals(1, result.countryID)
        assertEquals(2, result.cityID)
        assertEquals(3, result.commentsCount)
        assertEquals(456L, result.parkID)
        assertEquals(4, result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenAllStrings_thenReturnsEvent() {
        // Given
        val jsonString = """
            {
                "id": "123",
                "title": "Test Event",
                "description": "Test Description",
                "begin_date": "2024-01-15T10:30:00Z",
                "country_id": "1",
                "city_id": "2",
                "comment_count": "3",
                "preview": "preview.jpg",
                "area_id": "456",
                "latitude": "55.75",
                "longitude": "37.61",
                "user_count": "4",
                "is_current": true,
                "address": "Test Address",
                "photos": [{"id": 1, "photo": "photo1.jpg"}],
                "training_users": [],
                "author": {"id": 1, "name": "testuser", "image": "avatar.jpg", "lang": "ru"},
                "name": "Test Name",
                "comments": [],
                "is_organizer": true,
                "can_edit": true,
                "train_here": true
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Event>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals("Test Event", result.title)
        assertEquals("Test Description", result.description)
        assertEquals(1, result.countryID)
        assertEquals(2, result.cityID)
        assertEquals(3, result.commentsCount)
        assertEquals(456L, result.parkID)
        assertEquals(4, result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenMixedIntsAndStrings_thenReturnsEvent() {
        // Given
        val jsonString = """
            {
                "id": "123",
                "title": "Test Event",
                "description": "Test Description",
                "begin_date": "2024-01-15T10:30:00Z",
                "country_id": 1,
                "city_id": "2",
                "comment_count": null,
                "preview": "preview.jpg",
                "area_id": null,
                "latitude": "55.75",
                "longitude": "37.61",
                "user_count": null,
                "is_current": true,
                "address": "Test Address",
                "photos": [{"id": 1, "photo": "photo1.jpg"}],
                "training_users": [],
                "author": {"id": 1, "name": "testuser", "image": "avatar.jpg", "lang": "ru"},
                "name": "Test Name",
                "comments": [],
                "is_organizer": true,
                "can_edit": true,
                "train_here": true
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Event>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals("Test Event", result.title)
        assertEquals("Test Description", result.description)
        assertEquals(1, result.countryID)
        assertEquals(2, result.cityID)
        assertNull(result.commentsCount)
        assertNull(result.parkID)
        assertNull(result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenZeroValues_thenReturnsEvent() {
        // Given
        val jsonString = """
            {
                "id": "123",
                "title": "Test Event",
                "description": "Test Description",
                "begin_date": "2024-01-15T10:30:00Z",
                "country_id": 0,
                "city_id": "0",
                "comment_count": 0,
                "preview": "preview.jpg",
                "area_id": "0",
                "latitude": "55.75",
                "longitude": "37.61",
                "user_count": "0",
                "is_current": true,
                "address": "Test Address",
                "photos": [{"id": 1, "photo": "photo1.jpg"}],
                "training_users": [],
                "author": {"id": 1, "name": "testuser", "image": "avatar.jpg", "lang": "ru"},
                "name": "Test Name",
                "comments": [],
                "is_organizer": true,
                "can_edit": true,
                "train_here": true
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Event>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals(0, result.countryID)
        assertEquals(0, result.cityID)
        assertEquals(0, result.commentsCount)
        assertEquals(0L, result.parkID)
        assertEquals(0, result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenNegativeValues_thenReturnsEvent() {
        // Given
        val jsonString = """
            {
                "id": "123",
                "title": "Test Event",
                "description": "Test Description",
                "begin_date": "2024-01-15T10:30:00Z",
                "country_id": -1,
                "city_id": "-2",
                "comment_count": -3,
                "preview": "preview.jpg",
                "area_id": "-456",
                "latitude": "55.75",
                "longitude": "37.61",
                "user_count": -4,
                "is_current": true,
                "address": "Test Address",
                "photos": [{"id": 1, "photo": "photo1.jpg"}],
                "training_users": [],
                "author": {"id": 1, "name": "testuser", "image": "avatar.jpg", "lang": "ru"},
                "name": "Test Name",
                "comments": [],
                "is_organizer": true,
                "can_edit": true,
                "train_here": true
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Event>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals(-1, result.countryID)
        assertEquals(-2, result.cityID)
        assertEquals(-3, result.commentsCount)
        assertEquals(-456L, result.parkID)
        assertEquals(-4, result.trainingUsersCount)
    }
}
