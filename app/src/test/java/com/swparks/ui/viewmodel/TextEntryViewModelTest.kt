package com.swparks.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.swparks.R
import com.swparks.domain.usecase.ITextEntryUseCase
import com.swparks.ui.model.EditInfo
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.TextEntryEvent
import com.swparks.util.ErrorReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для TextEntryViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TextEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var textEntryUseCase: ITextEntryUseCase
    private lateinit var errorReporter: ErrorReporter
    private lateinit var context: Context
    private lateinit var viewModel: TextEntryViewModel

    private val testOwnerId = 123L
    private val testJournalId = 456L
    private val testEntryId = 789L
    private val testParkId = 101L
    private val testEventId = 202L
    private val testText = "Test comment"
    private val testOldEntry = "Old comment"

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any()) } returns 0
        mockkStatic(Context::class)
        textEntryUseCase = mockk()
        errorReporter = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Mock ConnectivityManager для проверки сети
        val connectivityManager: ConnectivityManager = mockk(relaxed = true)
        val networkCapabilities: NetworkCapabilities = mockk(relaxed = true)
        val network: android.net.Network = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        every { context.getString(R.string.text_entry_empty_error) } returns "Text cannot be empty"
        every { context.getString(R.string.error_network_io) } returns "Network error"
        every { context.getString(R.string.text_entry_error, any()) } returns "Error: error message"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onTextChanged_WhenNewForPark_ThenUpdatesStateAndEnablesButton() {
        // Given
        val mode = TextEntryMode.NewForPark(testParkId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // When
        viewModel.onTextChanged(testText)

        // Then
        val state = viewModel.uiState.value
        assertEquals(testText, state.text)
        assertTrue(state.isSendEnabled)
        assertNull(state.error)
    }

    @Test
    fun onTextChanged_WhenNewForParkWithEmptyText_ThenDisablesButton() {
        // Given
        val mode = TextEntryMode.NewForPark(testParkId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // When
        viewModel.onTextChanged("   ")

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSendEnabled)
    }

    @Test
    fun onTextChanged_WhenEditParkWithSameText_ThenDisablesButton() {
        // Given
        val editInfo = EditInfo(testParkId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditPark(editInfo)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // Then - проверяем, что текст предзаполнен с oldEntry при создании
        assertEquals(testOldEntry, viewModel.uiState.value.text)

        // When
        viewModel.onTextChanged(testOldEntry)

        // Then
        val state = viewModel.uiState.value
        assertEquals(testOldEntry, state.text)
        assertFalse(state.isSendEnabled)
    }

    @Test
    fun onTextChanged_WhenEditParkWithDifferentText_ThenEnablesButton() {
        // Given
        val editInfo = EditInfo(testParkId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditPark(editInfo)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // When
        viewModel.onTextChanged(testText)

        // Then
        val state = viewModel.uiState.value
        assertEquals(testText, state.text)
        assertTrue(state.isSendEnabled)
    }

    @Test
    fun onSend_WhenNewForParkAndSuccess_ThenEmitsSuccessEvent() = runTest {
        // Given
        val mode = TextEntryMode.NewForPark(testParkId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)
        coEvery { textEntryUseCase.addParkComment(testParkId, testText) } returns Result.success(
            Unit
        )

        // When
        viewModel.onSend()
        advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is TextEntryEvent.Success)
        coVerify(exactly = 1) { textEntryUseCase.addParkComment(testParkId, testText) }
    }

    @Test
    fun onSend_WhenNewForEventAndSuccess_ThenEmitsSuccessEvent() = runTest {
        // Given
        val mode = TextEntryMode.NewForEvent(testEventId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)
        coEvery { textEntryUseCase.addEventComment(testEventId, testText) } returns Result.success(
            Unit
        )

        // When
        viewModel.onSend()
        advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is TextEntryEvent.Success)
        coVerify(exactly = 1) { textEntryUseCase.addEventComment(testEventId, testText) }
    }

    @Test
    fun onSend_WhenNewForJournalAndSuccess_ThenEmitsSuccessEvent() = runTest {
        // Given
        val mode = TextEntryMode.NewForJournal(testOwnerId, testJournalId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)
        coEvery {
            textEntryUseCase.addJournalEntry(
                testOwnerId,
                testJournalId,
                testText
            )
        } returns Result.success(Unit)

        // When
        viewModel.onSend()
        advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is TextEntryEvent.Success)
        coVerify(exactly = 1) {
            textEntryUseCase.addJournalEntry(
                testOwnerId,
                testJournalId,
                testText
            )
        }
    }

    @Test
    fun onSend_WhenEditParkAndSuccess_ThenEmitsSuccessEvent() = runTest {
        // Given
        val editInfo = EditInfo(testParkId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditPark(editInfo)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)
        coEvery {
            textEntryUseCase.editParkComment(testParkId, testEntryId, testText)
        } returns Result.success(Unit)

        // When
        viewModel.onSend()
        advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is TextEntryEvent.Success)
        coVerify(exactly = 1) {
            textEntryUseCase.editParkComment(
                testParkId,
                testEntryId,
                testText
            )
        }
    }

    @Test
    fun onSend_WhenEditEventAndSuccess_ThenEmitsSuccessEvent() = runTest {
        // Given
        val editInfo = EditInfo(testEventId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditEvent(editInfo)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)
        coEvery {
            textEntryUseCase.editEventComment(testEventId, testEntryId, testText)
        } returns Result.success(Unit)

        // When
        viewModel.onSend()
        advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is TextEntryEvent.Success)
        coVerify(exactly = 1) {
            textEntryUseCase.editEventComment(
                testEventId,
                testEntryId,
                testText
            )
        }
    }

    @Test
    fun onSend_WhenEditJournalEntryAndSuccess_ThenEmitsSuccessEvent() = runTest {
        // Given
        val editInfo = EditInfo(testJournalId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditJournalEntry(testOwnerId, editInfo)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)
        coEvery {
            textEntryUseCase.editJournalEntry(testOwnerId, testJournalId, testEntryId, testText)
        } returns Result.success(Unit)

        // When
        viewModel.onSend()
        advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is TextEntryEvent.Success)
        coVerify(exactly = 1) {
            textEntryUseCase.editJournalEntry(
                testOwnerId,
                testJournalId,
                testEntryId,
                testText
            )
        }
    }

    @Test
    fun onSend_WhenEmptyText_ThenShowsError() {
        // Given
        val mode = TextEntryMode.NewForPark(testParkId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // When
        viewModel.onSend()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertEquals("Text cannot be empty", state.error)
        coVerify(exactly = 0) { textEntryUseCase.addParkComment(any(), any()) }
    }

    @Test
    fun onSend_WhenEditWithSameText_ThenShowsError() {
        // Given
        val editInfo = EditInfo(testParkId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditPark(editInfo)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testOldEntry)

        // When
        viewModel.onSend()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        coVerify(exactly = 0) { textEntryUseCase.editParkComment(any(), any(), any()) }
    }

    @Test
    fun onDismissError_WhenCalled_ThenClearsError() {
        // Given
        val mode = TextEntryMode.NewForPark(testParkId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged("")
        viewModel.onSend()

        // When
        viewModel.onDismissError()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    @Test
    fun init_WhenEditModePark_ThenTextPrepopulatedWithOldEntry() {
        // Given
        val editInfo = EditInfo(testParkId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditPark(editInfo)

        // When
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // Then
        assertEquals(mode, viewModel.uiState.value.mode)
        assertEquals(testOldEntry, viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isSendEnabled) // Кнопка отключена, так как текст равен oldEntry
    }

    @Test
    fun init_WhenEditModeEvent_ThenTextPrepopulatedWithOldEntry() {
        // Given
        val editInfo = EditInfo(testEventId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditEvent(editInfo)

        // When
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // Then
        assertEquals(mode, viewModel.uiState.value.mode)
        assertEquals(testOldEntry, viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isSendEnabled) // Кнопка отключена, так как текст равен oldEntry
    }

    @Test
    fun init_WhenEditModeJournalEntry_ThenTextPrepopulatedWithOldEntry() {
        // Given
        val editInfo = EditInfo(testJournalId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditJournalEntry(testOwnerId, editInfo)

        // When
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // Then
        assertEquals(mode, viewModel.uiState.value.mode)
        assertEquals(testOldEntry, viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isSendEnabled) // Кнопка отключена, так как текст равен oldEntry
    }

    @Test
    fun init_WhenNewMode_ThenTextIsEmpty() {
        // Given
        val mode = TextEntryMode.NewForPark(testParkId)

        // When
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // Then
        assertEquals(mode, viewModel.uiState.value.mode)
        assertEquals("", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isSendEnabled) // Кнопка отключена, так как текст пустой
    }

    @Test
    fun resetState_WhenCalledForNewMode_ThenResetsToInitialState() {
        // Given
        val mode = TextEntryMode.NewForPark(testParkId)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)

        // When
        viewModel.resetState()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mode, state.mode)
        assertEquals("", state.text)
        assertFalse(state.isLoading)
        assertFalse(state.isSendEnabled)
        assertNull(state.error)
    }

    @Test
    fun resetState_WhenCalledForEditMode_ThenResetsToOldEntry() {
        // Given
        val editInfo = EditInfo(testParkId, testEntryId, testOldEntry)
        val mode = TextEntryMode.EditPark(editInfo)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)

        // When
        viewModel.resetState()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mode, state.mode)
        assertEquals(testOldEntry, state.text) // Текст должен сброситься на oldEntry
        assertFalse(state.isLoading)
        assertFalse(state.isSendEnabled) // Кнопка отключена, так как текст равен oldEntry
        assertNull(state.error)
    }

    @Test
    fun onTextChanged_WhenMessage_ThenUpdatesStateAndEnablesButton() {
        // Given
        val userId = 123L
        val userName = "Test User"
        val mode = TextEntryMode.Message(userId, userName)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)

        // When
        viewModel.onTextChanged(testText)

        // Then
        val state = viewModel.uiState.value
        assertEquals(testText, state.text)
        assertTrue(state.isSendEnabled)
        assertNull(state.error)
    }

    @Test
    fun onSend_WhenMessageAndSuccess_ThenEmitsSuccessEvent() = runTest {
        // Given
        val userId = 123L
        val userName = "Test User"
        val mode = TextEntryMode.Message(userId, userName)
        viewModel = TextEntryViewModel(textEntryUseCase, errorReporter, mode, context)
        viewModel.onTextChanged(testText)
        coEvery { textEntryUseCase.sendMessageTo(userId, testText) } returns Result.success(Unit)

        // When
        viewModel.onSend()
        advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is TextEntryEvent.Success)
        coVerify(exactly = 1) { textEntryUseCase.sendMessageTo(userId, testText) }
    }
}
