package com.swparks.viewmodel

import com.swparks.data.database.UserDao
import com.swparks.data.database.UserEntity
import com.swparks.data.repository.SWRepository
import com.swparks.ui.viewmodel.MainDispatcherRule
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для FriendsListViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userDao: UserDao
    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var friendsListViewModel: FriendsListViewModel

    @Before
    fun setup() {
        userDao = mockk(relaxed = true)
        coEvery { userDao.getCurrentUserFlow() } returns flowOf(null)

        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        friendsListViewModel = FriendsListViewModel(
            userDao,
            swRepository,
            logger
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun uiState_initial_shouldBeLoading() {
        // Given
        // When
        // Then
        val state = friendsListViewModel.uiState.value
        assertTrue("Начальное состояние должно быть Loading", state is FriendsListUiState.Loading)
    }

    @Test
    fun uiState_whenDataLoaded_shouldReturnSuccess() = runTest {
        // Given
        val testUser1 = mockk<UserEntity>(relaxed = true)
        val testUser2 = mockk<UserEntity>(relaxed = true)
        val testList = listOf(testUser1, testUser2)

        coEvery { userDao.getFriendRequestsFlow() } returns flowOf(testList)
        coEvery { userDao.getFriendsFlow() } returns flowOf(testList)
        coEvery { userDao.getCurrentUserFlow() } returns flowOf(null)

        // When
        val viewModel = FriendsListViewModel(
            userDao,
            swRepository,
            logger
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Состояние должно быть Success", state is FriendsListUiState.Success)
        val successState = state as FriendsListUiState.Success
        assertEquals("Заявки должны содержать 2 пользователя", 2, successState.friendRequests.size)
        assertEquals("Друзья должны содержать 2 пользователя", 2, successState.friends.size)
    }

    @Test
    fun onAcceptFriendRequest_shouldCallRepository() = runTest {
        // Given
        val testUserId = 123L
        coEvery { swRepository.respondToFriendRequest(testUserId, accept = true) } returns mockk(
            relaxed = true
        )

        // When
        friendsListViewModel.onAcceptFriendRequest(testUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            swRepository.respondToFriendRequest(testUserId, accept = true)
        }
    }

    @Test
    fun onDeclineFriendRequest_shouldCallRepository() = runTest {
        // Given
        val testUserId = 456L
        coEvery { swRepository.respondToFriendRequest(testUserId, accept = false) } returns mockk(
            relaxed = true
        )

        // When
        friendsListViewModel.onDeclineFriendRequest(testUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            swRepository.respondToFriendRequest(testUserId, accept = false)
        }
    }

    @Test
    fun onAcceptFriendRequest_whenRepositorySuccess_thenLogsSuccess() = runTest {
        // Given
        val testUserId = 123L
        coEvery {
            swRepository.respondToFriendRequest(
                testUserId,
                accept = true
            )
        } returns Result.success(Unit)

        // When
        friendsListViewModel.onAcceptFriendRequest(testUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            logger.i(any(), eq("Заявка успешно принята: userId=$testUserId"))
        }
    }

    @Test
    fun onAcceptFriendRequest_whenRepositoryFails_thenLogsError() = runTest {
        // Given
        val testUserId = 123L
        val testException = Exception("Test error")
        coEvery {
            swRepository.respondToFriendRequest(
                testUserId,
                accept = true
            )
        } returns Result.failure(testException)

        // When
        friendsListViewModel.onAcceptFriendRequest(testUserId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            logger.e(any(), eq("Ошибка принятия заявки: ${testException.message}"))
        }
    }

    @Test
    fun onFriendClick_shouldLogAction() {
        // Given
        val testUserId = 789L

        // When
        friendsListViewModel.onFriendClick(testUserId)

        // Then
        coVerify(exactly = 1) {
            logger.i(any(), eq("Нажатие на друга: userId=$testUserId"))
        }
    }
}
