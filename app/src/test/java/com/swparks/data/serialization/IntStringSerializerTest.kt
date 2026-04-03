package com.swparks.data.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Тесты для [IntStringSerializer] - десериализации чисел из строк и чисел
 */
class IntStringSerializerTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun deserialize_whenInt_thenReturnsInt() {
        // Given
        val jsonString =
            """
            {"value": 123}
            """.trimIndent()

        // When
        val result = json.decodeFromString<TestModel>(jsonString)

        // Then
        assertEquals(123, result.value)
    }

    @Test
    fun deserialize_whenStringInt_thenReturnsInt() {
        // Given
        val jsonString =
            """
            {"value": "456"}
            """.trimIndent()

        // When
        val result = json.decodeFromString<TestModel>(jsonString)

        // Then
        assertEquals(456, result.value)
    }

    @Test
    fun deserialize_whenZero_thenReturnsZero() {
        // Given
        val jsonString =
            """
            {"value": 0}
            """.trimIndent()

        // When
        val result = json.decodeFromString<TestModel>(jsonString)

        // Then
        assertEquals(0, result.value)
    }

    @Test
    fun deserialize_whenStringZero_thenReturnsZero() {
        // Given
        val jsonString =
            """
            {"value": "0"}
            """.trimIndent()

        // When
        val result = json.decodeFromString<TestModel>(jsonString)

        // Then
        assertEquals(0, result.value)
    }

    @Test
    fun deserialize_whenNegativeInt_thenReturnsNegativeInt() {
        // Given
        val jsonString =
            """
            {"value": -789}
            """.trimIndent()

        // When
        val result = json.decodeFromString<TestModel>(jsonString)

        // Then
        assertEquals(-789, result.value)
    }

    @Test
    fun deserialize_whenNegativeStringInt_thenReturnsNegativeInt() {
        // Given
        val jsonString =
            """
            {"value": "-789"}
            """.trimIndent()

        // When
        val result = json.decodeFromString<TestModel>(jsonString)

        // Then
        assertEquals(-789, result.value)
    }

    @Test
    fun deserialize_whenInvalidString_thenThrowsSerializationException() {
        // Given
        val jsonString =
            """
            {"value": "invalid"}
            """.trimIndent()

        // When/Then
        assertThrows(kotlinx.serialization.SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun serialize_whenInt_thenReturnsIntInJson() {
        // Given
        val model = TestModel(value = 123)

        // When
        val jsonString = json.encodeToString(TestModel.serializer(), model)

        // Then
        assertEquals("""{"value":123}""", jsonString)
    }

    @Test
    fun multipleFields_whenMixedIntAndString_thenAllDeserializedCorrectly() {
        // Given
        val jsonString =
            """
            {"value1": 100, "value2": "200", "value3": 300}
            """.trimIndent()

        // When
        val result = json.decodeFromString<MultipleTestModel>(jsonString)

        // Then
        assertEquals(100, result.value1)
        assertEquals(200, result.value2)
        assertEquals(300, result.value3)
    }

    /**
     * Тестовая модель с одним полем с IntStringSerializer
     */
    @Serializable
    data class TestModel(
        @Serializable(with = IntStringSerializer::class)
        val value: Int
    )

    /**
     * Тестовая модель с несколькими полями с IntStringSerializer
     */
    @Serializable
    data class MultipleTestModel(
        @Serializable(with = IntStringSerializer::class)
        val value1: Int,
        @Serializable(with = IntStringSerializer::class)
        val value2: Int,
        @Serializable(with = IntStringSerializer::class)
        val value3: Int
    )
}
