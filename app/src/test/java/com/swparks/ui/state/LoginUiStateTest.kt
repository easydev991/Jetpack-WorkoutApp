package com.swparks.ui.state

import com.swparks.model.SocialUpdates
import com.swparks.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для LoginUiState.
 */
class LoginUiStateTest {

    @Test
    fun isBusy_whenIdle_returnsFalse() {
        val state = LoginUiState.Idle
        assertFalse("Idle состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenLoading_returnsTrue() {
        val state = LoginUiState.Loading
        assertTrue("Loading состояние должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenLoginSuccessWithNullSocialUpdates_returnsTrue() {
        val state = LoginUiState.LoginSuccess(socialUpdates = null)
        assertTrue("LoginSuccess с null socialUpdates должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenLoginSuccessWithSocialUpdates_returnsFalse() {
        val state = LoginUiState.LoginSuccess(socialUpdates = createMockSocialUpdates())
        assertFalse("LoginSuccess с socialUpdates не должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenResetSuccess_returnsFalse() {
        val state = LoginUiState.ResetSuccess
        assertFalse("ResetSuccess состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenLoginError_returnsFalse() {
        val state = LoginUiState.LoginError(message = "Error")
        assertFalse("LoginError состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenResetError_returnsFalse() {
        val state = LoginUiState.ResetError(message = "Error")
        assertFalse("ResetError состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun loginSuccess_whenCreatedWithSocialUpdates_containsCorrectData() {
        val socialUpdates = createMockSocialUpdates()
        val state = LoginUiState.LoginSuccess(socialUpdates)

        assertEquals(socialUpdates, state.socialUpdates)
    }

    @Test
    fun loginError_whenCreatedWithMessage_containsCorrectMessage() {
        val message = "Неверный логин или пароль"
        val state = LoginUiState.LoginError(message)

        assertEquals(message, state.message)
    }

    @Test
    fun resetError_whenCreatedWithMessage_containsCorrectMessage() {
        val message = "Email не найден"
        val state = LoginUiState.ResetError(message)

        assertEquals(message, state.message)
    }

    /**
     * Создает мок SocialUpdates для тестов.
     */
    private fun createMockSocialUpdates(): SocialUpdates {
        val user = User(
            id = 1L,
            name = "test_user",
            image = "https://example.com/avatar.jpg",
            cityID = null,
            countryID = null,
            birthDate = null,
            email = "test@example.com",
            fullName = "Test User",
            genderCode = null,
            friendRequestCount = null,
            friendsCount = null,
            parksCount = null,
            addedParks = null,
            journalCount = null,
            lang = "ru"
        )

        return SocialUpdates(
            user = user,
            friends = emptyList(),
            friendRequests = emptyList(),
            blacklist = emptyList()
        )
    }
}
