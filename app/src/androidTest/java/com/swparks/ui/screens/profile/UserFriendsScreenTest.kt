package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.UserFriendsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для UserFriendsScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение списка друзей, пустого состояния и ошибок.
 */
@RunWith(AndroidJUnit4::class)
class UserFriendsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: UserFriendsUiState = UserFriendsUiState.Loading,
        onBackClick: () -> Unit = {},
        onUserClick: (Long) -> Unit = {},
        onRefresh: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                UserFriendsScreenContent(
                    uiState = uiState,
                    config = FriendsScreenConfig(
                        parentPaddingValues = PaddingValues()
                    ),
                    onAction = { action ->
                        when (action) {
                            is UserFriendsAction.Back -> onBackClick()
                            is UserFriendsAction.UserClick -> onUserClick(action.userId)
                            is UserFriendsAction.Refresh -> onRefresh()
                        }
                    },
                    onRefresh = onRefresh
                )
            }
        }
    }

    // === Тесты для AppBar ===

    @Test
    fun userFriendsScreen_displaysAppBarWithFriendsTitle() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.friends))
            .assertIsDisplayed()
    }

    // === Тесты для Loading состояния ===

    @Test
    fun userFriendsScreen_displaysLoadingState() {
        // Given
        val state = UserFriendsUiState.Loading

        // When
        setContent(uiState = state)

        // Then - AppBar отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.friends))
            .assertIsDisplayed()
    }

    // === Тесты для Error состояния ===

    @Test
    fun userFriendsScreen_displaysErrorState() {
        // Given
        val errorMessage = "Network error"
        val state = UserFriendsUiState.Error(message = errorMessage)

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.error_network_general))
            .assertIsDisplayed()
    }

    @Test
    fun userFriendsScreen_displaysRetryButton_whenError() {
        // Given
        val state = UserFriendsUiState.Error(message = "Error")

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .assertIsDisplayed()
    }

    @Test
    fun userFriendsScreen_clickRetry_callsOnRefresh() {
        // Given
        val state = UserFriendsUiState.Error(message = "Error")
        var refreshClicked = false

        setContent(
            uiState = state,
            onRefresh = { refreshClicked = true }
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        // Then
        assert(refreshClicked) { "Ожидался вызов onRefresh при клике на кнопку повтора" }
    }

    // === Тесты для Success с пустым списком ===

    @Test
    fun userFriendsScreen_displaysEmptyState_whenNoFriends() {
        // Given
        val state = UserFriendsUiState.Success(friends = emptyList())

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.no_friends_yet))
            .assertIsDisplayed()
    }

    // === Тесты для Success со списком друзей ===

    @Test
    fun userFriendsScreen_displaysFriend_whenHasFriends() {
        // Given
        val testUser = User(
            id = 1L,
            name = "TestUser",
            image = null
        )
        val state = UserFriendsUiState.Success(friends = listOf(testUser))

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()
    }

    @Test
    fun userFriendsScreen_displaysMultipleFriends() {
        // Given
        val friends = listOf(
            User(id = 1L, name = "Friend1", image = null),
            User(id = 2L, name = "Friend2", image = null),
            User(id = 3L, name = "Friend3", image = null)
        )
        val state = UserFriendsUiState.Success(friends = friends)

        // When
        setContent(uiState = state)

        // Then - Все друзья отображаются
        friends.forEach { friend ->
            composeTestRule
                .onNodeWithText(friend.name)
                .assertIsDisplayed()
        }
    }

    @Test
    fun userFriendsScreen_doesNotDisplayEmptyState_whenHasFriends() {
        // Given
        val testUser = User(
            id = 1L,
            name = "TestUser",
            image = null
        )
        val state = UserFriendsUiState.Success(friends = listOf(testUser))

        // When
        setContent(uiState = state)

        // Then - Пустое состояние НЕ отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.no_friends_yet))
            .assertDoesNotExist()
    }

    @Test
    fun userFriendsScreen_doesNotDisplayError_whenSuccess() {
        // Given
        val testUser = User(
            id = 1L,
            name = "TestUser",
            image = null
        )
        val state = UserFriendsUiState.Success(friends = listOf(testUser))

        // When
        setContent(uiState = state)

        // Then - Сообщение об ошибке НЕ отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.error_network_general))
            .assertDoesNotExist()
    }
}
