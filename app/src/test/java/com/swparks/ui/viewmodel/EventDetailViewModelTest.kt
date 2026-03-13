package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Comment
import com.swparks.data.model.Event
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.model.TextEntryOption
import com.swparks.ui.state.EventDetailUIState
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
class EventDetailViewModelTest {

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
        savedStateHandle = SavedStateHandle(mapOf("eventId" to TEST_EVENT_ID))

        every { userPreferencesRepository.isAuthorized } returns flowOf(true)
        every { userPreferencesRepository.currentUserId } returns flowOf(1L)

        coEvery { countriesRepository.getCountryById(any()) } returns null
        coEvery { countriesRepository.getCityById(any()) } returns null
    }

    @Test
    fun onCommentActionClick_whenReport_thenEmitsSendCommentComplaintEvent() = runTest {
        // Given
        val comment = Comment(
            id = TEST_COMMENT_ID,
            body = "<b>Текст жалобы</b>",
            date = "2026-03-13 12:00:00",
            user = User(id = 7L, name = "Автор", image = null)
        )
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(
            createEvent(comments = listOf(comment))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.events.test {
            viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.REPORT)

            // Then
            val event = awaitItem()
            assertTrue(event is EventDetailEvent.SendCommentComplaint)
            val complaintEvent = event as EventDetailEvent.SendCommentComplaint
            assertEquals(TEST_EVENT_TITLE, complaintEvent.complaint.eventTitle)
            assertEquals("Автор", complaintEvent.complaint.author)
            assertEquals("Текст жалобы", complaintEvent.complaint.commentText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCommentActionClick_whenReportAndAuthorMissing_thenUsesUnknownAuthor() = runTest {
        // Given
        val comment = Comment(
            id = TEST_COMMENT_ID,
            body = null,
            date = "2026-03-13 12:00:00",
            user = null
        )
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(
            createEvent(comments = listOf(comment))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.events.test {
            viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.REPORT)

            // Then
            val event = awaitItem()
            assertTrue(event is EventDetailEvent.SendCommentComplaint)
            val complaintEvent = event as EventDetailEvent.SendCommentComplaint
            assertEquals("неизвестен", complaintEvent.complaint.author)
            assertEquals("", complaintEvent.complaint.commentText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onAddCommentClick_whenContentLoaded_thenEmitsOpenTextEntryForNewComment() = runTest {
        // Given
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(
            createEvent(comments = emptyList())
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.events.test {
            viewModel.onAddCommentClick()

            // Then
            val event = awaitItem()
            assertTrue(event is EventDetailEvent.OpenCommentTextEntry)
            val openEvent = event as EventDetailEvent.OpenCommentTextEntry
            assertTrue(openEvent.mode is TextEntryMode.NewForEvent)
            assertEquals(TEST_EVENT_ID, (openEvent.mode as TextEntryMode.NewForEvent).eventId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCommentActionClick_whenEdit_thenEmitsOpenTextEntryForEdit() = runTest {
        // Given
        val comment = Comment(
            id = TEST_COMMENT_ID,
            body = "<b>Исходный текст</b>",
            date = "2026-03-13 12:00:00",
            user = User(id = 1L, name = "Автор", image = null)
        )
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(
            createEvent(comments = listOf(comment))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.events.test {
            viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.EDIT)

            // Then
            val event = awaitItem()
            assertTrue(event is EventDetailEvent.OpenCommentTextEntry)
            val openEvent = event as EventDetailEvent.OpenCommentTextEntry
            assertTrue(openEvent.mode is TextEntryMode.EditEvent)
            val editMode = openEvent.mode as TextEntryMode.EditEvent
            assertEquals(TEST_EVENT_ID, editMode.editInfo.parentObjectId)
            assertEquals(TEST_COMMENT_ID, editMode.editInfo.entryId)
            assertEquals("Исходный текст", editMode.editInfo.oldEntry)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCommentActionClick_whenDeleteOwnComment_thenEmitsDeleteConfirmDialog() = runTest {
        // Given
        val comment = Comment(
            id = TEST_COMMENT_ID,
            body = "Комментарий",
            date = "2026-03-13 12:00:00",
            user = User(id = 1L, name = "Автор", image = null)
        )
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(
            createEvent(comments = listOf(comment))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.events.test {
            viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.DELETE)

            // Then
            val event = awaitItem()
            assertEquals(EventDetailEvent.ShowDeleteCommentConfirmDialog, event)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onCommentDeleteConfirm_whenSuccess_thenCallsDeleteCommentForEvent() = runTest {
        // Given
        val comment = Comment(
            id = TEST_COMMENT_ID,
            body = "Комментарий",
            date = "2026-03-13 12:00:00",
            user = User(id = 1L, name = "Автор", image = null)
        )
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(
            createEvent(comments = listOf(comment))
        )
        coEvery {
            swRepository.deleteComment(
                option = TextEntryOption.Event(TEST_EVENT_ID),
                commentId = TEST_COMMENT_ID
            )
        } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Инициируем pending удаление через действие DELETE
        viewModel.onCommentActionClick(TEST_COMMENT_ID, CommentAction.DELETE)
        advanceUntilIdle()

        // When
        viewModel.onCommentDeleteConfirm()
        advanceUntilIdle()

        // Then
        coVerify {
            swRepository.deleteComment(
                option = TextEntryOption.Event(TEST_EVENT_ID),
                commentId = TEST_COMMENT_ID
            )
        }
    }

    private fun createViewModel(): EventDetailViewModel {
        return EventDetailViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            userPreferencesRepository = userPreferencesRepository,
            savedStateHandle = savedStateHandle,
            userNotifier = userNotifier,
            logger = logger
        )
    }

    private fun createEvent(comments: List<Comment>): Event {
        return createEvent(comments = comments, photos = emptyList())
    }

    private fun createEvent(
        comments: List<Comment> = emptyList(),
        photos: List<Photo> = emptyList()
    ): Event {
        return Event(
            id = TEST_EVENT_ID,
            title = TEST_EVENT_TITLE,
            description = "Описание",
            beginDate = "2026-03-13 12:00:00",
            countryID = 1,
            cityID = 1,
            commentsCount = comments.size,
            preview = "",
            parkID = null,
            latitude = "55.751244",
            longitude = "37.618423",
            trainingUsersCount = 0,
            isCurrent = true,
            address = "Moscow",
            photos = photos,
            trainingUsers = emptyList(),
            author = User(id = 1L, name = "Организатор", image = null),
            name = "event",
            comments = comments,
            isOrganizer = true,
            canEdit = true,
            trainHere = false
        )
    }

    // === Runtime exception handling tests ===

    @Test
    fun loadEvent_whenRuntimeException_thenShowsError() = runTest {
        // Given - мокируем выброс RuntimeException
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } throws RuntimeException("Unexpected error")

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - состояние должно быть Error
        assertTrue(viewModel.uiState.value is EventDetailUIState.Error)
    }

    @Test
    fun onDeleteConfirm_whenRuntimeException_thenHandlesError() = runTest {
        // Given - загружаем мероприятие
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(createEvent())
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Мокируем выброс RuntimeException при удалении
        coEvery { swRepository.deleteEvent(TEST_EVENT_ID) } throws RuntimeException("Unexpected error")

        // When
        viewModel.onDeleteClick()
        advanceUntilIdle()
        viewModel.onDeleteConfirm()
        advanceUntilIdle()

        // Then - состояние должно остаться Content (ошибка обработана)
        assertTrue(viewModel.uiState.value is EventDetailUIState.Content)
    }

    @Test
    fun onPhotoDeleteConfirm_whenRuntimeException_thenHandlesError() = runTest {
        // Given - загружаем мероприятие с фото
        val photo = Photo(id = 1L, photo = "http://example.com/photo.jpg")
        coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(
            createEvent(photos = listOf(photo))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Мокируем выброс RuntimeException при удалении фото
        coEvery {
            swRepository.deleteEventPhoto(
                TEST_EVENT_ID,
                1L
            )
        } throws RuntimeException("Unexpected error")

        // When
        viewModel.onPhotoDeleteClick(photo)
        advanceUntilIdle()
        viewModel.onPhotoDeleteConfirm()
        advanceUntilIdle()

        // Then - состояние должно остаться Content (ошибка обработана)
        assertTrue(viewModel.uiState.value is EventDetailUIState.Content)
    }

    private companion object {
        const val TEST_EVENT_ID = 100L
        const val TEST_COMMENT_ID = 555L
        const val TEST_EVENT_TITLE = "Тестовое мероприятие"
    }
}
