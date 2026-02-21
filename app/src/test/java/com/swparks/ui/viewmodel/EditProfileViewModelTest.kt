package com.swparks.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.swparks.R
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.model.MainUserForm
import com.swparks.util.AppError
import com.swparks.util.ImageUtils
import com.swparks.util.Logger
import com.swparks.util.UriUtils
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
 * Unit тесты для EditProfileViewModel.
 *
 * Тестирует функциональность выбора аватара:
 * - onAvatarSelected обновляет state
 * - Отмена выбора не меняет state
 * - Валидация MIME-типа
 * - hasChanges учитывает аватар
 * - onSaveClick отправляет фото в repository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swRepository: SWRepository
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var context: Context
    private lateinit var logger: Logger
    private lateinit var userNotifier: UserNotifier

    private val testUser = createTestUser()
    private val testCountry = Country(id = "1", name = "Россия", cities = emptyList())
    private val testCity = City(id = "1", name = "Москва", lat = "55.75", lon = "37.62")
    private val userFlow = MutableStateFlow<User?>(testUser)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0

        mockkObject(UriUtils)
        mockkObject(ImageUtils)

        swRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)

        // Настройка базовых моков
        every { swRepository.getCurrentUserFlow() } returns userFlow
        every { countriesRepository.getCountriesFlow() } returns flowOf(listOf(testCountry))
        coEvery { countriesRepository.getCitiesByCountry(any()) } returns listOf(testCity)
        every { context.getString(R.string.avatar_error_unsupported_type) } returns
            "Неподдерживаемый формат изображения"
        every { context.getString(R.string.avatar_error_read_failed) } returns
            "Не удалось прочитать изображение"
        every { context.contentResolver } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Создает ViewModel для тестов.
     */
    private fun createViewModel(): EditProfileViewModel {
        return EditProfileViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            context = context,
            logger = logger,
            userNotifier = userNotifier
        )
    }

    @Test
    fun onAvatarSelected_updatesState() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { ImageUtils.isSupportedMimeType(context, uri) } returns true

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onAvatarSelected(uri)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("URI должен быть сохранен в state", uri, state.selectedAvatarUri)
        assertNull("Ошибка должна быть сброшена", state.avatarError)
    }

    @Test
    fun onAvatarSelected_nullUri_doesNothing() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        val initialState = viewModel.uiState.value

        // When
        viewModel.onAvatarSelected(null)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("State не должен измениться", initialState, state)
        assertNull("selectedAvatarUri должен остаться null", state.selectedAvatarUri)
    }

    @Test
    fun onAvatarSelected_unsupportedMimeType_showsError() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { ImageUtils.isSupportedMimeType(context, uri) } returns false

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onAvatarSelected(uri)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("URI не должен быть сохранен", state.selectedAvatarUri)
        assertNotNull("Должна быть ошибка", state.avatarError)
        assertEquals(
            "Текст ошибки должен соответствовать",
            "Неподдерживаемый формат изображения",
            state.avatarError
        )
    }

    @Test
    fun hasChanges_returnsTrue_whenAvatarSelected() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { ImageUtils.isSupportedMimeType(context, uri) } returns true

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Initially no changes
        assertFalse("Изначально изменений нет", viewModel.uiState.value.hasChanges)

        // When
        viewModel.onAvatarSelected(uri)
        advanceUntilIdle()

        // Then
        assertTrue(
            "После выбора аватара hasChanges должен быть true",
            viewModel.uiState.value.hasChanges
        )
    }

    @Test
    fun hasChanges_returnsFalse_whenFormUnchanged() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - форма загружена, но изменений нет
        assertFalse("Форма без изменений", viewModel.uiState.value.hasChanges)
    }

    @Test
    fun hasChanges_returnsTrue_whenFormChanged() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onLoginChange("Новое имя")
        advanceUntilIdle()

        // Then
        assertTrue(
            "После изменения формы hasChanges должен быть true",
            viewModel.uiState.value.hasChanges
        )
    }

    @Test
    fun onSaveClick_sendsImageToRepository() = runTest {
        // Given
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { ImageUtils.isSupportedMimeType(context, uri) } returns true
        every { UriUtils.uriToByteArray(context, uri) } returns Result.success(imageBytes)
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes
        coEvery {
            swRepository.editUser(
                any(),
                any<MainUserForm>(),
                any<ByteArray>()
            )
        } returns Result.success(
            testUser
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Выбираем аватар
        viewModel.onAvatarSelected(uri)
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify {
            swRepository.editUser(
                userId = testUser.id,
                form = any(),
                image = imageBytes
            )
        }
    }

    @Test
    fun onSaveClick_whenNoChanges_doesNotCallRepository() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - пытаемся сохранить без изменений
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { swRepository.editUser(any(), any<MainUserForm>(), any()) }
    }

    @Test
    fun onSaveClick_whenUriReadFails_showsError() = runTest {
        // Given
        val uri = mockk<Uri>()
        val exception = java.io.IOException("Read error")

        every { ImageUtils.isSupportedMimeType(context, uri) } returns true
        every { UriUtils.uriToByteArray(context, uri) } returns Result.failure(exception)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Выбираем аватар
        viewModel.onAvatarSelected(uri)
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull("Должна быть ошибка", state.avatarError)
        assertEquals(
            "Текст ошибки должен соответствовать",
            "Не удалось прочитать изображение",
            state.avatarError
        )
        assertFalse("isSaving должен быть false", state.isSaving)
        assertFalse("isUploadingAvatar должен быть false", state.isUploadingAvatar)

        verify {
            userNotifier.handleError(any<AppError>())
        }
    }

    @Test
    fun onSaveClick_whenNoUser_handlesError() = runTest {
        // Given
        userFlow.value = null
        every { swRepository.getCurrentUserFlow() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then
        verify {
            userNotifier.handleError(match<AppError> {
                it is AppError.Generic && it.message.contains("не авторизован")
            })
        }
    }

    @Test
    fun onSaveClick_setsUploadingFlags() = runTest {
        // Given
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3)

        every { ImageUtils.isSupportedMimeType(context, uri) } returns true
        every { UriUtils.uriToByteArray(context, uri) } returns Result.success(imageBytes)
        every { ImageUtils.compressIfNeeded(imageBytes) } returns imageBytes
        coEvery {
            swRepository.editUser(
                any(),
                any<MainUserForm>(),
                any<ByteArray>()
            )
        } returns Result.success(
            testUser
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAvatarSelected(uri)
        advanceUntilIdle()

        // When
        viewModel.onSaveClick()
        advanceUntilIdle()

        // Then - после завершения флаги должны быть false
        val state = viewModel.uiState.value
        assertFalse("isSaving должен быть false после завершения", state.isSaving)
        assertFalse("isUploadingAvatar должен быть false после завершения", state.isUploadingAvatar)
    }

    @Test
    fun resetChanges_clearsAvatarSelection() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { ImageUtils.isSupportedMimeType(context, uri) } returns true

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAvatarSelected(uri)
        advanceUntilIdle()
        assertNotNull("Аватар должен быть выбран", viewModel.uiState.value.selectedAvatarUri)

        // When
        viewModel.resetChanges()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("selectedAvatarUri должен быть null после сброса", state.selectedAvatarUri)
        assertNull("avatarError должен быть null после сброса", state.avatarError)
    }

    /**
     * Создает тестового пользователя.
     */
    private fun createTestUser(): User {
        return User(
            id = 1,
            name = "Test User",
            fullName = "Test User Full",
            email = "test@example.com",
            image = "https://example.com/avatar.jpg",
            genderCode = 0,
            countryID = 1,
            cityID = 1,
            friendsCount = 10,
            friendRequestCount = "5",
            parksCount = "3",
            addedParks = emptyList(),
            journalCount = 2
        )
    }
}
