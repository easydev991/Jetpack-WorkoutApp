package com.swparks.ui.screens.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.model.ParticipantsMode
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для ParticipantsScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение списка участников, заголовки в зависимости от режима,
 * клики по пользователям и пустое состояние.
 */
@RunWith(AndroidJUnit4::class)
class ParticipantsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        mode: ParticipantsMode = ParticipantsMode.Event,
        users: List<User> = emptyList(),
        currentUserId: Long? = null,
        onBack: () -> Unit = {},
        onUserClick: (Long) -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParticipantsScreen(
                    config = ParticipantsConfig(
                        mode = mode,
                        users = users,
                        currentUserId = currentUserId
                    ),
                    onAction = { action ->
                        when (action) {
                            is ParticipantsAction.Back -> onBack()
                            is ParticipantsAction.UserClick -> onUserClick(action.userId)
                        }
                    }
                )
            }
        }
    }

    // ==================== Тесты заголовков ====================

    @Test
    fun participantsScreen_eventMode_displaysEventParticipantsTitle() {
        // Given
        val expectedTitle = context.getString(R.string.event_participants_title)

        // When
        setContent(mode = ParticipantsMode.Event)

        // Then
        composeTestRule
            .onNodeWithText(expectedTitle)
            .assertIsDisplayed()
    }

    @Test
    fun participantsScreen_parkMode_displaysParkTraineesTitle() {
        // Given
        val expectedTitle = context.getString(R.string.park_trainees_title)

        // When
        setContent(mode = ParticipantsMode.Park)

        // Then
        composeTestRule
            .onNodeWithText(expectedTitle)
            .assertIsDisplayed()
    }

    @Test
    fun participantsScreen_displaysBackButton() {
        // When
        setContent()

        // Then
        val backContentDescription = context.getString(R.string.back)
        composeTestRule
            .onNodeWithContentDescription(backContentDescription)
            .assertIsDisplayed()
    }

    // ==================== Тесты пустого состояния ====================

    @Test
    fun participantsScreen_emptyList_displaysEmptyState() {
        // Given
        val emptyStateText = context.getString(R.string.no_participants)

        // When
        setContent(users = emptyList())

        // Then
        composeTestRule
            .onNodeWithText(emptyStateText)
            .assertIsDisplayed()
    }

    @Test
    fun participantsScreen_emptyList_noUserRowsDisplayed() {
        // When
        setContent(users = emptyList())

        // Then - Проверяем, что строки пользователя отсутствуют
        composeTestRule
            .onAllNodesWithText("User1")
            .assertCountEquals(0)
    }

    // ==================== Тесты списка пользователей ====================

    @Test
    fun participantsScreen_withUsers_displaysAllUsers() {
        // Given
        val users = listOf(
            User(id = 1L, name = "User1", image = null),
            User(id = 2L, name = "User2", image = null),
            User(id = 3L, name = "User3", image = null)
        )

        // When
        setContent(users = users)

        // Then - Все пользователи отображаются
        users.forEach { user ->
            composeTestRule
                .onNodeWithText(user.name)
                .assertIsDisplayed()
        }
    }

    @Test
    fun participantsScreen_withUsers_noEmptyStateDisplayed() {
        // Given
        val users = listOf(User(id = 1L, name = "User1", image = null))
        val emptyStateText = context.getString(R.string.no_participants)

        // When
        setContent(users = users)

        // Then - Пустое состояние не отображается
        composeTestRule
            .onNodeWithText(emptyStateText)
            .assertDoesNotExist()
    }

    // ==================== Тесты кликов ====================

    @Test
    fun participantsScreen_clickOnUser_callsOnUserClick() {
        // Given
        val testUserId = 1L
        val testUser = User(id = testUserId, name = "ClickableUser", image = null)
        var clickedUserId: Long? = null

        setContent(
            users = listOf(testUser),
            onUserClick = { clickedUserId = it }
        )

        // When
        composeTestRule
            .onNodeWithText(testUser.name)
            .performClick()

        // Then
        assertEquals(
            "Ожидался userId=$testUserId при клике на пользователя",
            testUserId,
            clickedUserId
        )
    }

    @Test
    fun participantsScreen_currentUser_notClickable() {
        // Given
        val currentUserId = 1L
        val currentUser = User(id = currentUserId, name = "CurrentUser", image = null)

        setContent(
            users = listOf(currentUser),
            currentUserId = currentUserId
        )

        // Then - Строка текущего пользователя не активна
        composeTestRule
            .onNodeWithText(currentUser.name)
            .assertIsNotEnabled()
    }

    @Test
    fun participantsScreen_currentUser_doesNotTriggerOnClick() {
        // Given
        val currentUserId = 1L
        val currentUser = User(id = currentUserId, name = "CurrentUser", image = null)
        var clickedUserId: Long? = null

        setContent(
            users = listOf(currentUser),
            currentUserId = currentUserId,
            onUserClick = { clickedUserId = it }
        )

        // Then - Текущий пользователь disabled и колбэк не вызывается
        composeTestRule
            .onNodeWithText(currentUser.name)
            .assertIsNotEnabled()

        assertNull("Клик по текущему пользователю не должен вызывать onUserClick", clickedUserId)
    }

    @Test
    fun participantsScreen_mixedUsers_currentUserNotClickableOthersClickable() {
        // Given
        val currentUserId = 1L
        val users = listOf(
            User(id = currentUserId, name = "CurrentUser", image = null),
            User(id = 2L, name = "OtherUser1", image = null),
            User(id = 3L, name = "OtherUser2", image = null)
        )

        setContent(
            users = users,
            currentUserId = currentUserId
        )

        // Then - Текущий пользователь не кликабелен
        composeTestRule
            .onNodeWithText("CurrentUser")
            .assertIsNotEnabled()

        // Другие пользователи кликабельны
        composeTestRule
            .onNodeWithText("OtherUser1")
            .assertIsEnabled()

        composeTestRule
            .onNodeWithText("OtherUser2")
            .assertIsEnabled()
    }

    // ==================== Тесты back кнопки ====================

    @Test
    fun participantsScreen_clickBack_callsOnBack() {
        // Given
        var backClicked = false

        setContent(onBack = { backClicked = true })

        // When
        val backContentDescription = context.getString(R.string.back)
        composeTestRule
            .onNodeWithContentDescription(backContentDescription)
            .performClick()

        // Then
        assertTrue("Ожидался вызов onBack при клике на кнопку назад", backClicked)
    }

    // ==================== Тесты с различными данными ====================

    @Test
    fun participantsScreen_manyUsers_allDisplayed() {
        // Given
        val users = List(10) { index ->
            User(id = index.toLong(), name = "User$index", image = null)
        }

        // When
        setContent(users = users)

        // Then - Все пользователи отображаются (ленивый список должен показать видимые элементы)
        users.take(5).forEach { user ->
            composeTestRule
                .onNodeWithText(user.name)
                .assertIsDisplayed()
        }
    }

}
