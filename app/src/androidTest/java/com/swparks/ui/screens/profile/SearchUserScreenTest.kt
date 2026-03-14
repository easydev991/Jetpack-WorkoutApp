package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.state.SearchUserUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для SearchUserScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение различных состояний UI и реакции на действия пользователя.
 */
@RunWith(AndroidJUnit4::class)
class SearchUserScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: SearchUserUiState = SearchUserUiState.Initial,
        searchQuery: String = "",
        onSearchQueryChange: (String) -> Unit = {},
        onSearch: () -> Unit = {},
        onUserClick: (Long) -> Unit = {},
        onBackClick: () -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                SearchUserScreenContent(
                    uiState = uiState,
                    searchQueryState = SearchQueryState(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange
                    ),
                    config = SearchUserConfig(
                        parentPaddingValues = PaddingValues()
                    ),
                    onAction = { action ->
                        when (action) {
                            is SearchUserAction.Search -> onSearch()
                            is SearchUserAction.UserClick -> onUserClick(action.userId)
                            is SearchUserAction.Back -> onBackClick()
                            is SearchUserAction.Retry -> onRetry()
                        }
                    }
                )
            }
        }
    }

    @Test
    fun searchUserScreen_displaysAppBarWithSearchUsersTitle() {
        // When
        setContent()

        // Then
        val title = context.getString(R.string.search_users)
        composeTestRule
            .onNodeWithText(title)
            .assertIsDisplayed()
    }

    @Test
    fun searchUserScreen_displaysSearchField() {
        // When
        setContent()

        // Then
        val label = context.getString(R.string.username_in_english)
        composeTestRule
            .onNodeWithText(label)
            .assertIsDisplayed()
    }

    @Test
    fun searchUserScreen_displaysSearchHint() {
        // When
        setContent()

        // Then
        val hint = context.getString(R.string.search_min_length_hint)
        composeTestRule
            .onNodeWithText(hint)
            .assertIsDisplayed()
    }

    @Test
    fun searchUserScreen_initial_showsNoContent() {
        // When
        setContent(uiState = SearchUserUiState.Initial)

        // Then - начальное состояние, нет результатов и нет ошибки
        composeTestRule
            .onNodeWithText(context.getString(R.string.user_not_found))
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText(context.getString(R.string.search_network_error))
            .assertDoesNotExist()
    }

    @Test
    fun searchUserScreen_success_displaysUsersList() {
        // Given
        val users = listOf(
            User(id = 1L, name = "User1", image = null),
            User(id = 2L, name = "User2", image = null)
        )

        // When
        setContent(uiState = SearchUserUiState.Success(users))

        // Then
        users.forEach { user ->
            composeTestRule
                .onNodeWithText(user.name)
                .assertIsDisplayed()
        }
    }

    @Test
    fun searchUserScreen_success_clickOnUser_callsOnUserClick() {
        // Given
        val testUserId = 1L
        val testUser = User(id = testUserId, name = "TestUser", image = null)
        var clickedUserId: Long? = null

        setContent(
            uiState = SearchUserUiState.Success(listOf(testUser)),
            onUserClick = { clickedUserId = it }
        )

        // When
        composeTestRule
            .onNodeWithText(testUser.name)
            .performClick()

        // Then
        assert(clickedUserId == testUserId) { "Ожидался userId=$testUserId при клике на пользователя" }
    }

    @Test
    fun searchUserScreen_success_userRowIsClickable() {
        // Given
        val testUser = User(id = 1L, name = "TestUser", image = null)

        setContent(uiState = SearchUserUiState.Success(listOf(testUser)))

        // Then
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertHasClickAction()
    }

    @Test
    fun searchUserScreen_empty_displaysEmptyMessage() {
        // When
        setContent(uiState = SearchUserUiState.Empty)

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.user_not_found))
            .assertIsDisplayed()
    }

    @Test
    fun searchUserScreen_networkError_displaysErrorContent() {
        // When
        setContent(uiState = SearchUserUiState.NetworkError)

        // Then - ErrorContentView отображает заголовок "Error"
        composeTestRule
            .onNodeWithText(context.getString(R.string.error_label))
            .assertIsDisplayed()

        // И сообщение об ошибке сети
        composeTestRule
            .onNodeWithText(context.getString(R.string.search_network_error))
            .assertIsDisplayed()

        // И кнопку "Try again"
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .assertIsDisplayed()
    }

    @Test
    fun searchUserScreen_networkError_clickRetry_callsOnRetry() {
        // Given
        var retryClicked = false

        setContent(
            uiState = SearchUserUiState.NetworkError,
            onRetry = { retryClicked = true }
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        // Then
        assert(retryClicked) { "Ожидался вызов onRetry при нажатии кнопки Try Again" }
    }

    @Test
    fun searchUserScreen_loading_displaysLoadingOverlay() {
        // When
        setContent(uiState = SearchUserUiState.Loading)

        // Then - AppBar и поле поиска отображаются
        composeTestRule
            .onNodeWithText(context.getString(R.string.search_users))
            .assertIsDisplayed()

        // Loading overlay отображается (проверяем через contentDescription)
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()
    }

    @Test
    fun searchUserScreen_backButton_click_callsOnBackClick() {
        // Given
        var backClicked = false

        setContent(onBackClick = { backClicked = true })

        // When - используем contentDescription для IconButton
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()

        // Then
        assert(backClicked) { "Ожидался вызов onBackClick при нажатии кнопки назад" }
    }

    @Test
    fun searchUserScreen_multipleUsers_allDisplayed() {
        // Given
        val users = listOf(
            User(id = 1L, name = "User1", image = null),
            User(id = 2L, name = "User2", image = null),
            User(id = 3L, name = "User3", image = null)
        )

        // When
        setContent(uiState = SearchUserUiState.Success(users))

        // Then
        users.forEach { user ->
            composeTestRule
                .onNodeWithText(user.name)
                .assertIsDisplayed()
        }
    }

    @Test
    fun searchUserScreen_userWithFullName_displaysName() {
        // Given
        val testUser = User(
            id = 1L,
            name = "username",
            fullName = "Full Name",
            image = null
        )

        // When
        setContent(uiState = SearchUserUiState.Success(listOf(testUser)))

        // Then - отображается имя пользователя (name, не fullName)
        composeTestRule
            .onNodeWithText(testUser.name)
            .assertIsDisplayed()
    }

    @Test
    fun searchUserScreen_loading_showsAppBarUnderOverlay() {
        // When
        setContent(uiState = SearchUserUiState.Loading)

        // Then - AppBar отображается (контент под оверлеем)
        composeTestRule
            .onNodeWithText(context.getString(R.string.search_users))
            .assertIsDisplayed()
    }
}
