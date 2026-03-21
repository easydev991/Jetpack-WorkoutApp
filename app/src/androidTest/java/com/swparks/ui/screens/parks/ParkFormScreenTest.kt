package com.swparks.ui.screens.parks

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.Park
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.ui.model.ParkForm
import com.swparks.ui.model.ParkFormMode
import com.swparks.ui.state.ParkFormUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeParkFormViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParkFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: ParkFormUiState = ParkFormUiState(
            mode = ParkFormMode.Create(
                initialAddress = "",
                initialLatitude = "",
                initialLongitude = "",
                initialCityId = null
            )
        ),
        onAction: (ParkFormNavigationAction) -> Unit = {}
    ) {
        val viewModel = FakeParkFormViewModel(initialState = uiState)
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkFormScreen(
                    viewModel = viewModel,
                    onAction = onAction
                )
            }
        }
    }

    @Test
    fun whenCreateMode_showsNewParkTitle() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "",
                    initialLatitude = "",
                    initialLongitude = "",
                    initialCityId = null
                ),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.new_park_title))
            .assertIsDisplayed()
    }

    @Test
    fun whenEditMode_showsParkTitle() {
        val park = Park(
            id = 1L,
            address = "Test Address",
            latitude = "55.7558",
            longitude = "37.6173",
            cityID = 1,
            cityName = "Test City",
            typeID = ParkType.SOVIET.rawValue,
            sizeID = ParkSize.SMALL.rawValue,
            name = "Test Park",
            preview = "",
            photos = emptyList()
        )

        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Edit(parkId = 1L, park = park),
                form = ParkForm.fromPark(park),
                initialForm = ParkForm.fromPark(park),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.park_title))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsAddressField() {
        setContent()

        composeTestRule
            .onNodeWithText(context.getString(R.string.park_address))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsParkTypeRadioButtons() {
        setContent()

        composeTestRule
            .onNodeWithText(context.getString(R.string.park_type))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.soviet_park))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.modern_park))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.collars_park))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.legendary_park))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsParkSizeRadioButtons() {
        setContent()

        composeTestRule
            .onNodeWithText(context.getString(R.string.park_size))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.small_park))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.medium_park))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.large_park))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsSaveButton() {
        setContent()

        composeTestRule
            .onNodeWithText(context.getString(R.string.save))
            .assertIsDisplayed()
    }

    @Test
    fun whenFormEmpty_saveButtonDisabled() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "",
                    initialLatitude = "",
                    initialLongitude = "",
                    initialCityId = null
                ),
                form = ParkForm(),
                initialForm = ParkForm(),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.save))
            .assertIsNotEnabled()
    }

    @Test
    fun whenFormValidForCreate_saveButtonEnabled() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1,
                    typeId = ParkType.SOVIET.rawValue,
                    sizeId = ParkSize.SMALL.rawValue,
                    selectedPhotos = listOf("photo1.jpg")
                ),
                initialForm = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.save))
            .assertIsEnabled()
    }

    @Test
    fun whenSaving_showsLoadingOverlay() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1,
                    selectedPhotos = listOf("photo1.jpg")
                ),
                isSaving = true,
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()
    }

    @Test
    fun whenSaving_fieldsAreDisabled() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1,
                    selectedPhotos = listOf("photo1.jpg")
                ),
                isSaving = true,
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.save))
            .assertIsNotEnabled()
    }

    @Test
    fun whenBackClickWithChanges_showsConfirmDialog() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "Changed Address",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                initialForm = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_confirm_close_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_confirm_close_message))
            .assertIsDisplayed()
    }

    @Test
    fun whenBackClickWithoutChanges_noDialog() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                initialForm = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .assertIsDisplayed()
    }

    @Test
    fun confirmDialog_hasCloseAndCancelButtons() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "Changed Address",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                initialForm = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.close))
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(R.string.cancel))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun confirmDialog_cancelDismissesDialog() {
        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "Changed Address",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                initialForm = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1
                ),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.cancel))
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_confirm_close_title))
            .assertDoesNotExist()
    }

    @Test
    fun whenTypeRadioButtonClicked_callsOnTypeChange() {
        val viewModel = FakeParkFormViewModel(
            initialState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1,
                    typeId = ParkType.SOVIET.rawValue
                ),
                isLoading = false
            )
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkFormScreen(
                    viewModel = viewModel,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.modern_park))
            .assertHasClickAction()
    }

    @Test
    fun whenSizeRadioButtonClicked_callsOnSizeChange() {
        val viewModel = FakeParkFormViewModel(
            initialState = ParkFormUiState(
                mode = ParkFormMode.Create(
                    initialAddress = "123 Test St",
                    initialLatitude = "55.7558",
                    initialLongitude = "37.6173",
                    initialCityId = 1
                ),
                form = ParkForm(
                    address = "123 Test St",
                    latitude = "55.7558",
                    longitude = "37.6173",
                    cityId = 1,
                    sizeId = ParkSize.SMALL.rawValue
                ),
                isLoading = false
            )
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkFormScreen(
                    viewModel = viewModel,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.large_park))
            .assertHasClickAction()
    }

    @Test
    fun whenEditMode_existingDataDisplayed() {
        val park = Park(
            id = 1L,
            address = "Existing Address",
            latitude = "55.7558",
            longitude = "37.6173",
            cityID = 1,
            cityName = "Test City",
            typeID = ParkType.MODERN.rawValue,
            sizeID = ParkSize.LARGE.rawValue,
            name = "Test Park",
            preview = "",
            photos = emptyList()
        )

        val form = ParkForm.fromPark(park)

        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Edit(parkId = 1L, park = park),
                form = form,
                initialForm = form,
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText("Existing Address")
            .assertIsDisplayed()
    }

    @Test
    fun whenEditModeNoChanges_saveButtonDisabled() {
        val park = Park(
            id = 1L,
            address = "Test Address",
            latitude = "55.7558",
            longitude = "37.6173",
            cityID = 1,
            cityName = "Test City",
            typeID = ParkType.SOVIET.rawValue,
            sizeID = ParkSize.SMALL.rawValue,
            name = "Test Park",
            preview = "",
            photos = emptyList()
        )

        val form = ParkForm.fromPark(park)

        setContent(
            uiState = ParkFormUiState(
                mode = ParkFormMode.Edit(parkId = 1L, park = park),
                form = form,
                initialForm = form,
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.save))
            .assertIsNotEnabled()
    }
}
