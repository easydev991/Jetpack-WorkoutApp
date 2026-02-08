package com.swparks.model

import com.swparks.ui.model.MainUserForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainUserFormTest {
    // Вспомогательные методы для создания тестовых данных
    @Suppress("LongParameterList")
    private fun createTestForm(
        name: String = "testuser",
        fullname: String = "Test User",
        email: String = "test@example.com",
        password: String = "password123",
        birthDate: String = "1990-01-01",
        genderCode: Int = 0,
        countryId: Int? = null,
        cityId: Int? = null
    ) = MainUserForm(
        name = name,
        fullname = fullname,
        email = email,
        password = password,
        birthDate = birthDate,
        genderCode = genderCode,
        countryId = countryId,
        cityId = cityId
    )

    @Test
    fun name_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(name = "johnsmith")

        // When & Then
        assertEquals("johnsmith", form.name)
    }

    @Test
    fun fullname_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(fullname = "John Smith")

        // When & Then
        assertEquals("John Smith", form.fullname)
    }

    @Test
    fun email_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(email = "john@example.com")

        // When & Then
        assertEquals("john@example.com", form.email)
    }

    @Test
    fun password_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(password = "secret123")

        // When & Then
        assertEquals("secret123", form.password)
    }

    @Test
    fun birthDate_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(birthDate = "1985-05-15")

        // When & Then
        assertEquals("1985-05-15", form.birthDate)
    }

    @Test
    fun genderCode_whenValueIs0_thenReturns0() {
        // Given
        val form = createTestForm(genderCode = 0)

        // When & Then
        assertEquals(0, form.genderCode)
    }

    @Test
    fun genderCode_whenValueIs1_thenReturns1() {
        // Given
        val form = createTestForm(genderCode = 1)

        // When & Then
        assertEquals(1, form.genderCode)
    }

    @Test
    fun countryId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(countryId = 1)

        // When & Then
        assertEquals(1, form.countryId)
    }

    @Test
    fun countryId_whenValueIsNull_thenReturnsNull() {
        // Given
        val form = createTestForm(countryId = null)

        // When & Then
        assertNull(form.countryId)
    }

    @Test
    fun cityId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(cityId = 2)

        // When & Then
        assertEquals(2, form.cityId)
    }

    @Test
    fun cityId_whenValueIsNull_thenReturnsNull() {
        // Given
        val form = createTestForm(cityId = null)

        // When & Then
        assertNull(form.cityId)
    }

    @Test
    fun form_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            name = "johndoe",
            fullname = "John Doe",
            email = "john.doe@example.com",
            password = "mypass123",
            birthDate = "1990-11-25",
            genderCode = 0,
            countryId = 1,
            cityId = 2
        )

        // When & Then
        assertEquals("johndoe", form.name)
        assertEquals("John Doe", form.fullname)
        assertEquals("john.doe@example.com", form.email)
        assertEquals("mypass123", form.password)
        assertEquals("1990-11-25", form.birthDate)
        assertEquals(0, form.genderCode)
        assertEquals(1, form.countryId)
        assertEquals(2, form.cityId)
    }

    @Test
    fun form_whenOptionalFieldsAreNull_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            name = "johndoe",
            fullname = "John Doe",
            email = "john.doe@example.com",
            password = "mypass123",
            birthDate = "1990-11-25",
            genderCode = 0,
            countryId = null,
            cityId = null
        )

        // When & Then
        assertEquals("johndoe", form.name)
        assertEquals("John Doe", form.fullname)
        assertEquals("john.doe@example.com", form.email)
        assertEquals("mypass123", form.password)
        assertEquals("1990-11-25", form.birthDate)
        assertEquals(0, form.genderCode)
        assertNull(form.countryId)
        assertNull(form.cityId)
    }
}
