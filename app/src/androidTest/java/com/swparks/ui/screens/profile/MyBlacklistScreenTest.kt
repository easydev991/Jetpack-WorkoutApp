package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.model.User
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.viewmodel.BlacklistUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для MyBlacklistScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение черного списка, реакции на нажатия
 * и поведение при разных состояниях UI.
 */
@RunWith(AndroidJUnit4::class)
class MyBlacklistScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: BlacklistUiState = BlacklistUiState.Success(),
        onBackClick: () -> Unit = {},
        onShowRemoveDialog: (User) -> Unit = {},
        onRemoveFromBlacklist: (User) -> Unit = {},
        onCancelRemove: () -> Unit = {},
        onDismissSuccessAlert: () -> Unit = {},
        itemToRemove: User? = null,
        showRemoveDialog: Boolean = false,
        isRemoving: Boolean = false,
        showSuccessAlert: Boolean = false,
        unblockedUserName: String? = null
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                MyBlacklistScreenContent(
                    uiState = uiState,
                    onBackClick = onBackClick,
                    parentPaddingValues = PaddingValues(),
                    onShowRemoveDialog = onShowRemoveDialog,
                    onRemoveFromBlacklist = onRemoveFromBlacklist,
                    onCancelRemove = onCancelRemove,
                    onDismissSuccessAlert = onDismissSuccessAlert,
                    itemToRemove = itemToRemove,
                    showRemoveDialog = showRemoveDialog,
                    isRemoving = isRemoving,
                    showSuccessAlert = showSuccessAlert,
                    unblockedUserName = unblockedUserName
                )
            }
        }
    }

    @Test
    fun myBlacklistScreen_displaysAppBarWithBlacklistTitle() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.black_list))
            .assertIsDisplayed()
    }

    @Test
    fun myBlacklistScreen_displaysBlacklist_whenHasUsers() {
        // Given
        val testUser = User(
            id = 1L,
            name = "BlockedUser",
            image = null
        )
        val state = BlacklistUiState.Success(
            blacklist = listOf(testUser)
        )

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()
    }

    @Test
    fun myBlacklistScreen_displaysMultipleUsers() {
        // Given
        val users = listOf(
            User(id = 1L, name = "BlockedUser1", image = null),
            User(id = 2L, name = "BlockedUser2", image = null),
            User(id = 3L, name = "BlockedUser3", image = null)
        )
        val state = BlacklistUiState.Success(
            blacklist = users
        )

        // When
        setContent(uiState = state)

        // Then - Все пользователи отображаются
        users.forEach { user ->
            composeTestRule
                .onNodeWithText(user.name)
                .assertIsDisplayed()
        }
    }

    @Test
    fun myBlacklistScreen_displaysEmptyState_whenNoUsers() {
        // Given
        val state = BlacklistUiState.Success(
            blacklist = emptyList()
        )

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.blacklist_empty))
            .assertIsDisplayed()
    }

    @Test
    fun myBlacklistScreen_clicksUser_callsOnShowRemoveDialog() {
        // Given
        val testUserId = 1L
        val testUser = User(
            id = testUserId,
            name = "BlockedUser",
            image = null
        )
        var clickedUser: User? = null

        val state = BlacklistUiState.Success(
            blacklist = listOf(testUser)
        )

        setContent(
            uiState = state,
            onShowRemoveDialog = { clickedUser = it }
        )

        // When
        composeTestRule
            .onNodeWithText(testUser.name)
            .performClick()

        // Then
        assert(clickedUser?.id == testUserId) { "Ожидался userId=$testUserId при клике на пользователя" }
    }

    @Test
    fun myBlacklistScreen_userRowIsClickable() {
        // Given
        val testUserId = 1L
        val testUser = User(
            id = testUserId,
            name = "BlockedUser",
            image = null
        )
        val state = BlacklistUiState.Success(
            blacklist = listOf(testUser)
        )

        setContent(uiState = state)

        // Then - Строка пользователя должна быть кликабельной
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertHasClickAction()
    }

    @Test
    fun myBlacklistScreen_displaysLoadingState() {
        // Given
        val state = BlacklistUiState.Loading

        // When
        setContent(uiState = state)

        // Then - AppBar отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.black_list))
            .assertIsDisplayed()
    }

    @Test
    fun myBlacklistScreen_displaysErrorState() {
        // Given
        val errorMessage = "Error loading blacklist"
        val state = BlacklistUiState.Error(message = errorMessage)

        // When
        setContent(uiState = state)

        // Then - Сообщение об ошибке отображается
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun myBlacklistScreen_displaysBusyStateWithLastSuccessState() {
        // Given
        val testUser = User(
            id = 1L,
            name = "BlockedUser",
            image = null
        )
        val state = BlacklistUiState.Success(
            blacklist = listOf(testUser),
            isLoading = true
        )

        // When
        setContent(
            uiState = state
        )

        // Then - Имя пользователя отображается даже при загрузке
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()
    }
}
