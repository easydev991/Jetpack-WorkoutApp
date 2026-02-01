package com.swparks.navigation

import androidx.navigation.NavHostController
import com.swparks.model.User
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для AppState.
 * Проверяют функциональность состояния авторизации и обновления текущего пользователя.
 */
class AppStateTest {

    private lateinit var navController: NavHostController
    private lateinit var appState: AppState

    // Вспомогательный метод для создания тестового пользователя
    private fun createTestUser(id: Long = 1L) = User(
        id = id,
        name = "testuser",
        image = "https://example.com/image.jpg",
        cityID = null,
        countryID = null,
        birthDate = null,
        email = null,
        fullName = null,
        genderCode = null,
        friendRequestCount = null,
        friendsCount = null,
        parksCount = null,
        addedParks = null,
        journalCount = null,
        lang = "ru"
    )

    @Before
    fun setup() {
        navController = mockk(relaxed = true)
        appState = AppState(navController)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun currentUser_whenInitialized_thenReturnsNull() {
        // Given - AppState только что создан

        // When
        val currentUser = appState.currentUser

        // Then
        assertNull("Текущий пользователь должен быть null после инициализации", currentUser)
    }

    @Test
    fun isAuthorized_whenCurrentUserIsNull_thenReturnsFalse() {
        // Given - AppState только что создан, currentUser = null

        // When
        val isAuthorized = appState.isAuthorized

        // Then
        assertFalse("Пользователь не должен быть авторизован при null currentUser", isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledWithUser_thenUpdatesCurrentUser() {
        // Given
        val testUser = createTestUser()

        // When
        appState.updateCurrentUser(testUser)

        // Then
        val currentUser = appState.currentUser
        assertTrue("Текущий пользователь должен быть обновлен", currentUser != null)
        assertTrue("ID пользователя должен совпадать", currentUser?.id == testUser.id)
        assertTrue("Имя пользователя должно совпадать", currentUser?.name == testUser.name)
    }

    @Test
    fun updateCurrentUser_whenCalledWithUser_thenReturnsTrueIsAuthorized() {
        // Given
        val testUser = createTestUser()

        // When
        appState.updateCurrentUser(testUser)

        // Then
        assertTrue("Пользователь должен быть авторизован после обновления", appState.isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledWithNull_thenClearsCurrentUser() {
        // Given
        val testUser = createTestUser()
        appState.updateCurrentUser(testUser)
        assertTrue("Предварительная проверка: пользователь должен быть авторизован", appState.isAuthorized)

        // When
        appState.updateCurrentUser(null)

        // Then
        assertNull("Текущий пользователь должен быть null после очистки", appState.currentUser)
        assertFalse("Пользователь не должен быть авторизован после очистки", appState.isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledMultipleTimes_thenReflectsLatestValue() {
        // Given
        val firstUser = createTestUser(id = 1L)
        val secondUser = createTestUser(id = 2L)
        val thirdUser = createTestUser(id = 3L)

        // When - обновляем пользователя несколько раз
        appState.updateCurrentUser(firstUser)
        val afterFirstUpdate = appState.currentUser

        appState.updateCurrentUser(secondUser)
        val afterSecondUpdate = appState.currentUser

        appState.updateCurrentUser(thirdUser)
        val afterThirdUpdate = appState.currentUser

        // Then - проверяем что каждый раз сохраняется последнее значение
        assertTrue("После первого обновления ID должен быть 1", afterFirstUpdate?.id == 1L)
        assertTrue("После второго обновления ID должен быть 2", afterSecondUpdate?.id == 2L)
        assertTrue("После третьего обновления ID должен быть 3", afterThirdUpdate?.id == 3L)
        assertTrue("Финальный currentUser должен быть третьим пользователем", appState.currentUser?.id == 3L)
    }

    @Test
    fun isAuthorized_whenUserSet_thenReturnsTrue() {
        // Given
        val testUser = createTestUser()

        // When
        appState.updateCurrentUser(testUser)

        // Then
        assertTrue("isAuthorized должен возвращать true при наличии пользователя", appState.isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledWithSameUser_multipleTimes_thenRemainsConsistent() {
        // Given
        val testUser = createTestUser(id = 5L)

        // When - обновляем одним и тем же пользователем несколько раз
        appState.updateCurrentUser(testUser)
        val firstValue = appState.currentUser

        appState.updateCurrentUser(testUser)
        val secondValue = appState.currentUser

        appState.updateCurrentUser(testUser)
        val thirdValue = appState.currentUser

        // Then - все значения должны быть одинаковыми
        assertTrue("Первое обновление должно сохранить пользователя", firstValue?.id == 5L)
        assertTrue("Второе обновление должно сохранить пользователя", secondValue?.id == 5L)
        assertTrue("Третье обновление должно сохранить пользователя", thirdValue?.id == 5L)
        assertTrue("Пользователь должен быть авторизован после всех обновлений", appState.isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledWithNull_thenNullThenUser_thenTransitionsCorrectly() {
        // Given
        val testUser = createTestUser()

        // When - последовательность: null -> user -> null -> user
        appState.updateCurrentUser(null)
        val afterFirstNull = appState.currentUser
        val authorizedAfterFirstNull = appState.isAuthorized

        appState.updateCurrentUser(testUser)
        val afterUser = appState.currentUser
        val authorizedAfterUser = appState.isAuthorized

        appState.updateCurrentUser(null)
        val afterSecondNull = appState.currentUser
        val authorizedAfterSecondNull = appState.isAuthorized

        appState.updateCurrentUser(testUser)
        val afterSecondUser = appState.currentUser
        val authorizedAfterSecondUser = appState.isAuthorized

        // Then - проверяем переходы состояний
        assertNull("После первого null currentUser должен быть null", afterFirstNull)
        assertFalse("После первого null пользователь не должен быть авторизован", authorizedAfterFirstNull)

        assertTrue("После установки пользователя currentUser должен быть не null", afterUser != null)
        assertTrue("После установки пользователь должен быть авторизован", authorizedAfterUser)

        assertNull("После второго null currentUser должен быть null", afterSecondNull)
        assertFalse("После второго null пользователь не должен быть авторизован", authorizedAfterSecondNull)

        assertTrue("После повторной установки пользователя currentUser должен быть не null", afterSecondUser != null)
        assertTrue("После повторной установки пользователь должен быть авторизован", authorizedAfterSecondUser)
    }
}
