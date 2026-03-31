package com.swparks.ui.screens.parks

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swparks.data.model.City
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.data.model.User
import com.swparks.navigation.AppState
import com.swparks.ui.model.ParksTab
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
            val packageName =
                ApplicationProvider.getApplicationContext<android.content.Context>().packageName
            Runtime.getRuntime().exec(
                arrayOf(
                    "pm",
                    "grant",
                    packageName,
                    "android.permission.ACCESS_FINE_LOCATION"
                )
            )
            Runtime.getRuntime().exec(
                arrayOf(
                    "pm",
                    "grant",
                    packageName,
                    "android.permission.ACCESS_COARSE_LOCATION"
                )
            )
        } catch (e: Exception) {
            // Permission grant may fail on emulator without root, tests will handle this
        }
    }

    private fun createPark(
        id: Long,
        cityID: Int,
        sizeID: Int,
        typeID: Int
    ) = Park(
        id = id,
        name = "Park$id",
        cityID = cityID,
        sizeID = sizeID,
        typeID = typeID,
        longitude = "0.0",
        latitude = "0.0",
        address = "Address$id",
        countryID = 1,
        preview = "preview$id"
    )

    @Test
    fun whenUserIsAuthorized_fabIsDisplayed() {
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)
            appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

            Surface {
                ParksRootScreen(
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

    @Test
    fun whenShowNoParksFoundTrue_noParksFoundViewIsDisplayed() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        fakeViewModel.setSelectedCity(city)
        fakeViewModel.setParksState(hasParks = true, filteredParks = emptyList())

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("No parks found")
            .assertIsDisplayed()
    }

    @Test
    fun whenShowNoParksFoundFalse_noParksFoundViewIsNotDisplayed() {
        fakeViewModel.setParksState(
            hasParks = true,
            filteredParks = listOf(createPark(1L, 1, 1, 1))
        )

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("No parks found")
            .assertIsNotDisplayed()
    }

    @Test
    fun whenShowNoParksFoundTrue_selectAnotherCityButtonIsVisible() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        fakeViewModel.setSelectedCity(city)
        fakeViewModel.setParksState(hasParks = true, filteredParks = emptyList())

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Select another city")
            .assertIsDisplayed()
    }

    @Test
    fun whenIsSizeTypeFilterEditedTrue_changeFiltersButtonIsVisible() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        fakeViewModel.setSelectedCity(city)
        fakeViewModel.onLocalFilterChange(
            ParkFilter(
                sizes = setOf(ParkSize.SMALL),
                types = ParkType.entries.toSet()
            )
        )
        fakeViewModel.setParksState(hasParks = true, filteredParks = emptyList())

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Change filters")
            .assertIsDisplayed()
    }

    @Test
    fun whenIsSizeTypeFilterEditedFalse_changeFiltersButtonIsNotVisible() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        fakeViewModel.setSelectedCity(city)
        fakeViewModel.onLocalFilterChange(ParkFilter())
        fakeViewModel.setParksState(hasParks = true, filteredParks = emptyList())

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Change filters")
            .assertIsNotDisplayed()
    }

    @Test
    fun whenTabSelectedIsList_listTabIsSelected() {
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("List")
            .assertIsDisplayed()
    }

    @Test
    fun whenTabSelectedIsMap_mapPlaceholderIsDisplayed() {
        fakeViewModel.onTabSelected(ParksTab.MAP)

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("park_map")
            .assertIsDisplayed()
    }

    @Test
    fun whenTabSelectedIsList_listContentIsDisplayed() {
        fakeViewModel.setParksState(
            hasParks = true,
            filteredParks = listOf(createPark(1L, 1, 1, 1))
        )

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("park_map")
            .assertIsNotDisplayed()
    }

    @Test
    fun whenTabChangedToMap_mapPlaceholderIsDisplayed() {
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Map")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("park_map")
            .assertIsDisplayed()
    }

    @Test
    fun whenTabChangedToList_listContentIsDisplayed() {
        fakeViewModel.onTabSelected(ParksTab.MAP)
        fakeViewModel.setParksState(
            hasParks = true,
            filteredParks = listOf(createPark(1L, 1, 1, 1))
        )

        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val appState = AppState(navController)

            Surface {
                ParksRootScreen(
                    appState = appState,
                    onCreateParkClick = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("List")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("park_map")
            .assertIsNotDisplayed()
    }
}
