package com.swparks.model

import com.swparks.data.model.Park
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты десериализации модели [com.swparks.data.model.Park] с использованием IntStringSerializer и LongStringSerializer
 */
class ParkDeserializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun deserialize_whenAllInts_thenReturnsPark() {
        // Given
        val jsonString = """
            {
                "id": 123,
                "name": "Test Park",
                "class_id": 1,
                "type_id": 2,
                "longitude": "55.75",
                "latitude": "37.61",
                "address": "Test Address",
                "city_id": 3,
                "country_id": 4,
                "comments_count": 5,
                "preview": "preview.jpg",
                "trainings": 6,
                "create_date": "2024-01-15T10:30:00Z",
                "modify_date": "2024-01-16T10:30:00Z"
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Park>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals("Test Park", result.name)
        assertEquals(1, result.sizeID)
        assertEquals(2, result.typeID)
        assertEquals(3, result.cityID)
        assertEquals(4, result.countryID)
        assertEquals(5, result.commentsCount)
        assertEquals(6, result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenAllStrings_thenReturnsPark() {
        // Given
        val jsonString = """
            {
                "id": "123",
                "name": "Test Park",
                "class_id": "1",
                "type_id": "2",
                "longitude": "55.75",
                "latitude": "37.61",
                "address": "Test Address",
                "city_id": "3",
                "country_id": "4",
                "comments_count": "5",
                "preview": "preview.jpg",
                "trainings": "6",
                "create_date": "2024-01-15T10:30:00Z",
                "modify_date": "2024-01-16T10:30:00Z"
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Park>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals("Test Park", result.name)
        assertEquals(1, result.sizeID)
        assertEquals(2, result.typeID)
        assertEquals(3, result.cityID)
        assertEquals(4, result.countryID)
        assertEquals(5, result.commentsCount)
        assertEquals(6, result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenMixedIntsAndStrings_thenReturnsPark() {
        // Given
        val jsonString = """
            {
                "id": "123",
                "name": "Test Park",
                "class_id": 1,
                "type_id": "2",
                "longitude": "55.75",
                "latitude": "37.61",
                "address": "Test Address",
                "city_id": "3",
                "country_id": "4",
                "comments_count": null,
                "preview": "preview.jpg",
                "trainings": null,
                "create_date": "2024-01-15T10:30:00Z",
                "modify_date": "2024-01-16T10:30:00Z"
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Park>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals("Test Park", result.name)
        assertEquals(1, result.sizeID)
        assertEquals(2, result.typeID)
        assertEquals(3, result.cityID)
        assertEquals(4, result.countryID)
        assertEquals(null, result.commentsCount)
        assertEquals(null, result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenZeroValues_thenReturnsPark() {
        // Given
        val jsonString = """
            {
                "id": "0",
                "name": "Test Park",
                "class_id": 0,
                "type_id": "0",
                "longitude": "55.75",
                "latitude": "37.61",
                "address": "Test Address",
                "city_id": "0",
                "country_id": 0,
                "comments_count": 0,
                "preview": "preview.jpg",
                "trainings": 0
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Park>(jsonString)

        // Then
        assertEquals(0L, result.id)
        assertEquals(0, result.sizeID)
        assertEquals(0, result.typeID)
        assertEquals(0, result.cityID)
        assertEquals(0, result.countryID)
        assertEquals(0, result.commentsCount)
        assertEquals(0, result.trainingUsersCount)
    }

    @Test
    fun deserialize_whenNegativeValues_thenReturnsPark() {
        // Given
        val jsonString = """
            {
                "id": "123",
                "name": "Test Park",
                "class_id": -1,
                "type_id": "-2",
                "longitude": "55.75",
                "latitude": "37.61",
                "address": "Test Address",
                "city_id": -3,
                "country_id": "-4",
                "comments_count": -5,
                "preview": "preview.jpg",
                "trainings": "-6"
            }
        """.trimIndent()

        // When
        val result = json.decodeFromString<Park>(jsonString)

        // Then
        assertEquals(123L, result.id)
        assertEquals(-1, result.sizeID)
        assertEquals(-2, result.typeID)
        assertEquals(-3, result.cityID)
        assertEquals(-4, result.countryID)
        assertEquals(-5, result.commentsCount)
        assertEquals(-6, result.trainingUsersCount)
    }
}
