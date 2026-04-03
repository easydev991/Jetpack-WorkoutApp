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
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParksFilterDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        filter: ParkFilter = ParkFilter(),
        onFilterChange: (ParkFilter) -> Unit = {},
        onApply: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParksFilterDialog(
                    filter = filter,
                    onFilterChange = onFilterChange,
                    onApply = onApply,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun parksFilterDialog_render_withCustomFilter_displaysCorrectSelections() {
        val customFilter =
            ParkFilter(
                sizes = setOf(ParkSize.SMALL, ParkSize.LARGE),
                types = setOf(ParkType.SOVIET, ParkType.MODERN)
            )
        setContent(filter = customFilter)

        composeTestRule
            .onNodeWithText(context.getString(R.string.filter_parks))
            .assertIsDisplayed()
    }

    @Test
    fun parksFilterDialog_closeButton_callsOnDismiss() {
        var dismissCalled = false
        setContent(onDismiss = { dismissCalled = true })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.close))
            .performClick()

        assert(dismissCalled) { "Callback onDismiss was not called" }
    }

    @Test
    fun parksFilterDialog_resetButton_doesNotCallOnFilterChange() {
        var filterChangeCalled = false
        setContent(
            filter =
                ParkFilter(
                    sizes = setOf(ParkSize.SMALL),
                    types = setOf(ParkType.SOVIET)
                ),
            onFilterChange = { filter ->
                filterChangeCalled = true
            }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.reset_filter))
            .performClick()

        assert(!filterChangeCalled) { "Callback onFilterChange should not be called on reset" }
    }

    @Test
    fun parksFilterDialog_applyButton_callsOnApply() {
        var applyCalled = false
        val initialFilter =
            ParkFilter(sizes = setOf(ParkSize.MEDIUM), types = ParkType.entries.toSet())
        setContent(filter = initialFilter, onApply = { applyCalled = true })

        composeTestRule
            .onNodeWithText(context.getString(ParkSize.LARGE.description), useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(context.getString(R.string.apply_button))
            .performClick()

        assert(applyCalled) { "Callback onApply was not called" }
    }

    @Test
    fun parksFilterDialog_resetButton_isDisabled_whenDefaultFilter() {
        setContent(filter = ParkFilter())

        composeTestRule
            .onNodeWithText(context.getString(R.string.reset_filter))
            .assertIsNotEnabled()
    }

    @Test
    fun parksFilterDialog_applyButton_isDisabled_whenNoChanges() {
        setContent(filter = ParkFilter())

        composeTestRule
            .onNodeWithText(context.getString(R.string.apply_button))
            .assertIsNotEnabled()
    }

    @Test
    fun parksFilterDialog_sizeRows_areClickable() {
        setContent()

        composeTestRule
            .onNodeWithText(context.getString(ParkSize.SMALL.description))
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(ParkSize.MEDIUM.description))
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(ParkSize.LARGE.description))
            .assertHasClickAction()
    }

    @Test
    fun parksFilterDialog_typeRows_areClickable() {
        setContent()

        composeTestRule
            .onNodeWithText(context.getString(ParkType.SOVIET.description))
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(ParkType.MODERN.description))
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(ParkType.COLLARS.description))
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(ParkType.LEGENDARY.description))
            .assertHasClickAction()
    }

    @Test
    fun parksFilterDialog_openWithNonDefaultFilter_applyButton_isDisabled() {
        val nonDefaultFilter =
            ParkFilter(
                sizes = setOf(ParkSize.SMALL),
                types = setOf(ParkType.SOVIET)
            )
        setContent(filter = nonDefaultFilter)

        composeTestRule
            .onNodeWithText(context.getString(R.string.apply_button))
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText(context.getString(R.string.reset_filter))
            .assertIsEnabled()
    }
}
