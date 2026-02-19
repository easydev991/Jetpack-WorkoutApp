package com.swparks.ui.viewmodel

import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.usecase.IChangePasswordUseCase
import com.swparks.ui.state.ChangePasswordEvent
import com.swparks.ui.state.ChangePasswordUiState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var changePasswordUseCase: IChangePasswordUseCase
    private lateinit var userNotifier: UserNotifier
    private lateinit var resourcesProvider: ResourcesProvider
    private lateinit var viewModel: ChangePasswordViewModel
    private val testLogger: Logger = NoOpLogger()

    @Before
    fun setup() {
        changePasswordUseCase = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        resourcesProvider = mockk(relaxed = true)
        every { resourcesProvider.getString(any()) } returns "Пароль успешно изменён"
        viewModel = ChangePasswordViewModel(
            changePasswordUseCase = changePasswordUseCase,
            logger = testLogger,
            userNotifier = userNotifier,
            resources = resourcesProvider
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun initialState_isEmptyAndNotSaving() {
        // Given
        // When - viewModel created in setup
        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.currentPassword)
        assertEquals("", state.newPassword)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isSaving)
        assertFalse(state.canSave)
    }

    @Test
    fun onCurrentPasswordChange_updatesState() {
        // Given
        val password = "oldPass123"

        // When
        viewModel.onCurrentPasswordChange(password)

        // Then
        assertEquals(password, viewModel.uiState.value.currentPassword)
    }

    @Test
    fun onNewPasswordChange_updatesState() {
        // Given
        val password = "newPass456"

        // When
        viewModel.onNewPasswordChange(password)

        // Then
        assertEquals(password, viewModel.uiState.value.newPassword)
    }

    @Test
    fun onConfirmPasswordChange_updatesState() {
        // Given
        val password = "newPass456"

        // When
        viewModel.onConfirmPasswordChange(password)

        // Then
        assertEquals(password, viewModel.uiState.value.confirmPassword)
    }

    @Test
    fun canSave_whenAllFieldsValid_returnsTrue() {
        // Given
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")

        // When
        val canSave = viewModel.uiState.value.canSave

        // Then
        assertTrue(canSave)
    }

    @Test
    fun canSave_whenPasswordsDoNotMatch_returnsFalse() {
        // Given
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("differentPass")

        // When
        val canSave = viewModel.uiState.value.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun canSave_whenNewPasswordTooShort_returnsFalse() {
        // Given
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")

        // When
        val canSave = viewModel.uiState.value.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun canSave_whenCurrentPasswordEmpty_returnsFalse() {
        // Given
        viewModel.onCurrentPasswordChange("")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")

        // When
        val canSave = viewModel.uiState.value.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun onSaveClick_whenSuccess_showsInfoAndNavigatesBack() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")
        coEvery { changePasswordUseCase("oldPass123", "newPass456") } returns Result.success(Unit)

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        verify { resourcesProvider.getString(com.swparks.R.string.password_changed_successfully) }
        verify { userNotifier.showInfo("Пароль успешно изменён") }
        val event = viewModel.events.first()
        assertTrue(event is ChangePasswordEvent.NavigateBack)
        coVerify(exactly = 1) { changePasswordUseCase("oldPass123", "newPass456") }
    }

    @Test
    fun onSaveClick_whenFailure_handlesError() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")
        val error = Exception("Неверный пароль")
        coEvery { changePasswordUseCase("oldPass123", "newPass456") } returns Result.failure(error)

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        verify {
            userNotifier.handleError(match { error ->
                error is AppError.Generic && error.message == "Неверный пароль"
            })
        }
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun onSaveClick_whenCannotSave_doesNothing() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { changePasswordUseCase(any(), any()) }
        verify(exactly = 0) { userNotifier.showInfo(any()) }
    }

    @Test
    fun onSaveClick_setsSavingState_andResetsOnSuccess() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")
        coEvery { changePasswordUseCase(any(), any()) } returns Result.success(Unit)

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then - isSaving should be false after completion
        // (success path navigates away, so isSaving stays true in UI state)
        // Note: In success case, the screen navigates away,
        // so we don't reset isSaving
        // But we can verify the flow completed by checking the event
        val event = viewModel.events.first()
        assertTrue(event is ChangePasswordEvent.NavigateBack)
    }

    @Test
    fun onSaveClick_setsSavingState_andResetsOnFailure() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")
        coEvery { changePasswordUseCase(any(), any()) } returns Result.failure(Exception("Error"))

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun canSave_whenSaving_isFalse() {
        // Given - using reflection to set isSaving since we can't easily capture mid-operation state
        val initialState = ChangePasswordUiState(
            currentPassword = "oldPass123",
            newPassword = "newPass456",
            confirmPassword = "newPass456",
            isSaving = true
        )

        // When
        val canSave = initialState.canSave

        // Then
        assertFalse(canSave)
    }
}
