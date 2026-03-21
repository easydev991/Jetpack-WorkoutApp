package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Comment
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.model.TextEntryOption
import com.swparks.ui.state.ParkDetailUIState
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParkDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swRepository: SWRepository
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var userNotifier: UserNotifier
    private lateinit var savedStateHandle: SavedStateHandle
    private val logger: Logger = NoOpLogger()

    @Before
    fun setUp() {
        swRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("parkId" to TEST_PARK_ID))

        every { userPreferencesRepository.isAuthorized } returns flowOf(true)
        every { userPreferencesRepository.currentUserId } returns flowOf(1L)

        coEvery { countriesRepository.getCountryById(any()) } returns null
        coEvery { countriesRepository.getCityById(any()) } returns null
    }

    @Test
    fun init_whenParkLoaded_thenUiStateContent() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ParkDetailUIState.Content)
        val content = state as ParkDetailUIState.Content
        assertEquals(TEST_PARK_ID, content.park.id)
        assertEquals(TEST_PARK_NAME, content.park.name)
    }

    @Test
    fun init_whenLoadFails_thenUiStateError() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.failure(
            RuntimeException("Network error")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ParkDetailUIState.Error)
    }

    @Test
    fun refresh_whenCalled_thenReloadsPark() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { swRepository.getPark(TEST_PARK_ID) }
        assertTrue(viewModel.uiState.value is ParkDetailUIState.Content)
    }

    @Test
    fun onTrainHereToggle_whenSuccess_thenUpdatesTrainHereAndUsers() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(trainHere = false)
        )
        coEvery { swRepository.changeTrainHereStatus(true, TEST_PARK_ID) } returns Result.success(
            Unit
        )
        val currentUser = User(id = 1L, name = "Текущий пользователь", image = null)
        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(currentUser)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTrainHereToggle()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ParkDetailUIState.Content
        assertTrue(state.park.trainHere == true)
        assertTrue(state.park.trainingUsers?.any { it.id == 1L } == true)
    }

    @Test
    fun onTrainHereToggle_whenFailure_thenRevertsOptimisticState() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(trainHere = false)
        )
        coEvery { swRepository.changeTrainHereStatus(true, TEST_PARK_ID) } returns Result.failure(
            RuntimeException("Network error")
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTrainHereToggle()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ParkDetailUIState.Content
        assertTrue(state.park.trainHere == false)
    }

    @Test
    fun onTraineesCountClick_thenEmitsNavigateToTrainees() = runTest {
        val user1 = User(id = 1L, name = "User 1", image = null)
        val user2 = User(id = 2L, name = "User 2", image = null)
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(trainingUsers = listOf(user1, user2))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onTraineesCountClick()

            val event = awaitItem()
            assertTrue(event is ParkDetailEvent.NavigateToTrainees)
            val navEvent = event as ParkDetailEvent.NavigateToTrainees
            assertEquals(TEST_PARK_ID, navEvent.parkId)
            assertEquals(2, navEvent.users.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCreateEventClick_thenEmitsNavigateToCreateEvent() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onCreateEventClick()

            val event = awaitItem()
            assertTrue(event is ParkDetailEvent.NavigateToCreateEvent)
            val navEvent = event as ParkDetailEvent.NavigateToCreateEvent
            assertEquals(TEST_PARK_ID, navEvent.parkId)
            assertEquals(TEST_PARK_NAME, navEvent.parkName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onDeleteConfirm_whenSuccess_thenEmitsParkDeleted() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        coEvery { swRepository.deletePark(TEST_PARK_ID) } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteClick()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onDeleteConfirm()
            advanceUntilIdle()

            assertEquals(ParkDetailEvent.ParkDeleted(TEST_PARK_ID), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { swRepository.deletePark(TEST_PARK_ID) }
    }

    @Test
    fun onOpenMapClick_thenEmitsOpenMap() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onOpenMapClick()

            assertEquals(ParkDetailEvent.OpenMap, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onRouteClick_thenEmitsBuildRoute() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onRouteClick()

            assertEquals(ParkDetailEvent.BuildRoute, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onEditClick_thenOnlyLogsAndDoesNotNavigate() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onEditClick()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onParkUpdated_whenCalled_thenReloadsPark() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        val updatedPark = createPark().copy(name = "Обновленное название")
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(updatedPark)

        viewModel.onParkUpdated(TEST_PARK_ID)
        advanceUntilIdle()

        coVerify(exactly = 2) { swRepository.getPark(TEST_PARK_ID) }
        val state = viewModel.uiState.value as ParkDetailUIState.Content
        assertEquals("Обновленное название", state.park.name)
    }

    @Test
    fun onDeleteClick_whenNotAuthor_thenDoesNotShowDialog() = runTest {
        val otherUserPark = createPark().copy(
            author = User(id = 999L, name = "Другой автор", image = null)
        )
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(otherUserPark)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onDeleteClick()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPhotoClick_thenEmitsNavigateToPhotoDetail() = runTest {
        val photo = Photo(id = 1L, photo = "http://example.com/photo.jpg")
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(photos = listOf(photo))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPhotoClick(photo)

            val event = awaitItem()
            assertTrue(event is ParkDetailEvent.NavigateToPhotoDetail)
            val photoEvent = event as ParkDetailEvent.NavigateToPhotoDetail
            assertEquals(photo, photoEvent.photo)
            assertEquals(TEST_PARK_ID, photoEvent.parkId)
            assertEquals(TEST_PARK_NAME, photoEvent.parkTitle)
            assertTrue(photoEvent.isParkAuthor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPhotoDeleteConfirm_whenSuccess_thenEmitsPhotoDeleted() = runTest {
        val photo = Photo(id = 1L, photo = "http://example.com/photo.jpg")
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(photos = listOf(photo))
        )
        coEvery { swRepository.deleteParkPhoto(TEST_PARK_ID, 1L) } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPhotoDeleteClick(photo)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPhotoDeleteConfirm()
            advanceUntilIdle()

            assertEquals(ParkDetailEvent.PhotoDeleted(1L), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { swRepository.deleteParkPhoto(TEST_PARK_ID, 1L) }
    }

    @Test
    fun init_whenRuntimeException_thenShowsError() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } throws RuntimeException("Unexpected error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ParkDetailUIState.Error)
    }

    @Test
    fun onDeleteConfirm_whenRuntimeException_thenHandlesError() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { swRepository.deletePark(TEST_PARK_ID) } throws RuntimeException("Unexpected error")

        viewModel.onDeleteClick()
        advanceUntilIdle()
        viewModel.onDeleteConfirm()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ParkDetailUIState.Content)
    }

    @Test
    fun onPhotoDeleteConfirm_whenRuntimeException_thenHandlesError() = runTest {
        val photo = Photo(id = 1L, photo = "http://example.com/photo.jpg")
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(photos = listOf(photo))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { swRepository.deleteParkPhoto(TEST_PARK_ID, 1L) } throws RuntimeException(
            "Unexpected error"
        )

        viewModel.onPhotoDeleteClick(photo)
        advanceUntilIdle()
        viewModel.onPhotoDeleteConfirm()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ParkDetailUIState.Content)
    }

    @Test
    fun onAddCommentClick_thenEmitsOpenCommentTextEntryNewForPark() = runTest {
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(createPark())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAddCommentClick()

            val event = awaitItem()
            assertTrue(event is ParkDetailEvent.OpenCommentTextEntry)
            val openEvent = event as ParkDetailEvent.OpenCommentTextEntry
            assertTrue(openEvent.mode is TextEntryMode.NewForPark)
            assertEquals(TEST_PARK_ID, (openEvent.mode as TextEntryMode.NewForPark).parkId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCommentActionClick_whenEdit_thenEmitsOpenCommentTextEntryEditPark() = runTest {
        val comment = Comment(
            id = TEST_COMMENT_ID,
            body = "<b>Исходный текст</b>",
            date = "2026-03-13 12:00:00",
            user = User(id = 1L, name = "Автор", image = null)
        )
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(comments = listOf(comment))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.EDIT)

            val event = awaitItem()
            assertTrue(event is ParkDetailEvent.OpenCommentTextEntry)
            val openEvent = event as ParkDetailEvent.OpenCommentTextEntry
            assertTrue(openEvent.mode is TextEntryMode.EditPark)
            val editMode = openEvent.mode as TextEntryMode.EditPark
            assertEquals(TEST_PARK_ID, editMode.editInfo.parentObjectId)
            assertEquals(TEST_COMMENT_ID, editMode.editInfo.entryId)
            assertEquals("Исходный текст", editMode.editInfo.oldEntry)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCommentActionClick_whenReport_thenEmitsSendCommentComplaintPark() = runTest {
        val comment = Comment(
            id = TEST_COMMENT_ID,
            body = "<b>Текст жалобы</b>",
            date = "2026-03-13 12:00:00",
            user = User(id = 7L, name = "Автор", image = null)
        )
        coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
            createPark(comments = listOf(comment))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.REPORT)

            val event = awaitItem()
            assertTrue(event is ParkDetailEvent.SendCommentComplaint)
            val complaintEvent = event as ParkDetailEvent.SendCommentComplaint
            val parkComment = complaintEvent.complaint
            assertEquals(TEST_PARK_NAME, parkComment.parkTitle)
            assertEquals("Автор", parkComment.author)
            assertEquals("Текст жалобы", parkComment.commentText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCommentDeleteConfirm_whenSuccess_thenCallsDeleteCommentWithTextEntryOptionPark() =
        runTest {
            val comment = Comment(
                id = TEST_COMMENT_ID,
                body = "Комментарий",
                date = "2026-03-13 12:00:00",
                user = User(id = 1L, name = "Автор", image = null)
            )
            coEvery { swRepository.getPark(TEST_PARK_ID) } returns Result.success(
                createPark(comments = listOf(comment))
            )
            coEvery {
                swRepository.deleteComment(
                    option = TextEntryOption.Park(TEST_PARK_ID),
                    commentId = TEST_COMMENT_ID
                )
            } returns Result.success(Unit)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.DELETE)
            advanceUntilIdle()

            viewModel.onCommentDeleteConfirm()
            advanceUntilIdle()

            coVerify {
                swRepository.deleteComment(
                    option = TextEntryOption.Park(TEST_PARK_ID),
                    commentId = TEST_COMMENT_ID
                )
            }
        }

    private fun createViewModel(): ParkDetailViewModel {
        return ParkDetailViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            userPreferencesRepository = userPreferencesRepository,
            savedStateHandle = savedStateHandle,
            userNotifier = userNotifier,
            logger = logger
        )
    }

    private fun createPark(
        trainHere: Boolean = false,
        trainingUsers: List<User> = emptyList(),
        photos: List<Photo> = emptyList(),
        comments: List<Comment> = emptyList()
    ): Park {
        return Park(
            id = TEST_PARK_ID,
            name = TEST_PARK_NAME,
            sizeID = 1,
            typeID = 1,
            longitude = "37.618423",
            latitude = "55.751244",
            address = "Moscow",
            cityID = 1,
            countryID = 1,
            commentsCount = comments.size,
            preview = "",
            trainingUsersCount = trainingUsers.size,
            createDate = "2026-03-13 12:00:00",
            modifyDate = null,
            author = User(id = 1L, name = "Автор площадки", image = null),
            photos = photos,
            comments = comments,
            trainHere = trainHere,
            equipmentIDS = null,
            mine = true,
            canEdit = true,
            trainingUsers = trainingUsers
        )
    }

    private companion object {
        const val TEST_PARK_ID = 100L
        const val TEST_PARK_NAME = "Тестовая площадка"
        const val TEST_COMMENT_ID = 500L
    }
}
