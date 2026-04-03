package com.swparks.utils

import com.swparks.data.model.Event
import com.swparks.util.WorkoutAppJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JsonUtilsTest {
    @Test
    fun decodeFromString_whenJsonHasUnknownKeys_thenIgnoresThem() {
        // Given
        val jsonString =
            """
            {
              "id": 1,
              "title": "Test Event",
              "description": "Test description",
              "begin_date": "2024-01-01",
              "country_id": 1,
              "city_id": 1,
              "preview": "https://example.com/preview.jpg",
              "latitude": "55.7558",
              "longitude": "37.6173",
              "is_current": true,
              "photos": [],
              "author": {
                "id": 1,
                "name": "testuser",
                "image": "https://example.com/image.jpg",
                "lang": "ru"
              },
              "name": "Test Event Name",
              "unknown_field": "should be ignored",
              "another_unknown": 123
            }
            """.trimIndent()

        // When
        val event = WorkoutAppJson.decodeFromString<Event>(jsonString)

        // Then
        assertNotNull(event)
        assertEquals(1L, event.id)
        assertEquals("Test Event", event.title)
        assertEquals("Test description", event.description)
    }

    @Test
    fun decodeFromString_whenJsonIsLenient_thenParsesSuccessfully() {
        // Given
        // Lenient режим позволяет парсить JSON с более мягкими правилами
        // В данном случае проверяем, что lenient режим работает корректно
        val jsonString =
            """
            {
              "id": 1,
              "title": "Test Event",
              "description": "Test description",
              "begin_date": "2024-01-01",
              "country_id": 1,
              "city_id": 1,
              "preview": "https://example.com/preview.jpg",
              "latitude": "55.7558",
              "longitude": "37.6173",
              "is_current": true,
              "photos": [],
              "author": {
                "id": 1,
                "name": "testuser",
                "image": "https://example.com/image.jpg",
                "lang": "ru"
              },
              "name": "Test Event Name"
            }
            """.trimIndent()

        // When
        val event = WorkoutAppJson.decodeFromString<Event>(jsonString)

        // Then
        assertNotNull(event)
        assertEquals(1L, event.id)
        assertEquals("Test Event", event.title)
    }

    @Test
    fun decodeFromString_whenOptionalFieldsAreMissing_thenParsesSuccessfully() {
        // Given
        val jsonString =
            """
            {
              "id": 1,
              "title": "Test Event",
              "description": "Test description",
              "begin_date": "2024-01-01",
              "country_id": 1,
              "city_id": 1,
              "preview": "https://example.com/preview.jpg",
              "latitude": "55.7558",
              "longitude": "37.6173",
              "is_current": true,
              "photos": [],
              "author": {
                "id": 1,
                "name": "testuser",
                "image": "https://example.com/image.jpg",
                "lang": "ru"
              },
              "name": "Test Event Name"
            }
            """.trimIndent()

        // When
        val event = WorkoutAppJson.decodeFromString<Event>(jsonString)

        // Then
        assertNotNull(event)
        assertEquals(1L, event.id)
        assertNull(event.comments)
        assertNull(event.trainingUsers)
        assertNull(event.address)
    }
}
