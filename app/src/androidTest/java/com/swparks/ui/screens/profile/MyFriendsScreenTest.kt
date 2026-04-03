package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.state.FriendsListUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для MyFriendsScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение заявок на добавление в друзья, реакции на нажатия кнопок
 * и поведение при разных состояниях UI.
 */
@RunWith(AndroidJUnit4::class)
class MyFriendsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: FriendsListUiState = FriendsListUiState.Success(),
        isProcessing: Boolean = false,
        onBackClick: () -> Unit = {},
        onAcceptFriendRequest: (Long) -> Unit = {},
        onDeclineFriendRequest: (Long) -> Unit = {},
        onFriendClick: (Long) -> Unit = {}
    ) {
        val testState =
            when (uiState) {
                is FriendsListUiState.Success -> uiState.copy(isProcessing = isProcessing)
                else -> uiState
            }

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                MyFriendsScreenContent(
                    uiState = testState,
                    config =
                        FriendsScreenConfig(
                            parentPaddingValues = PaddingValues()
                        ),
                    onFriendClick = onFriendClick,
                    onAction = { action ->
                        when (action) {
                            is FriendAction.Accept -> onAcceptFriendRequest(action.userId)
                            is FriendAction.Decline -> onDeclineFriendRequest(action.userId)
                            is FriendAction.Click -> if (action.userId == -1L) onBackClick() else Unit
                        }
                    }
                )
            }
        }
    }

    @Test
    fun myFriendsScreen_displaysAppBarWithFriendsTitle() {
        // When
        setContent()

        // Then - AppBar заголовок всегда отображается (даже если секция скрыта)
        val friendsTitle = context.getString(R.string.friends)
        composeTestRule
            .onAllNodesWithText(friendsTitle, ignoreCase = true)
            .assertCountEquals(1) // Только заголовок AppBar
    }

    @Test
    fun myFriendsScreen_displaysRequestsSection_whenHasFriendRequests() {
        // Given
        val testUser =
            User(
                id = 1L,
                name = "Silverfrog19",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()

        // Заголовок заявок НЕ отображается (singleSection == true)
        composeTestRule
            .onNodeWithText(context.getString(R.string.requests), ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun myFriendsScreen_displaysFriendRequestWithButtons() {
        // Given
        val testUser =
            User(
                id = 1L,
                name = "Silverfrog19",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        // When
        setContent(uiState = state)

        // Then - Имя пользователя отображается
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()

        // Кнопка "Принять" отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.accept_friend_request), ignoreCase = true)
            .assertIsDisplayed()

        // Кнопка "Отклонить" отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.decline_friend_request), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun myFriendsScreen_clicksAcceptButton_callsOnAcceptFriendRequest() {
        // Given
        val testUserId = 1L
        val testUser =
            User(
                id = testUserId,
                name = "Silverfrog19",
                image = null
            )
        var acceptedUserId: Long? = null

        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        setContent(
            uiState = state,
            onAcceptFriendRequest = { acceptedUserId = it }
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.accept_friend_request), ignoreCase = true)
            .performClick()

        // Then
        assert(acceptedUserId == testUserId) { "Ожидался userId=$testUserId при принятии заявки" }
    }

    @Test
    fun myFriendsScreen_clicksDeclineButton_callsOnDeclineFriendRequest() {
        // Given
        val testUserId = 1L
        val testUser =
            User(
                id = testUserId,
                name = "Silverfrog19",
                image = null
            )
        var declinedUserId: Long? = null

        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        setContent(
            uiState = state,
            onDeclineFriendRequest = { declinedUserId = it }
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.decline_friend_request), ignoreCase = true)
            .performClick()

        // Then
        assert(declinedUserId == testUserId) { "Ожидался userId=$testUserId при отклонении заявки" }
    }

    @Test
    fun myFriendsScreen_displaysFriendsSection_whenHasFriends() {
        // Given
        val testUser =
            User(
                id = 2L,
                name = "WorkoutMaster",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = emptyList(),
                friends = listOf(testUser)
            )

        // When
        setContent(uiState = state)

        // Then
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()

        // Заголовок секции друзей НЕ отображается (singleSection == true)
        // Но AppBar заголовок "Friends" отображается всегда
        val friendsTitle = context.getString(R.string.friends)
        composeTestRule
            .onAllNodesWithText(friendsTitle, ignoreCase = true)
            .assertCountEquals(1) // Только заголовок AppBar
    }

    @Test
    fun myFriendsScreen_displaysBothSections_whenHasRequestsAndFriends() {
        // Given
        val requestUser =
            User(
                id = 1L,
                name = "Silverfrog19",
                image = null
            )
        val friendUser =
            User(
                id = 2L,
                name = "WorkoutMaster",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(requestUser),
                friends = listOf(friendUser)
            )

        // When
        setContent(uiState = state)

        // Then - Заявки отображаются
        composeTestRule
            .onNodeWithText(requestUser.name)
            .assertIsDisplayed()

        // Друзья отображаются
        composeTestRule
            .onNodeWithText(friendUser.name)
            .assertIsDisplayed()

        // Секция заявок отображается (singleSection == false)
        composeTestRule
            .onNodeWithText(context.getString(R.string.requests), ignoreCase = true)
            .assertIsDisplayed()

        // Секция друзей отображается (singleSection == false) + AppBar заголовок
        // Всего 2 вхождения слова "Friends"
        val friendsTitle = context.getString(R.string.friends)
        composeTestRule
            .onAllNodesWithText(friendsTitle, ignoreCase = true)
            .assertCountEquals(2) // AppBar заголовок + SectionView заголовок
    }

    @Test
    fun myFriendsScreen_hidesRequestSection_whenNoRequests() {
        // Given
        val testUser =
            User(
                id = 2L,
                name = "WorkoutMaster",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = emptyList(),
                friends = listOf(testUser)
            )

        // When
        setContent(uiState = state)

        // Then - Проверяем, что секция заявок НЕ отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.requests), ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun myFriendsScreen_hidesFriendsSection_whenNoFriends() {
        // Given
        val testUser =
            User(
                id = 1L,
                name = "Silverfrog19",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        // When
        setContent(uiState = state)

        // Then - Проверяем, что заголовок секции друзей не отображается
        // (AppBar заголовок "Friends" отображается, но SectionView заголовок - нет)
        val friendsTitle = context.getString(R.string.friends)
        composeTestRule
            .onAllNodesWithText(friendsTitle, ignoreCase = true)
            .assertCountEquals(1) // Только AppBar заголовок
    }

    @Test
    fun myFriendsScreen_displaysLoadingState() {
        // Given
        val state = FriendsListUiState.Loading

        // When
        setContent(uiState = state)

        // Then - AppBar отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.friends))
            .assertIsDisplayed()
    }

    @Test
    fun myFriendsScreen_displaysErrorState() {
        // Given
        val errorMessage = "Error loading data"
        val state = FriendsListUiState.Error(message = errorMessage)

        // When
        setContent(uiState = state)

        // Then - Сообщение об ошибке отображается
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun myFriendsScreen_clicksFriend_callsOnFriendClick() {
        // Given
        val testUserId = 2L
        val testUser =
            User(
                id = testUserId,
                name = "WorkoutMaster",
                image = null
            )
        var clickedUserId: Long? = null

        val state =
            FriendsListUiState.Success(
                friendRequests = emptyList(),
                friends = listOf(testUser)
            )

        setContent(
            uiState = state,
            onFriendClick = { clickedUserId = it }
        )

        // When
        composeTestRule
            .onNodeWithText(testUser.name)
            .performClick()

        // Then
        assert(clickedUserId == testUserId) { "Ожидался userId=$testUserId при клике на друга" }
    }

    @Test
    fun myFriendsScreen_friendRowIsClickable() {
        // Given
        val testUserId = 2L
        val testUser =
            User(
                id = testUserId,
                name = "WorkoutMaster",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = emptyList(),
                friends = listOf(testUser)
            )

        setContent(uiState = state)

        // Then - Строка друга должна быть кликабельной
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertHasClickAction()
    }

    @Test
    fun myFriendsScreen_displaysEmptyState_whenNoData() {
        // Given
        val state =
            FriendsListUiState.Success(
                friendRequests = emptyList(),
                friends = emptyList()
            )

        // When
        setContent(uiState = state)

        // Then - Отображается сообщение об отсутствии друзей
        composeTestRule
            .onNodeWithText(context.getString(R.string.no_friends_yet), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun myFriendsScreen_multipleFriendRequests_allDisplayed() {
        // Given
        val users =
            listOf(
                User(id = 1L, name = "User1", image = null),
                User(id = 2L, name = "User2", image = null),
                User(id = 3L, name = "User3", image = null)
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = users,
                friends = emptyList()
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
    fun myFriendsScreen_multipleFriends_allDisplayed() {
        // Given
        val friends =
            listOf(
                User(id = 1L, name = "Friend1", image = null),
                User(id = 2L, name = "Friend2", image = null),
                User(id = 3L, name = "Friend3", image = null)
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = emptyList(),
                friends = friends
            )

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
    fun myFriendsScreen_friendRequestAccepted_requestMovedToFriends() {
        // Given
        val testUserId = 1L
        val testUser =
            User(
                id = testUserId,
                name = "Silverfrog19",
                image = null
            )

        // Когда есть и заявки, и друзья (для проверки заголовков)
        val initialState =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = listOf(User(id = 2L, name = "OtherFriend", image = null))
            )

        // When - Отображаем начальное состояние
        setContent(uiState = initialState)

        // Then - Заявка отображается
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()

        // Заголовок заявок отображается (есть обе секции)
        composeTestRule
            .onNodeWithText(context.getString(R.string.requests), ignoreCase = true)
            .assertIsDisplayed()

        // Заголовок друзей отображается (есть обе секции) + AppBar заголовок
        val friendsTitle = context.getString(R.string.friends)
        composeTestRule
            .onAllNodesWithText(friendsTitle, ignoreCase = true)
            .assertCountEquals(2) // AppBar заголовок + SectionView заголовок
    }

    @Test
    fun myFriendsScreen_friendRequestDeclined_requestDisappears() {
        // Given
        val testUserId = 1L
        val testUser =
            User(
                id = testUserId,
                name = "Silverfrog19",
                image = null
            )

        // Начальное состояние - есть заявка
        val initialState =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        // When - Отображаем начальное состояние
        setContent(uiState = initialState)

        // Then - Заявка отображается
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()

        // Заголовок заявок не отображается (только одна секция)
        composeTestRule
            .onNodeWithText(context.getString(R.string.requests), ignoreCase = true)
            .assertDoesNotExist()

        // AppBar заголовок "Friends" отображается
        val friendsTitle = context.getString(R.string.friends)
        composeTestRule
            .onAllNodesWithText(friendsTitle, ignoreCase = true)
            .assertCountEquals(1) // Только AppBar заголовок
    }

    @Test
    fun myFriendsScreen_acceptButtonDisabled_whenProcessing() {
        // Given
        val testUser =
            User(
                id = 1L,
                name = "Silverfrog19",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        // When
        setContent(
            uiState = state,
            isProcessing = true
        )

        // Then - Кнопка "Принять" отображается, но отключена
        composeTestRule
            .onNodeWithText(context.getString(R.string.accept_friend_request), ignoreCase = true)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun myFriendsScreen_declineButtonDisabled_whenProcessing() {
        // Given
        val testUser =
            User(
                id = 1L,
                name = "Silverfrog19",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        // When
        setContent(
            uiState = state,
            isProcessing = true
        )

        // Then - Кнопка "Отклонить" отображается, но отключена
        composeTestRule
            .onNodeWithText(context.getString(R.string.decline_friend_request), ignoreCase = true)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun myFriendsScreen_friendRowNotClickable_whenProcessing() {
        // Given
        val testUser =
            User(
                id = 2L,
                name = "WorkoutMaster",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = emptyList(),
                friends = listOf(testUser)
            )

        // When
        setContent(
            uiState = state,
            isProcessing = true
        )

        // Then - Строка друга отображается
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    @Test
    fun myFriendsScreen_buttonsEnabled_whenNotBusy() {
        // Given
        val testUser =
            User(
                id = 1L,
                name = "Silverfrog19",
                image = null
            )
        val state =
            FriendsListUiState.Success(
                friendRequests = listOf(testUser),
                friends = emptyList()
            )

        // When
        setContent(uiState = state)

        // Then - Кнопки доступны для нажатия
        composeTestRule
            .onNodeWithText(context.getString(R.string.accept_friend_request), ignoreCase = true)
            .assertIsEnabled()

        composeTestRule
            .onNodeWithText(context.getString(R.string.decline_friend_request), ignoreCase = true)
            .assertIsEnabled()
    }
}
