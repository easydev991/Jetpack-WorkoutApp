package com.swparks.ui.screens.parks

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.Park
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IUserAddedParksViewModel
import com.swparks.ui.viewmodel.UserAddedParksUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParksAddedByUserScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(viewModel: IUserAddedParksViewModel) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParksAddedByUserScreen(
                    config =
                        ParksAddedByUserConfig(
                            viewModel = viewModel,
                            onBackClick = {},
                            parentPaddingValues = PaddingValues()
                        )
                )
            }
        }
    }

    @Test
    fun errorState_retryClick_showsLoadingThenContent() {
        val uiState = MutableStateFlow<UserAddedParksUiState>(UserAddedParksUiState.Error("Ошибка"))
        val viewModel =
            object : IUserAddedParksViewModel {
                override val uiState: StateFlow<UserAddedParksUiState> = uiState
                override val isRefreshing = MutableStateFlow(false)

                override fun refresh() = Unit

                override fun retry() {
                    uiState.value = UserAddedParksUiState.Loading
                }

                override fun removePark(parkId: Long) = Unit
            }

        setContent(viewModel)

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value = UserAddedParksUiState.Success(listOf(createPark()))
        }

        composeTestRule
            .onNodeWithText(createPark().name)
            .assertIsDisplayed()
    }

    @Test
    fun errorState_retryClick_showsLoadingThenError() {
        val retryError = "Повторная ошибка"
        val uiState = MutableStateFlow<UserAddedParksUiState>(UserAddedParksUiState.Error("Ошибка"))
        val viewModel =
            object : IUserAddedParksViewModel {
                override val uiState: StateFlow<UserAddedParksUiState> = uiState
                override val isRefreshing = MutableStateFlow(false)

                override fun refresh() = Unit

                override fun retry() {
                    uiState.value = UserAddedParksUiState.Loading
                }

                override fun removePark(parkId: Long) = Unit
            }

        setContent(viewModel)

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value = UserAddedParksUiState.Error(retryError)
        }

        composeTestRule
            .onNodeWithText(retryError)
            .assertIsDisplayed()
    }

    private fun createPark(id: Long = 1L): Park =
        Park(
            id = id,
            name = "Park $id",
            sizeID = 1,
            typeID = 1,
            longitude = "37.6173",
            latitude = "55.7558",
            address = "Address $id",
            cityID = 1,
            countryID = 1,
            preview = "https://example.com/$id.jpg"
        )
}
