package com.swparks.viewmodel

import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppErrorOperation
import com.swparks.analytics.UserActionType
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.UserEntity
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.FriendsListUiState
import com.swparks.ui.viewmodel.FriendsListViewModel
import com.swparks.ui.viewmodel.MainDispatcherRule
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
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
import java.io.IOException

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
    private lateinit var userNotifier: UserNotifier
    private lateinit var friendsListViewModel: FriendsListViewModel
    private lateinit var analyticsService: AnalyticsService

    @Before
    fun setup() {
        userDao = mockk(relaxed = true)
        coEvery { userDao.getCurrentUserFlow() } returns flowOf(null)

        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        analyticsService = mockk(relaxed = true)
        friendsListViewModel =
            FriendsListViewModel(
                userDao,
                swRepository,
                logger,
                userNotifier,
                analyticsService
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
    fun uiState_whenDataLoaded_shouldReturnSuccess() =
        runTest {
            // Given
            val testUser1 = mockk<UserEntity>(relaxed = true)
            val testUser2 = mockk<UserEntity>(relaxed = true)
            val testList = listOf(testUser1, testUser2)

            coEvery { userDao.getFriendRequestsFlow() } returns flowOf(testList)
            coEvery { userDao.getFriendsFlow() } returns flowOf(testList)
            coEvery { userDao.getCurrentUserFlow() } returns flowOf(null)

            // When
            val viewModel =
                FriendsListViewModel(
                    userDao,
                    swRepository,
                    logger,
                    userNotifier,
                    analyticsService
                )
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue("Состояние должно быть Success", state is FriendsListUiState.Success)
            val successState = state as FriendsListUiState.Success
            assertEquals(
                "Заявки должны содержать 2 пользователя",
                2,
                successState.friendRequests.size
            )
            assertEquals("Друзья должны содержать 2 пользователя", 2, successState.friends.size)
        }

    @Test
    fun onAcceptFriendRequest_shouldCallRepository() =
        runTest {
            // Given
            val testUserId = 123L
            coEvery { swRepository.respondToFriendRequest(testUserId, accept = true) } returns
                mockk(
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
    fun onDeclineFriendRequest_shouldCallRepository() =
        runTest {
            // Given
            val testUserId = 456L
            coEvery { swRepository.respondToFriendRequest(testUserId, accept = false) } returns
                mockk(
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
    fun onAcceptFriendRequest_whenRepositorySuccess_thenLogsSuccess() =
        runTest {
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
                logger.i(any(), "Заявка успешно принята: userId=$testUserId")
            }
        }

    @Test
    fun onAcceptFriendRequest_whenRepositoryFails_thenReportsError() =
        runTest {
            // Given
            val testUserId = 123L
            val testError = IOException("Нет подключения к сети")
            coEvery {
                swRepository.respondToFriendRequest(
                    testUserId,
                    accept = true
                )
            } returns Result.failure(testError)

            // When
            friendsListViewModel.onAcceptFriendRequest(testUserId)
            advanceUntilIdle()

            // Then
            verify {
                userNotifier.handleError(
                    match<AppError> {
                        it is AppError.Network && it.message.contains("Не удалось принять заявку")
                    }
                )
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
            logger.i(any(), "Нажатие на друга: userId=$testUserId")
        }
    }

    @Test
    fun onDeclineFriendRequest_whenRepositoryFails_thenReportsError() =
        runTest {
            // Given
            val testUserId = 456L
            val testError = IOException("Нет подключения к сети")
            coEvery {
                swRepository.respondToFriendRequest(
                    testUserId,
                    accept = false
                )
            } returns Result.failure(testError)

            // When
            friendsListViewModel.onDeclineFriendRequest(testUserId)
            advanceUntilIdle()

            // Then
            verify {
                userNotifier.handleError(
                    match<AppError> {
                        it is AppError.Network && it.message.contains("Не удалось отклонить заявку")
                    }
                )
            }
        }

    @Test
    fun onAcceptFriendRequest_shouldLogRespondAcceptAction() =
        runTest {
            val testUserId = 123L
            coEvery {
                swRepository.respondToFriendRequest(testUserId, accept = true)
            } returns mockk(relaxed = true)

            friendsListViewModel.onAcceptFriendRequest(testUserId)

            verify(exactly = 1) {
                analyticsService.log(
                    AnalyticsEvent.UserAction(UserActionType.RESPOND_FRIEND_REQUEST_ACCEPT)
                )
            }
        }

    @Test
    fun onAcceptFriendRequest_whenRepositoryFails_shouldLogAppError() =
        runTest {
            val testUserId = 123L
            val testError = IOException("Нет подключения к сети")
            coEvery {
                swRepository.respondToFriendRequest(testUserId, accept = true)
            } returns Result.failure(testError)

            friendsListViewModel.onAcceptFriendRequest(testUserId)
            advanceUntilIdle()

            verify(exactly = 1) {
                analyticsService.log(
                    AnalyticsEvent.AppError(AppErrorOperation.FRIEND_REQUEST_FAILED, testError)
                )
            }
        }

    @Test
    fun onDeclineFriendRequest_shouldLogRespondDeclineAction() =
        runTest {
            val testUserId = 456L
            coEvery {
                swRepository.respondToFriendRequest(testUserId, accept = false)
            } returns mockk(relaxed = true)

            friendsListViewModel.onDeclineFriendRequest(testUserId)

            verify(exactly = 1) {
                analyticsService.log(
                    AnalyticsEvent.UserAction(UserActionType.RESPOND_FRIEND_REQUEST_DECLINE)
                )
            }
        }

    @Test
    fun onDeclineFriendRequest_whenRepositoryFails_shouldLogAppError() =
        runTest {
            val testUserId = 456L
            val testError = IOException("Нет подключения к сети")
            coEvery {
                swRepository.respondToFriendRequest(testUserId, accept = false)
            } returns Result.failure(testError)

            friendsListViewModel.onDeclineFriendRequest(testUserId)
            advanceUntilIdle()

            verify(exactly = 1) {
                analyticsService.log(
                    AnalyticsEvent.AppError(AppErrorOperation.FRIEND_REQUEST_FAILED, testError)
                )
            }
        }
}
