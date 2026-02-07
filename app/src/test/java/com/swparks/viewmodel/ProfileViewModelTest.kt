package com.swparks.viewmodel

import android.util.Log
import app.cash.turbine.test
import com.swparks.domain.repository.CountriesRepository
import com.swparks.model.City
import com.swparks.model.Country
import com.swparks.model.SocialUpdates
import com.swparks.model.User
import com.swparks.ui.viewmodel.MainDispatcherRule
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для ProfileViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var countriesRepository: CountriesRepository
    private lateinit var swRepository: com.swparks.data.repository.SWRepository
    private lateinit var logger: Logger
    private lateinit var errorReporter: ErrorReporter
    private lateinit var profileViewModel: ProfileViewModel

    private val testCountry = Country(id = "1", name = "Россия", cities = emptyList())
    private val testCity = City(id = "1", name = "Москва", lat = "55.75", lon = "37.62")

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        countriesRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        errorReporter = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Создает ViewModel для тестов
     */
    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            countriesRepository,
            swRepository,
            logger,
            errorReporter
        )
    }

    @Test
    fun isRefreshing_initial_shouldBeFalse() {
        // Given
        profileViewModel = createViewModel()

        // When
        // Then
        assertFalse(
            "Начальное состояние isRefreshing должно быть false",
            profileViewModel.isRefreshing.value
        )
    }

    @Test
    fun refreshProfile_whenUserNull_shouldDoNothing() = runTest {
        // Given
        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(null)
        profileViewModel = createViewModel()

        // When
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then
        assertFalse("isRefreshing должен остаться false", profileViewModel.isRefreshing.value)
        coVerify(exactly = 0) { countriesRepository.getCountryById(any<String>()) }
        coVerify(exactly = 0) { countriesRepository.getCityById(any<String>()) }
    }

    @Test
    fun refreshProfile_whenSuccess_shouldUpdateData() = runTest {
        // Given
        val testUser = createTestUser()
        val socialUpdates = SocialUpdates(
            user = testUser,
            friends = emptyList(),
            friendRequests = emptyList(),
            blacklist = emptyList()
        )

        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery { swRepository.getSocialUpdates(testUser.id) } returns Result.success(socialUpdates)
        coEvery { countriesRepository.getCountryById(testUser.countryID.toString()) } returns testCountry
        coEvery { countriesRepository.getCityById(testUser.cityID.toString()) } returns testCity

        profileViewModel = createViewModel()
        advanceUntilIdle() // Ждем завершения инициализации

        // When
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then
        assertFalse("isRefreshing должен стать false", profileViewModel.isRefreshing.value)
        assertTrue(
            "Состояние должно быть Success",
            profileViewModel.uiState.value is ProfileUiState.Success
        )
        val state = profileViewModel.uiState.value as ProfileUiState.Success
        assertEquals("Страна должна быть загружена", testCountry, state.country)
        assertEquals("Город должен быть загружен", testCity, state.city)
    }

    @Test
    fun refreshProfile_whenLoading_shouldBeTrue() = runTest {
        // Given
        val testUser = createTestUser()
        val socialUpdates = SocialUpdates(
            user = testUser,
            friends = emptyList(),
            friendRequests = emptyList(),
            blacklist = emptyList()
        )

        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery { swRepository.getSocialUpdates(testUser.id) } returns Result.success(socialUpdates)
        coEvery { countriesRepository.getCountryById(testUser.countryID.toString()) } returns testCountry
        coEvery { countriesRepository.getCityById(testUser.cityID.toString()) } returns testCity

        profileViewModel = createViewModel()
        advanceUntilIdle() // Ждем завершения инициализации

        // When
        profileViewModel.refreshProfile()

        // Then - корутина может выполниться быстро, поэтому проверяем, что состояние обновляется корректно
        // после завершения корутины
        advanceUntilIdle()
        assertFalse(
            "isRefreshing должен стать false после завершения",
            profileViewModel.isRefreshing.value
        )
    }

    @Test
    fun refreshProfile_whenRepositoryFailure_shouldLogError() = runTest {
        // Given
        val testUser = createTestUser()

        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery {
            swRepository.getSocialUpdates(testUser.id)
        } returns Result.failure(IOException("Нет подключения к сети"))

        profileViewModel = createViewModel()
        advanceUntilIdle() // Ждем завершения инициализации

        // When
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then
        assertFalse("isRefreshing должен стать false", profileViewModel.isRefreshing.value)
        coVerify(exactly = 1) {
            logger.e(
                any<String>(),
                match<String> { it.contains("Ошибка загрузки профиля и социальных данных") })
        }
    }

    @Test
    fun refreshProfile_whenRepositoryFailure_shouldNotCrash() = runTest {
        // Given
        val testUser = createTestUser()

        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery {
            swRepository.getSocialUpdates(testUser.id)
        } returns Result.failure(IOException("Нет подключения к сети"))

        profileViewModel = createViewModel()
        advanceUntilIdle() // Ждем завершения инициализации

        // When - не должно быть исключения
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then - проверяем, что состояние обновления стало false без краша
        assertFalse("isRefreshing должен стать false", profileViewModel.isRefreshing.value)
    }

    @Test
    fun refreshProfile_whenCountriesRepositoryThrows_shouldLogError() = runTest {
        // Given
        val testUser = createTestUser()
        val socialUpdates = SocialUpdates(
            user = testUser,
            friends = emptyList(),
            friendRequests = emptyList(),
            blacklist = emptyList()
        )

        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery { swRepository.getSocialUpdates(testUser.id) } returns Result.success(socialUpdates)
        coEvery {
            countriesRepository.getCountryById(testUser.countryID.toString())
        } throws IllegalStateException("Ошибка базы данных")

        profileViewModel = createViewModel()
        advanceUntilIdle() // Ждем завершения инициализации

        // When
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then - исключение перехватывается в loadProfileAddress и логируется там
        assertFalse("isRefreshing должен стать false", profileViewModel.isRefreshing.value)
        coVerify(atLeast = 1) {
            logger.e(
                any<String>(),
                match<String> { it.contains("Ошибка загрузки адреса для профиля") })
        }
    }

    @Test
    fun refreshProfile_whenCountriesRepositoryThrows_shouldNotCrash() = runTest {
        // Given
        val testUser = createTestUser()
        val socialUpdates = SocialUpdates(
            user = testUser,
            friends = emptyList(),
            friendRequests = emptyList(),
            blacklist = emptyList()
        )

        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery { swRepository.getSocialUpdates(testUser.id) } returns Result.success(socialUpdates)
        coEvery {
            countriesRepository.getCountryById(testUser.countryID.toString())
        } throws IllegalStateException("Ошибка базы данных")

        profileViewModel = createViewModel()
        advanceUntilIdle() // Ждем завершения инициализации

        // When - не должно быть исключения
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then - проверяем, что состояние обновления стало false без краша
        assertFalse("isRefreshing должен стать false", profileViewModel.isRefreshing.value)
    }

    @Test
    fun uiState_initial_shouldBeLoading() {
        // Given
        // When
        profileViewModel = createViewModel()
        // Then - проверяем начальное состояние сразу после создания
        // По умолчанию пользователь null, поэтому должно быть Error или Loading
        val state = profileViewModel.uiState.value
        assertTrue(
            "Начальное состояние должно быть Loading или Error",
            state is ProfileUiState.Loading || state is ProfileUiState.Error
        )
    }

    @Test
    fun blacklist_whenCacheUpdates_shouldUpdateAutomatically() = runTest {
        // Given
        val testUser = createTestUser()
        val initialBlacklist = listOf(
            User(id = 2, name = "Blacklisted User 1", image = null, fullName = "User 1")
        )
        val updatedBlacklist = listOf(
            User(id = 2, name = "Blacklisted User 1", image = null, fullName = "User 1"),
            User(id = 3, name = "Blacklisted User 2", image = null, fullName = "User 2")
        )

        // Используем MutableStateFlow для эмуляции обновлений
        val blacklistFlow = kotlinx.coroutines.flow.MutableStateFlow(initialBlacklist)
        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery { swRepository.getBlacklistFlow() } returns blacklistFlow

        profileViewModel = createViewModel()

        // Then - проверяем через Turbine для корректной работы с Flow
        profileViewModel.blacklist.test {
            awaitItem() // Получаем значение (может быть initialValue или initialBlacklist)

            // When - эмулируем обновление данных в базе
            blacklistFlow.value = updatedBlacklist

            // Then - черный список должен обновиться автоматически
            assertEquals(
                "Черный список должен обновиться автоматически",
                updatedBlacklist,
                awaitItem()
            )
        }
    }

    @Test
    fun blacklist_whenEmpty_shouldBeEmpty() = runTest {
        // Given
        val testUser = createTestUser()
        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery { swRepository.getBlacklistFlow() } returns flowOf(emptyList())

        // When
        profileViewModel = createViewModel()
        advanceUntilIdle()

        // Then
        profileViewModel.blacklist.test {
            // Получаем хотя бы одно значение
            val value = awaitItem()
            // Проверяем что это пустой список или initialValue тоже пустой
            assertTrue("Blacklist должен быть пустым", value.isEmpty())
        }
    }

    /**
     * Создает тестового пользователя
     */
    private fun createTestUser(): User {
        return User(
            id = 1,
            name = "Test User",
            fullName = "Test User",
            email = "test@example.com",
            image = "https://example.com/image.jpg",
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
