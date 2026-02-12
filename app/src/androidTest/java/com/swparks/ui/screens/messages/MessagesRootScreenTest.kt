package com.swparks.ui.screens.messages

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.model.User
import com.swparks.ui.model.Gender
import com.swparks.ui.state.DialogsUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для MessagesRootScreen.
 *
 * Тестирует UI компонент DialogsContent в изоляции.
 * Проверяет отображение списка диалогов, пустого состояния и ошибок.
 */
@RunWith(AndroidJUnit4::class)
class MessagesRootScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: DialogsUiState = DialogsUiState.Success(emptyList()),
        isRefreshing: Boolean = false,
        syncError: String? = null,
        currentUser: User? = null,
        onRefresh: () -> Unit = {},
        onDismissSyncError: () -> Unit = {},
        onDialogClick: (Long, Int?) -> Unit = { _, _ -> },
        onNavigateToFriends: () -> Unit = {},
        onNavigateToSearchUsers: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                DialogsContent(
                    modifier = androidx.compose.ui.Modifier,
                    uiState = uiState,
                    isRefreshing = isRefreshing,
                    syncError = syncError,
                    currentUser = currentUser,
                    onRefresh = onRefresh,
                    onDismissSyncError = onDismissSyncError,
                    onDialogClick = onDialogClick,
                    onNavigateToFriends = onNavigateToFriends,
                    onNavigateToSearchUsers = onNavigateToSearchUsers
                )
            }
        }
    }

    @Test
    fun dialogsContent_whenLoading_displaysLoadingOverlay() {
        // Given
        val uiState = DialogsUiState.Loading

        // When
        setContent(uiState = uiState)

        // Then - должен отображаться индикатор загрузки (по contentDescription)
        val loadingContentDescription = context.getString(R.string.loading_content_description)
        composeTestRule
            .onNodeWithContentDescription(loadingContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenDialogsLoaded_displaysDialogsList() {
        // Given
        val dialogs = listOf(createTestDialog())
        val uiState = DialogsUiState.Success(dialogs)

        // When
        setContent(uiState = uiState)

        // Then - имя собеседника отображается
        composeTestRule
            .onNodeWithText("Test User", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenDialogsLoaded_displaysLastMessage() {
        // Given
        val dialogs = listOf(createTestDialog())
        val uiState = DialogsUiState.Success(dialogs)

        // When
        setContent(uiState = uiState)

        // Then - последнее сообщение отображается
        composeTestRule
            .onNodeWithText("Hello!", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenDialogsLoaded_dialogIsClickable() {
        // Given
        val dialogs = listOf(createTestDialog())
        val uiState = DialogsUiState.Success(dialogs)

        // When
        setContent(uiState = uiState)

        // Then - диалог кликабелен
        composeTestRule
            .onNodeWithText("Test User", ignoreCase = true)
            .assertHasClickAction()
    }

    @Test
    fun dialogsContent_whenEmptyWithFriends_displaysOpenFriendsButton() {
        // Given
        val uiState = DialogsUiState.Success(emptyList())
        val user = createTestUser(friendsCount = 5)

        // When
        setContent(uiState = uiState, currentUser = user)

        // Then - отображается кнопка "Открыть друзей"
        val openFriendsText = context.getString(R.string.dialogs_open_friends)
        composeTestRule
            .onNodeWithText(openFriendsText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenEmptyWithoutFriends_displaysFindUserButton() {
        // Given
        val uiState = DialogsUiState.Success(emptyList())
        val user = createTestUser(friendsCount = 0)

        // When
        setContent(uiState = uiState, currentUser = user)

        // Then - отображается кнопка "Найти пользователя"
        val findUserText = context.getString(R.string.dialogs_find_user)
        composeTestRule
            .onNodeWithText(findUserText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenEmptyWithoutUser_displaysFindUserButton() {
        // Given
        val uiState = DialogsUiState.Success(emptyList())

        // When
        setContent(uiState = uiState, currentUser = null)

        // Then - отображается кнопка "Найти пользователя" (fallback)
        val findUserText = context.getString(R.string.dialogs_find_user)
        composeTestRule
            .onNodeWithText(findUserText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenError_displaysErrorState() {
        // Given
        val errorMessage = "Network error"
        val uiState = DialogsUiState.Error(errorMessage)

        // When
        setContent(uiState = uiState)

        // Then - отображается сообщение об ошибке
        composeTestRule
            .onNodeWithText(errorMessage, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenError_displaysRetryButton() {
        // Given
        val uiState = DialogsUiState.Error("Error")

        // When
        setContent(uiState = uiState)

        // Then - отображается кнопка "Повторить"
        val retryText = context.getString(R.string.try_again_button)
        composeTestRule
            .onNodeWithText(retryText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenError_retryButtonIsClickable() {
        // Given
        val uiState = DialogsUiState.Error("Error")

        // When
        setContent(uiState = uiState)

        // Then - кнопка "Повторить" кликабельна
        val retryText = context.getString(R.string.try_again_button)
        composeTestRule
            .onNodeWithText(retryText, ignoreCase = true)
            .assertHasClickAction()
    }

    @Test
    fun dialogsContent_displaysMultipleDialogs() {
        // Given
        val dialogs = listOf(
            createTestDialog(id = 1, name = "User One", message = "Message 1"),
            createTestDialog(id = 2, name = "User Two", message = "Message 2"),
            createTestDialog(id = 3, name = "User Three", message = "Message 3")
        )
        val uiState = DialogsUiState.Success(dialogs)

        // When
        setContent(uiState = uiState)

        // Then - все диалоги отображаются
        composeTestRule.onNodeWithText("User One").assertIsDisplayed()
        composeTestRule.onNodeWithText("Message 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("User Two").assertIsDisplayed()
        composeTestRule.onNodeWithText("Message 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("User Three").assertIsDisplayed()
        composeTestRule.onNodeWithText("Message 3").assertIsDisplayed()
    }

    @Test
    fun dialogsContent_whenDialogClick_callsOnDialogClick() {
        // Given
        val testDialogId = 42L
        val testUserId = 99
        val dialogs = listOf(
            createTestDialog(id = testDialogId, name = "Clickable User", anotherUserId = testUserId)
        )
        var clickedDialogId: Long? = null
        var clickedUserId: Int? = null

        // When
        setContent(
            uiState = DialogsUiState.Success(dialogs),
            onDialogClick = { dialogId, userId ->
                clickedDialogId = dialogId
                clickedUserId = userId
            }
        )

        // Then - кликаем по диалогу
        composeTestRule
            .onNodeWithText("Clickable User")
            .performClick()

        // Проверяем, что callback был вызван с правильными параметрами
        assert(clickedDialogId == testDialogId) { "Ожидался dialogId=$testDialogId" }
        assert(clickedUserId == testUserId) { "Ожидался userId=$testUserId" }
    }

    // ========== Helper methods ==========

    private fun createTestUser(friendsCount: Int = 0): User {
        return User(
            id = 1L,
            name = "Current User",
            fullName = "Current User",
            email = "current@example.com",
            image = "https://example.com/image.jpg",
            genderCode = Gender.MALE.rawValue,
            friendsCount = friendsCount,
            friendRequestCount = null,
            parksCount = null,
            addedParks = emptyList(),
            journalCount = 0
        )
    }

    private fun createTestDialog(
        id: Long = 1L,
        name: String = "Test User",
        message: String = "Hello!",
        anotherUserId: Int = 2
    ): DialogEntity {
        return DialogEntity(
            id = id,
            name = name,
            image = "https://example.com/avatar.jpg",
            lastMessageText = message,
            lastMessageDate = "2026-02-13T10:00:00",
            unreadCount = 2,
            anotherUserId = anotherUserId
        )
    }
}
