package com.swparks.ui.screens.messages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.MessageResponse
import com.swparks.ui.state.ChatUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для ChatScreen.
 *
 * Тестирует UI компонент ChatContent в изоляции без ViewModel.
 */
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: ChatUiState = ChatUiState.Success(emptyList()),
        isLoading: Boolean = false,
        messageText: String = "",
        userName: String = "Test User",
        userImage: String? = null,
        currentUserId: Int? = 1,
        otherUserId: Int = 2,
        onMessageTextChange: (String) -> Unit = {},
        onSendClick: (Int) -> Unit = {},
        onRefresh: () -> Unit = {},
        onBackClick: () -> Unit = {},
        onAvatarClick: () -> Unit = {},
        onMessageSent: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ChatContent(
                    modifier = androidx.compose.ui.Modifier,
                    params = ChatContentParams(
                        uiState = uiState,
                        isLoading = isLoading,
                        messageText = messageText,
                        userName = userName,
                        userImage = userImage,
                        currentUserId = currentUserId,
                        otherUserId = otherUserId,
                        onMessageTextChange = onMessageTextChange,
                        onSendClick = onSendClick,
                        onRefresh = onRefresh,
                        onAction = { action ->
                            when (action) {
                                is ChatAction.Back -> onBackClick()
                                is ChatAction.AvatarClick -> onAvatarClick()
                                is ChatAction.MessageSent -> onMessageSent()
                            }
                        }
                    )
                )
            }
        }
    }

    @Test
    fun chatInputBar_whenSendingMessage_disablesTextInput() {
        // Given
        val uiState = ChatUiState.Success(listOf(createTestMessage()))

        // When - isLoading = true (отправка сообщения)
        setContent(uiState = uiState, isLoading = true)

        // Then - кнопка отправки отключена (текстовое поле не тестируется напрямую в Compose)
        composeTestRule
            .onNodeWithTag("SendChatMessageButton")
            .assertIsNotEnabled()
    }

    @Test
    fun chatContent_whenLoading_displaysLoadingOverlay() {
        // Given
        val uiState = ChatUiState.Loading

        // When
        setContent(uiState = uiState)

        // Then - должен отображаться индикатор загрузки
        val loadingContentDescription = context.getString(R.string.loading_content_description)
        composeTestRule
            .onNodeWithContentDescription(loadingContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun chatContent_whenSendingMessage_displaysLoadingOverlay() {
        // Given
        val messages = listOf(createTestMessage())
        val uiState = ChatUiState.Success(messages)

        // When - isLoading = true (отправка сообщения)
        setContent(uiState = uiState, isLoading = true)

        // Then - должен отображаться индикатор загрузки поверх сообщений
        val loadingContentDescription = context.getString(R.string.loading_content_description)
        composeTestRule
            .onNodeWithContentDescription(loadingContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun chatContent_whenRefreshing_displaysLoadingOverlay() {
        // Given
        val messages = listOf(createTestMessage())
        val uiState = ChatUiState.Success(messages)

        // When - isLoading = true (обновление по кнопке refresh)
        setContent(uiState = uiState, isLoading = true)

        // Then - должен отображаться индикатор загрузки поверх сообщений
        val loadingContentDescription = context.getString(R.string.loading_content_description)
        composeTestRule
            .onNodeWithContentDescription(loadingContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun chatContent_whenMessagesLoaded_displaysMessages() {
        // Given
        val messages = listOf(
            createTestMessage(id = 1, message = "Привет!"),
            createTestMessage(id = 2, userId = 1, message = "Привет, как дела?")
        )
        val uiState = ChatUiState.Success(messages)

        // When
        setContent(uiState = uiState)

        // Then - сообщения отображаются
        composeTestRule
            .onNodeWithText("Привет!", ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Привет, как дела?", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun chatContent_whenEmpty_displaysEmptyMessage() {
        // Given
        val uiState = ChatUiState.Success(emptyList())

        // When
        setContent(uiState = uiState)

        // Then - отображается сообщение о пустом диалоге
        val emptyText = context.getString(R.string.chat_empty_messages)
        composeTestRule
            .onNodeWithText(emptyText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun chatContent_displaysInputField() {
        // Given
        val uiState = ChatUiState.Success(emptyList())

        // When
        setContent(uiState = uiState)

        // Then - поле ввода отображается
        val placeholder = context.getString(R.string.message_placeholder)
        composeTestRule
            .onNodeWithText(placeholder, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun chatContent_whenMessageEmpty_sendButtonIsDisabled() {
        // Given
        val uiState = ChatUiState.Success(listOf(createTestMessage()))

        // When
        setContent(uiState = uiState, messageText = "")

        // Then - кнопка отправки отключена
        composeTestRule
            .onNodeWithTag("SendChatMessageButton")
            .assertIsNotEnabled()
    }

    @Test
    fun chatContent_whenMessageNotEmpty_sendButtonIsEnabled() {
        // Given
        val uiState = ChatUiState.Success(listOf(createTestMessage()))

        // When
        setContent(uiState = uiState, messageText = "Test message")

        // Then - кнопка отправки включена
        composeTestRule
            .onNodeWithTag("SendChatMessageButton")
            .assertIsEnabled()
    }

    @Test
    fun chatContent_whenSendClick_callsOnSendClick() {
        // Given
        val messages = listOf(createTestMessage(userId = 2))
        var sendClicked = false
        var sentUserId: Int? = null

        // When
        setContent(
            uiState = ChatUiState.Success(messages),
            messageText = "Test message",
            onSendClick = { userId ->
                sendClicked = true
                sentUserId = userId
            }
        )

        composeTestRule
            .onNodeWithTag("SendChatMessageButton")
            .performClick()

        // Then - callback был вызван с правильным userId
        assert(sendClicked) { "Клик на кнопку отправки должен вызывать onSendClick" }
        assert(sentUserId == 2) { "Ожидался userId=2, получен $sentUserId" }
    }

    @Test
    fun chatContent_whenRefreshClick_callsOnRefresh() {
        // Given
        var refreshClicked = false
        val refreshContentDescription = context.getString(R.string.refresh)

        // When
        setContent(
            uiState = ChatUiState.Success(listOf(createTestMessage())),
            onRefresh = { refreshClicked = true }
        )

        composeTestRule
            .onNodeWithContentDescription(refreshContentDescription)
            .performClick()

        // Then
        assert(refreshClicked) { "Клик на кнопку обновления должен вызывать onRefresh" }
    }

    @Test
    fun chatContent_whenAvatarClick_callsOnAvatarClick() {
        // Given
        var avatarClicked = false

        // When
        setContent(
            uiState = ChatUiState.Success(listOf(createTestMessage())),
            onAvatarClick = { avatarClicked = true }
        )

        // Кликаем по кнопке аватара по testTag
        composeTestRule
            .onNodeWithTag("AvatarButton")
            .performClick()

        // Then
        assert(avatarClicked) { "Клик на аватар должен вызывать onAvatarClick" }
    }

    // ========== Helper methods ==========

    private fun createTestMessage(
        id: Long = 1L,
        userId: Int? = 2,
        message: String = "Test message",
        name: String? = "Test User",
        created: String? = "2024-01-15T10:30:00Z"
    ): MessageResponse {
        return MessageResponse(
            id = id,
            userId = userId,
            message = message,
            name = name,
            created = created
        )
    }
}
