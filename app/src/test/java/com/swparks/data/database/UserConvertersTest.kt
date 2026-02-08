package com.swparks.data.database

import android.util.Log
import com.swparks.data.model.Park
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для [UserConverters]
 */
class UserConvertersTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun fromParksList_whenNonNullParks_returnsJsonString() {
        // Given
        val parks = listOf(
            createTestPark(id = 1, name = "Park 1"),
            createTestPark(id = 2, name = "Park 2")
        )

        // When
        val result = UserConverters.fromParksList(parks)

        // Then
        assertNotNull(result)
        assertTrue(result!!.contains("\"id\":1"))
        assertTrue(result.contains("\"name\":\"Park 1\""))
        assertTrue(result.contains("\"id\":2"))
        assertTrue(result.contains("\"name\":\"Park 2\""))
    }

    @Test
    fun fromParksList_whenNullParks_returnsNull() {
        // When
        val result = UserConverters.fromParksList(null)

        // Then
        assertNull(result)
    }

    @Test
    fun fromParksList_whenEmptyParks_returnsEmptyJsonArray() {
        // Given
        val parks = emptyList<Park>()

        // When
        val result = UserConverters.fromParksList(parks)

        // Then
        assertNotNull(result)
        assertEquals("[]", result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun toParksList_whenValidJson_returnsParkList() {
        // Given
        val park1Json =
            """{"id":1,"name":"Park 1","class_id":1,"type_id":1,""" +
                    """"longitude":"1.0","latitude":"1.0","address":"Address",""" +
                    """"city_id":1,"country_id":1,"preview":"preview.jpg"}"""
        val park2Json =
            """{"id":2,"name":"Park 2","class_id":2,"type_id":2,""" +
                    """"longitude":"2.0","latitude":"2.0","address":"Address 2",""" +
                    """"city_id":2,"country_id":2,"preview":"preview2.jpg"}"""
        val json = "[$park1Json,$park2Json]"

        // When
        val result = UserConverters.toParksList(json)

        // Then
        assertNotNull(result)
        assertEquals(2, result?.size)
        assertEquals(1L, result?.get(0)?.id)
        assertEquals("Park 1", result?.get(0)?.name)
        assertEquals(2L, result?.get(1)?.id)
        assertEquals("Park 2", result?.get(1)?.name)
    }

    @Test
    fun toParksList_whenNullJson_returnsNull() {
        // When
        val result = UserConverters.toParksList(null)

        // Then
        assertNull(result)
    }

    @Test
    fun toParksList_whenInvalidJson_returnsNull() {
        // Given
        val invalidJson = "invalid json"

        // When
        val result = UserConverters.toParksList(invalidJson)

        // Then
        assertNull(result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun toParksList_whenJsonWithUnknownKeys_returnsParkList() {
        // Given - JSON с неизвестными полями, которые должны игнорироваться благодаря ignoreUnknownKeys = true
        val jsonWithUnknownKeys = """[{"id":"1","name":"Park 1","class_id":1,"type_id":1,""" +
                """"longitude":"1.0","latitude":"1.0","address":"Address","city_id":1,"country_id":1,""" +
                """"preview":"preview.jpg","unknownField":"value","anotherUnknown":123}]"""

        // When
        val result = UserConverters.toParksList(jsonWithUnknownKeys)

        // Then
        assertNotNull(result)
        assertEquals(1, result?.size)
        assertEquals(1L, result?.get(0)?.id)
        assertEquals("Park 1", result?.get(0)?.name)
    }

    @Test
    @Suppress("MaxLineLength")
    fun roundTrip_conversionReturnsSameParks() {
        // Given
        val originalParks = listOf(
            createTestPark(id = 1, name = "Park 1"),
            createTestPark(id = 2, name = "Park 2")
        )

        // When
        val jsonString = UserConverters.fromParksList(originalParks)
        val restoredParks = UserConverters.toParksList(jsonString)

        // Then
        assertNotNull(restoredParks)
        assertEquals(originalParks.size, restoredParks?.size)
        assertEquals(originalParks[0].id, restoredParks?.get(0)?.id)
        assertEquals(originalParks[0].name, restoredParks?.get(0)?.name)
        assertEquals(originalParks[1].id, restoredParks?.get(1)?.id)
        assertEquals(originalParks[1].name, restoredParks?.get(1)?.name)
    }

    /**
     * Вспомогательный метод для создания тестовой площадки
     */
    private fun createTestPark(id: Long, name: String): Park {
        return Park(
            id = id,
            name = name,
            sizeID = 1,
            typeID = 1,
            longitude = "1.0",
            latitude = "1.0",
            address = "Address",
            cityID = 1,
            countryID = 1,
            preview = "preview.jpg"
        )
    }
}
