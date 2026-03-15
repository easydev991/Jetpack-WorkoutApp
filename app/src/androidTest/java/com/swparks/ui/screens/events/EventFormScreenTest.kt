package com.swparks.ui.screens.events

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
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
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventFormMode
import com.swparks.ui.state.EventFormUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeEventFormViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val testUser = User(id = 1L, name = "testuser", image = null)

    private fun createTestEvent(
        id: Long = 1L,
        title: String = "Test Event",
        description: String = "Test Description",
        beginDate: String = "2026-03-20"
    ): Event {
        return Event(
            id = id,
            title = title,
            description = description,
            beginDate = beginDate,
            countryID = 1,
            cityID = 1,
            preview = "",
            latitude = "55.7558",
            longitude = "37.6173",
            isCurrent = true,
            photos = emptyList(),
            author = testUser,
            name = title
        )
    }

    private fun setContent(
        uiState: EventFormUiState = EventFormUiState(),
        onAction: (EventFormNavigationAction) -> Unit = {}
    ) {
        val viewModel = FakeEventFormViewModel(initialState = uiState)
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventFormScreen(
                    viewModel = viewModel,
                    onAction = onAction
                )
            }
        }
    }

    @Test
    fun whenRegularCreateMode_showsCreateTitle() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.RegularCreate,
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_title_create))
            .assertIsDisplayed()
    }

    @Test
    fun whenEditExistingMode_showsEditTitle() {
        val event = createTestEvent()
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.EditExisting(eventId = 1L, event = event),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_title_edit))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsTitleField() {
        setContent(
            uiState = EventFormUiState(isLoading = false)
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_name_label))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsDescriptionField() {
        setContent(
            uiState = EventFormUiState(isLoading = false)
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_description_label))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsParkSelector() {
        setContent(
            uiState = EventFormUiState(isLoading = false)
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_park_label))
            .assertIsDisplayed()
    }

    @Test
    fun whenParkNotSelected_showsSelectParkPlaceholder() {
        setContent(
            uiState = EventFormUiState(
                form = EventForm(parkName = ""),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_park_select))
            .assertIsDisplayed()
    }

    @Test
    fun whenParkSelected_showsParkName() {
        setContent(
            uiState = EventFormUiState(
                form = EventForm(parkId = 1L, parkName = "Test Park"),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText("Test Park")
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsDatePicker() {
        setContent(
            uiState = EventFormUiState(isLoading = false)
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_date_label))
            .assertIsDisplayed()
    }

    @Test
    fun whenDisplayed_showsSaveButton() {
        setContent(
            uiState = EventFormUiState(isLoading = false)
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsDisplayed()
    }

    @Test
    fun whenFormEmpty_saveButtonDisabled() {
        setContent(
            uiState = EventFormUiState(
                form = EventForm(),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsNotEnabled()
    }

    @Test
    fun whenFormValidForCreate_saveButtonEnabled() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.RegularCreate,
                form = EventForm(
                    title = "Test Event",
                    description = "Description",
                    date = "2026-03-20",
                    parkId = 1L,
                    parkName = "Test Park"
                ),
                initialForm = EventForm(),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun whenFormHasOnlyTitle_saveButtonDisabled() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.RegularCreate,
                form = EventForm(title = "Test Event"),
                initialForm = EventForm(),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsNotEnabled()
    }

    @Test
    fun whenFormHasTitleAndParkButNoDate_saveButtonDisabled() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.RegularCreate,
                form = EventForm(
                    title = "Test Event",
                    parkId = 1L,
                    parkName = "Test Park"
                ),
                initialForm = EventForm(),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsNotEnabled()
    }

    @Test
    fun whenSaving_showsLoadingOverlay() {
        setContent(
            uiState = EventFormUiState(
                form = EventForm(
                    title = "Test",
                    parkId = 1L,
                    date = "2026-03-20"
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
            uiState = EventFormUiState(
                form = EventForm(title = "Test Event"),
                isSaving = true,
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsNotEnabled()
    }

    @Test
    fun whenBackClickWithChanges_showsConfirmDialog() {
        setContent(
            uiState = EventFormUiState(
                form = EventForm(title = "Changed Title"),
                initialForm = EventForm(),
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
            uiState = EventFormUiState(
                form = EventForm(),
                initialForm = EventForm(),
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
            uiState = EventFormUiState(
                form = EventForm(title = "Changed Title"),
                initialForm = EventForm(),
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
            uiState = EventFormUiState(
                form = EventForm(title = "Changed Title"),
                initialForm = EventForm(),
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
    fun parkSelector_hasClickAction() {
        setContent(
            uiState = EventFormUiState(isLoading = false)
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_park_label))
            .assertHasClickAction()
    }

    @Test
    fun whenEditExisting_existingDataDisplayed() {
        val event = createTestEvent(
            title = "Existing Event",
            description = "Existing Description",
            beginDate = "2026-03-25"
        )

        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.EditExisting(eventId = 1L, event = event),
                form = EventForm(
                    title = "Existing Event",
                    description = "Existing Description",
                    date = "2026-03-25",
                    parkId = 1L,
                    parkName = "Test Park"
                ),
                initialForm = EventForm(
                    title = "Existing Event",
                    description = "Existing Description",
                    date = "2026-03-25",
                    parkId = 1L,
                    parkName = "Test Park"
                ),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText("Existing Event")
            .assertIsDisplayed()
    }

    @Test
    fun whenEditExistingNoChanges_saveButtonDisabled() {
        val event = createTestEvent()
        val form = EventForm(
            title = "Test Event",
            description = "Description",
            date = "2026-03-20",
            parkId = 1L,
            parkName = "Test Park"
        )

        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.EditExisting(eventId = 1L, event = event),
                form = form,
                initialForm = form,
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsNotEnabled()
    }

    @Test
    fun whenCreateForSelectedMode_showsCreateTitle() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.CreateForSelected(
                    parkId = 100L,
                    parkName = "Selected Park"
                ),
                form = EventForm(parkId = 100L, parkName = "Selected Park"),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_title_create))
            .assertIsDisplayed()
    }

    @Test
    fun whenCreateForSelectedMode_parkNameDisplayed() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.CreateForSelected(
                    parkId = 100L,
                    parkName = "Selected Park"
                ),
                form = EventForm(parkId = 100L, parkName = "Selected Park"),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText("Selected Park")
            .assertIsDisplayed()
    }

    @Test
    fun whenCreateForSelectedMode_parkNotClickable() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.CreateForSelected(
                    parkId = 100L,
                    parkName = "Selected Park"
                ),
                form = EventForm(parkId = 100L, parkName = "Selected Park"),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_park_label))
            .assertHasNoClickAction()
    }

    @Test
    fun whenCreateForSelectedMode_formValid_saveButtonEnabled() {
        setContent(
            uiState = EventFormUiState(
                mode = EventFormMode.CreateForSelected(
                    parkId = 100L,
                    parkName = "Selected Park"
                ),
                form = EventForm(
                    title = "New Event",
                    description = "Description",
                    date = "2026-03-20",
                    parkId = 100L,
                    parkName = "Selected Park"
                ),
                initialForm = EventForm(parkId = 100L, parkName = "Selected Park"),
                isLoading = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.event_form_save))
            .assertIsEnabled()
    }
}
