package com.swparks.ui.screens.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.testing.TimeoutTest
import com.swparks.ui.state.EditProfileUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeEditProfileViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileScreenTest : TimeoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val testUser = User(id = 1L, name = "testuser", image = null)

    private fun setContent(
        uiState: EditProfileUiState = EditProfileUiState(isLoading = false),
        onAction: (EditProfileNavigationAction) -> Unit = {}
    ) {
        val viewModel = FakeEditProfileViewModel(initialState = uiState)
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EditProfileScreen(
                    currentUser = testUser,
                    viewModel = viewModel,
                    onAction = onAction
                )
            }
        }
    }

    /** Вводит текст в поле логина и закрывает клавиатуру, чтобы back не перехватил IME. */
    private fun changeLogin(value: String) {
        composeTestRule
            .onNodeWithText(context.getString(R.string.login))
            .performTextInput(value)
        closeSoftKeyboard()
    }

    private fun pressTopBarBack() {
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()
    }

    @Test
    fun topBarBack_whenHasChanges_thenShowsConfirmDialogAndStaysOnScreen() {
        // Given - форма с несохранёнными правками
        var navigatedBack = false
        setContent(
            onAction = { action ->
                if (action == EditProfileNavigationAction.Back) navigatedBack = true
            }
        )
        changeLogin("Changed Name")

        // When - клик по стрелке назад в TopBar
        pressTopBarBack()

        // Then - показан диалог подтверждения, экран не закрыт, правки на месте
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.edit_profile))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Changed Name")
            .assertIsDisplayed()
        assertFalse("Ожидался диалог вместо ухода назад", navigatedBack)
    }

    @Test
    fun topBarBack_whenNoChanges_thenNavigatesBack() {
        // Given - форма без правок
        var navigatedBack = false
        setContent(
            onAction = { action ->
                if (action == EditProfileNavigationAction.Back) navigatedBack = true
            }
        )

        // When - клик по стрелке назад в TopBar
        pressTopBarBack()

        // Then - экран закрыт без диалога подтверждения
        assertEquals("Ожидался уход назад без правок", true, navigatedBack)
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertDoesNotExist()
    }

    @Test
    fun pressBack_whenHasChanges_thenShowsConfirmDialogAndStaysOnScreen() {
        // Given - форма с несохранёнными правками
        setContent()
        changeLogin("Changed Name")

        // When - системный возврат
        pressBack()

        // Then - показан диалог подтверждения, экран не закрыт, правки на месте
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.edit_profile))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Changed Name")
            .assertIsDisplayed()
    }

    @Test
    fun pressBack_whenNoChanges_thenNavigatesBack() {
        // Given - форма без правок
        setContent()

        // When - системный возврат; Espresso бросает NoActivityResumedException,
        // когда back не перехвачен формой и закрыл единственную activity — это и есть уход назад
        try {
            pressBack()
            org.junit.Assert.fail("Ожидался уход назад без правок: back должен закрыть экран")
        } catch (expected: NoActivityResumedException) {
            // Ожидаемое поведение
        }
    }

    @Test
    fun confirmCloseDialog_whenConfirmed_thenNavigatesBack() {
        // Given - форма с несохранёнными правками и открытым диалогом
        var navigatedBack = false
        setContent(
            onAction = { action ->
                if (action == EditProfileNavigationAction.Back) navigatedBack = true
            }
        )
        changeLogin("Changed Name")
        pressBack()

        // When - подтверждение закрытия
        composeTestRule
            .onNodeWithText(context.getString(R.string.close))
            .performClick()

        // Then - экран закрыт, диалог скрыт
        assertEquals("Ожидался уход назад после подтверждения", true, navigatedBack)
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertDoesNotExist()
    }

    @Test
    fun confirmCloseDialog_whenCancelled_thenStaysOnScreen() {
        // Given - форма с несохранёнными правками и открытым диалогом
        var navigatedBack = false
        setContent(
            onAction = { action ->
                if (action == EditProfileNavigationAction.Back) navigatedBack = true
            }
        )
        changeLogin("Changed Name")
        pressBack()

        // When - отмена закрытия
        composeTestRule
            .onNodeWithText(context.getString(R.string.cancel))
            .performClick()

        // Then - экран открыт, диалог закрыт, правки на месте
        assertFalse("Отмена не должна закрывать экран", navigatedBack)
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.edit_profile))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Changed Name")
            .assertIsDisplayed()
    }
}
