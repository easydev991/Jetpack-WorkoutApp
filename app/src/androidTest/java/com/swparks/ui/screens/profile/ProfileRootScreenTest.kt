package com.swparks.ui.screens.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.model.Gender
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeProfileViewModel
import com.swparks.ui.viewmodel.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для ProfileRootScreen.
 *
 * Тестирует UI компонент с pull-to-refresh функциональностью.
 * Проверяет отображение индикатора обновления и поведение при разных состояниях.
 */
@RunWith(AndroidJUnit4::class)
class ProfileRootScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(viewModel: FakeProfileViewModel) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel,
                    config = ProfileRootConfig(),
                    onAuthAction = {}
                )
            }
        }
    }

    @Test
    fun profileRootScreen_displaysProfile_whenUserAuthorized() {
        // Given
        val testUser = createTestUser()
        val viewModel = FakeProfileViewModel(
            currentUser = MutableStateFlow(testUser),
            uiState = MutableStateFlow(ProfileUiState.Success(country = null, city = null)),
            isRefreshing = MutableStateFlow(false),
            blacklist = MutableStateFlow(emptyList())
        )

        // When
        setContent(viewModel)

        // Then - проверяем, что имя пользователя отображается
        composeTestRule
            .onNodeWithText("Test User", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_displaysEditProfileButton_whenUserAuthorized() {
        // Given
        val testUser = createTestUser()
        val viewModel = FakeProfileViewModel(
            currentUser = MutableStateFlow(testUser),
            uiState = MutableStateFlow(ProfileUiState.Success(country = null, city = null)),
            isRefreshing = MutableStateFlow(false),
            blacklist = MutableStateFlow(emptyList())
        )

        // When
        setContent(viewModel)

        // Then
        val editProfileText = context.getString(R.string.edit_profile)
        composeTestRule
            .onNodeWithText(editProfileText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_displaysLogoutButton_whenUserAuthorized() {
        // Given
        val testUser = createTestUser()
        val viewModel = FakeProfileViewModel(
            currentUser = MutableStateFlow(testUser),
            uiState = MutableStateFlow(ProfileUiState.Success(country = null, city = null)),
            isRefreshing = MutableStateFlow(false),
            blacklist = MutableStateFlow(emptyList())
        )

        // When
        setContent(viewModel)

        // Then
        val logoutText = context.getString(R.string.logout)
        composeTestRule
            .onNodeWithText(logoutText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_whenNotAuthorized_displaysIncognitoProfile() {
        // Given
        val viewModel = FakeProfileViewModel(
            currentUser = MutableStateFlow(null),
            uiState = MutableStateFlow(ProfileUiState.Success(country = null, city = null)),
            isRefreshing = MutableStateFlow(false),
            blacklist = MutableStateFlow(emptyList())
        )

        // When
        setContent(viewModel)

        // Then
        val authorizationText = context.getString(R.string.authorization)
        composeTestRule
            .onNodeWithText(authorizationText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_whenNotAuthorized_displaysNoFriendsButton() {
        // Given
        val viewModel = FakeProfileViewModel(
            currentUser = MutableStateFlow(null),
            uiState = MutableStateFlow(ProfileUiState.Success(null)),
            isRefreshing = MutableStateFlow(false),
            blacklist = MutableStateFlow(emptyList())
        )

        // When
        setContent(viewModel)

        // Then - кнопка "Друзья" не должна отображаться
        val friendsText = context.getString(R.string.friends)
        composeTestRule
            .onNodeWithText(friendsText, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun profileRootScreen_whenAuthorized_withFriends_displaysFriendsButton() {
        // Given
        val user = createTestUser().copy(friendsCount = 5)
        val viewModel = FakeProfileViewModel(
            currentUser = MutableStateFlow(user),
            uiState = MutableStateFlow(ProfileUiState.Success(country = null, city = null)),
            isRefreshing = MutableStateFlow(false),
            blacklist = MutableStateFlow(emptyList())
        )

        // When
        setContent(viewModel)

        // Then
        val friendsText = context.getString(R.string.friends)
        composeTestRule
            .onNodeWithText(friendsText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_whenAuthorized_withFriendRequests_displaysBadge() {
        // Given
        val user = createTestUser().copy(friendRequestCount = "3")
        val viewModel = FakeProfileViewModel(
            currentUser = MutableStateFlow(user),
            uiState = MutableStateFlow(ProfileUiState.Success(country = null, city = null)),
            isRefreshing = MutableStateFlow(false),
            blacklist = MutableStateFlow(emptyList())
        )

        // When
        setContent(viewModel)

        // Then - должны увидеть badge с количеством заявок
        composeTestRule
            .onNodeWithText("3", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Создает тестового пользователя
     */
    private fun createTestUser(): User {
        return User(
            id = 1L,
            name = "Test User",
            fullName = "Test User",
            email = "test@example.com",
            image = "https://example.com/image.jpg",
            genderCode = Gender.MALE.rawValue,
            friendsCount = null,
            friendRequestCount = null,
            parksCount = null,
            addedParks = emptyList(),
            journalCount = 0
        )
    }
}
