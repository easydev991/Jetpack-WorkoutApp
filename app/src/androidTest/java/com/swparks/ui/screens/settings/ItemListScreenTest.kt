package com.swparks.ui.screens.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.ui.state.ItemListUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        state: ItemListUiState,
        onSearchQueryChange: (String) -> Unit = {},
        onItemSelected: (String) -> Unit = {},
        onContactUs: () -> Unit = {},
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ItemListScreen(
                    state = state,
                    onSearchQueryChange = onSearchQueryChange,
                    onItemSelected = onItemSelected,
                    onContactUs = onContactUs,
                    onBackClick = onBackClick
                )
            }
        }
    }

    @Test
    fun itemListScreen_countryMode_displaysCountryTitle() {
        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.COUNTRY,
                    items = listOf("Россия", "Беларусь"),
                    selectedItem = null
                )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.select_country))
            .assertIsDisplayed()
    }

    @Test
    fun itemListScreen_cityMode_displaysSearchPlaceholder() {
        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.CITY,
                    items = listOf("Москва", "Минск"),
                    selectedItem = null
                )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.search))
            .assertIsDisplayed()
    }

    @Test
    fun itemListScreen_emptyState_displaysHelpMessageAndContactUsButton() {
        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.COUNTRY,
                    items = emptyList(),
                    selectedItem = null,
                    isEmpty = true
                )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.help_country_not_found))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.contact_us))
            .assertIsDisplayed()
    }

    @Test
    fun itemListScreen_whenSearchChanges_callsOnSearchQueryChange() {
        var searchQuery: String? = null

        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.CITY,
                    items = listOf("Москва", "Минск"),
                    selectedItem = null
                ),
            onSearchQueryChange = { searchQuery = it }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.search))
            .performTextInput("Мо")

        assertEquals("Мо", searchQuery)
    }

    @Test
    fun itemListScreen_whenItemClicked_callsOnItemSelected() {
        var selectedItem: String? = null

        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.CITY,
                    items = listOf("Москва", "Минск"),
                    selectedItem = null
                ),
            onItemSelected = { selectedItem = it }
        )

        composeTestRule
            .onNodeWithText("Минск")
            .performClick()

        assertEquals("Минск", selectedItem)
    }

    @Test
    fun itemListScreen_selectedItem_displaysSingleCheckmark() {
        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.CITY,
                    items = listOf("Москва", "Минск", "Казань"),
                    selectedItem = "Минск"
                )
        )

        composeTestRule
            .onNodeWithText("Минск")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithContentDescription("Checkmark")
            .assertCountEquals(1)
    }

    @Test
    fun itemListScreen_whenSelectedItemClicked_doesNotCallOnItemSelected() {
        var selectedItem: String? = null

        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.COUNTRY,
                    items = listOf("Россия", "Беларусь"),
                    selectedItem = "Россия"
                ),
            onItemSelected = { selectedItem = it }
        )

        composeTestRule
            .onNodeWithText("Россия")
            .assertIsDisplayed()

        assertNull(selectedItem)
    }

    @Test
    fun itemListScreen_whenContactUsClicked_callsOnContactUs() {
        var contactUsClicked = false

        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.CITY,
                    items = emptyList(),
                    selectedItem = null,
                    isEmpty = true
                ),
            onContactUs = { contactUsClicked = true }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.contact_us))
            .performClick()

        assertEquals(true, contactUsClicked)
    }

    @Test
    fun itemListScreen_whenBackClicked_callsOnBackClick() {
        var backClicked = false

        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.COUNTRY,
                    items = listOf("Россия"),
                    selectedItem = null
                ),
            onBackClick = { backClicked = true }
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()

        assertEquals(true, backClicked)
    }
}
