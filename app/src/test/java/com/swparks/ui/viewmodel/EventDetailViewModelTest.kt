package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.swparks.R
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.Comment
import com.swparks.data.model.Event
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.exception.NotFoundException
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.DeleteEventUseCase
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.model.TextEntryOption
import com.swparks.ui.state.EventDetailUIState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
    private lateinit var deleteEventUseCase: DeleteEventUseCase
    private lateinit var resourcesProvider: ResourcesProvider
    private val logger: Logger = NoOpLogger()

    @Before
    fun setUp() {
        swRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("eventId" to TEST_EVENT_ID))
        deleteEventUseCase = mockk(relaxed = true)
        resourcesProvider = mockk(relaxed = true)

        every { userPreferencesRepository.isAuthorized } returns flowOf(true)
        every { userPreferencesRepository.currentUserId } returns flowOf(1L)

        coEvery { countriesRepository.getCountryById(any()) } returns null
        coEvery { countriesRepository.getCityById(any()) } returns null
        coEvery { swRepository.getEventFromCache(any()) } returns null

        every { resourcesProvider.getString(R.string.error_server_not_found) } returns "Ресурс не найден на сервере"
    }

    @Test
    fun onCommentActionClick_whenReport_thenEmitsSendCommentComplaintEvent() =
        runTest {
            // Given
            val comment =
                Comment(
                    id = TEST_COMMENT_ID,
                    body = "<b>Текст жалобы</b>",
                    date = "2026-03-13 12:00:00",
                    user = User(id = 7L, name = "Автор", image = null)
                )
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
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
    fun onCommentActionClick_whenReportAndAuthorMissing_thenUsesUnknownAuthor() =
        runTest {
            // Given
            val comment =
                Comment(
                    id = TEST_COMMENT_ID,
                    body = null,
                    date = "2026-03-13 12:00:00",
                    user = null
                )
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
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
    fun onAddCommentClick_whenContentLoaded_thenEmitsOpenTextEntryForNewComment() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
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
    fun onCommentActionClick_whenEdit_thenEmitsOpenTextEntryForEdit() =
        runTest {
            // Given
            val comment =
                Comment(
                    id = TEST_COMMENT_ID,
                    body = "<b>Исходный текст</b>",
                    date = "2026-03-13 12:00:00",
                    user = User(id = 1L, name = "Автор", image = null)
                )
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
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
    fun onCommentActionClick_whenDeleteOwnComment_thenEmitsDeleteConfirmDialog() =
        runTest {
            // Given
            val comment =
                Comment(
                    id = TEST_COMMENT_ID,
                    body = "Комментарий",
                    date = "2026-03-13 12:00:00",
                    user = User(id = 1L, name = "Автор", image = null)
                )
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
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
    fun onCommentDeleteConfirm_whenSuccess_thenCallsDeleteCommentForEvent() =
        runTest {
            // Given
            val comment =
                Comment(
                    id = TEST_COMMENT_ID,
                    body = "Комментарий",
                    date = "2026-03-13 12:00:00",
                    user = User(id = 1L, name = "Автор", image = null)
                )
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
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

    private fun createViewModel(): EventDetailViewModel =
        EventDetailViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            userPreferencesRepository = userPreferencesRepository,
            savedStateHandle = savedStateHandle,
            userNotifier = userNotifier,
            logger = logger,
            deleteEventUseCase = deleteEventUseCase,
            resourcesProvider = resourcesProvider
        )

    private fun createEvent(comments: List<Comment>): Event = createEvent(comments = comments, photos = emptyList())

    private fun createEvent(
        comments: List<Comment> = emptyList(),
        photos: List<Photo> = emptyList(),
        trainHere: Boolean = false,
        isCurrent: Boolean = true,
        trainingUsers: List<User> = emptyList()
    ): Event =
        Event(
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
            trainingUsersCount = trainingUsers.size,
            isCurrent = isCurrent,
            address = "Moscow",
            photos = photos,
            trainingUsers = trainingUsers,
            author = User(id = 1L, name = "Организатор", image = null),
            name = "event",
            comments = comments,
            isOrganizer = true,
            canEdit = true,
            trainHere = trainHere
        )

    // === Load event tests ===

    @Test
    fun loadEvent_whenSuccess_thenShowsContent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(createEvent())

            // When
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Content)
            val content = state as EventDetailUIState.Content
            assertEquals(TEST_EVENT_ID, content.event.id)
            assertEquals(TEST_EVENT_TITLE, content.event.title)
        }

    @Test
    fun loadEvent_whenPastEventCached_thenShowsCachedContentAndRefreshesInBackground() =
        runTest {
            val cachedEvent =
                createEvent(isCurrent = false, photos = listOf(Photo(1L, "cached.jpg")))
            val refreshGate = CompletableDeferred<Unit>()
            val refreshedEvent =
                createEvent(
                    isCurrent = false,
                    photos = listOf(Photo(2L, "fresh.jpg")),
                    comments =
                        listOf(
                            Comment(
                                id = 2L,
                                body = "Комментарий",
                                date = "2026-03-13 12:00:00",
                                user = User(id = 2L, name = "Пользователь", image = null)
                            )
                        )
                )

            coEvery { swRepository.getEventFromCache(TEST_EVENT_ID) } returns cachedEvent
            coEvery {
                swRepository.getEvent(TEST_EVENT_ID)
            } coAnswers {
                refreshGate.await()
                Result.success(refreshedEvent)
            }
            coEvery { swRepository.saveEventFull(refreshedEvent) } returns Unit

            val viewModel = createViewModel()
            runCurrent()

            val immediateState = viewModel.uiState.value
            assertTrue(immediateState is EventDetailUIState.Content)
            assertEquals(
                "cached.jpg",
                (immediateState as EventDetailUIState.Content)
                    .event.photos
                    .first()
                    .photo
            )

            refreshGate.complete(Unit)
            advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertTrue(finalState is EventDetailUIState.Content)
            assertEquals(
                "fresh.jpg",
                (finalState as EventDetailUIState.Content)
                    .event.photos
                    .first()
                    .photo
            )
            coVerify { swRepository.getEventFromCache(TEST_EVENT_ID) }
            coVerify { swRepository.getEvent(TEST_EVENT_ID) }
            coVerify { swRepository.saveEventFull(refreshedEvent) }
        }

    @Test
    fun loadEvent_whenCachedPastEventAndBackgroundRefreshFails_keepsCachedContent() =
        runTest {
            val cachedEvent =
                createEvent(isCurrent = false, photos = listOf(Photo(1L, "cached.jpg")))
            coEvery { swRepository.getEventFromCache(TEST_EVENT_ID) } returns cachedEvent
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.failure(Exception("Ошибка обновления"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Content)
            assertEquals(
                "cached.jpg",
                (state as EventDetailUIState.Content)
                    .event.photos
                    .first()
                    .photo
            )
        }

    @Test
    fun loadEvent_whenFutureEventLoaded_thenDoesNotSaveToPastCache() =
        runTest {
            val futureEvent = createEvent(isCurrent = true)
            coEvery { swRepository.getEventFromCache(TEST_EVENT_ID) } returns null
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(futureEvent)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is EventDetailUIState.Content)
            coVerify(exactly = 0) { swRepository.saveEventFull(any()) }
        }

    @Test
    fun refresh_whenSuccess_thenUpdatesContent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(createEvent())
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.refresh()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { swRepository.getEvent(TEST_EVENT_ID) }
            assertTrue(viewModel.uiState.value is EventDetailUIState.Content)
        }

    @Test
    fun refresh_whenPastEventSuccess_thenUpdatesCache() =
        runTest {
            val pastEvent = createEvent(isCurrent = false)
            coEvery { swRepository.getEventFromCache(TEST_EVENT_ID) } returns null
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(pastEvent)
            coEvery { swRepository.saveEventFull(pastEvent) } returns Unit

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            coVerify(atLeast = 1) { swRepository.saveEventFull(pastEvent) }
        }

    @Test
    fun refresh_whenRetryStartsFromError_thenShowsInitialLoadingBeforeSuccess() =
        runTest {
            val retryGate = CompletableDeferred<Unit>()
            var callCount = 0
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } coAnswers {
                callCount += 1
                if (callCount == 1) {
                    Result.failure(Exception("Первичная ошибка"))
                } else {
                    retryGate.await()
                    Result.success(createEvent())
                }
            }

            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is EventDetailUIState.Error)

            viewModel.refresh()
            runCurrent()

            assertEquals(EventDetailUIState.InitialLoading, viewModel.uiState.value)

            retryGate.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is EventDetailUIState.Content)
        }

    @Test
    fun refresh_whenRetryStartsFromErrorAndFails_thenReturnsToError() =
        runTest {
            val retryError = "Повторная ошибка"
            val retryGate = CompletableDeferred<Unit>()
            var callCount = 0
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } coAnswers {
                callCount += 1
                if (callCount == 1) {
                    Result.failure(Exception("Первичная ошибка"))
                } else {
                    retryGate.await()
                    Result.failure(Exception(retryError))
                }
            }

            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is EventDetailUIState.Error)

            viewModel.refresh()
            runCurrent()

            assertEquals(EventDetailUIState.InitialLoading, viewModel.uiState.value)

            retryGate.complete(Unit)
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Error)
            assertEquals(retryError, (state as EventDetailUIState.Error).message)
        }

    @Test
    fun refresh_whenFailureHappensAfterContent_thenKeepsExistingContent() =
        runTest {
            val existingEvent = createEvent().copy(title = "Уже загруженное мероприятие")
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(existingEvent) andThen
                Result.failure(Exception("Ошибка обновления"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Content)
            assertEquals(
                "Уже загруженное мероприятие",
                (state as EventDetailUIState.Content).event.title
            )
        }

    // === Participant toggle tests ===

    @Test
    fun onParticipantToggle_whenSuccess_thenUpdatesTrainHere() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
                    createEvent(trainHere = false)
                )
            coEvery { swRepository.changeIsGoingToEvent(true, TEST_EVENT_ID) } returns
                Result.success(
                    Unit
                )
            coEvery { swRepository.getCurrentUserFlow() } returns flowOf(null)
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onParticipantToggle()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value as EventDetailUIState.Content
            assertTrue(state.event.trainHere == true)
        }

    @Test
    fun onParticipantToggle_whenError_thenRevertsToOriginal() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
                    createEvent(trainHere = false)
                )
            coEvery { swRepository.changeIsGoingToEvent(true, TEST_EVENT_ID) } returns
                Result.failure(
                    RuntimeException("Network error")
                )
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onParticipantToggle()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value as EventDetailUIState.Content
            assertTrue(state.event.trainHere == false)
        }

    // === Map actions tests ===

    @Test
    fun onOpenMapClick_whenContentLoaded_thenEmitsOpenMapEvent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(createEvent())
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.events.test {
                viewModel.onOpenMapClick()

                // Then
                assertEquals(EventDetailEvent.OpenMap, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun onRouteClick_whenContentLoaded_thenEmitsBuildRouteEvent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(createEvent())
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.events.test {
                viewModel.onRouteClick()

                // Then
                assertEquals(EventDetailEvent.BuildRoute, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // === Delete photo success test ===

    @Test
    fun onPhotoDeleteConfirm_whenSuccess_thenEmitsPhotoDeletedEvent() =
        runTest {
            // Given
            val photo = Photo(id = 1L, photo = "http://example.com/photo.jpg")
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
                    createEvent(photos = listOf(photo))
                )
            coEvery {
                swRepository.deleteEventPhoto(
                    TEST_EVENT_ID,
                    1L
                )
            } returns Result.success(Unit)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onPhotoDeleteClick(photo)
            advanceUntilIdle()

            // When
            viewModel.events.test {
                viewModel.onPhotoDeleteConfirm()
                advanceUntilIdle()

                // Then
                assertEquals(EventDetailEvent.PhotoDeleted(1L), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            coVerify { swRepository.deleteEventPhoto(TEST_EVENT_ID, 1L) }
        }

    // === Delete event success test ===

    @Test
    fun onDeleteConfirm_whenSuccess_thenEmitsEventDeletedEvent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(createEvent())
            coEvery { swRepository.deleteEvent(TEST_EVENT_ID) } returns Result.success(Unit)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onDeleteClick()
            advanceUntilIdle()

            // When
            viewModel.events.test {
                viewModel.onDeleteConfirm()
                advanceUntilIdle()

                // Then
                assertEquals(EventDetailEvent.EventDeleted(TEST_EVENT_ID), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            coVerify { swRepository.deleteEvent(TEST_EVENT_ID) }
        }

    // === Participants navigation test ===

    @Test
    fun onParticipantsCountClick_whenContentLoaded_thenEmitsNavigateToParticipantsEvent() =
        runTest {
            // Given
            val user1 = User(id = 1L, name = "User 1", image = null)
            val user2 = User(id = 2L, name = "User 2", image = null)
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
                    createEvent(trainingUsers = listOf(user1, user2))
                )
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.events.test {
                viewModel.onParticipantsCountClick()

                // Then
                val event = awaitItem()
                assertTrue(event is EventDetailEvent.NavigateToParticipants)
                val navEvent = event as EventDetailEvent.NavigateToParticipants
                assertEquals(TEST_EVENT_ID, navEvent.eventId)
                assertEquals(2, navEvent.users.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // === Calendar tests ===

    @Test
    fun onAddToCalendarClick_whenCurrentEvent_thenEmitsOpenCalendarEvent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
                    createEvent(isCurrent = true)
                )
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.events.test {
                viewModel.onAddToCalendarClick()

                // Then
                val event = awaitItem()
                assertTrue(event is EventDetailEvent.OpenCalendar)
                val calendarEvent = event as EventDetailEvent.OpenCalendar
                assertEquals(TEST_EVENT_TITLE, calendarEvent.title)
                assertEquals("2026-03-13 12:00:00", calendarEvent.beginDate)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun onAddToCalendarClick_whenPastEvent_thenDoesNotEmitEvent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
                    createEvent(isCurrent = false)
                )
            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.events.test {
                viewModel.onAddToCalendarClick()

                // Then
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // === Runtime exception handling tests ===

    @Test
    fun loadEvent_whenRuntimeException_thenShowsError() =
        runTest {
            // Given - мокируем выброс RuntimeException
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } throws RuntimeException("Unexpected error")

            // When
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Then - состояние должно быть Error
            assertTrue(viewModel.uiState.value is EventDetailUIState.Error)
        }

    @Test
    fun onDeleteConfirm_whenRuntimeException_thenHandlesError() =
        runTest {
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
    fun onPhotoDeleteConfirm_whenRuntimeException_thenHandlesError() =
        runTest {
            // Given - загружаем мероприятие с фото
            val photo = Photo(id = 1L, photo = "http://example.com/photo.jpg")
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.success(
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

    @Test
    fun onEventUpdated_whenContentState_thenUpdatesEventInState() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.success(createEvent())
            val viewModel = createViewModel()
            advanceUntilIdle()

            val updatedEvent = createEvent().copy(title = "Обновленное название")

            // When
            viewModel.onEventUpdated(updatedEvent)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Content)
            assertEquals("Обновленное название", (state as EventDetailUIState.Content).event.title)
        }

    @Test
    fun onEventUpdated_whenErrorState_thenUpdatesToContent() =
        runTest {
            // Given
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns Result.failure(Exception("Error"))
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is EventDetailUIState.Error)

            val updatedEvent = createEvent()

            // When
            viewModel.onEventUpdated(updatedEvent)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Content)
            assertEquals(TEST_EVENT_ID, (state as EventDetailUIState.Content).event.id)
        }

    @Test
    fun onEventUpdated_whenLoadingState_thenUpdatesToContent() =
        runTest {
            // Given - не мокаем swRepository.getEvent чтобы остаться в Loading
            val viewModel = createViewModel()
            // Не вызываем advanceUntilIdle() чтобы остаться в Loading

            val updatedEvent = createEvent()

            // When
            viewModel.onEventUpdated(updatedEvent)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Content)
            assertEquals(TEST_EVENT_ID, (state as EventDetailUIState.Content).event.id)
        }

    // === 404 NotFound handling tests ===

    @Test
    fun loadEvent_whenEventNotFound_thenDeletesLocallyNotifiesAndNavigatesBack() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(testDispatcher)

            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.failure(
                    NotFoundException.EventNotFound(TEST_EVENT_ID)
                )
            coEvery { deleteEventUseCase.invoke(TEST_EVENT_ID) } returns Result.success(Unit)

            val viewModel = createViewModel()

            viewModel.events.test {
                runCurrent()
                val event = awaitItem()
                assertTrue(event is EventDetailEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { deleteEventUseCase.invoke(TEST_EVENT_ID) }
            verify {
                userNotifier.handleError(
                    AppError.ResourceNotFound(
                        message = "Ресурс не найден на сервере",
                        resourceType = AppError.ResourceType.EVENT
                    )
                )
            }
        }

    @Test
    fun loadEvent_whenEventNotFound_thenUiStateShowsError() =
        runTest {
            coEvery { swRepository.getEvent(TEST_EVENT_ID) } returns
                Result.failure(
                    NotFoundException.EventNotFound(TEST_EVENT_ID)
                )
            coEvery { deleteEventUseCase.invoke(TEST_EVENT_ID) } returns Result.success(Unit)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is EventDetailUIState.Error)
        }

    private companion object {
        const val TEST_EVENT_ID = 100L
        const val TEST_COMMENT_ID = 555L
        const val TEST_EVENT_TITLE = "Тестовое мероприятие"
    }
}
