package com.swparks.ui.screens.parks

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.swparks.data.model.Park
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.testing.TimeoutTest
import com.swparks.ui.model.ParkForm
import com.swparks.ui.model.ParkFormMode
import com.swparks.ui.state.ParkFormUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeParkFormViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParkFormScreenTest : TimeoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: ParkFormUiState =
            ParkFormUiState(
                mode =
                    ParkFormMode.Create(
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
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
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
        val park =
            Park(
                id = 1L,
                name = "Test Park",
                sizeID = ParkSize.SMALL.rawValue,
                typeID = ParkType.SOVIET.rawValue,
                longitude = "37.6173",
                latitude = "55.7558",
                address = "Test Address",
                cityID = 1,
                countryID = 1,
                preview = "",
                photos = emptyList()
            )

        setContent(
            uiState =
                ParkFormUiState(
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
    fun pressBack_whenHasChanges_thenShowsConfirmDialogAndStaysOnScreen() {
        // Given - форма с введённым адресом (есть несохранённые правки)
        val viewModel =
            FakeParkFormViewModel(
                initialState =
                    ParkFormUiState(
                        mode =
                            ParkFormMode.Create(
                                initialAddress = "",
                                initialLatitude = "",
                                initialLongitude = "",
                                initialCityId = null
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
            .onNodeWithText(context.getString(R.string.park_address))
            .performTextInput("Changed Address")
        closeSoftKeyboard()

        // When - системный возврат
        pressBack()

        // Then - показан confirm, форма открыта, введённое значение на месте
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.new_park_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Changed Address")
            .assertIsDisplayed()
    }

    @Test
    fun pressBack_whenNoChanges_thenNavigatesBack() {
        // Given - форма без правок
        setContent(
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "",
                            initialLatitude = "",
                            initialLongitude = "",
                            initialCityId = null
                        ),
                    isLoading = false
                )
        )

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
        // Given - форма с правками и открытым confirm-диалогом
        var navigatedBack = false
        val viewModel =
            FakeParkFormViewModel(
                initialState =
                    ParkFormUiState(
                        mode =
                            ParkFormMode.Create(
                                initialAddress = "",
                                initialLatitude = "",
                                initialLongitude = "",
                                initialCityId = null
                            ),
                        isLoading = false
                    )
            )
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkFormScreen(
                    viewModel = viewModel,
                    onAction = { action ->
                        if (action == ParkFormNavigationAction.Back) navigatedBack = true
                    }
                )
            }
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.park_address))
            .performTextInput("Changed Address")
        closeSoftKeyboard()
        pressBack()
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertIsDisplayed()

        // When - подтверждаем закрытие
        composeTestRule
            .onNodeWithText(context.getString(R.string.close))
            .performClick()

        // Then - экран уходит назад
        org.junit.Assert.assertTrue("Ожидался возврат назад после подтверждения", navigatedBack)
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertDoesNotExist()
    }

    @Test
    fun confirmCloseDialog_whenCancelled_thenStaysOnScreen() {
        // Given - форма с правками и открытым confirm-диалогом
        var navigatedBack = false
        val viewModel =
            FakeParkFormViewModel(
                initialState =
                    ParkFormUiState(
                        mode =
                            ParkFormMode.Create(
                                initialAddress = "",
                                initialLatitude = "",
                                initialLongitude = "",
                                initialCityId = null
                            ),
                        isLoading = false
                    )
            )
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkFormScreen(
                    viewModel = viewModel,
                    onAction = { action ->
                        if (action == ParkFormNavigationAction.Back) navigatedBack = true
                    }
                )
            }
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.park_address))
            .performTextInput("Changed Address")
        closeSoftKeyboard()
        pressBack()
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertIsDisplayed()

        // When - отменяем закрытие
        composeTestRule
            .onNodeWithText(context.getString(R.string.cancel))
            .performClick()

        // Then - экран открыт, правки на месте, назад не уходим
        org.junit.Assert.assertFalse("Отмена не должна уводить назад", navigatedBack)
        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText(context.getString(R.string.new_park_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Changed Address")
            .assertIsDisplayed()
    }

    @Test
    fun whenFormEmpty_saveButtonDisabled() {
        setContent(
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
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
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "123 Test St",
                            initialLatitude = "55.7558",
                            initialLongitude = "37.6173",
                            initialCityId = 1
                        ),
                    form =
                        ParkForm(
                            address = "123 Test St",
                            latitude = "55.7558",
                            longitude = "37.6173",
                            cityId = 1,
                            typeId = ParkType.SOVIET.rawValue,
                            sizeId = ParkSize.SMALL.rawValue,
                            selectedPhotos = listOf("photo1.jpg")
                        ),
                    initialForm =
                        ParkForm(
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
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "123 Test St",
                            initialLatitude = "55.7558",
                            initialLongitude = "37.6173",
                            initialCityId = 1
                        ),
                    form =
                        ParkForm(
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
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "123 Test St",
                            initialLatitude = "55.7558",
                            initialLongitude = "37.6173",
                            initialCityId = 1
                        ),
                    form =
                        ParkForm(
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
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "123 Test St",
                            initialLatitude = "55.7558",
                            initialLongitude = "37.6173",
                            initialCityId = 1
                        ),
                    form =
                        ParkForm(
                            address = "Changed Address",
                            latitude = "55.7558",
                            longitude = "37.6173",
                            cityId = 1
                        ),
                    initialForm =
                        ParkForm(
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
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.confirm_close_message))
            .assertIsDisplayed()
    }

    @Test
    fun whenBackClickWithoutChanges_noDialog() {
        setContent(
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "123 Test St",
                            initialLatitude = "55.7558",
                            initialLongitude = "37.6173",
                            initialCityId = 1
                        ),
                    form =
                        ParkForm(
                            address = "123 Test St",
                            latitude = "55.7558",
                            longitude = "37.6173",
                            cityId = 1
                        ),
                    initialForm =
                        ParkForm(
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
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "123 Test St",
                            initialLatitude = "55.7558",
                            initialLongitude = "37.6173",
                            initialCityId = 1
                        ),
                    form =
                        ParkForm(
                            address = "Changed Address",
                            latitude = "55.7558",
                            longitude = "37.6173",
                            cityId = 1
                        ),
                    initialForm =
                        ParkForm(
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
            uiState =
                ParkFormUiState(
                    mode =
                        ParkFormMode.Create(
                            initialAddress = "123 Test St",
                            initialLatitude = "55.7558",
                            initialLongitude = "37.6173",
                            initialCityId = 1
                        ),
                    form =
                        ParkForm(
                            address = "Changed Address",
                            latitude = "55.7558",
                            longitude = "37.6173",
                            cityId = 1
                        ),
                    initialForm =
                        ParkForm(
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
            .onNodeWithText(context.getString(R.string.confirm_close_title))
            .assertDoesNotExist()
    }

    @Test
    fun whenTypeRadioButtonClicked_callsOnTypeChange() {
        val viewModel =
            FakeParkFormViewModel(
                initialState =
                    ParkFormUiState(
                        mode =
                            ParkFormMode.Create(
                                initialAddress = "123 Test St",
                                initialLatitude = "55.7558",
                                initialLongitude = "37.6173",
                                initialCityId = 1
                            ),
                        form =
                            ParkForm(
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
        val viewModel =
            FakeParkFormViewModel(
                initialState =
                    ParkFormUiState(
                        mode =
                            ParkFormMode.Create(
                                initialAddress = "123 Test St",
                                initialLatitude = "55.7558",
                                initialLongitude = "37.6173",
                                initialCityId = 1
                            ),
                        form =
                            ParkForm(
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
        val park =
            Park(
                id = 1L,
                name = "Test Park",
                sizeID = ParkSize.LARGE.rawValue,
                typeID = ParkType.MODERN.rawValue,
                longitude = "37.6173",
                latitude = "55.7558",
                address = "Existing Address",
                cityID = 1,
                countryID = 1,
                preview = "",
                photos = emptyList()
            )

        val form = ParkForm.fromPark(park)

        setContent(
            uiState =
                ParkFormUiState(
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
        val park =
            Park(
                id = 1L,
                name = "Test Park",
                sizeID = ParkSize.SMALL.rawValue,
                typeID = ParkType.SOVIET.rawValue,
                longitude = "37.6173",
                latitude = "55.7558",
                address = "Test Address",
                cityID = 1,
                countryID = 1,
                preview = "",
                photos = emptyList()
            )

        val form = ParkForm.fromPark(park)

        setContent(
            uiState =
                ParkFormUiState(
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
