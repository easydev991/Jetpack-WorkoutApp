package com.swparks.ui.viewmodel

import android.net.Uri
import com.swparks.R
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.AvatarHelper
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.IDeleteUserUseCase
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Тесты для методов выбора страны и города в EditProfileViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelSelectionTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var swRepository: SWRepository
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var deleteUserUseCase: IDeleteUserUseCase
    private lateinit var avatarHelper: AvatarHelper
    private lateinit var logger: Logger
    private lateinit var userNotifier: UserNotifier
    private lateinit var resources: ResourcesProvider

    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val countriesFlow = MutableStateFlow<List<Country>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        swRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        deleteUserUseCase = mockk(relaxed = true)
        avatarHelper = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        resources = mockk(relaxed = true)

        every { swRepository.getCurrentUserFlow() } returns currentUserFlow
        every { countriesRepository.getCountriesFlow() } returns countriesFlow
        every { resources.getString(R.string.email_invalid) } returns "Enter a valid email"
        every { resources.getString(R.string.avatar_error_unsupported_type) } returns "Unsupported image format"
        every { resources.getString(R.string.avatar_error_read_failed) } returns "Failed to read image"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onCountrySelected_updatesCountry() = runTest {
        // Arrange
        val countries = makeTestCountries()
        val user = makeTestUser()

        // Устанавливаем данные ДО создания ViewModel
        currentUserFlow.value = user
        countriesFlow.value = countries

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onCountrySelected("Россия")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.first()
        Assert.assertEquals("Россия", state.selectedCountry?.name)
    }

    @Test
    fun onCountrySelected_keepsCity_ifCityInNewCountry() = runTest {
        // Arrange
        val countries = makeTestCountries()
        val user = makeTestUser()

        currentUserFlow.value = user
        countriesFlow.value = countries

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Сначала выбираем Россию и Москву
        viewModel.onCountrySelected("Россия")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onCitySelected("Москва")
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - выбираем ту же страну
        viewModel.onCountrySelected("Россия")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - город должен сохраниться
        val state = viewModel.uiState.first()
        Assert.assertEquals("Москва", state.selectedCity?.name)
    }

    @Test
    fun onCountrySelected_selectsFirstCity_whenCityNotInNewCountry() = runTest {
        // Arrange
        val countries = makeTestCountries()
        val user = makeTestUser()

        currentUserFlow.value = user
        countriesFlow.value = countries

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Сначала выбираем Россию и Москву
        viewModel.onCountrySelected("Россия")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onCitySelected("Москва")
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - выбираем США (где нет Москвы)
        viewModel.onCountrySelected("США")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - должен выбраться первый город из США (так как сервер требует city_id)
        val state = viewModel.uiState.first()
        Assert.assertEquals("США", state.selectedCountry?.name)
        Assert.assertEquals("Нью-Йорк", state.selectedCity?.name)
    }

    @Test
    fun onCitySelected_updatesCity() = runTest {
        // Arrange
        val countries = makeTestCountries()
        val user = makeTestUser()

        currentUserFlow.value = user
        countriesFlow.value = countries

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Сначала выбираем Россию
        viewModel.onCountrySelected("Россия")
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onCitySelected("Москва")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.first()
        Assert.assertEquals("Москва", state.selectedCity?.name)
    }

    @Test
    fun onCitySelected_updatesCountry_ifCityFromDifferentCountry() = runTest {
        // Arrange
        val countries = makeTestCountries()
        val user = makeTestUser()

        currentUserFlow.value = user
        countriesFlow.value = countries

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Сначала выбираем Россию
        viewModel.onCountrySelected("Россия")
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - выбираем город из США
        viewModel.onCitySelected("Нью-Йорк")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - страна должна обновиться на США
        val state = viewModel.uiState.first()
        Assert.assertEquals("Нью-Йорк", state.selectedCity?.name)
        Assert.assertEquals("США", state.selectedCountry?.name)
    }

    // MARK: - Avatar tests

    @Test
    fun onAvatarSelected_nullUri_doesNothing() = runTest {
        // Arrange
        val countries = makeTestCountries()
        val user = makeTestUser()

        currentUserFlow.value = user
        countriesFlow.value = countries

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - передаем null (пользователь отменил выбор)
        viewModel.onAvatarSelected(null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - состояние не изменилось
        val state = viewModel.uiState.first()
        Assert.assertNull(state.selectedAvatarUri)
        Assert.assertNull(state.avatarError)
    }

    @Test
    fun hasChanges_returnsTrue_whenAvatarSelected() = runTest {
        // Arrange
        val countries = makeTestCountries()
        val user = makeTestUser()

        currentUserFlow.value = user
        countriesFlow.value = countries

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Получаем начальное состояние (hasChanges должен быть false)
        val initialState = viewModel.uiState.first()
        Assert.assertFalse("Initial hasChanges should be false", initialState.hasChanges)

        // Act - выбираем аватар
        val uri = mockk<Uri>()
        every { avatarHelper.isSupportedMimeType(uri) } returns true

        viewModel.onAvatarSelected(uri)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - hasChanges должен быть true
        val state = viewModel.uiState.first()
        Assert.assertTrue("hasChanges should be true after avatar selection", state.hasChanges)
        Assert.assertEquals(uri, state.selectedAvatarUri)
    }

    // MARK: - Helper methods

    private fun createViewModel(): EditProfileViewModel =
        EditProfileViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            deleteUserUseCase = deleteUserUseCase,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier,
            resources = resources
        )

    private fun makeTestUser(): User = User(
        id = 1,
        name = "Test User",
        image = null,
        fullName = "Test User Full",
        email = "test@example.com",
        genderCode = 1,
        countryID = null,
        cityID = null
    )

    private fun makeTestCountries(): List<Country> = listOf(
        Country(
            id = "1",
            name = "Россия",
            cities = listOf(
                City(id = "1", name = "Москва", lat = "55.75", lon = "37.61"),
                City(id = "2", name = "Санкт-Петербург", lat = "59.93", lon = "30.33")
            )
        ),
        Country(
            id = "2",
            name = "США",
            cities = listOf(
                City(id = "3", name = "Нью-Йорк", lat = "40.71", lon = "-74.00"),
                City(id = "4", name = "Лос-Анджелес", lat = "34.05", lon = "-118.24")
            )
        )
    )
}
