package com.swparks.ui.screens.parks

import android.Manifest
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.User
import com.swparks.navigation.AppState
import com.swparks.ui.viewmodel.FakeParksRootViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParksRootScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeParksRootViewModel

    @Before
    fun setup() {
        fakeViewModel = FakeParksRootViewModel()
        try {
            val packageName = ApplicationProvider.getApplicationContext<android.content.Context>().packageName
            Runtime.getRuntime().exec(arrayOf("pm", "grant", packageName, "android.permission.ACCESS_FINE_LOCATION"))
            Runtime.getRuntime().exec(arrayOf("pm", "grant", packageName, "android.permission.ACCESS_COARSE_LOCATION"))
        } catch (e: Exception) {
            // Permission grant may fail on emulator without root, tests will handle this
        }
    }

    @Test
    fun whenUserIsAuthorized_fabIsDisplayed() {
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Создать площадку")
            .assertIsDisplayed()
    }

    @Test
    fun whenUserIsNotAuthorized_fabIsNotDisplayed() {
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Создать площадку")
            .assertDoesNotExist()
    }

    @Test
    fun whenUserIsAuthorized_fabIsEnabled() {
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Создать площадку")
            .assertIsEnabled()
    }

    @Test
    fun onFabClicked_whenPermissionGranted_fetchesLocationAndNavigates() {
        val draft = NewParkDraft.EMPTY.withCoordinates(55.75, 37.62)
        fakeViewModel.nextDraft = draft

        var capturedDraft: NewParkDraft? = null

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = { capturedDraft = it },
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Создать площадку")
            .performClick()

        composeTestRule.waitForIdle()

        assert(capturedDraft != null) { "onCreateParkClick should be called with draft" }
        capturedDraft!!
        assert(capturedDraft.latitude == 55.75) { "Draft should have correct latitude" }
        assert(capturedDraft.longitude == 37.62) { "Draft should have correct longitude" }
    }

    @Test
    fun onFabClicked_whenLocationFails_navigatesWithEmptyDraft() {
        fakeViewModel.nextDraft = NewParkDraft.EMPTY

        var capturedDraft: NewParkDraft? = null

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = { capturedDraft = it },
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Создать площадку")
            .performClick()

        composeTestRule.waitForIdle()

        assert(capturedDraft != null) { "onCreateParkClick should be called even when location fails" }
        capturedDraft!!
        assert(capturedDraft.isEmpty) { "Draft should be empty when location fails" }
    }

    @Test
    fun onFabClicked_withCoordinates_draftContainsCorrectLatitude() {
        val draft = NewParkDraft.EMPTY.withCoordinates(48.8566, 2.3522)
        fakeViewModel.nextDraft = draft

        var capturedDraft: NewParkDraft? = null

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = { capturedDraft = it },
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Создать площадку")
            .performClick()

        composeTestRule.waitForIdle()

        capturedDraft!!
        assert(capturedDraft.latitude == 48.8566) { "Draft should have correct latitude 48.8566" }
        assert(capturedDraft.longitude == 2.3522) { "Draft should have correct longitude 2.3522" }
    }

    @Test
    fun onFabClicked_withGeocodingData_draftContainsAddressAndCityId() {
        val draft = NewParkDraft(
            latitude = 55.7558,
            longitude = 37.6173,
            address = "Moscow, Red Square",
            cityId = 1
        )
        fakeViewModel.nextDraft = draft

        var capturedDraft: NewParkDraft? = null

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
                    parks = emptyList(),
                    appState = appState,
                    onCreateParkClick = { capturedDraft = it },
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Создать площадку")
            .performClick()

        composeTestRule.waitForIdle()

        capturedDraft!!
        assert(capturedDraft.address == "Moscow, Red Square") { "Draft should have address" }
        assert(capturedDraft.cityId == 1) { "Draft should have cityId" }
    }
}
