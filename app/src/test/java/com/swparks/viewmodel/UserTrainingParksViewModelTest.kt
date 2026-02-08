package com.swparks.viewmodel

import android.util.Log
import app.cash.turbine.test
import com.swparks.data.model.Park
import com.swparks.data.repository.SWRepository
import com.swparks.ui.viewmodel.MainDispatcherRule
import com.swparks.ui.viewmodel.UserTrainingParksUiState
import com.swparks.ui.viewmodel.UserTrainingParksViewModel
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
import java.io.IOException

/**
 * Unit тесты для UserTrainingParksViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserTrainingParksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var errorReporter: ErrorReporter
    private lateinit var viewModel: UserTrainingParksViewModel

    private val testUserId = 1L
    private val testParks = listOf(
        Park(
            id = 1L,
            name = "Парк 1",
            sizeID = 1,
            typeID = 1,
            longitude = "37.62",
            latitude = "55.75",
            address = "Москва, ул. Примерная, 1",
            cityID = 1,
            countryID = 1,
            preview = "https://example.com/preview.jpg"
        ),
        Park(
            id = 2L,
            name = "Парк 2",
            sizeID = 2,
            typeID = 2,
            longitude = "37.63",
            latitude = "55.76",
            address = "Москва, ул. Примерная, 2",
            cityID = 1,
            countryID = 1,
            preview = "https://example.com/preview2.jpg"
        )
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

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
    private fun createViewModel(userId: Long = testUserId): UserTrainingParksViewModel {
        return UserTrainingParksViewModel(
            swRepository = swRepository,
            userId = userId,
            logger = logger,
            errorReporter = errorReporter
        )
    }

    @Test
    fun loadParks_whenRepositoryReturnsSuccess_thenUpdatesUiStateToSuccess() = runTest {
        // Given
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)
        viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            val successState = state as UserTrainingParksUiState.Success
            assertEquals(testParks, successState.parks)
        }

        coVerify { swRepository.getParksForUser(testUserId) }
    }

    @Test
    fun loadParks_whenRepositoryReturnsFailure_thenUpdatesUiStateToError() = runTest {
        // Given
        val errorMessage = "Ошибка загрузки площадок"
        val error = IOException(errorMessage)
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.failure(error)
        viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Error)
            val errorState = state as UserTrainingParksUiState.Error
            assertTrue(errorState.message.isNotEmpty())
        }

        coVerify { swRepository.getParksForUser(testUserId) }
    }

    @Test
    fun refreshParks_whenCalled_thenSetsIsRefreshingCorrectly() = runTest {
        // Given
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.refreshParks()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun refreshParks_whenRepositoryReturnsSuccess_thenUpdatesUiStateToSuccess() = runTest {
        // Given
        val updatedParks = listOf(
            Park(
                id = 3L,
                name = "Обновленный парк",
                sizeID = 1,
                typeID = 1,
                longitude = "37.62",
                latitude = "55.75",
                address = "Москва, ул. Новая, 1",
                cityID = 1,
                countryID = 1,
                preview = "https://example.com/preview3.jpg"
            )
        )
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(
            updatedParks
        )
        viewModel.refreshParks()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            val successState = state as UserTrainingParksUiState.Success
            assertEquals(updatedParks, successState.parks)
        }
    }

    @Test
    fun init_whenViewModelCreated_thenLoadsParksAutomatically() = runTest {
        // Given
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        coVerify { swRepository.getParksForUser(testUserId) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
        }
    }
}
