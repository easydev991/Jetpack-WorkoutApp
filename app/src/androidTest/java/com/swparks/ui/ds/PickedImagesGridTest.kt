package com.swparks.ui.ds

import android.net.Uri
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
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickedImagesGridTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        images: List<Uri> = emptyList(),
        selectionLimit: Int = 15,
        enabled: Boolean = true,
        onAction: (PickedImagesGridAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                PickedImagesGrid(
                    images = images,
                    selectionLimit = selectionLimit,
                    onAction = onAction,
                    config = PickedImagesGridConfig(enabled = enabled)
                )
            }
        }
    }

    @Test
    fun whenEmpty_showsAddButton() {
        setContent(images = emptyList())

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.photos_add))
            .assertIsDisplayed()
    }

    @Test
    fun whenEmpty_showsEmptySubtitle() {
        setContent(images = emptyList(), selectionLimit = 15)

        composeTestRule
            .onNodeWithText(context.getString(R.string.photos_add_subtitle_empty, 15))
            .assertIsDisplayed()
    }

    @Test
    fun whenHasImages_showsMoreSubtitle() {
        val images = createUriList(3)
        setContent(images = images, selectionLimit = 15)

        composeTestRule
            .onNodeWithText(context.getString(R.string.photos_add_subtitle_more, 12))
            .assertIsDisplayed()
    }

    @Test
    fun whenFull_showsMaxReachedSubtitle() {
        val images = createUriList(15)
        setContent(images = images, selectionLimit = 15)

        composeTestRule
            .onNodeWithText(context.getString(R.string.photos_max_reached))
            .assertIsDisplayed()
    }

    @Test
    fun whenFull_hidesAddButton() {
        val images = createUriList(15)
        setContent(images = images, selectionLimit = 15)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.photos_add))
            .assertDoesNotExist()
    }

    @Test
    fun whenEnabled_addButtonHasClickAction() {
        setContent(images = emptyList(), enabled = true)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.photos_add))
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun whenDisabled_addButtonIsDisabled() {
        setContent(images = emptyList(), enabled = false)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.photos_add))
            .assertIsNotEnabled()
    }

    @Test
    fun whenAddClicked_callsOnAction() {
        var addClicked = false
        setContent(
            images = emptyList(),
            onAction = { action ->
                if (action is PickedImagesGridAction.AddImage) addClicked = true
            }
        )

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.photos_add))
            .performClick()

        assertTrue(addClicked)
    }

    @Test
    fun whenHasImages_showsTitleWithCount() {
        val images = createUriList(3)
        setContent(images = images, selectionLimit = 15)

        val expectedTitle =
            context.resources.getQuantityString(
                R.plurals.photoSectionHeader,
                3,
                3
            )
        composeTestRule
            .onNodeWithText(expectedTitle)
            .assertIsDisplayed()
    }

    @Test
    fun whenCustomLimit_showsCorrectSubtitle() {
        setContent(images = emptyList(), selectionLimit = 5)

        composeTestRule
            .onNodeWithText(context.getString(R.string.photos_add_subtitle_empty, 5))
            .assertIsDisplayed()
    }

    @Test
    fun whenHasOneImage_showsOnePhotoTitle() {
        val images = createUriList(1)
        setContent(images = images, selectionLimit = 15)

        val expectedTitle =
            context.resources.getQuantityString(
                R.plurals.photoSectionHeader,
                1,
                1
            )
        composeTestRule
            .onNodeWithText(expectedTitle)
            .assertIsDisplayed()
    }

    private fun createUriList(count: Int): List<Uri> =
        List(count) { index ->
            Uri.parse("content://media/external/images/media/$index")
        }
}
