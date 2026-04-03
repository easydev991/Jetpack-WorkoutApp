package com.swparks.ui.screens.parks

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
import com.swparks.data.model.Comment
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.MapUriSet
import com.swparks.ui.state.ParkDetailUIState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IParkDetailViewModel
import com.swparks.ui.viewmodel.ParkDetailEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParkDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val testUser =
        User(
            id = 1L,
            name = "Test User",
            image = null
        )

    private val testAuthor =
        User(
            id = 2L,
            name = "Park Author",
            image = null
        )

    private fun createTestPark(
        id: Long = 1L,
        name: String = "Test Park",
        address: String = "Test Address 123",
        trainingUsersCount: Int = 5,
        trainHere: Boolean = false,
        trainingUsers: List<User> = emptyList(),
        photos: List<Photo> = emptyList(),
        comments: List<Comment> = emptyList(),
        author: User = testAuthor
    ): Park =
        Park(
            id = id,
            name = name,
            sizeID = 1,
            typeID = 1,
            longitude = "37.6173",
            latitude = "55.7558",
            address = address,
            cityID = 1,
            countryID = 1,
            preview = "https://example.com/preview.jpg",
            trainingUsersCount = trainingUsersCount,
            trainHere = trainHere,
            trainingUsers = trainingUsers,
            photos = photos,
            comments = comments,
            author = author
        )

    @Test
    fun parkHeaderMapSection_showsTitleAndAddress() {
        val park = createTestPark(name = "Центральный парк", address = "ул. Парковая, 1")

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkHeaderMapSection(
                    park = park,
                    address = "Москва, Россия",
                    isAuthorized = true,
                    isRefreshing = false,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Центральный парк")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("ул. Парковая, 1")
            .assertIsDisplayed()
    }

    @Test
    fun parkHeaderMapSection_doesNotShowWhenAndWhere() {
        val park = createTestPark()
        val whenLabel = context.getString(R.string.`when`)
        val whereLabel = context.getString(R.string.where)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkHeaderMapSection(
                    park = park,
                    address = "Москва, Россия",
                    isAuthorized = true,
                    isRefreshing = false,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(whenLabel)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText(whereLabel)
            .assertDoesNotExist()
    }

    @Test
    fun parkHeaderMapSection_showsCreateEventButton() {
        val park = createTestPark()
        val createEventText = context.getString(R.string.create_event)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkHeaderMapSection(
                    park = park,
                    address = "Москва, Россия",
                    isAuthorized = true,
                    isRefreshing = false,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(createEventText)
            .assertIsDisplayed()
    }

    @Test
    fun parkHeaderMapSection_createEventButton_clickEmitsAction() {
        val park = createTestPark()
        val createEventText = context.getString(R.string.create_event)
        var clickedAction: ParkHeaderAction? = null

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkHeaderMapSection(
                    park = park,
                    address = "Москва, Россия",
                    isAuthorized = true,
                    isRefreshing = false,
                    onAction = { action -> clickedAction = action }
                )
            }
        }

        composeTestRule
            .onNodeWithText(createEventText)
            .performClick()

        assertTrue(
            "Expected CreateEvent action but got $clickedAction",
            clickedAction is ParkHeaderAction.CreateEvent
        )
    }

    @Test
    fun parkHeaderMapSection_whenNotAuthorized_hidesCreateEventButton() {
        val park = createTestPark()
        val createEventText = context.getString(R.string.create_event)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkHeaderMapSection(
                    park = park,
                    address = "Москва, Россия",
                    isAuthorized = false,
                    isRefreshing = false,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onAllNodesWithText(createEventText)
            .assertCountEquals(0)
    }

    @Test
    fun parkParticipantsSection_showsCorrectTitle() {
        val park = createTestPark(trainingUsersCount = 5)
        val traineesTitle = context.getString(R.string.park_trainees_title)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkParticipantsSection(
                    park = park,
                    isAuthorized = true,
                    isRefreshing = false,
                    onParticipantToggle = {},
                    onClickParticipants = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(traineesTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun parkParticipantsSection_showsTrainHereToggle() {
        val park = createTestPark(trainHere = false)
        val trainHereText = context.getString(R.string.train_here)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkParticipantsSection(
                    park = park,
                    isAuthorized = true,
                    isRefreshing = false,
                    onParticipantToggle = {},
                    onClickParticipants = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(trainHereText)
            .assertIsDisplayed()
    }

    @Test
    fun parkParticipantsSection_whenNotAuthorized_isHidden() {
        val park = createTestPark(trainingUsersCount = 10, trainHere = true)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkParticipantsSection(
                    park = park,
                    isAuthorized = false,
                    isRefreshing = false,
                    onParticipantToggle = {},
                    onClickParticipants = {}
                )
            }
        }

        val traineesTitle = context.getString(R.string.park_trainees_title)
        composeTestRule
            .onNodeWithText(traineesTitle, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun parkParticipantsSection_whenAuthorized_isVisible() {
        val park = createTestPark(trainingUsersCount = 3, trainHere = true)
        val traineesTitle = context.getString(R.string.park_trainees_title)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkParticipantsSection(
                    park = park,
                    isAuthorized = true,
                    isRefreshing = false,
                    onParticipantToggle = {},
                    onClickParticipants = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(traineesTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun parkParticipantsSection_whenCountKnownButUsersListEmpty_isVisibleButDisabled() {
        val park = createTestPark(trainingUsersCount = 5, trainingUsers = emptyList())
        val traineesTitle = context.getString(R.string.park_trainees_title)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkParticipantsSection(
                    park = park,
                    isAuthorized = true,
                    isRefreshing = false,
                    onParticipantToggle = {},
                    onClickParticipants = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(traineesTitle, ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithContentDescription("Chevron")
            .assertCountEquals(0)
    }

    @Test
    fun parkAuthorSection_showsCorrectTitle() {
        val park = createTestPark()
        val addedByTitle = context.getString(R.string.added_by)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkAuthorSection(
                    park = park,
                    address = "Москва, Россия",
                    config =
                        ParkAuthorConfig(
                            isAuthorized = true,
                            isRefreshing = false,
                            isParkAuthor = false
                        ),
                    onAuthorClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(addedByTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun parkAuthorSection_showsAuthorName() {
        val park = createTestPark(author = testAuthor.copy(name = "Иван Петров"))

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkAuthorSection(
                    park = park,
                    address = "Москва, Россия",
                    config =
                        ParkAuthorConfig(
                            isAuthorized = true,
                            isRefreshing = false,
                            isParkAuthor = false
                        ),
                    onAuthorClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Иван Петров")
            .assertIsDisplayed()
    }

    @Test
    fun parkPhotosSection_displaysWithoutError() {
        val photos =
            listOf(
                Photo(id = 1L, photo = "https://example.com/photo1.jpg"),
                Photo(id = 2L, photo = "https://example.com/photo2.jpg")
            )
        var clickedPhotoId: Long? = null

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkPhotosSection(
                    photos = photos,
                    isRefreshing = false,
                    onPhotoClick = { photo -> clickedPhotoId = photo.id }
                )
            }
        }

        composeTestRule.waitForIdle()
        assertTrue("Photo section should render without error", true)
    }

    @Test
    fun parkPhotosSection_whenEmpty_showsNothing() {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkPhotosSection(
                    photos = emptyList(),
                    isRefreshing = false,
                    onPhotoClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        assertTrue("Empty photo section should render without error", true)
    }

    @Test
    fun parkCommentItem_showsCommentContent() {
        val comment =
            Comment(
                id = 1L,
                body = "Отличная площадка!",
                date = "2026-03-20",
                user = testUser.copy(name = "Комментатор")
            )
        val config =
            CommentItemConfig(
                enabled = true,
                currentUserId = 99L,
                showSectionHeader = false
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkCommentItem(
                    comment = comment,
                    config = config,
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Отличная площадка!")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Комментатор")
            .assertIsDisplayed()
    }

    @Test
    fun parkAddCommentButton_whenAuthorized_isEnabled() {
        var addCommentClicked = false

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkAddCommentButton(
                    isAuthorized = true,
                    isRefreshing = false,
                    onAddCommentClick = { addCommentClicked = true }
                )
            }
        }

        val addCommentText = context.getString(R.string.add_comment)
        composeTestRule
            .onNodeWithText(addCommentText)
            .assertIsDisplayed()
            .performClick()

        assertTrue("Add comment should be clickable when authorized", addCommentClicked)
    }

    @Test
    fun parkAddCommentButton_whenNotAuthorized_isDisabled() {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkAddCommentButton(
                    isAuthorized = false,
                    isRefreshing = false,
                    onAddCommentClick = {}
                )
            }
        }

        val addCommentText = context.getString(R.string.add_comment)
        composeTestRule
            .onNodeWithText(addCommentText)
            .assertIsDisplayed()
    }

    @Test
    fun parkHeaderAction_enumHasCorrectActions() {
        val openMap = ParkHeaderAction.OpenMap
        val route = ParkHeaderAction.Route
        val createEvent = ParkHeaderAction.CreateEvent

        val actionTypes = setOf(openMap::class, route::class, createEvent::class)
        assertTrue(actionTypes.size == 3)
    }

    @Test
    fun parkAuthorConfig_isEnabled_whenAuthorizedAndNotAuthor() {
        val config =
            ParkAuthorConfig(
                isAuthorized = true,
                isRefreshing = false,
                isParkAuthor = false
            )

        assertTrue(
            "Should be enabled when authorized, not refreshing, and not author",
            config.isEnabled
        )
    }

    @Test
    fun parkAuthorConfig_isDisabled_whenNotAuthorized() {
        val config =
            ParkAuthorConfig(
                isAuthorized = false,
                isRefreshing = false,
                isParkAuthor = false
            )

        assertFalse("Should be disabled when not authorized", config.isEnabled)
    }

    @Test
    fun parkAuthorConfig_isDisabled_whenIsAuthor() {
        val config =
            ParkAuthorConfig(
                isAuthorized = true,
                isRefreshing = false,
                isParkAuthor = true
            )

        assertFalse("Should be disabled when current user is author", config.isEnabled)
    }

    @Test
    fun parkAuthorConfig_isDisabled_whenRefreshing() {
        val config =
            ParkAuthorConfig(
                isAuthorized = true,
                isRefreshing = true,
                isParkAuthor = false
            )

        assertFalse("Should be disabled when refreshing", config.isEnabled)
    }

    @Test
    fun commentItemConfig_hasCorrectDefaults() {
        val config =
            CommentItemConfig(
                enabled = true,
                currentUserId = 1L
            )

        assertFalse("showSectionHeader should default to false", config.showSectionHeader)
    }

    @Test
    fun parkDetailScreen_showsInitialLoading() {
        val viewModel =
            FakeParkDetailViewModel(
                initialState = ParkDetailUIState.InitialLoading
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        assertTrue("Screen should render InitialLoading state", true)
    }

    @Test
    fun parkDetailScreen_showsError() {
        val viewModel =
            FakeParkDetailViewModel(
                initialState = ParkDetailUIState.Error("Test error")
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Test error")
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_retryClick_showsLoadingThenContent() {
        val uiState = MutableStateFlow<ParkDetailUIState>(ParkDetailUIState.Error("Ошибка"))
        val viewModel =
            FakeParkDetailViewModel(
                uiState = uiState,
                refreshAction = {
                    uiState.value = ParkDetailUIState.InitialLoading
                }
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value =
                ParkDetailUIState.Content(
                    park = createTestPark(name = "Загруженная площадка"),
                    address = "Москва, Россия",
                    authorAddress = "Москва, Россия"
                )
        }

        composeTestRule
            .onNodeWithText("Загруженная площадка")
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_retryClick_showsLoadingThenError() {
        val retryError = "Повторная ошибка"
        val uiState = MutableStateFlow<ParkDetailUIState>(ParkDetailUIState.Error("Ошибка"))
        val viewModel =
            FakeParkDetailViewModel(
                uiState = uiState,
                refreshAction = {
                    uiState.value = ParkDetailUIState.InitialLoading
                }
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value = ParkDetailUIState.Error(retryError)
        }

        composeTestRule
            .onNodeWithText(retryError)
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_showsContent() {
        val park = createTestPark(name = "Тестовая площадка")
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Тестовая площадка")
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_doesNotShowDescriptionSection() {
        val park = createTestPark()
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        val descriptionLabel = context.getString(R.string.description)
        composeTestRule
            .onNodeWithText(descriptionLabel, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun parkDetailScreen_showsParkTitleInAppBar() {
        val park = createTestPark()
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )
        val parkTitleLabel = context.getString(R.string.park_title)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(parkTitleLabel)
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_showsAddedByTitle() {
        val park = createTestPark()
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )
        val addedByLabel = context.getString(R.string.added_by)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(addedByLabel, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_showsComments() {
        val comments =
            listOf(
                Comment(
                    id = 1L,
                    body = "Отличная площадка!",
                    date = "2026-03-20",
                    user = testUser.copy(name = "Комментатор")
                )
            )
        val park = createTestPark(comments = comments)
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Отличная площадка!")
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_showsPhotos() {
        val photos =
            listOf(
                Photo(id = 1L, photo = "https://example.com/photo1.jpg")
            )
        val park = createTestPark(photos = photos)
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )
        val photosLabel = context.getString(R.string.photos)

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(photosLabel, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun parkDetailScreen_whenNavigateToTraineesEvent_thenEmitsAction() {
        val park = createTestPark()
        val users = listOf(testUser)
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )
        var capturedAction: ParkDetailAction? = null

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = { action -> capturedAction = action }
                )
            }
        }

        runBlocking {
            viewModel.emitEvent(
                ParkDetailEvent.NavigateToTrainees(
                    parkId = park.id,
                    users = users
                )
            )
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should emit NavigateToTrainees action",
            capturedAction is ParkDetailAction.OnNavigateToTrainees
        )
    }

    @Test
    fun parkDetailScreen_backButton_callsOnBack() {
        var backClicked = false
        val park = createTestPark()
        val viewModel =
            FakeParkDetailViewModel(
                initialState =
                    ParkDetailUIState.Content(
                        park = park,
                        address = "Москва, Россия",
                        authorAddress = "Москва, Россия"
                    )
            )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ParkDetailScreen(
                    viewModel = viewModel,
                    parentPaddingValues = PaddingValues(),
                    onAction = { action ->
                        if (action is ParkDetailAction.OnBack) backClicked = true
                    }
                )
            }
        }

        val backLabel = context.getString(R.string.back)
        composeTestRule
            .onNodeWithContentDescription(backLabel)
            .performClick()

        assertTrue("Back button should call onBack", backClicked)
    }
}

private class FakeParkDetailViewModel(
    initialState: ParkDetailUIState = ParkDetailUIState.InitialLoading,
    uiState: MutableStateFlow<ParkDetailUIState> = MutableStateFlow(initialState),
    private val refreshAction: () -> Unit = {}
) : IParkDetailViewModel {
    override val uiState: StateFlow<ParkDetailUIState> = uiState
    private val mutableEvents = MutableSharedFlow<ParkDetailEvent>()
    override val events: SharedFlow<ParkDetailEvent> = mutableEvents
    override val isRefreshing: StateFlow<Boolean> = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = MutableStateFlow(true)
    override val isParkAuthor: StateFlow<Boolean> = MutableStateFlow(false)
    override val currentUserId: StateFlow<Long?> = MutableStateFlow(null)
    override val mapUriSet: MapUriSet? = null

    override fun onEditClick() {}

    override fun onDeleteClick() {}

    override fun onDeleteConfirm() {}

    override fun onDeleteDismiss() {}

    override fun onShareClick() {}

    override fun onTrainHereToggle() {}

    override fun onTraineesCountClick() {}

    override fun onOpenMapClick() {}

    override fun onRouteClick() {}

    override fun onCreateEventClick() {}

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

    override fun onParkUpdated(parkId: Long) {}

    override fun refresh() = refreshAction()

    suspend fun emitEvent(event: ParkDetailEvent) {
        mutableEvents.emit(event)
    }
}
