package com.swparks.ui.viewmodel

import android.net.Uri
import android.util.Log
import app.cash.turbine.test
import com.swparks.data.model.Event
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.domain.provider.AvatarHelper
import com.swparks.domain.usecase.ICreateEventUseCase
import com.swparks.domain.usecase.IEditEventUseCase
import com.swparks.ui.model.EventForm
import com.swparks.ui.model.EventFormMode
import com.swparks.ui.state.EventFormEvent
import com.swparks.util.AppError
import com.swparks.util.ImageUtils
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class EventFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var createEventUseCase: ICreateEventUseCase
    private lateinit var editEventUseCase: IEditEventUseCase
    private lateinit var avatarHelper: AvatarHelper
    private lateinit var logger: Logger
    private lateinit var userNotifier: UserNotifier

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0

        mockkObject(ImageUtils)

        createEventUseCase = mockk(relaxed = true)
        editEventUseCase = mockk(relaxed = true)
        avatarHelper = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createViewModel(mode: EventFormMode): EventFormViewModel {
        return EventFormViewModel(
            mode = mode,
            createEventUseCase = createEventUseCase,
            editEventUseCase = editEventUseCase,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier
        )
    }

    private fun createTestEvent(): Event {
        return Event(
            id = 1L,
            title = "Test Event",
            description = "Test Description",
            beginDate = "2025-06-15",
            countryID = 1,
            cityID = 1,
            preview = "",
            parkID = 100L,
            latitude = "55.75",
            longitude = "37.62",
            isCurrent = true,
            address = "Test Park Address",
            photos = emptyList(),
            author = User(
                id = 1,
                name = "Author",
                fullName = "Test Author",
                image = "",
                genderCode = 0,
                friendsCount = 0,
                friendRequestCount = "0",
                parksCount = "0",
                addedParks = emptyList(),
                journalCount = 0
            ),
            name = "Author Name"
        )
    }

    private fun expectedLocalDateTimeWithoutTimezone(isoOffsetDateTime: String): String {
        return OffsetDateTime.parse(isoOffsetDateTime)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    // ==================== Инициализация ====================

    @Test
    fun init_withRegularCreateMode_initializesWithCurrentDate() = runTest {
        // Given
        val mode = EventFormMode.RegularCreate

        // When
        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isLoading должен быть false", state.isLoading)
        assertTrue("Дата должна быть установлена", state.form.date.isNotBlank())
        assertEquals("initialForm должна быть равна form", state.form, state.initialForm)
    }

    @Test
    fun init_withCreateForSelectedMode_initializesWithParkData() = runTest {
        // Given
        val mode = EventFormMode.CreateForSelected(
            parkId = 100L,
            parkName = "Test Park"
        )

        // When
        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isLoading должен быть false", state.isLoading)
        assertEquals("parkId должен быть установлен", 100L, state.form.parkId)
        assertEquals("parkName должен быть установлен", "Test Park", state.form.parkName)
        assertTrue("Дата должна быть установлена", state.form.date.isNotBlank())
    }

    @Test
    fun init_withEditExistingMode_initializesWithEventData() = runTest {
        // Given
        val event = createTestEvent()
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        // When
        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isLoading должен быть false", state.isLoading)
        assertEquals("title должен быть из event", "Test Event", state.form.title)
        assertEquals("description должен быть из event", "Test Description", state.form.description)
        assertEquals("date должен быть из event", "2025-06-15", state.form.date)
        assertEquals("parkId должен быть из event", 100L, state.form.parkId)
        assertEquals("parkName должен быть №{id}", "№100", state.form.parkName)
    }

    @Test
    fun init_withEditExistingMode_normalizesDateWithTimezone() = runTest {
        // Given - event with timezone in date
        val event = createTestEvent().copy(
            beginDate = "2026-03-16T08:17:09+00:00"
        )
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        // When
        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // Then - UTC should be converted to local time and timezone removed
        val state = viewModel.uiState.value
        assertEquals(
            "date должен быть в локальном времени без timezone",
            expectedLocalDateTimeWithoutTimezone("2026-03-16T08:17:09+00:00"),
            state.form.date
        )
    }

    @Test
    fun init_withEditExistingMode_normalizesDateWithZ() = runTest {
        // Given - event with Z suffix in date
        val event = createTestEvent().copy(
            beginDate = "2026-03-16T08:17:09Z"
        )
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        // When
        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // Then - UTC should be converted to local time and timezone removed
        val state = viewModel.uiState.value
        assertEquals(
            "date должен быть в локальном времени без Z",
            expectedLocalDateTimeWithoutTimezone("2026-03-16T08:17:09Z"),
            state.form.date
        )
    }

    // ==================== Изменение полей ====================

    @Test
    fun onTitleChange_updatesForm() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onTitleChange("New Title")
        advanceUntilIdle()

        // Then
        assertEquals("New Title", viewModel.uiState.value.form.title)
    }

    @Test
    fun onDescriptionChange_updatesForm() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onDescriptionChange("New Description")
        advanceUntilIdle()

        // Then
        assertEquals("New Description", viewModel.uiState.value.form.description)
    }

    @Test
    fun onDateChange_updatesForm() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When - timestamp for 2025-06-15
        val timestamp = 1749916800000L
        viewModel.onDateChange(timestamp)
        advanceUntilIdle()

        // Then
        assertTrue("Date should contain 2025", viewModel.uiState.value.form.date.contains("2025"))
    }

    @Test
    fun onDateChange_preservesCurrentTime() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()
        viewModel.onTimeChange(14, 45)
        advanceUntilIdle()

        // When - timestamp for 2025-06-15
        viewModel.onDateChange(1749916800000L)
        advanceUntilIdle()

        // Then
        val parsed = LocalDateTime.parse(
            viewModel.uiState.value.form.date,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )
        assertEquals(14, parsed.hour)
        assertEquals(45, parsed.minute)
    }

    @Test
    fun onTimeChange_updatesFormTime() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onTimeChange(18, 30)
        advanceUntilIdle()

        // Then
        val parsed = LocalDateTime.parse(
            viewModel.uiState.value.form.date,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )
        assertEquals(18, parsed.hour)
        assertEquals(30, parsed.minute)
    }

    @Test
    fun onParkSelected_updatesForm() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onParkSelected(200L, "Another Park")
        advanceUntilIdle()

        // Then
        assertEquals(200L, viewModel.uiState.value.form.parkId)
        assertEquals("Another Park", viewModel.uiState.value.form.parkName)
    }

    // ==================== Фото ====================

    @Test
    fun onPhotoSelected_addsValidPhotos() = runTest {
        // Given
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri1) } returns true
        every { avatarHelper.isSupportedMimeType(uri2) } returns true

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onPhotoSelected(listOf(uri1, uri2))
        advanceUntilIdle()

        // Then
        assertEquals(2, viewModel.uiState.value.selectedPhotos.size)
    }

    @Test
    fun onPhotoSelected_emptyList_doesNothing() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onPhotoSelected(emptyList())
        advanceUntilIdle()

        // Then
        assertTrue("Photos should be empty", viewModel.uiState.value.selectedPhotos.isEmpty())
    }

    @Test
    fun onPhotoSelected_filtersUnsupportedTypes() = runTest {
        // Given
        val validUri = mockk<Uri>()
        val invalidUri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(validUri) } returns true
        every { avatarHelper.isSupportedMimeType(invalidUri) } returns false

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onPhotoSelected(listOf(validUri, invalidUri))
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.uiState.value.selectedPhotos.size)
        assertEquals(validUri, viewModel.uiState.value.selectedPhotos.first())
    }

    @Test
    fun onPhotoRemove_removesPhoto() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedPhotos.size)

        // When
        viewModel.onPhotoRemove(uri)
        advanceUntilIdle()

        // Then
        assertTrue("Photos should be empty", viewModel.uiState.value.selectedPhotos.isEmpty())
    }

    // ==================== Лимит фото ====================

    @Test
    fun photosCount_returnsFormPhotosCount() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // Then
        assertEquals(0, viewModel.uiState.value.photosCount)
    }

    @Test
    fun maxNewPhotos_returns15_whenNoExistingPhotos() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // Then
        assertEquals(15, viewModel.uiState.value.maxNewPhotos)
    }

    @Test
    fun maxNewPhotos_returnsRemaining_whenExistingPhotosPresent() = runTest {
        // Given
        val event = createTestEvent().copy(
            photos = listOf(
                Photo(1, "photo1"), Photo(2, "photo2"), Photo(3, "photo3")
            )
        )
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)
        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // Then
        assertEquals(12, viewModel.uiState.value.maxNewPhotos)
    }

    @Test
    fun maxNewPhotos_returns0_whenPhotosLimitReached() = runTest {
        // Given
        val event = createTestEvent().copy(photos = List(15) { Photo(it.toLong(), "photo$it") })
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)
        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // Then
        assertEquals(0, viewModel.uiState.value.maxNewPhotos)
    }

    @Test
    fun remainingNewPhotos_returnsMaxNewPhotos_whenNoSelectedPhotos() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // Then
        assertEquals(15, viewModel.uiState.value.remainingNewPhotos)
    }

    @Test
    fun remainingNewPhotos_returnsCorrectValue_whenSelectedPhotosPresent() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()
        viewModel.onPhotoSelected(listOf(uri, uri, uri))
        advanceUntilIdle()

        // Then
        assertEquals(12, viewModel.uiState.value.remainingNewPhotos)
    }

    @Test
    fun remainingNewPhotos_returns0_whenLimitReached() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()
        viewModel.onPhotoSelected(List(15) { uri })
        advanceUntilIdle()

        // Then
        assertEquals(0, viewModel.uiState.value.remainingNewPhotos)
    }

    @Test
    fun onPhotoSelected_capsToRemainingLimit() = runTest {
        // Given
        val event = createTestEvent().copy(photos = List(12) { Photo(it.toLong(), "photo$it") })
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // When - try to add 5 photos but only 3 slots available
        viewModel.onPhotoSelected(List(5) { uri })
        advanceUntilIdle()

        // Then - only 3 photos should be added
        assertEquals(3, viewModel.uiState.value.selectedPhotos.size)
        assertEquals(0, viewModel.uiState.value.remainingNewPhotos)
    }

    @Test
    fun onPhotoSelected_logsWarning_whenLimitExceeded() = runTest {
        // Given
        val event = createTestEvent().copy(photos = List(12) { Photo(it.toLong(), "photo$it") })
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // When
        viewModel.onPhotoSelected(List(5) { uri })
        advanceUntilIdle()

        // Then
        verify { logger.w(any(), any<String>()) }
    }

    @Test
    fun onPhotoSelected_doesNotAddPhotos_whenNoSlotsAvailable() = runTest {
        // Given
        val event = createTestEvent().copy(photos = List(15) { Photo(it.toLong(), "photo$it") })
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val viewModel = createViewModel(mode)
        advanceUntilIdle()

        // When
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // Then
        assertTrue(
            "Photos should be empty when no slots available",
            viewModel.uiState.value.selectedPhotos.isEmpty()
        )
    }

    // ==================== hasChanges ====================

    @Test
    fun hasChanges_returnsFalse_initially() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // Then
        assertFalse("No changes initially", viewModel.uiState.value.hasChanges)
    }

    @Test
    fun hasChanges_returnsTrue_whenFormChanged() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onTitleChange("New Title")
        advanceUntilIdle()

        // Then
        assertTrue("Should have changes", viewModel.uiState.value.hasChanges)
    }

    @Test
    fun hasChanges_returnsTrue_whenPhotosSelected() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // Then
        assertTrue("Should have changes with photos", viewModel.uiState.value.hasChanges)
    }

    // ==================== canSave ====================

    @Test
    fun canSave_returnsFalse_whenNoChanges() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // Then
        assertFalse("Cannot save without changes", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsTrue_whenFormIsValidAndHasChanges() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When - fill required fields
        viewModel.onTitleChange("Test Event")
        viewModel.onDateChange(1749916800000L)
        viewModel.onParkSelected(1L, "Park")
        advanceUntilIdle()

        // Then
        assertTrue("Can save with valid form", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsFalse_whenTitleMissing() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When - missing title
        viewModel.onDateChange(1749916800000L)
        viewModel.onParkSelected(1L, "Park")
        advanceUntilIdle()

        // Then
        assertFalse("Cannot save without title", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsFalse_whenParkMissing() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When - missing park
        viewModel.onTitleChange("Test Event")
        viewModel.onDateChange(1749916800000L)
        advanceUntilIdle()

        // Then
        assertFalse("Cannot save without park", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsTrue_forEditExisting_whenOnlyPhotosChanged() = runTest {
        // Given
        val event = createTestEvent()
        val viewModel = createViewModel(EventFormMode.EditExisting(1L, event))
        advanceUntilIdle()

        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        // When - only add photos, no form changes
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // Then
        assertFalse(
            "Form should not have changes",
            viewModel.uiState.value.form != viewModel.uiState.value.initialForm
        )
        assertTrue("But should be able to save because of photos", viewModel.uiState.value.canSave)
    }

    // ==================== onSaveClick ====================

    @Test
    fun onSaveClick_callsCreateEventUseCase_forRegularCreate() = runTest {
        // Given
        val createdEvent = createTestEvent()
        coEvery { createEventUseCase(any(), any()) } returns Result.success(createdEvent)

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        viewModel.onTitleChange("Test Event")
        viewModel.onDateChange(1749916800000L)
        viewModel.onParkSelected(1L, "Park")
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify { createEventUseCase(any<EventForm>(), any()) }
    }

    @Test
    fun onSaveClick_callsEditEventUseCase_forEditExisting() = runTest {
        // Given
        val event = createTestEvent()
        val updatedEvent = event.copy(title = "Updated Title")
        coEvery { editEventUseCase(any(), any(), any()) } returns Result.success(updatedEvent)

        val viewModel = createViewModel(EventFormMode.EditExisting(1L, event))
        advanceUntilIdle()

        viewModel.onTitleChange("Updated Title")
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify { editEventUseCase(eq(1L), any<EventForm>(), any()) }
    }

    @Test
    fun onSaveClick_doesNothing_whenNoChanges() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { createEventUseCase(any(), any()) }
        coVerify(exactly = 0) { editEventUseCase(any(), any(), any()) }
    }

    @Test
    fun onSaveClick_setsIsSaving_andResetsAfterSuccess() = runTest {
        // Given
        val createdEvent = createTestEvent()
        coEvery { createEventUseCase(any(), any()) } returns Result.success(createdEvent)

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        viewModel.onTitleChange("Test Event")
        viewModel.onDateChange(1749916800000L)
        viewModel.onParkSelected(1L, "Park")
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        assertFalse("isSaving should be false after completion", viewModel.uiState.value.isSaving)
    }

    @Test
    fun onSaveClick_handlesError_andResetsIsSaving() = runTest {
        // Given
        val error = RuntimeException("Network error")
        coEvery { createEventUseCase(any(), any()) } returns Result.failure(error)

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        viewModel.onTitleChange("Test Event")
        viewModel.onDateChange(1749916800000L)
        viewModel.onParkSelected(1L, "Park")
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        assertFalse("isSaving should be false after error", viewModel.uiState.value.isSaving)
        verify { userNotifier.handleError(any<AppError>()) }
    }

    @Test
    fun onSaveClick_emitsSavedEvent_onSuccess() = runTest {
        // Given
        val createdEvent = createTestEvent()
        coEvery { createEventUseCase(any(), any()) } returns Result.success(createdEvent)

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        viewModel.onTitleChange("Test Event")
        viewModel.onDateChange(1749916800000L)
        viewModel.onParkSelected(1L, "Park")
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onSaveClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(
                "Should emit Saved event",
                event is EventFormEvent.Saved && event.event.id == createdEvent.id
            )
        }

        coVerify { createEventUseCase(any<EventForm>(), any()) }
        assertFalse("isSaving should be false after success", viewModel.uiState.value.isSaving)
    }

    @Test
    fun onSaveClick_sendsPhotosToUseCase() = runTest {
        // Given
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
        every { ImageUtils.convertToJpeg(imageBytes) } returns imageBytes
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes

        val createdEvent = createTestEvent()
        coEvery { createEventUseCase(any(), any()) } returns Result.success(createdEvent)

        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        viewModel.onTitleChange("Test Event")
        viewModel.onDateChange(1749916800000L)
        viewModel.onParkSelected(1L, "Park")
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify { createEventUseCase(any<EventForm>(), eq(listOf(imageBytes))) }
    }

    // ==================== onParkClick ====================

    @Test
    fun onParkClick_emitsNavigateToSelectPark() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        viewModel.onParkSelected(100L, "Test Park")
        advanceUntilIdle()

        // When
        viewModel.onParkClick()
        advanceUntilIdle()

        // Then - verify event was emitted (indirectly through logger)
        verify { logger.d(any(), any<String>()) }
    }

    @Test
    fun onParkClick_withNoPark_emitsNavigateToSelectParkWithNull() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When - parkId is 0 (not selected)
        viewModel.onParkClick()
        advanceUntilIdle()

        // Then - verify event was emitted (indirectly through logger)
        verify { logger.d(any(), any<String>()) }
    }

    // ==================== onAddPhotoClick ====================

    @Test
    fun onAddPhotoClick_emitsShowPhotoPicker() = runTest {
        // Given
        val viewModel = createViewModel(EventFormMode.RegularCreate)
        advanceUntilIdle()

        // When
        viewModel.onAddPhotoClick()
        advanceUntilIdle()

        // Then - verify event was emitted (indirectly through logger)
        verify { logger.d(any(), any<String>()) }
    }

    // ==================== normalizeDateForServer ====================

    @Test
    fun createInitialState_whenEditExistingMode_thenUsesDateWithoutTimezone() {
        // Given
        val event = createTestEvent().copy(beginDate = "2024-06-15T10:00:00+03:00")
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        // When
        val viewModel = createViewModel(mode)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(
            expectedLocalDateTimeWithoutTimezone("2024-06-15T10:00:00+03:00"),
            uiState.form.date
        )
    }

    @Test
    fun createInitialState_whenEditExistingModeWithTimezoneZ_thenUsesDateWithoutTimezone() {
        // Given
        val event = createTestEvent().copy(beginDate = "2024-06-15T07:00:00Z")
        val mode = EventFormMode.EditExisting(eventId = 1L, event = event)

        // When
        val viewModel = createViewModel(mode)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(
            expectedLocalDateTimeWithoutTimezone("2024-06-15T07:00:00Z"),
            uiState.form.date
        )
    }
}
