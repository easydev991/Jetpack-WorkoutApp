package com.swparks.ui.screens.parks

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swparks.data.model.User
import com.swparks.navigation.AppState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParksRootScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenUserIsAuthorized_fabIsDisplayed() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Создать площадку")
            .assertIsDisplayed()
    }

    @Test
    fun whenUserIsNotAuthorized_fabIsNotDisplayed() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Создать площадку")
            .assertDoesNotExist()
    }

    @Test
    fun whenUserIsAuthorized_fabIsEnabled() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Создать площадку")
            .assertIsEnabled()
    }
}

@RunWith(AndroidJUnit4::class)
class LocationPermissionAlertDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenVisibleAndIsDeniedForever_showsOpenSettingsAndCorrectMessage() {
        composeTestRule.setContent {
            LocationPermissionAlertDialog(
                visible = true,
                onDismiss = {},
                onConfirm = {},
                onOpenSettings = {},
                isDeniedForever = true
            )
        }

        composeTestRule
            .onNodeWithText("Доступ к геолокации")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Для отображения твоего местоположения необходимо разрешить доступ к геолокации в настройках")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Открыть настройки")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Отмена")
            .assertIsDisplayed()
    }

    @Test
    fun whenVisibleAndIsNotDeniedForever_showsAllowAccessAndCorrectMessage() {
        composeTestRule.setContent {
            LocationPermissionAlertDialog(
                visible = true,
                onDismiss = {},
                onConfirm = {},
                onOpenSettings = {},
                isDeniedForever = false
            )
        }

        composeTestRule
            .onNodeWithText("Доступ к геолокации")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Для создания площадки необходимо разрешить приложению доступ к геолокации для определения координат новой площадки.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Разрешить")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Отмена")
            .assertIsDisplayed()
    }

    @Test
    fun whenNotVisible_dialogIsNotDisplayed() {
        composeTestRule.setContent {
            LocationPermissionAlertDialog(
                visible = false,
                onDismiss = {},
                onConfirm = {},
                onOpenSettings = {},
                isDeniedForever = false
            )
        }

        composeTestRule
            .onNodeWithText("Доступ к геолокации")
            .assertDoesNotExist()
    }

    @Test
    fun whenConfirmClicked_callsOnConfirm() {
        var confirmCalled = false
        composeTestRule.setContent {
            LocationPermissionAlertDialog(
                visible = true,
                onDismiss = {},
                onConfirm = { confirmCalled = true },
                onOpenSettings = {},
                isDeniedForever = false
            )
        }

        composeTestRule
            .onNodeWithText("Разрешить")
            .performClick()

        assert(confirmCalled) { "onConfirm should be called when confirm button is clicked" }
    }

    @Test
    fun whenDeniedForeverAndConfirmClicked_callsOnOpenSettings() {
        var openSettingsCalled = false
        composeTestRule.setContent {
            LocationPermissionAlertDialog(
                visible = true,
                onDismiss = {},
                onConfirm = {},
                onOpenSettings = { openSettingsCalled = true },
                isDeniedForever = true
            )
        }

        composeTestRule
            .onNodeWithText("Открыть настройки")
            .performClick()

        assert(openSettingsCalled) { "onOpenSettings should be called when confirm button is clicked in forever denied state" }
    }

    @Test
    fun whenDismissClicked_callsOnDismiss() {
        var dismissCalled = false
        composeTestRule.setContent {
            LocationPermissionAlertDialog(
                visible = true,
                onDismiss = { dismissCalled = true },
                onConfirm = {},
                onOpenSettings = {},
                isDeniedForever = false
            )
        }

        composeTestRule
            .onNodeWithText("Отмена")
            .performClick()

        assert(dismissCalled) { "onDismiss should be called when dismiss button is clicked" }
    }
}