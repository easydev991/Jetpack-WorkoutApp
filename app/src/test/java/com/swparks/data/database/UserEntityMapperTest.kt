package com.swparks.data.database

import com.swparks.data.database.entity.UserEntity
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.model.Park
import com.swparks.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit тесты для [UserEntityMapper]
 */
class UserEntityMapperTest {
    @Test
    fun user_toEntity_convertsAllFieldsCorrectly() {
        // Given
        val user =
            createTestUser(
                id = 123,
                name = "testuser",
                addedParks =
                    listOf(
                        createTestPark(id = 1, name = "Park 1"),
                        createTestPark(id = 2, name = "Park 2")
                    )
            )

        // When
        val entity =
            user.toEntity(
                isCurrentUser = true,
                isFriend = false,
                isFriendRequest = false,
                isBlacklisted = false
            )

        // Then
        assertEquals(user.id, entity.id)
        assertEquals(user.name, entity.name)
        assertEquals(user.image, entity.image)
        assertEquals(user.cityID, entity.cityId)
        assertEquals(user.countryID, entity.countryId)
        assertEquals(user.birthDate, entity.birthDate)
        assertEquals(user.email, entity.email)
        assertEquals(user.fullName, entity.fullName)
        assertEquals(user.genderCode, entity.genderCode)
        assertEquals(user.friendRequestCount, entity.friendRequestCount)
        assertEquals(user.friendsCount, entity.friendsCount)
        assertEquals(user.parksCount, entity.parksCount)
        assertEquals(user.journalCount, entity.journalCount)

        // Проверяем addedParks
        assertNotNull(entity.addedParks)
        assertEquals(2, entity.addedParks?.size)
        assertEquals(1L, entity.addedParks?.get(0)?.id)
        assertEquals("Park 1", entity.addedParks?.get(0)?.name)
        assertEquals(2L, entity.addedParks?.get(1)?.id)
        assertEquals("Park 2", entity.addedParks?.get(1)?.name)

        // Проверяем флаги
        assertTrue(entity.isCurrentUser)
        assertTrue(!entity.isFriend)
        assertTrue(!entity.isFriendRequest)
        assertTrue(!entity.isBlacklisted)
    }

    @Test
    fun user_toEntity_whenAddedParksNull_convertsCorrectly() {
        // Given
        val user = createTestUser(id = 123, name = "testuser", addedParks = null)

        // When
        val entity = user.toEntity()

        // Then
        assertNotNull(entity)
        assertNull(entity.addedParks)
    }

    @Test
    fun user_toEntity_whenAddedParksEmpty_convertsCorrectly() {
        // Given
        val user = createTestUser(id = 123, name = "testuser", addedParks = emptyList())

        // When
        val entity = user.toEntity()

        // Then
        assertNotNull(entity)
        assertNotNull(entity.addedParks)
        assertTrue(entity.addedParks?.isEmpty() == true)
    }

    @Test
    fun userEntity_toDomain_convertsAllFieldsCorrectly() {
        // Given
        val entity =
            UserEntity(
                id = 123,
                name = "testuser",
                image = "https://example.com/avatar.jpg",
                cityId = 1,
                countryId = 1,
                birthDate = "1990-01-01",
                email = "test@example.com",
                fullName = "Test User",
                genderCode = 0,
                friendRequestCount = "1",
                friendsCount = 10,
                parksCount = "5",
                addedParks =
                    listOf(
                        createTestPark(id = 1, name = "Park 1"),
                        createTestPark(id = 2, name = "Park 2")
                    ),
                journalCount = 3,
                isCurrentUser = true,
                isFriend = false,
                isFriendRequest = false,
                isBlacklisted = false
            )

        // When
        val user = entity.toDomain()

        // Then
        assertEquals(entity.id, user.id)
        assertEquals(entity.name, user.name)
        assertEquals(entity.image, user.image)
        assertEquals(entity.cityId, user.cityID)
        assertEquals(entity.countryId, user.countryID)
        assertEquals(entity.birthDate, user.birthDate)
        assertEquals(entity.email, user.email)
        assertEquals(entity.fullName, user.fullName)
        assertEquals(entity.genderCode, user.genderCode)
        assertEquals(entity.friendRequestCount, user.friendRequestCount)
        assertEquals(entity.friendsCount, user.friendsCount)
        assertEquals(entity.parksCount, user.parksCount)
        assertEquals(entity.journalCount, user.journalCount)

        // Проверяем addedParks
        assertNotNull(user.addedParks)
        assertEquals(2, user.addedParks?.size)
        assertEquals(1L, user.addedParks?.get(0)?.id)
        assertEquals("Park 1", user.addedParks?.get(0)?.name)
        assertEquals(2L, user.addedParks?.get(1)?.id)
        assertEquals("Park 2", user.addedParks?.get(1)?.name)
    }

