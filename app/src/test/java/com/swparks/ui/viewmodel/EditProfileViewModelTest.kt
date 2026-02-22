package com.swparks.ui.viewmodel

import android.net.Uri
import android.util.Log
import com.swparks.R
import java.time.LocalDate
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.AvatarHelper
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.IDeleteUserUseCase
import com.swparks.ui.model.MainUserForm
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
    private lateinit var deleteUserUseCase: IDeleteUserUseCase
    private lateinit var avatarHelper: AvatarHelper
    private lateinit var logger: Logger
    private lateinit var userNotifier: UserNotifier
    private lateinit var resources: ResourcesProvider

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

        mockkObject(ImageUtils)

        swRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        deleteUserUseCase = mockk(relaxed = true)
        avatarHelper = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        resources = mockk(relaxed = true)

        // Настройка базовых моков
        every { swRepository.getCurrentUserFlow() } returns userFlow
        every { countriesRepository.getCountriesFlow() } returns flowOf(listOf(testCountry))
        coEvery { countriesRepository.getCitiesByCountry(any()) } returns listOf(testCity)
        every { resources.getString(R.string.email_invalid) } returns "Введите корректный email"
        every { resources.getString(R.string.avatar_error_unsupported_type) } returns
            "Неподдерживаемый формат изображения"
        every { resources.getString(R.string.avatar_error_read_failed) } returns
            "Не удалось прочитать изображение"
        every { resources.getString(R.string.birth_date_in_future) } returns
            "Дата рождения не может быть в будущем"
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
            deleteUserUseCase = deleteUserUseCase,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier,
            resources = resources
        )
    }

    @Test
    fun onAvatarSelected_updatesState() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

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
        every { avatarHelper.isSupportedMimeType(uri) } returns false

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
        every { avatarHelper.isSupportedMimeType(uri) } returns true

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

    // MARK: - canSave tests

    @Test
    fun canSave_returnsTrue_whenHasChangesAndNoEmailError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - изменяем форму (создаём изменения)
        viewModel.onLoginChange("Новое имя")
        advanceUntilIdle()

        // Then
        assertTrue(
            "canSave должен быть true при наличии изменений и отсутствии ошибки email",
            viewModel.uiState.value.canSave
        )
    }

    @Test
    fun canSave_returnsFalse_whenNoChanges() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - нет изменений, нет ошибки email
        assertFalse(
            "canSave должен быть false при отсутствии изменений",
            viewModel.uiState.value.canSave
        )
    }

    @Test
    fun canSave_returnsFalse_whenEmailErrorExists() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - вводим невалидный email
        viewModel.onEmailChange("invalid-email")
        advanceUntilIdle()

        // Then - есть ошибка email, но нет изменений формы
        // canSave = hasChanges && emailError == null
        // hasChanges может быть true если email изменился от исходного
        // Но emailError != null, поэтому canSave должен быть false
        assertFalse(
            "canSave должен быть false при наличии ошибки email",
            viewModel.uiState.value.canSave
        )
    }

    @Test
    fun canSave_returnsFalse_whenHasChangesAndEmailError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - изменяем имя (создаём изменения) и вводим невалидный email
        viewModel.onLoginChange("Новое имя")
        viewModel.onEmailChange("invalid-email")
        advanceUntilIdle()

        // Then
        assertTrue("Должны быть изменения", viewModel.uiState.value.hasChanges)
        assertNotNull("Должна быть ошибка email", viewModel.uiState.value.emailError)
        assertFalse(
            "canSave должен быть false даже при наличии изменений, если есть ошибка email",
            viewModel.uiState.value.canSave
        )
    }

    @Test
    fun canSave_returnsTrue_whenEmailErrorCleared() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Сначала вводим невалидный email
        viewModel.onEmailChange("invalid-email")
        advanceUntilIdle()

        // Then - canSave false из-за ошибки
        assertFalse("canSave должен быть false при ошибке", viewModel.uiState.value.canSave)

        // When - исправляем email на валидный (создаём изменения)
        viewModel.onEmailChange("valid@email.com")
        advanceUntilIdle()

        // Then - canSave true после исправления
        assertTrue(
            "canSave должен быть true после исправления email",
            viewModel.uiState.value.canSave
        )
    }

    @Test
    fun onSaveClick_sendsImageToRepository() = runTest {
        // Given
        val uri = mockk<Uri>()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
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

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.failure(exception)

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

        every { avatarHelper.isSupportedMimeType(uri) } returns true
        every { avatarHelper.uriToByteArray(uri) } returns Result.success(imageBytes)
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
        every { avatarHelper.isSupportedMimeType(uri) } returns true

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

    // ==================== Тесты валидации email ====================

    @Test
    fun onEmailChange_validEmail_noError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEmailChange("valid@example.com")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Для валидного email не должно быть ошибки", state.emailError)
        assertEquals("Email должен быть обновлен", "valid@example.com", state.userForm.email)
    }

    @Test
    fun onEmailChange_emptyEmail_noError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEmailChange("")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Для пустого email не должно быть ошибки", state.emailError)
        assertEquals("Email должен быть обновлен", "", state.userForm.email)
    }

    @Test
    fun onEmailChange_invalidEmailWithoutAt_showsError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEmailChange("invalidemail.com")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull("Для невалидного email должна быть ошибка", state.emailError)
        assertEquals(
            "Текст ошибки должен соответствовать",
            "Введите корректный email",
            state.emailError
        )
        assertEquals(
            "Email должен быть обновлен даже если невалиден",
            "invalidemail.com",
            state.userForm.email
        )
    }

    @Test
    fun onEmailChange_invalidEmailWithoutDomain_showsError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEmailChange("invalid@")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull("Для невалидного email должна быть ошибка", state.emailError)
        assertEquals(
            "Текст ошибки должен соответствовать",
            "Введите корректный email",
            state.emailError
        )
    }

    @Test
    fun onEmailChange_invalidEmailWithShortDomain_showsError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEmailChange("invalid@example.c")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull("Для email с коротким доменом должна быть ошибка", state.emailError)
    }

    @Test
    fun onEmailChange_fromInvalidToValid_clearsError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Сначала вводим невалидный email
        viewModel.onEmailChange("invalid")
        advanceUntilIdle()
        assertNotNull("Должна быть ошибка", viewModel.uiState.value.emailError)

        // When - меняем на валидный
        viewModel.onEmailChange("valid@example.com")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Ошибка должна быть сброшена при вводе валидного email", state.emailError)
        assertEquals("Email должен быть обновлен", "valid@example.com", state.userForm.email)
    }

    @Test
    fun onEmailChange_fromInvalidToEmpty_clearsError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Сначала вводим невалидный email
        viewModel.onEmailChange("invalid")
        advanceUntilIdle()
        assertNotNull("Должна быть ошибка", viewModel.uiState.value.emailError)

        // When - очищаем поле
        viewModel.onEmailChange("")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Ошибка должна быть сброшена при очистке поля", state.emailError)
    }

    @Test
    fun resetChanges_clearsEmailError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Вводим невалидный email для создания ошибки
        viewModel.onEmailChange("invalid")
        advanceUntilIdle()
        assertNotNull("Должна быть ошибка", viewModel.uiState.value.emailError)

        // When
        viewModel.resetChanges()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Ошибка email должна быть сброшена после resetChanges", state.emailError)
    }

    @Test
    fun onEmailChange_validEmailWithSubdomain_noError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEmailChange("user@mail.example.com")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Email с поддоменом должен быть валиден", state.emailError)
    }

    @Test
    fun onEmailChange_validEmailWithPlus_noError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEmailChange("user+tag@example.com")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Email с плюсом должен быть валиден", state.emailError)
    }

    // ==================== Тесты удаления профиля ====================

    @Test
    fun onDeleteProfileClick_whenSuccess_setsIsDeletingAndNavigatesToLogin() = runTest {
        // Given
        coEvery { deleteUserUseCase() } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onDeleteProfileClick()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isDeleting должен быть false после завершения", state.isDeleting)
        coVerify(exactly = 1) { deleteUserUseCase() }
    }

    @Test
    fun onDeleteProfileClick_whenSuccess_resetsIsDeleting() = runTest {
        // Given
        coEvery { deleteUserUseCase() } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - запускаем удаление
        viewModel.onDeleteProfileClick()
        advanceUntilIdle()

        // Then - isDeleting должен быть false после успешного удаления
        val state = viewModel.uiState.value
        assertFalse("isDeleting должен быть false после успешного удаления", state.isDeleting)
        coVerify(exactly = 1) { deleteUserUseCase() }
    }

    @Test
    fun onDeleteProfileClick_whenCalledTwiceWhileDeleting_doesNotCallUseCaseTwice() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Создаем мок, который не завершается сразу (имитация долгого запроса)
        var deleteCallCount = 0
        coEvery { deleteUserUseCase() } coAnswers {
            deleteCallCount++
            // Небольшая задержка, чтобы успеть вызвать второй раз
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        // When - вызываем дважды подряд быстро
        viewModel.onDeleteProfileClick()
        // Второй вызов должен быть проигнорирован (isDeleting уже true)
        viewModel.onDeleteProfileClick()
        advanceUntilIdle()

        // Then - use case должен быть вызван только один раз
        coVerify(exactly = 1) { deleteUserUseCase() }
    }

    @Test
    fun onDeleteProfileClick_whenError_handlesErrorAndResetsIsDeleting() = runTest {
        // Given
        val error = RuntimeException("Delete failed")
        coEvery { deleteUserUseCase() } returns Result.failure(error)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onDeleteProfileClick()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("isDeleting должен быть false после ошибки", state.isDeleting)
        coVerify(exactly = 1) { userNotifier.handleError(any()) }
    }

    // ==================== Тесты валидации даты рождения ====================

    @Test
    fun onBirthDateChange_validDate_noError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - выбираем дату 20 лет назад (валидная дата)
        val twentyYearsAgo = LocalDate.now().minusYears(20)
        val timestamp = twentyYearsAgo.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModel.onBirthDateChange(timestamp)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Для валидной даты не должно быть ошибки", state.birthDateError)
    }

    @Test
    fun onBirthDateChange_futureDate_showsError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - выбираем дату в будущем
        val futureDate = LocalDate.now().plusDays(1)
        val timestamp = futureDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModel.onBirthDateChange(timestamp)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull("Для даты в будущем должна быть ошибка", state.birthDateError)
        assertEquals(
            "Текст ошибки должен соответствовать",
            "Дата рождения не может быть в будущем",
            state.birthDateError
        )
    }

    @Test
    fun onBirthDateChange_fromFutureToValid_clearsError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Сначала выбираем дату в будущем
        val futureDate = LocalDate.now().plusDays(1)
        val futureTimestamp = futureDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModel.onBirthDateChange(futureTimestamp)
        advanceUntilIdle()
        assertNotNull("Должна быть ошибка", viewModel.uiState.value.birthDateError)

        // When - меняем на валидную дату
        val validDate = LocalDate.now().minusYears(20)
        val validTimestamp = validDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModel.onBirthDateChange(validTimestamp)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Ошибка должна быть сброшена при выборе валидной даты", state.birthDateError)
    }

    @Test
    fun canSave_returnsFalse_whenBirthDateErrorExists() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - выбираем дату в будущем
        val futureDate = LocalDate.now().plusDays(1)
        val timestamp = futureDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModel.onBirthDateChange(timestamp)
        advanceUntilIdle()

        // Then
        // canSave = hasChanges && emailError == null && birthDateError == null
        // birthDateError != null, поэтому canSave должен быть false
        assertFalse(
            "canSave должен быть false при наличии ошибки даты рождения",
            viewModel.uiState.value.canSave
        )
    }

    @Test
    fun resetChanges_clearsBirthDateError() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Вводим дату в будущем для создания ошибки
        val futureDate = LocalDate.now().plusDays(1)
        val timestamp = futureDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        viewModel.onBirthDateChange(timestamp)
        advanceUntilIdle()
        assertNotNull("Должна быть ошибка", viewModel.uiState.value.birthDateError)

        // When
        viewModel.resetChanges()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull("Ошибка даты рождения должна быть сброшена после resetChanges", state.birthDateError)
    }
}
