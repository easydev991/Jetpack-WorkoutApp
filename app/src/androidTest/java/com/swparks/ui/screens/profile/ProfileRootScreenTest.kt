package com.swparks.ui.screens.profile

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.model.Gender
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.ProfileViewModel
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
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

    private lateinit var countriesRepository: CountriesRepository
    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var errorReporter: ErrorReporter
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        countriesRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        errorReporter = mockk(relaxed = true)

        // Настраиваем mock репозитории для работы с кэшем
        val testUser = createTestUser()
        every { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun profileRootScreen_displaysProfile_whenUserAuthorized() {
        // Given
        viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel
                )
            }
        }

        // Then - проверяем, что имя пользователя отображается
        composeTestRule
            .onNodeWithText("Test User", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_displaysEditProfileButton_whenUserAuthorized() {
        // Given
        viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel
                )
            }
        }

        // Then
        val editProfileText = context.getString(R.string.edit_profile)
        composeTestRule
            .onNodeWithText(editProfileText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_displaysLogoutButton_whenUserAuthorized() {
        // Given
        viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel
                )
            }
        }

        // Then
        val logoutText = context.getString(R.string.logout)
        composeTestRule
            .onNodeWithText(logoutText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_whenNotAuthorized_displaysIncognitoProfile() {
        // Given
        every { swRepository.getCurrentUserFlow() } returns flowOf(null)
        viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel
                )
            }
        }

        // Then
        val authorizationText = context.getString(R.string.authorization)
        composeTestRule
            .onNodeWithText(authorizationText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileRootScreen_whenNotAuthorized_displaysNoFriendsButton() {
        // Given
        every { swRepository.getCurrentUserFlow() } returns flowOf(null)
        viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel
                )
            }
        }

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
        every { swRepository.getCurrentUserFlow() } returns flowOf(user)
        viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel
                )
            }
        }

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
        every { swRepository.getCurrentUserFlow() } returns flowOf(user)
        viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ProfileRootScreen(
                    viewModel = viewModel
                )
            }
        }

        // Then - должны увидеть badge с количеством заявок
        composeTestRule
            .onNodeWithText("3", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Создает ViewModel для тестов
     */
    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            countriesRepository,
            swRepository,
            logger,
            errorReporter
        )
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
