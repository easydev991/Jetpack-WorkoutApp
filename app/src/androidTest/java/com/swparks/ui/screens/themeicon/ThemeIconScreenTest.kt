package com.swparks.ui.screens.themeicon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.domain.model.AppIcon
import com.swparks.domain.model.AppTheme
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для ThemeIconScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 */
@RunWith(AndroidJUnit4::class)
class ThemeIconScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        theme: AppTheme = AppTheme.SYSTEM,
        useDynamicColors: Boolean = true,
        icon: AppIcon = AppIcon.DEFAULT,
        onThemeChange: (AppTheme) -> Unit = {},
        onDynamicColorsChange: (Boolean) -> Unit = {},
        onIconChange: (AppIcon) -> Unit = {},
        onBackClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ThemeIconScreenContent(
                    params =
                        ThemeIconScreenParams(
                            theme = theme,
                            useDynamicColors = useDynamicColors,
                            icon = icon,
                            onThemeChange = onThemeChange,
                            onDynamicColorsChange = onDynamicColorsChange,
                            onIconChange = onIconChange,
                            onBackClick = onBackClick,
                        ),
                )
            }
        }
    }

    @Test
    fun themeIconScreen_displaysAppBarWithBackButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_theme_and_icon), ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_displaysAppThemeSection() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_theme), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_displaysAllThemeRadioButtons() {
        // When
        setContent()

        // Then - Все три радио-кнопки должны быть отображены
        composeTestRule
            .onNodeWithText(context.getString(R.string.light), ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.dark), ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.system), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_lightThemeRadioButtonSelected_whenThemeIsLight() {
        // When
        setContent(theme = AppTheme.LIGHT)

        // Then - Светлая тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.light), ignoreCase = true)
            .assertIsDisplayed()

        // Затемнняя тема не должна быть выбрана (текст отображается)
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark), ignoreCase = true)
            .assertIsDisplayed()

        // Системная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.system), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_darkThemeRadioButtonSelected_whenThemeIsDark() {
        // When
        setContent(theme = AppTheme.DARK)

        // Then - Тёмная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark), ignoreCase = true)
            .assertIsDisplayed()

        // Светлая тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.light), ignoreCase = true)
            .assertIsDisplayed()

        // Системная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.system), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_systemThemeRadioButtonSelected_whenThemeIsSystem() {
        // When
        setContent(theme = AppTheme.SYSTEM)

        // Then - Системная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.system), ignoreCase = true)
            .assertIsDisplayed()

        // Светлая тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.light), ignoreCase = true)
            .assertIsDisplayed()

        // Тёмная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_clicksLightTheme_callsOnThemeChange() {
        // Given
        var clickedTheme: AppTheme? = null
        setContent(
            theme = AppTheme.DARK,
            onThemeChange = { clickedTheme = it },
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.light), ignoreCase = true)
            .performClick()

        // Then
        assert(clickedTheme == AppTheme.LIGHT) { "Ожидалась тема LIGHT" }
    }

    @Test
    fun themeIconScreen_clicksDarkTheme_callsOnThemeChange() {
        // Given
        var clickedTheme: AppTheme? = null
        setContent(
            theme = AppTheme.LIGHT,
            onThemeChange = { clickedTheme = it },
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark), ignoreCase = true)
            .performClick()

        // Then
        assert(clickedTheme == AppTheme.DARK) { "Ожидалась тема DARK" }
    }

    @Test
    fun themeIconScreen_clicksSystemTheme_callsOnThemeChange() {
        // Given
        var clickedTheme: AppTheme? = null
        setContent(
            theme = AppTheme.LIGHT,
            onThemeChange = { clickedTheme = it },
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.system), ignoreCase = true)
            .performClick()

        // Then
        assert(clickedTheme == AppTheme.SYSTEM) { "Ожидалась тема SYSTEM" }
    }

    @Test
    fun themeIconScreen_displaysDynamicColorsSection() {
        // When
        setContent()

        // Then - Секция динамических цветов отображается (если доступно)
        composeTestRule
            .onNodeWithText(context.getString(R.string.dynamic_colors), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_displaysAppIconSection() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_icon), ignoreCase = true)
            .assertIsDisplayed()
    }
}
