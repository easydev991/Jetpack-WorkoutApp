package com.swparks.ui.viewmodel

import android.util.Log
import app.cash.turbine.test
import com.swparks.data.model.Park
import com.swparks.data.repository.SWRepository
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
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
    private lateinit var userNotifier: UserNotifier
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
        userNotifier = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createViewModel(userId: Long = testUserId): UserTrainingParksViewModel {
        return UserTrainingParksViewModel(
            swRepository = swRepository,
            userId = userId,
            logger = logger,
            userNotifier = userNotifier
        )
    }

    @Test
    fun loadParks_whenRepositoryReturnsSuccess_thenUpdatesUiStateToSuccess() = runTest {
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)
        viewModel = createViewModel()

        advanceUntilIdle()

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
        val errorMessage = "Ошибка загрузки площадок"
        val error = IOException(errorMessage)
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.failure(error)
        viewModel = createViewModel()

        advanceUntilIdle()

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
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refreshParks()
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun refreshParks_whenRepositoryReturnsSuccess_thenUpdatesUiStateToSuccess() = runTest {
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

        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(updatedParks)
        viewModel.refreshParks()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            val successState = state as UserTrainingParksUiState.Success
            assertEquals(updatedParks, successState.parks)
        }
    }

    @Test
    fun init_whenViewModelCreated_thenLoadsParksAutomatically() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns false
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)

        viewModel = createViewModel()
        advanceUntilIdle()

        coVerify { swRepository.getParksForUser(testUserId) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
        }
    }

    @Test
    fun init_whenCacheExists_thenShowsCachedParksBeforeNetworkResult() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns true
        coEvery { swRepository.getCachedParksForUser(testUserId) } returns testParks
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)

        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            assertEquals(testParks, (state as UserTrainingParksUiState.Success).parks)
        }

        advanceUntilIdle()

        coVerify { swRepository.hasCachedParksForUser(testUserId) }
        coVerify { swRepository.getCachedParksForUser(testUserId) }
        coVerify { swRepository.getParksForUser(testUserId) }
    }

    @Test
    fun init_whenCacheExistsAndRefreshSucceeds_thenUpdatesContent() = runTest {
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
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns true
        coEvery { swRepository.getCachedParksForUser(testUserId) } returns testParks
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(updatedParks)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            assertEquals(updatedParks, (state as UserTrainingParksUiState.Success).parks)
        }
    }

    @Test
    fun init_whenCacheExistsAndRefreshFails_thenKeepsCurrentContent() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns true
        coEvery { swRepository.getCachedParksForUser(testUserId) } returns testParks
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.failure(IOException("Network error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            assertEquals(testParks, (state as UserTrainingParksUiState.Success).parks)
        }
    }

    @Test
    fun init_whenNoCache_thenShowsLoadingUntilNetworkResult() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns false
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            assertEquals(testParks, (state as UserTrainingParksUiState.Success).parks)
        }
    }

    @Test
    fun init_whenNoCacheAndNetworkFails_thenShowsError() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns false
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.failure(IOException("Network error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Error)
        }
    }

    @Test
    fun init_whenCacheLookupThrows_thenShowsError() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } throws IOException("Cache error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Error)
        }
        coVerify { userNotifier.handleError(any()) }
    }

    @Test
    fun refresh_whenContentAlreadyShownAndNetworkFails_thenKeepsContent() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns false
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
        }

        coEvery { swRepository.getParksForUser(testUserId) } returns Result.failure(IOException("Network error"))
        viewModel.refreshParks()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UserTrainingParksUiState.Success)
            assertEquals(testParks, (state as UserTrainingParksUiState.Success).parks)
        }
    }

    @Test
    fun refresh_whenNetworkFails_thenNotifiesUserAboutError() = runTest {
        coEvery { swRepository.hasCachedParksForUser(testUserId) } returns false
        coEvery { swRepository.getParksForUser(testUserId) } returns Result.success(testParks)
        viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { swRepository.getParksForUser(testUserId) } returns Result.failure(IOException("Network error"))
        viewModel.refreshParks()
        advanceUntilIdle()

        coVerify { userNotifier.handleError(any()) }
    }
}
