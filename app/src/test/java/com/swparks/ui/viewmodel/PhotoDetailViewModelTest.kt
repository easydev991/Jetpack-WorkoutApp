package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.PhotoDetailAction
import com.swparks.ui.state.PhotoDetailEvent
import com.swparks.ui.state.PhotoDetailUIState
import com.swparks.util.AppError
import com.swparks.util.Complaint
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var swRepository: SWRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var logger: Logger
    private lateinit var userNotifier: UserNotifier
    private lateinit var viewModel: PhotoDetailViewModel

    private val testPhotoId = 123L
    private val testEventId = 456L
    private val testEventTitle = "Test Event"
    private val testPhotoUrl = "https://example.com/photo.jpg"

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0

        savedStateHandle = SavedStateHandle()
        swRepository = mockk()
        userPreferencesRepository = mockk()
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)

        every { userPreferencesRepository.isAuthorized } returns flowOf(true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createViewModel(
        photoId: Long = testPhotoId,
        eventId: Long = testEventId,
        eventTitle: String = testEventTitle,
        isEventAuthor: Boolean = false,
        photoUrl: String = testPhotoUrl,
        isAuthorized: Boolean = true,
        deleteResult: Result<Unit> = Result.success(Unit)
    ) {
        savedStateHandle = SavedStateHandle().apply {
            this["photoId"] = photoId
            this["eventId"] = eventId
            this["eventTitle"] = eventTitle
            this["isEventAuthor"] = isEventAuthor
            this["photoUrl"] = photoUrl
        }
        every { userPreferencesRepository.isAuthorized } returns flowOf(isAuthorized)
        coEvery { swRepository.deleteEventPhoto(any(), any()) } returns deleteResult

        viewModel = PhotoDetailViewModel(
            savedStateHandle = savedStateHandle,
            swRepository = swRepository,
            userPreferencesRepository = userPreferencesRepository,
            logger = logger,
            userNotifier = userNotifier
        )
    }

    @Test
    fun init_whenCreated_thenLoadsPhotoData() {
        createViewModel(isEventAuthor = true)

        val state = viewModel.uiState.value
        assertTrue(state is PhotoDetailUIState.Content)
        val content = state as PhotoDetailUIState.Content
        assertEquals(testPhotoId, content.photo.id)
        assertEquals(testPhotoUrl, content.photo.photo)
        assertEquals(testEventTitle, content.eventTitle)
        assertTrue(content.isEventAuthor)
        assertFalse(content.isLoading)
    }

    @Test
    fun init_whenMissingParams_thenShowsError() {
        savedStateHandle = SavedStateHandle()

        viewModel = PhotoDetailViewModel(
            savedStateHandle = savedStateHandle,
            swRepository = swRepository,
            userPreferencesRepository = userPreferencesRepository,
            logger = logger,
            userNotifier = userNotifier
        )

        val state = viewModel.uiState.value
        assertTrue(state is PhotoDetailUIState.Error)
    }

    @Test
    fun onAction_Close_thenEmitsCloseScreen() = runTest {
        createViewModel()

        viewModel.onAction(PhotoDetailAction.Close)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.CloseScreen)
    }

    @Test
    fun onAction_DeleteClick_whenAuthor_thenEmitsShowDeleteConfirmDialog() = runTest {
        createViewModel(isEventAuthor = true)

        viewModel.onAction(PhotoDetailAction.DeleteClick)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.ShowDeleteConfirmDialog)
    }

    @Test
    fun onAction_DeleteClick_whenNotAuthor_thenDoesNothing() = runTest {
        createViewModel(isEventAuthor = false)

        viewModel.onAction(PhotoDetailAction.DeleteClick)
        advanceUntilIdle()

        verify { logger.w(any(), "Отклонено удаление фото: пользователь не автор мероприятия") }
    }

    @Test
    fun onAction_DeleteConfirm_whenAuthor_andSuccess_thenEmitsPhotoDeletedWithPhotoId() = runTest {
        createViewModel(isEventAuthor = true, deleteResult = Result.success(Unit))

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.PhotoDeleted)
        assertEquals(testPhotoId, (event as PhotoDetailEvent.PhotoDeleted).photoId)
        verify { logger.i(any(), "Фото id=$testPhotoId успешно удалено") }
    }

    @Test
    fun onAction_DeleteConfirm_whenAuthor_andSuccess_thenSetsLoadingTrue() = runTest {
        createViewModel(isEventAuthor = true, deleteResult = Result.success(Unit))

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)

        val loadingState = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertNotNull(loadingState)
        assertTrue(loadingState!!.isLoading)

        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.PhotoDeleted)
    }

    @Test
    fun onAction_DeleteConfirm_whenAuthor_andError_thenCallsUserNotifierHandleError() = runTest {
        val errorMessage = "Network error"
        createViewModel(
            isEventAuthor = true,
            deleteResult = Result.failure(Exception(errorMessage))
        )

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        val state = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertNotNull(state)
        assertFalse(state!!.isLoading)
        verify { userNotifier.handleError(any<AppError.Generic>()) }
    }

    @Test
    fun onAction_DeleteConfirm_whenNotAuthor_thenDoesNothing() = runTest {
        createViewModel(isEventAuthor = false)

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        verify { logger.w(any(), "Отклонено удаление фото: пользователь не автор мероприятия") }
    }

    @Test
    fun onAction_DeleteDismiss_thenLogs() {
        createViewModel(isEventAuthor = true)

        viewModel.onAction(PhotoDetailAction.DeleteDismiss)

        verify { logger.d(any(), "Отмена удаления фото") }
    }

    @Test
    fun onAction_Report_whenNotAuthor_thenEmitsSendPhotoComplaint() = runTest {
        createViewModel(isEventAuthor = false, isAuthorized = true)

        viewModel.onAction(PhotoDetailAction.Report)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.SendPhotoComplaint)
        val complaint = (event as PhotoDetailEvent.SendPhotoComplaint).complaint
        assertTrue(complaint is Complaint.EventPhoto)
        assertEquals(testEventTitle, complaint.eventTitle)
    }

    @Test
    fun onAction_Report_whenNotAuthorized_thenDoesNothing() = runTest {
        createViewModel(isEventAuthor = false, isAuthorized = false)

        viewModel.onAction(PhotoDetailAction.Report)
        advanceUntilIdle()

        verify { logger.w(any(), "Отклонена жалоба: пользователь не авторизован") }
    }

    @Test
    fun onAction_Report_whenAuthor_thenDoesNothing() = runTest {
        createViewModel(isEventAuthor = true, isAuthorized = true)

        viewModel.onAction(PhotoDetailAction.Report)
        advanceUntilIdle()

        verify { logger.w(any(), "Отклонена жалоба: автор не может жаловаться на свое фото") }
    }

    @Test
    fun isAuthorized_whenTrue_thenFlowReturnsTrue() = runTest {
        createViewModel(isAuthorized = true)
        advanceUntilIdle()

        assertTrue(viewModel.isAuthorized.value)
    }

    @Test
    fun isAuthorized_whenFalse_thenFlowReturnsFalse() = runTest {
        createViewModel(isAuthorized = false)
        advanceUntilIdle()

        assertFalse(viewModel.isAuthorized.value)
    }

    @Test
    fun init_whenErrorState_thenHasErrorMessage() {
        savedStateHandle = SavedStateHandle()

        viewModel = PhotoDetailViewModel(
            savedStateHandle = savedStateHandle,
            swRepository = swRepository,
            userPreferencesRepository = userPreferencesRepository,
            logger = logger,
            userNotifier = userNotifier
        )

        val state = viewModel.uiState.value
        assertTrue(state is PhotoDetailUIState.Error)
        assertNotNull((state as PhotoDetailUIState.Error).message)
    }

    @Test
    fun onAction_DeleteConfirm_whenError_thenResetsLoadingOnRetry() = runTest {
        val errorMessage = "Network error"
        createViewModel(
            isEventAuthor = true,
            deleteResult = Result.failure(Exception(errorMessage))
        )

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        var state = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertFalse(state!!.isLoading)

        coEvery { swRepository.deleteEventPhoto(any(), any()) } returns Result.success(Unit)
        viewModel.onAction(PhotoDetailAction.DeleteConfirm)

        state = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertTrue(state!!.isLoading)
    }
}
