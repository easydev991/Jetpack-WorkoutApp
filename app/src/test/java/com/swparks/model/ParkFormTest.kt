package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParkFormTest {
    // Вспомогательные методы для создания тестовых данных
    @Suppress("LongParameterList")
    private fun createTestForm(
        address: String = "Test Address",
        latitude: String = "55.7558",
        longitude: String = "37.6173",
        cityId: Int? = null,
        typeId: Int = 1,
        sizeId: Int = 1
    ) = ParkForm(
        address = address,
        latitude = latitude,
        longitude = longitude,
        cityId = cityId,
        typeId = typeId,
        sizeId = sizeId
    )

    @Test
    fun address_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(address = "123 Test Street")

        // When & Then
        assertEquals("123 Test Street", form.address)
    }

    @Test
    fun latitude_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(latitude = "59.9343")

        // When & Then
        assertEquals("59.9343", form.latitude)
    }

    @Test
    fun longitude_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(longitude = "30.3351")

        // When & Then
        assertEquals("30.3351", form.longitude)
    }

    @Test
    fun cityId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(cityId = 1)

        // When & Then
        assertEquals(1, form.cityId)
    }

    @Test
    fun cityId_whenValueIsNull_thenReturnsNull() {
        // Given
        val form = createTestForm(cityId = null)

        // When & Then
        assertNull(form.cityId)
    }

    @Test
    fun typeId_whenValueIs1_thenReturns1() {
        // Given
        val form = createTestForm(typeId = 1)

        // When & Then
        assertEquals(1, form.typeId)
    }

    @Test
    fun typeId_whenValueIs2_thenReturns2() {
        // Given
        val form = createTestForm(typeId = 2)

        // When & Then
        assertEquals(2, form.typeId)
    }

    @Test
    fun sizeId_whenValueIs1_thenReturns1() {
        // Given
        val form = createTestForm(sizeId = 1)

        // When & Then
        assertEquals(1, form.sizeId)
    }

    @Test
    fun sizeId_whenValueIs2_thenReturns2() {
        // Given
        val form = createTestForm(sizeId = 2)

        // When & Then
        assertEquals(2, form.sizeId)
    }

    @Test
    fun sizeId_whenValueIs3_thenReturns3() {
        // Given
        val form = createTestForm(sizeId = 3)

        // When & Then
        assertEquals(3, form.sizeId)
    }

    @Test
    fun form_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            address = "123 Fitness Street",
            latitude = "55.7558",
            longitude = "37.6173",
            cityId = 1,
            typeId = 2,
            sizeId = 2
        )

        // When & Then
        assertEquals("123 Fitness Street", form.address)
        assertEquals("55.7558", form.latitude)
        assertEquals("37.6173", form.longitude)
        assertEquals(1, form.cityId)
        assertEquals(2, form.typeId)
        assertEquals(2, form.sizeId)
    }

    @Test
    fun form_whenCityIdIsNull_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            address = "123 Fitness Street",
            latitude = "55.7558",
            longitude = "37.6173",
            cityId = null,
            typeId = 1,
            sizeId = 1
        )

        // When & Then
        assertEquals("123 Fitness Street", form.address)
        assertEquals("55.7558", form.latitude)
        assertEquals("37.6173", form.longitude)
        assertNull(form.cityId)
        assertEquals(1, form.typeId)
        assertEquals(1, form.sizeId)
    }
}
