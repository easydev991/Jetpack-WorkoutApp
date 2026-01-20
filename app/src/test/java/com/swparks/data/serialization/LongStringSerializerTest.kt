package com.swparks.data.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Тесты для [LongStringSerializer] - десериализации чисел Long из строк и чисел
 */
class LongStringSerializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun deserialize_whenLong_thenReturnsLong() {
        // Given
        val jsonString = """
            {"value": 1234567890}
        """.trimIndent()

        // When
        val result = json.decodeFromString<LongTestModel>(jsonString)

        // Then
        assertEquals(1234567890L, result.value)
    }

    @Test
    fun deserialize_whenStringLong_thenReturnsLong() {
        // Given
        val jsonString = """
            {"value": "9876543210"}
        """.trimIndent()

        // When
        val result = json.decodeFromString<LongTestModel>(jsonString)

        // Then
        assertEquals(9876543210L, result.value)
    }

    @Test
    fun deserialize_whenZero_thenReturnsZero() {
        // Given
        val jsonString = """
            {"value": 0}
        """.trimIndent()

        // When
        val result = json.decodeFromString<LongTestModel>(jsonString)

        // Then
        assertEquals(0L, result.value)
    }

    @Test
    fun deserialize_whenStringZero_thenReturnsZero() {
        // Given
        val jsonString = """
            {"value": "0"}
        """.trimIndent()

        // When
        val result = json.decodeFromString<LongTestModel>(jsonString)

        // Then
        assertEquals(0L, result.value)
    }

    @Test
    fun deserialize_whenNegativeLong_thenReturnsNegativeLong() {
        // Given
        val jsonString = """
            {"value": -1234567890}
        """.trimIndent()

        // When
        val result = json.decodeFromString<LongTestModel>(jsonString)

        // Then
        assertEquals(-1234567890L, result.value)
    }

    @Test
    fun deserialize_whenNegativeStringLong_thenReturnsNegativeLong() {
        // Given
        val jsonString = """
            {"value": "-9876543210"}
        """.trimIndent()

        // When
        val result = json.decodeFromString<LongTestModel>(jsonString)

        // Then
        assertEquals(-9876543210L, result.value)
    }

    @Test
    fun deserialize_whenInvalidString_thenThrowsSerializationException() {
        // Given
        val jsonString = """
            {"value": "invalid"}
        """.trimIndent()

        // When/Then
        assertThrows(kotlinx.serialization.SerializationException::class.java) {
            json.decodeFromString<LongTestModel>(jsonString)
        }
    }

    @Test
    fun serialize_whenLong_thenReturnsLongInJson() {
        // Given
        val model = LongTestModel(value = 1234567890L)

        // When
        val jsonString = json.encodeToString(LongTestModel.serializer(), model)

        // Then
        assertEquals("""{"value":1234567890}""", jsonString)
    }

    @Test
    fun multipleFields_whenMixedLongAndString_thenAllDeserializedCorrectly() {
        // Given
        val jsonString = """
            {"value1": 10000000000, "value2": "20000000000", "value3": 30000000000}
        """.trimIndent()

        // When
        val result = json.decodeFromString<MultipleLongTestModel>(jsonString)

        // Then
        assertEquals(10000000000L, result.value1)
        assertEquals(20000000000L, result.value2)
        assertEquals(30000000000L, result.value3)
    }

    /**
     * Тестовая модель с одним полем с LongStringSerializer
     */
    @Serializable
    data class LongTestModel(
        @Serializable(with = LongStringSerializer::class)
        val value: Long
    )

    /**
     * Тестовая модель с несколькими полями с LongStringSerializer
     */
    @Serializable
    data class MultipleLongTestModel(
        @Serializable(with = LongStringSerializer::class)
        val value1: Long,
        @Serializable(with = LongStringSerializer::class)
        val value2: Long,
        @Serializable(with = LongStringSerializer::class)
        val value3: Long
    )
}
