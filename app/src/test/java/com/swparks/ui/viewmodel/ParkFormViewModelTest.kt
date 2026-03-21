package com.swparks.ui.viewmodel

import android.net.Uri
import android.util.Log
import app.cash.turbine.test
import com.swparks.data.model.Park
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.data.model.Photo
import com.swparks.domain.provider.AvatarHelper
import com.swparks.ui.model.ParkForm
import com.swparks.ui.model.ParkFormMode
import com.swparks.ui.state.ParkFormEvent
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

@OptIn(ExperimentalCoroutinesApi::class)
class ParkFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

        avatarHelper = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createViewModel(
        mode: ParkFormMode,
        swRepository: com.swparks.data.repository.SWRepository
    ): ParkFormViewModel {
        return ParkFormViewModel(
            mode = mode,
            swRepository = swRepository,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier
        )
    }

    private fun createTestPark(): Park {
        return Park(
            id = 1L,
            name = "Test Park",
            sizeID = ParkSize.MEDIUM.rawValue,
            typeID = ParkType.MODERN.rawValue,
            longitude = "37.62",
            latitude = "55.75",
            address = "Test Address 123",
            cityID = 1,
            countryID = 1,
            preview = "",
            photos = listOf(
                Photo(1, "photo1"),
                Photo(2, "photo2")
            )
        )
    }

    // ==================== Инициализация ====================

    @Test
    fun init_withCreateMode_setsEmptyForm() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "New Park Address",
            initialLatitude = "55.75",
            initialLongitude = "37.62",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)

        // When
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isLoading должен быть false", state.isLoading)
        assertEquals("New Park Address", state.form.address)
        assertEquals("55.75", state.form.latitude)
        assertEquals("37.62", state.form.longitude)
        assertEquals(1, state.form.cityId)
        assertEquals("form должна быть равна initialForm", state.form, state.initialForm)
    }

    @Test
    fun init_withCreateMode_hasDefaultTypeAndSize() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)

        // When
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(ParkType.SOVIET.rawValue, state.form.typeId)
        assertEquals(ParkSize.SMALL.rawValue, state.form.sizeId)
    }

    @Test
    fun init_withEditMode_loadsParkData() = runTest {
        // Given
        val park = createTestPark()
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)

        // When
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isLoading должен быть false", state.isLoading)
        assertEquals("Test Address 123", state.form.address)
        assertEquals("55.75", state.form.latitude)
        assertEquals("37.62", state.form.longitude)
        assertEquals(1, state.form.cityId)
        assertEquals(ParkType.MODERN.rawValue, state.form.typeId)
        assertEquals(ParkSize.MEDIUM.rawValue, state.form.sizeId)
        assertEquals(2, state.form.photosCount)
    }

    // ==================== Изменение полей ====================

    @Test
    fun onAddressChange_updatesForm() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Old Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When
        viewModel.onAddressChange("New Address")
        advanceUntilIdle()

        // Then
        assertEquals("New Address", viewModel.uiState.value.form.address)
    }

    @Test
    fun onTypeChange_updatesFormType() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When
        viewModel.onTypeChange(ParkType.LEGENDARY.rawValue)
        advanceUntilIdle()

        // Then
        assertEquals(ParkType.LEGENDARY.rawValue, viewModel.uiState.value.form.typeId)
    }

    @Test
    fun onSizeChange_updatesFormSize() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When
        viewModel.onSizeChange(ParkSize.LARGE.rawValue)
        advanceUntilIdle()

        // Then
        assertEquals(ParkSize.LARGE.rawValue, viewModel.uiState.value.form.sizeId)
    }

    // ==================== Фото ====================

    @Test
    fun onPhotoSelected_addsValidPhotos() = runTest {
        // Given
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri1) } returns true
        every { avatarHelper.isSupportedMimeType(uri2) } returns true

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
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
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
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

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
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

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
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
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        assertEquals(0, viewModel.uiState.value.photosCount)
    }

    @Test
    fun photosCount_returnsExistingPhotosCount_inEditMode() = runTest {
        // Given
        val park = createTestPark()
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        assertEquals(2, viewModel.uiState.value.photosCount)
    }

    @Test
    fun maxNewPhotos_returns15_whenNoExistingPhotos() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        assertEquals(15, viewModel.uiState.value.maxNewPhotos)
    }

    @Test
    fun maxNewPhotos_returnsRemaining_whenExistingPhotosPresent() = runTest {
        // Given
        val park = createTestPark().copy(photos = List(5) { Photo(it.toLong(), "photo$it") })
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        assertEquals(10, viewModel.uiState.value.maxNewPhotos)
    }

    @Test
    fun maxNewPhotos_returns0_whenPhotosLimitReached() = runTest {
        // Given
        val park = createTestPark().copy(photos = List(15) { Photo(it.toLong(), "photo$it") })
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        assertEquals(0, viewModel.uiState.value.maxNewPhotos)
    }

    @Test
    fun onPhotoSelected_capsToRemainingLimit() = runTest {
        // Given
        val park = createTestPark().copy(photos = List(12) { Photo(it.toLong(), "photo$it") })
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When - try to add 5 photos but only 3 slots available
        viewModel.onPhotoSelected(List(5) { uri })
        advanceUntilIdle()

        // Then - only 3 photos should be added
        assertEquals(3, viewModel.uiState.value.selectedPhotos.size)
        assertEquals(0, viewModel.uiState.value.remainingNewPhotos)
    }

    @Test
    fun onPhotoSelected_doesNotAddPhotos_whenNoSlotsAvailable() = runTest {
        // Given
        val park = createTestPark().copy(photos = List(15) { Photo(it.toLong(), "photo$it") })
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
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
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then
        assertFalse("No changes initially", viewModel.uiState.value.hasChanges)
    }

    @Test
    fun hasChanges_returnsTrue_whenFormChanged() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When
        viewModel.onAddressChange("New Address")
        advanceUntilIdle()

        // Then
        assertTrue("Should have changes", viewModel.uiState.value.hasChanges)
    }

    @Test
    fun hasChanges_returnsTrue_whenPhotosSelected() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // Then
        assertTrue("Should have changes with photos", viewModel.uiState.value.hasChanges)
    }

    // ==================== canSave ====================

    @Test
    fun canSave_returnsFalse_whenNoChanges_inCreateMode() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then - isReadyToCreate requires selectedPhotos.isNotEmpty(), so initially false
        assertFalse("Cannot save without photos", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsTrue_whenFormIsValidAndHasPhotos_inCreateMode() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When - add photos (isReadyToCreate requires selectedPhotos.isNotEmpty())
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // Then - form is valid (has address/lat/long/cityId) and has photos
        assertTrue("Can save with valid form and photos", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsFalse_forEditMode_whenNoChanges() = runTest {
        // Given
        val park = createTestPark()
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // Then - no changes, no photos added
        assertFalse("Cannot save without changes in edit mode", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsTrue_forEditMode_whenOnlyPhotosChanged() = runTest {
        // Given
        val park = createTestPark()
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When - only add photos, no form changes
        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // Then - can save because photos added
        assertTrue("Can save with photos even if form unchanged", viewModel.uiState.value.canSave)
    }

    @Test
    fun canSave_returnsTrue_forEditMode_whenFormChanged() = runTest {
        // Given
        val park = createTestPark()
        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When - change address
        viewModel.onAddressChange("Updated Address")
        advanceUntilIdle()

        // Then
        assertTrue("Can save when form changed", viewModel.uiState.value.canSave)
    }

    // ==================== onSaveClick ====================

    @Test
    fun onSaveClick_callsSavePark_forCreateMode() = runTest {
        // Given
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
        every { ImageUtils.convertToJpeg(imageBytes) } returns imageBytes
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes

        val createdPark = createTestPark()
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        coEvery { swRepository.savePark(any(), any(), any()) } returns Result.success(createdPark)

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify { swRepository.savePark(any(), any<ParkForm>(), any()) }
    }

    @Test
    fun onSaveClick_callsSavePark_forEditMode() = runTest {
        // Given
        val park = createTestPark()
        val updatedPark = park.copy(address = "Updated Address")
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        coEvery { swRepository.savePark(any(), any(), any()) } returns Result.success(updatedPark)

        val mode = ParkFormMode.Edit(parkId = 1L, park = park)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        viewModel.onAddressChange("Updated Address")
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify { swRepository.savePark(eq(1L), any<ParkForm>(), any()) }
    }

    @Test
    fun onSaveClick_doesNothing_whenCannotSave() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When - no photos, so cannot save
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { swRepository.savePark(any(), any(), any()) }
    }

    @Test
    fun onSaveClick_setsIsSaving_andResetsAfterSuccess() = runTest {
        // Given
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
        every { ImageUtils.convertToJpeg(imageBytes) } returns imageBytes
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes

        val createdPark = createTestPark()
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        coEvery { swRepository.savePark(any(), any(), any()) } returns Result.success(createdPark)

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        viewModel.onPhotoSelected(listOf(uri))
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
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
        every { ImageUtils.convertToJpeg(imageBytes) } returns imageBytes
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes

        val error = RuntimeException("Network error")
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        coEvery { swRepository.savePark(any(), any(), any()) } returns Result.failure(error)

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        viewModel.onPhotoSelected(listOf(uri))
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
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
        every { ImageUtils.convertToJpeg(imageBytes) } returns imageBytes
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes

        val createdPark = createTestPark()
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        coEvery { swRepository.savePark(any(), any(), any()) } returns Result.success(createdPark)

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onSaveClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(
                "Should emit Saved event",
                event is ParkFormEvent.Saved && event.park.id == createdPark.id
            )
        }

        coVerify { swRepository.savePark(any(), any(), any()) }
        assertFalse("isSaving should be false after success", viewModel.uiState.value.isSaving)
    }

    @Test
    fun onSaveClick_sendsPhotosToRepository() = runTest {
        // Given
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
        every { ImageUtils.convertToJpeg(imageBytes) } returns imageBytes
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes

        val createdPark = createTestPark()
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        coEvery { swRepository.savePark(any(), any(), any()) } returns Result.success(createdPark)

        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        viewModel.onPhotoSelected(listOf(uri))
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify { swRepository.savePark(any(), any(), eq(listOf(imageBytes))) }
    }

    // ==================== onAddPhotoClick ====================

    @Test
    fun onAddPhotoClick_emitsShowPhotoPicker() = runTest {
        // Given
        val mode = ParkFormMode.Create(
            initialAddress = "Address",
            initialLatitude = "55.0",
            initialLongitude = "37.0",
            initialCityId = 1
        )
        val swRepository = mockk<com.swparks.data.repository.SWRepository>(relaxed = true)
        val viewModel = createViewModel(mode, swRepository)
        advanceUntilIdle()

        // When & Then
        viewModel.events.test {
            viewModel.onAddPhotoClick()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue("Should emit ShowPhotoPicker", event is ParkFormEvent.ShowPhotoPicker)
        }
    }
}