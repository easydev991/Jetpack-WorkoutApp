package com.swparks.model

import android.util.Log
import com.swparks.data.model.Park
import com.swparks.data.model.User
import com.swparks.ui.model.Gender
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserTest {
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Вспомогательные методы для создания тестовых данных
    private fun createTestPark(id: Long = 1L) = Park(
        id = id,
        name = "Test Park",
        sizeID = 1,
        typeID = 1,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "Test Address",
        cityID = 1,
        countryID = 1,
        preview = "https://example.com/preview.jpg"
    )

    private fun createTestUser(
        genderCode: Int? = null,
        journalCount: Int? = null,
        friendsCount: Int? = null,
        parksCount: String? = null,
        addedParks: List<Park>? = null,
        birthDate: String? = null
    ) = User(
        id = 1L,
        name = "testuser",
        image = "https://example.com/image.jpg",
        cityID = null,
        countryID = null,
        birthDate = birthDate,
        email = null,
        fullName = null,
        genderCode = genderCode,
        friendRequestCount = null,
        friendsCount = friendsCount,
        parksCount = parksCount,
        addedParks = addedParks,
        journalCount = journalCount
    )

    @Test
    fun genderOption_whenGenderCodeIs0_thenReturnsMale() {
        // Given
        val user = createTestUser(genderCode = 0)

        // When & Then
        assertEquals(Gender.MALE, user.genderOption)
    }

    @Test
    fun genderOption_whenGenderCodeIs1_thenReturnsFemale() {
        // Given
        val user = createTestUser(genderCode = 1)

        // When & Then
        assertEquals(Gender.FEMALE, user.genderOption)
    }

    @Test
    fun genderOption_whenGenderCodeIsNull_thenReturnsNull() {
        // Given
        val user = createTestUser(genderCode = null)

        // When & Then
        assertNull(user.genderOption)
    }

    @Test
    fun genderOption_whenGenderCodeIsInvalid_thenReturnsNull() {
        // Given
        val user = createTestUser(genderCode = 999)

        // When & Then
        assertNull(user.genderOption)
    }

    @Test
    fun hasJournals_whenJournalCountIsGreaterThanZero_thenReturnsTrue() {
        // Given
        val user = createTestUser(journalCount = 5)

        // When & Then
        assertTrue(user.hasJournals)
    }

    @Test
    fun hasJournals_whenJournalCountIsZero_thenReturnsFalse() {
        // Given
        val user = createTestUser(journalCount = 0)

        // When & Then
        assertFalse(user.hasJournals)
    }

    @Test
    fun hasJournals_whenJournalCountIsNull_thenReturnsFalse() {
        // Given
        val user = createTestUser(journalCount = null)

        // When & Then
        assertFalse(user.hasJournals)
    }

    @Test
    fun hasFriends_whenFriendsCountIsGreaterThanZero_thenReturnsTrue() {
        // Given
        val user = createTestUser(friendsCount = 3)

        // When & Then
        assertTrue(user.hasFriends)
    }

    @Test
    fun hasFriends_whenFriendsCountIsZero_thenReturnsFalse() {
        // Given
        val user = createTestUser(friendsCount = 0)

        // When & Then
        assertFalse(user.hasFriends)
    }

    @Test
    fun hasFriends_whenFriendsCountIsNull_thenReturnsFalse() {
        // Given
        val user = createTestUser(friendsCount = null)

        // When & Then
        assertFalse(user.hasFriends)
    }

    @Test
    fun hasUsedParks_whenParksCountIsValidAndGreaterThanZero_thenReturnsTrue() {
        // Given
        val user = createTestUser(parksCount = "5")

        // When & Then
        assertTrue(user.hasUsedParks)
    }

    @Test
    fun hasUsedParks_whenParksCountIsZero_thenReturnsFalse() {
        // Given
        val user = createTestUser(parksCount = "0")

        // When & Then
        assertFalse(user.hasUsedParks)
    }

    @Test
    fun hasUsedParks_whenParksCountIsNull_thenReturnsFalse() {
        // Given
        val user = createTestUser(parksCount = null)

        // When & Then
        assertFalse(user.hasUsedParks)
    }

    @Test
    fun hasUsedParks_whenParksCountIsInvalidString_thenReturnsFalse() {
        // Given
        val user = createTestUser(parksCount = "invalid")

        // When & Then
        assertFalse(user.hasUsedParks)
    }

    @Test
    fun hasUsedParks_whenParksCountIsEmptyString_thenReturnsFalse() {
        // Given
        val user = createTestUser(parksCount = "")

        // When & Then
        assertFalse(user.hasUsedParks)
    }

    @Test
    fun hasAddedParks_whenAddedParksListIsNotEmpty_thenReturnsTrue() {
        // Given
        val park = createTestPark()
        val user = createTestUser(addedParks = listOf(park))

        // When & Then
        assertTrue(user.hasAddedParks)
    }

    @Test
    fun hasAddedParks_whenAddedParksIsNull_thenReturnsFalse() {
        // Given
        val user = createTestUser(addedParks = null)

        // When & Then
        assertFalse(user.hasAddedParks)
    }

    @Test
    fun hasAddedParks_whenAddedParksListIsEmpty_thenReturnsFalse() {
        // Given
        val user = createTestUser(addedParks = emptyList())

        // When & Then
        assertFalse(user.hasAddedParks)
    }

    // Тесты для свойства age
    @Test
    fun age_whenBirthDateIsNull_thenReturnsZero() {
        // Given
        val user = createTestUser(birthDate = null)

        // When & Then
        assertEquals(0, user.age)
    }

    @Test
    fun age_whenBirthDateIsBlank_thenReturnsZero() {
        // Given
        val user = createTestUser(birthDate = "")

        // When & Then
        assertEquals(0, user.age)
    }

    @Test
    fun age_whenBirthDateIsValid_thenReturnsPositiveAge() {
        // Given
        val currentYear = java.time.LocalDate.now().year
        val user = createTestUser(birthDate = "$currentYear-01-01")

        // When & Then
        assertTrue(user.age >= 0)
    }

    @Test
    fun age_whenBirthDateIsPastYear_thenReturnsCorrectAge() {
        // Given
        val currentYear = java.time.LocalDate.now().year
        val birthYear = currentYear - 30
        val user = createTestUser(birthDate = "$birthYear-01-01")

        // When
        val age = user.age

        // Then - возраст должен быть 29 или 30 в зависимости от текущей даты
        assertTrue(age == 29 || age == 30)
    }

    @Test
    fun age_whenBirthDateIsInFuture_thenReturnsZero() {
        // Given
        val futureYear = java.time.LocalDate.now().year + 10
        val user = createTestUser(birthDate = "$futureYear-01-01")

        // When & Then
        assertEquals(0, user.age)
    }

    @Test
    fun age_whenBirthDateIsInvalidFormat_thenReturnsZero() {
        // Given
        val user = createTestUser(birthDate = "not-a-date")

        // When & Then
        assertEquals(0, user.age)
    }

    @Test
    fun age_whenBirthDateIsPartialFormat_thenReturnsZero() {
        // Given
        val user = createTestUser(birthDate = "1990-11")

        // When & Then
        assertEquals(0, user.age)
    }
}