    @Test
    fun userEntity_toDomain_whenAddedParksNull_convertsCorrectly() {
        // Given
        val entity =
            UserEntity(
                id = 123,
                name = "testuser",
                image = "https://example.com/avatar.jpg",
                cityId = 1,
                countryId = 1,
                birthDate = "1990-01-01",
                email = "test@example.com",
                fullName = "Test User",
                genderCode = 0,
                friendRequestCount = "1",
                friendsCount = 10,
                parksCount = "5",
                addedParks = null,
                journalCount = 3,
                isCurrentUser = false,
                isFriend = false,
                isFriendRequest = false,
                isBlacklisted = false
            )

        // When
        val user = entity.toDomain()

        // Then
        assertNotNull(user)
        assertNull(user.addedParks)
    }

    @Test
    fun userEntity_toDomain_whenAddedParksEmpty_convertsCorrectly() {
        // Given
        val entity =
            UserEntity(
                id = 123,
                name = "testuser",
                image = "https://example.com/avatar.jpg",
                cityId = 1,
                countryId = 1,
                birthDate = "1990-01-01",
                email = "test@example.com",
                fullName = "Test User",
                genderCode = 0,
                friendRequestCount = "1",
                friendsCount = 10,
                parksCount = "5",
                addedParks = emptyList(),
                journalCount = 3,
                isCurrentUser = false,
                isFriend = false,
                isFriendRequest = false,
                isBlacklisted = false
            )

        // When
        val user = entity.toDomain()

        // Then
        assertNotNull(user)
        assertNotNull(user.addedParks)
        assertTrue(user.addedParks?.isEmpty() == true)
    }

    @Test
    fun roundTrip_conversionReturnsSameUser() {
        // Given
        val originalUser =
            createTestUser(
                id = 123,
                name = "testuser",
                addedParks =
                    listOf(
                        createTestPark(id = 1, name = "Park 1"),
                        createTestPark(id = 2, name = "Park 2"),
                        createTestPark(id = 3, name = "Park 3")
                    )
            )

        // When
        val entity = originalUser.toEntity()
        val restoredUser = entity.toDomain()

        // Then
        assertNotNull(restoredUser)
        assertEquals(originalUser.id, restoredUser.id)
        assertEquals(originalUser.name, restoredUser.name)
        assertEquals(originalUser.image, restoredUser.image)
        assertEquals(originalUser.cityID, restoredUser.cityID)
        assertEquals(originalUser.countryID, restoredUser.countryID)
        assertEquals(originalUser.birthDate, restoredUser.birthDate)
        assertEquals(originalUser.email, restoredUser.email)
        assertEquals(originalUser.fullName, restoredUser.fullName)
        assertEquals(originalUser.genderCode, restoredUser.genderCode)
        assertEquals(originalUser.friendRequestCount, restoredUser.friendRequestCount)
        assertEquals(originalUser.friendsCount, restoredUser.friendsCount)
        assertEquals(originalUser.parksCount, restoredUser.parksCount)
        assertEquals(originalUser.journalCount, restoredUser.journalCount)

        // Проверяем addedParks
        assertEquals(originalUser.addedParks?.size, restoredUser.addedParks?.size)
        assertEquals(originalUser.addedParks?.get(0)?.id, restoredUser.addedParks?.get(0)?.id)
        assertEquals(originalUser.addedParks?.get(0)?.name, restoredUser.addedParks?.get(0)?.name)
        assertEquals(originalUser.addedParks?.get(1)?.id, restoredUser.addedParks?.get(1)?.id)
        assertEquals(originalUser.addedParks?.get(1)?.name, restoredUser.addedParks?.get(1)?.name)
        assertEquals(originalUser.addedParks?.get(2)?.id, restoredUser.addedParks?.get(2)?.id)
        assertEquals(originalUser.addedParks?.get(2)?.name, restoredUser.addedParks?.get(2)?.name)
    }

    /**
     * Вспомогательный метод для создания тестового пользователя
     */
    private fun createTestUser(
        id: Long,
        name: String,
        addedParks: List<Park>? = null
    ): User =
        User(
            id = id,
            name = name,
            image = "https://example.com/avatar.jpg",
            cityID = 1,
            countryID = 1,
            birthDate = "1990-01-01",
            email = "test@example.com",
            fullName = "Test User",
            genderCode = 0,
            friendRequestCount = "1",
            friendsCount = 10,
            parksCount = "5",
            addedParks = addedParks,
            journalCount = 3
        )

    /**
     * Вспомогательный метод для создания тестовой площадки
     */
    private fun createTestPark(
        id: Long,
        name: String
    ): Park =
        Park(
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
