package com.swparks.ui.viewmodel

import com.swparks.data.model.Park
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.util.AppError
import com.swparks.util.AppNotification
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserAddedParksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val swRepository: SWRepository = mockk<SWRepository>()
    private val logger: Logger = mockk<Logger>(relaxed = true)
    private val userNotifier: UserNotifier = mockk<UserNotifier>(relaxed = true).also {
        every { it.errorFlow } returns MutableSharedFlow<AppError>()
        every { it.notificationFlow } returns MutableSharedFlow<AppNotification>()
    }

    @Test
    fun init_withSeedAndNoRequiredFetch_thenEmitsSuccessWithoutRepositoryCall() = runTest {
        val seedParks = listOf(createPark(1), createPark(2))

        val viewModel = UserAddedParksViewModel(
            swRepository = swRepository,
            userId = 10L,
            seedParks = seedParks,
            requiresFetch = false,
            logger = logger,
            userNotifier = userNotifier
        )

        val state = viewModel.uiState.value
        assertTrue(state is UserAddedParksUiState.Success)
        assertEquals(seedParks, (state as UserAddedParksUiState.Success).parks)
        coVerify(exactly = 0) { swRepository.getUser(any()) }
    }

    @Test
    fun init_withoutSeed_thenLoadsUserAndEmitsSuccess() = runTest {
        val loadedParks = listOf(createPark(3))
        coEvery { swRepository.getUser(11L) } returns Result.success(createUser(loadedParks))

        val viewModel = UserAddedParksViewModel(
            swRepository = swRepository,
            userId = 11L,
            seedParks = null,
            requiresFetch = false,
            logger = logger,
            userNotifier = userNotifier
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UserAddedParksUiState.Success)
        assertEquals(loadedParks, (state as UserAddedParksUiState.Success).parks)
        coVerify(exactly = 1) { swRepository.getUser(11L) }
    }

    @Test
    fun init_whenRepositoryFailure_thenEmitsError() = runTest {
        coEvery { swRepository.getUser(12L) } returns Result.failure(Exception("network"))

        val viewModel = UserAddedParksViewModel(
            swRepository = swRepository,
            userId = 12L,
            seedParks = null,
            requiresFetch = true,
            logger = logger,
            userNotifier = userNotifier
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UserAddedParksUiState.Error)
        coVerify(exactly = 1) { swRepository.getUser(12L) }
    }

    @Test
    fun refresh_whenCalled_thenAlwaysFetchesAndResetsRefreshingState() = runTest {
        val seedParks = listOf(createPark(1))
        val refreshedParks = listOf(createPark(10), createPark(11))
        coEvery { swRepository.getUser(13L) } returns Result.success(createUser(refreshedParks))

        val viewModel = UserAddedParksViewModel(
            swRepository = swRepository,
            userId = 13L,
            seedParks = seedParks,
            requiresFetch = false,
            logger = logger,
            userNotifier = userNotifier
        )

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UserAddedParksUiState.Success)
        assertEquals(refreshedParks, (state as UserAddedParksUiState.Success).parks)
        assertFalse(viewModel.isRefreshing.value)
        coVerify(exactly = 1) { swRepository.getUser(13L) }
    }

    private fun createUser(addedParks: List<Park>?): User = User(
        id = 1L,
        name = "user",
        image = null,
        addedParks = addedParks
    )

    private fun createPark(id: Long): Park = Park(
        id = id,
        name = "Park $id",
        sizeID = 1,
        typeID = 1,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "Address $id",
        cityID = 1,
        countryID = 1,
        preview = "https://example.com/$id.jpg"
    )
}
