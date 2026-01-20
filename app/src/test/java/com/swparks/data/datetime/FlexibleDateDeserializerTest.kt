package com.swparks.data.datetime

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit-тесты для FlexibleDateDeserializer
 */
class FlexibleDateDeserializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Тестовая модель для проверки десериализации дат
     */
    @kotlinx.serialization.Serializable
    private data class TestModel(
        @kotlinx.serialization.Serializable(with = FlexibleDateDeserializer::class)
        val date: String? = null
    )

    @Test
    fun deserialize_whenIso8601WithFractionalSeconds_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00.123Z"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00.123Z", result.date)
    }

    @Test
    fun deserialize_whenIso8601WithoutFractionalSeconds_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00Z"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00Z", result.date)
    }

    @Test
    fun deserialize_whenServerDateTimeNoTimezone_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00", result.date)
    }

    @Test
    fun deserialize_whenIsoShortDate_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15", result.date)
    }

    @Test
    fun deserialize_whenDateIsNull_thenReturnsNull() {
        val jsonString = """{"date": null}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals(null, result.date)
    }

    @Test
    fun deserialize_whenDateIsEmptyString_thenReturnsEmptyString() {
        val jsonString = """{"date": ""}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("", result.date)
    }

    @Test
    fun deserialize_whenDateIsBlankString_thenReturnsBlankString() {
        val jsonString = """{"date": "   "}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("   ", result.date)
    }

    @Test
    fun deserialize_whenInvalidDateFormat_thenThrowsSerializationException() {
        val jsonString = """{"date": "invalid-date-format"}"""
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun deserialize_whenPartialDate_thenThrowsSerializationException() {
        val jsonString = """{"date": "2024-01"}"""
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun deserialize_whenOnlyTime_thenThrowsSerializationException() {
        val jsonString = """{"date": "10:30:00"}"""
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun deserialize_whenWrongSeparator_thenThrowsSerializationException() {
        val jsonString = """{"date": "2024/01/15"}"""
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun deserialize_whenMonthOutOfRange_thenThrowsSerializationException() {
        val jsonString = """{"date": "2024-13-15"}"""
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun deserialize_whenDayOutOfRange_thenThrowsSerializationException() {
        val jsonString = """{"date": "2024-01-32"}"""
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun deserialize_whenHourOutOfRange_thenThrowsSerializationException() {
        val jsonString = """{"date": "2024-01-15T25:30:00"}"""
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<TestModel>(jsonString)
        }
    }

    @Test
    fun serialize_whenValidDate_thenReturnsSameDateString() {
        val model = TestModel(date = "2024-01-15T10:30:00Z")
        val jsonString = json.encodeToString(model)
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00Z", result.date)
    }

    @Test
    fun deserialize_whenLeapYear_thenReturnsDate() {
        val jsonString = """{"date": "2024-02-29"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-02-29", result.date)
    }

    @Test
    fun deserialize_whenMidnight_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T00:00:00Z"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T00:00:00Z", result.date)
    }

    @Test
    fun deserialize_whenEndOfDay_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T23:59:59Z"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T23:59:59Z", result.date)
    }

    @Test
    fun deserialize_whenDateWithoutFractionalSecondsButWithZ_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00Z"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00Z", result.date)
    }

    @Test
    fun deserialize_whenDateWithMultipleFractionalSeconds_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00.123456Z"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00.123456Z", result.date)
    }

    @Test
    fun deserialize_whenDateWithPlusTimezoneOffset_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00+00:00"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00+00:00", result.date)
    }

    @Test
    fun deserialize_whenDateWithMinusTimezoneOffset_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00-05:00"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00-05:00", result.date)
    }

    @Test
    fun deserialize_whenDateWithPositiveTimezoneOffset_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00+03:00"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00+03:00", result.date)
    }

    @Test
    fun deserialize_whenDateWithFractionalSecondsAndTimezoneOffset_thenReturnsDate() {
        val jsonString = """{"date": "2024-01-15T10:30:00.123+00:00"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2024-01-15T10:30:00.123+00:00", result.date)
    }

    @Test
    fun deserialize_whenDateFromBugReport_thenReturnsDate() {
        val jsonString = """{"date": "2026-01-18T07:00:00+00:00"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        assertEquals("2026-01-18T07:00:00+00:00", result.date)
    }
}
