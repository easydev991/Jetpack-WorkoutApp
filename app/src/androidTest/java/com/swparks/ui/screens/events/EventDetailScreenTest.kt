package com.swparks.ui.screens.events

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.Event
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.MapUriSet
import com.swparks.ui.state.EventDetailUIState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.EventDetailEvent
import com.swparks.ui.viewmodel.IEventDetailViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTestEvent(
        id: Long = 1L,
        title: String = "Тестовое мероприятие"
    ): Event =
        Event(
            id = id,
            title = title,
            description = "Описание",
            beginDate = "2026-03-15 12:00:00",
            countryID = 1,
            cityID = 1,
            commentsCount = 0,
            preview = "",
            parkID = null,
            latitude = "55.751244",
            longitude = "37.618423",
            trainingUsersCount = 0,
            isCurrent = true,
            address = "Moscow",
            photos = emptyList(),
            trainingUsers = emptyList(),
            author = User(id = 1L, name = "Организатор", image = null),
            name = title,
            comments = emptyList(),
            isOrganizer = true,
            canEdit = true,
            trainHere = false
        )

    private fun setContent(viewModel: IEventDetailViewModel) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }
    }

    @Test
    fun errorState_retryClick_showsLoadingThenContent() {
        val uiState = MutableStateFlow<EventDetailUIState>(EventDetailUIState.Error("Ошибка"))
        val viewModel =
            createViewModel(uiState) {
                uiState.value = EventDetailUIState.InitialLoading
            }

        setContent(viewModel)

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value =
                EventDetailUIState.Content(
                    event = createTestEvent(title = "Загруженное мероприятие"),
                    address = "Россия, Москва",
                    authorAddress = "Россия, Москва"
                )
        }

        composeTestRule
            .onNodeWithText("Загруженное мероприятие")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_retryClick_showsLoadingThenError() {
        val retryError = "Повторная ошибка"
        val uiState = MutableStateFlow<EventDetailUIState>(EventDetailUIState.Error("Ошибка"))
        val viewModel =
            createViewModel(uiState) {
                uiState.value = EventDetailUIState.InitialLoading
            }

        setContent(viewModel)

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value = EventDetailUIState.Error(retryError)
        }

        composeTestRule
            .onNodeWithText(retryError)
            .assertIsDisplayed()
    }

    @Test
    fun eventParticipantsSection_whenCountKnownButUsersListEmpty_isVisibleButDisabled() {
        val event =
            createTestEvent().copy(
                trainingUsersCount = 5,
                trainingUsers = emptyList()
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventParticipantsSection(
                    event = event,
                    isAuthorized = true,
                    isRefreshing = false,
                    onParticipantToggle = {},
                    onClickParticipants = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.participants))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithContentDescription("Chevron")
            .assertCountEquals(0)
    }

    @Test
    fun contentState_whenPastEventCached_showsContentWithoutError() {
        val cachedEvent =
            createTestEvent(
                title = "Кэшированное мероприятие"
            ).copy(
                isCurrent = false
            )
        val viewModel =
            createViewModel(
                uiState =
                    MutableStateFlow(
                        EventDetailUIState.Content(
                            event = cachedEvent,
                            address = "Россия, Москва",
                            authorAddress = "Россия, Москва"
                        )
                    )
            ) {}

        setContent(viewModel)

        composeTestRule
            .onNodeWithText("Кэшированное мероприятие")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText(context.getString(R.string.try_again_button))
            .assertCountEquals(0)
    }

    private fun createViewModel(
        uiState: MutableStateFlow<EventDetailUIState>,
        onRefresh: () -> Unit
    ): IEventDetailViewModel =
        object : IEventDetailViewModel {
            override val uiState = uiState
            override val events: SharedFlow<EventDetailEvent> = MutableSharedFlow()
            override val isRefreshing = MutableStateFlow(false)
            override val isAuthorized = MutableStateFlow(false)
            override val isEventAuthor = MutableStateFlow(false)
            override val currentUserId = MutableStateFlow<Long?>(null)
            override val mapUriSet: MapUriSet? = null

            override fun onEditClick() {}

            override fun onDeleteClick() {}

            override fun onDeleteConfirm() {}

            override fun onDeleteDismiss() {}

            override fun onShareClick() {}

            override fun onParticipantToggle() {}

            override fun onParticipantsCountClick() {}

            override fun onOpenMapClick() {}

            override fun onRouteClick() {}

            override fun onAddToCalendarClick() {}

            override fun onAddToCalendarFailed() {}

            override fun onPhotoClick(photo: Photo) {}

            override fun onPhotoDeleteClick(photo: Photo) {}

            override fun onPhotoDeleteConfirm() {}

            override fun onPhotoDeleteDismiss() {}

            override fun onAddCommentClick() {}

            override fun onCommentActionClick(
                commentId: Long,
                action: CommentAction
            ) {
            }

            override fun onCommentDeleteConfirm() {}

            override fun onCommentDeleteDismiss() {}

            override fun onPhotoDeleted(photoId: Long) {}

            override fun refresh() = onRefresh()

            override fun onEventUpdated(event: Event) {}
        }
}
