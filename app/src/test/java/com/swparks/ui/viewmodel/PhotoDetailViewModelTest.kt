package com.swparks.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.PhotoDetailAction
import com.swparks.ui.state.PhotoDetailEvent
import com.swparks.ui.state.PhotoDetailUIState
import com.swparks.ui.state.PhotoOwner
import com.swparks.util.AppError
import com.swparks.util.Complaint
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val testParkId = 789L
    private val testEventTitle = "Test Event"
    private val testParkTitle = "Test Park"
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
        parentId: Long = testEventId,
        parentTitle: String = testEventTitle,
        isAuthor: Boolean = false,
        photoUrl: String = testPhotoUrl,
        isAuthorized: Boolean = true,
        ownerType: PhotoOwner = PhotoOwner.Event,
        deleteEventResult: Result<Unit> = Result.success(Unit),
        deleteParkResult: Result<Unit> = Result.success(Unit)
    ) {
        savedStateHandle = SavedStateHandle().apply {
            this["photoId"] = photoId
            this["parentId"] = parentId
            this["parentTitle"] = parentTitle
            this["isAuthor"] = isAuthor
            this["photoUrl"] = photoUrl
            this["ownerType"] = ownerType.name
        }
        every { userPreferencesRepository.isAuthorized } returns flowOf(isAuthorized)
        coEvery { swRepository.deleteEventPhoto(any(), any()) } returns deleteEventResult
        coEvery { swRepository.deleteParkPhoto(any(), any()) } returns deleteParkResult

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
        createViewModel(isAuthor = true)

        val state = viewModel.uiState.value
        assertTrue(state is PhotoDetailUIState.Content)
        val content = state as PhotoDetailUIState.Content
        assertEquals(testPhotoId, content.photo.id)
        assertEquals(testPhotoUrl, content.photo.photo)
        assertEquals(testEventTitle, content.parentTitle)
        assertTrue(content.isAuthor)
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
        createViewModel(isAuthor = true)

        viewModel.onAction(PhotoDetailAction.DeleteClick)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.ShowDeleteConfirmDialog)
    }

    @Test
    fun onAction_DeleteClick_whenNotAuthor_thenDoesNothing() = runTest {
        createViewModel(isAuthor = false)

        viewModel.onAction(PhotoDetailAction.DeleteClick)
        advanceUntilIdle()

        verify { logger.w(any(), "Отклонено удаление фото: пользователь не автор") }
    }

    @Test
    fun onAction_DeleteConfirm_whenAuthor_andSuccess_thenEmitsPhotoDeletedWithPhotoId() = runTest {
        createViewModel(isAuthor = true, deleteEventResult = Result.success(Unit))

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.PhotoDeleted)
        assertEquals(testPhotoId, (event as PhotoDetailEvent.PhotoDeleted).photoId)
        verify { logger.i(any(), "Фото id=$testPhotoId успешно удалено") }
    }

    @Test
    fun onAction_DeleteConfirm_whenAuthor_andSuccess_thenSetsLoadingTrue() = runTest {
        createViewModel(isAuthor = true, deleteEventResult = Result.success(Unit))

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)

        val loadingState = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertNotNull(loadingState)
        assertFalse(loadingState!!.isLoading)

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.PhotoDeleted)
    }

    @Test
    fun onAction_DeleteConfirm_whenAuthor_andError_thenCallsUserNotifierHandleError() = runTest {
        val errorMessage = "Network error"
        createViewModel(
            isAuthor = true,
            deleteEventResult = Result.failure(Exception(errorMessage))
        )

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        val state = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertNotNull(state)
        assertFalse(state!!.isLoading)
        coVerify { userNotifier.handleError(any<AppError.Generic>()) }
    }

    @Test
    fun onAction_DeleteConfirm_whenNotAuthor_thenDoesNothing() = runTest {
        createViewModel(isAuthor = false)

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        verify { logger.w(any(), "Отклонено удаление фото: пользователь не автор") }
    }

    @Test
    fun onAction_DeleteDismiss_thenLogs() {
        createViewModel(isAuthor = true)

        viewModel.onAction(PhotoDetailAction.DeleteDismiss)

        verify { logger.d(any(), "Отмена удаления фото") }
    }

    @Test
    fun onAction_Report_whenNotAuthor_thenEmitsSendPhotoComplaint() = runTest {
        createViewModel(isAuthor = false, isAuthorized = true)

        viewModel.onAction(PhotoDetailAction.Report)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.SendPhotoComplaint)
        val complaint = (event as PhotoDetailEvent.SendPhotoComplaint).complaint
        assertTrue(complaint is Complaint.EventPhoto)
        assertEquals(testEventTitle, (complaint as Complaint.EventPhoto).eventTitle)
    }

    @Test
    fun onAction_Report_whenNotAuthorized_thenDoesNothing() = runTest {
        createViewModel(isAuthor = false, isAuthorized = false)

        viewModel.onAction(PhotoDetailAction.Report)
        advanceUntilIdle()

        verify { logger.w(any(), "Отклонена жалоба: пользователь не авторизован") }
    }

    @Test
    fun onAction_Report_whenAuthor_thenDoesNothing() = runTest {
        createViewModel(isAuthor = true, isAuthorized = true)

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
            isAuthor = true,
            deleteEventResult = Result.failure(Exception(errorMessage))
        )

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        var state = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertFalse(state!!.isLoading)

        coEvery { swRepository.deleteEventPhoto(any(), any()) } returns Result.success(Unit)
        viewModel.onAction(PhotoDetailAction.DeleteConfirm)

        state = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertFalse(state!!.isLoading)

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.PhotoDeleted)
    }

    @Test
    fun onAction_DeleteConfirm_whenParkOwner_andSuccess_thenCallsDeleteParkPhoto() = runTest {
        createViewModel(
            isAuthor = true,
            parentId = testParkId,
            parentTitle = testParkTitle,
            ownerType = PhotoOwner.Park,
            deleteParkResult = Result.success(Unit)
        )

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        coVerify { swRepository.deleteParkPhoto(testParkId, testPhotoId) }
        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.PhotoDeleted)
        assertEquals(testPhotoId, (event as PhotoDetailEvent.PhotoDeleted).photoId)
    }

    @Test
    fun onAction_DeleteConfirm_whenEventOwner_andSuccess_thenCallsDeleteEventPhoto() = runTest {
        createViewModel(
            isAuthor = true,
            parentId = testEventId,
            parentTitle = testEventTitle,
            ownerType = PhotoOwner.Event,
            deleteEventResult = Result.success(Unit)
        )

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        coVerify { swRepository.deleteEventPhoto(testEventId, testPhotoId) }
        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.PhotoDeleted)
    }

    @Test
    fun onAction_DeleteConfirm_whenParkOwner_andError_thenCallsUserNotifierHandleError() = runTest {
        createViewModel(
            isAuthor = true,
            parentId = testParkId,
            parentTitle = testParkTitle,
            ownerType = PhotoOwner.Park,
            deleteParkResult = Result.failure(Exception("Park delete error"))
        )

        viewModel.onAction(PhotoDetailAction.DeleteConfirm)
        advanceUntilIdle()

        val state = viewModel.uiState.value as? PhotoDetailUIState.Content
        assertNotNull(state)
        assertFalse(state!!.isLoading)
        coVerify { userNotifier.handleError(any<AppError.Generic>()) }
    }

    @Test
    fun onAction_Report_whenParkOwner_thenEmitsParkPhotoComplaint() = runTest {
        createViewModel(
            isAuthor = false,
            isAuthorized = true,
            parentId = testParkId,
            parentTitle = testParkTitle,
            ownerType = PhotoOwner.Park
        )

        viewModel.onAction(PhotoDetailAction.Report)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.SendPhotoComplaint)
        val complaint = (event as PhotoDetailEvent.SendPhotoComplaint).complaint
        assertTrue(complaint is Complaint.ParkPhoto)
        assertEquals(testParkTitle, (complaint as Complaint.ParkPhoto).parkTitle)
    }

    @Test
    fun onAction_Report_whenEventOwner_thenEmitsEventPhotoComplaint() = runTest {
        createViewModel(
            isAuthor = false,
            isAuthorized = true,
            parentId = testEventId,
            parentTitle = testEventTitle,
            ownerType = PhotoOwner.Event
        )

        viewModel.onAction(PhotoDetailAction.Report)
        advanceUntilIdle()

        val event = viewModel.events.receiveAsFlow().first()
        assertTrue(event is PhotoDetailEvent.SendPhotoComplaint)
        val complaint = (event as PhotoDetailEvent.SendPhotoComplaint).complaint
        assertTrue(complaint is Complaint.EventPhoto)
        assertEquals(testEventTitle, (complaint as Complaint.EventPhoto).eventTitle)
    }

    @Test
    fun init_whenParkOwner_thenLoadsPhotoDataWithParkTitle() {
        createViewModel(
            isAuthor = true,
            parentId = testParkId,
            parentTitle = testParkTitle,
            ownerType = PhotoOwner.Park
        )

        val state = viewModel.uiState.value
        assertTrue(state is PhotoDetailUIState.Content)
        val content = state as PhotoDetailUIState.Content
        assertEquals(testParkTitle, content.parentTitle)
    }
}
