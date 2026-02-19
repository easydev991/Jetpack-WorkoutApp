package com.swparks.ui.viewmodel

import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
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
    private lateinit var logger: Logger
    private lateinit var userNotifier: UserNotifier

    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val countriesFlow = MutableStateFlow<List<Country>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        swRepository = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)

        every { swRepository.getCurrentUserFlow() } returns currentUserFlow
        every { countriesRepository.getCountriesFlow() } returns countriesFlow
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
    fun onCountrySelected_resetsCity_ifCityNotInNewCountry() = runTest {
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

        // Assert - город должен сброситься
        val state = viewModel.uiState.first()
        Assert.assertEquals("США", state.selectedCountry?.name)
        Assert.assertNull("Город должен быть null", state.selectedCity)
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

    // MARK: - Helper methods

    private fun createViewModel(): EditProfileViewModel =
        EditProfileViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            logger = logger,
            userNotifier = userNotifier
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
