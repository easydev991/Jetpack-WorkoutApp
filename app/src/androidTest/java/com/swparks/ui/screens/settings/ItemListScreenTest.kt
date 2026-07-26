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
import com.swparks.ui.state.SelectableItem
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
    private val checkmarkDescription = context.getString(R.string.checkmark_content_description)

    private fun setContent(
        state: ItemListUiState,
        onSearchQueryChange: (String) -> Unit = {},
        onItemSelected: (SelectableItem) -> Unit = {},
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
                    items = listOf(SelectableItem("1", "Россия"), SelectableItem("2", "Беларусь")),
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
                    items = listOf(SelectableItem("1", "Москва"), SelectableItem("2", "Минск")),
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
                    items = listOf(SelectableItem("1", "Москва"), SelectableItem("2", "Минск")),
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
                    items = listOf(SelectableItem("1", "Москва"), SelectableItem("2", "Минск")),
                    selectedItem = null
                ),
            onItemSelected = { selectedItem = it.label }
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
                    items = listOf(
                        SelectableItem("1", "Москва"),
                        SelectableItem("2", "Минск"),
                        SelectableItem("3", "Казань")
                    ),
                    selectedItem = "2"
                )
        )

        composeTestRule
            .onNodeWithText("Минск")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithContentDescription(checkmarkDescription)
            .assertCountEquals(1)
    }

    @Test
    fun itemListScreen_whenSelectedItemClicked_doesNotCallOnItemSelected() {
        var selectedItem: String? = null

        setContent(
            state =
                ItemListUiState(
                    mode = ItemListMode.COUNTRY,
                    items = listOf(SelectableItem("1", "Россия"), SelectableItem("2", "Беларусь")),
                    selectedItem = "1"
                ),
            onItemSelected = { selectedItem = it.label }
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
                    items = listOf(SelectableItem("1", "Россия")),
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
