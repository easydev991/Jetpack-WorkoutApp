package com.swparks.ui.viewmodel

import com.swparks.R
import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppErrorOperation
import com.swparks.analytics.UserActionType
import com.swparks.data.model.ApiBlacklistOption
import com.swparks.data.model.ApiFriendAction
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OtherUserProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockSwRepository = mockk<SWRepository>()
    private val mockCountriesRepository = mockk<CountriesRepository>()
    private val mockLogger = mockk<Logger>(relaxed = true)
    private val mockUserNotifier = mockk<UserNotifier>(relaxed = true)
    private val mockResources = mockk<ResourcesProvider>(relaxed = true)
    private val mockAnalyticsService = mockk<AnalyticsService>(relaxed = true)

    private val currentUserFlow = MutableSharedFlow<User?>(replay = 1)
    private val friendsFlow = MutableSharedFlow<List<User>>(replay = 1)
    private val blacklistFlow = MutableSharedFlow<List<User>>(replay = 1)
    private val errorFlow = MutableSharedFlow<AppError>()

    @Before
    fun setup() {
        every { mockSwRepository.getCurrentUserFlow() } returns currentUserFlow
        every { mockSwRepository.getFriendsFlow() } returns friendsFlow
        every { mockSwRepository.getBlacklistFlow() } returns blacklistFlow
        every { mockUserNotifier.errorFlow } returns errorFlow

        // Мокируем getString для локализованных строк
        every { mockResources.getString(R.string.friend_request_sent) } returns "Request sent!"
        every { mockResources.getString(R.string.friends_list_updated) } returns "Friends list updated"

        // Эмулируем авторизованного пользователя по умолчанию
        currentUserFlow.tryEmit(User(id = 1L, name = "current", image = null))
        friendsFlow.tryEmit(emptyList())
        blacklistFlow.tryEmit(emptyList())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // === loadUser tests ===

    @Test
    fun init_whenCreated_thenLoadsUser() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            assertEquals(user, viewModel.viewedUser.value)
            coVerify { mockSwRepository.getUser(userId) }
        }

    @Test
    fun loadUser_whenNotFound_thenShowsUserNotFound() =
        runTest {
            val userId = 999L
            val error = HttpException(Response.error<Any>(404, "".toResponseBody()))
            coEvery { mockSwRepository.getUser(userId) } returns Result.failure(error)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is OtherUserProfileUiState.UserNotFound)
        }

    @Test
    fun loadUser_whenNetworkError_thenShowsErrorWithRetry() =
        runTest {
            val userId = 999L
            coEvery { mockSwRepository.getUser(userId) } returns Result.failure(Exception("Network error"))

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is OtherUserProfileUiState.Error)
            assertTrue((state as OtherUserProfileUiState.Error).canRetry)
        }

    // === refreshUser tests ===

    @Test
    fun refreshUser_whenNetworkError_doesNotChangeUiStateToError() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user) andThen
                Result.failure(Exception("Network error"))

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            val stateBeforeRefresh = viewModel.uiState.value
            assertTrue(stateBeforeRefresh is OtherUserProfileUiState.Success)

            viewModel.refreshUser()
            advanceUntilIdle()

            // После ошибки refresh состояние остается Success (не меняется на Error)
            assertTrue(viewModel.uiState.value is OtherUserProfileUiState.Success)
        }

    // === Address loading tests ===

    @Test
    fun loadUser_whenNoCountryIdOrCityId_doesNotLoadAddress() =
        runTest {
            val userId = 123L
            val user =
                User(id = userId, name = "test", image = null, countryID = null, cityID = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is OtherUserProfileUiState.Success)
            assertEquals(null, (state as OtherUserProfileUiState.Success).country)
            assertEquals(null, state.city)

            // Проверяем, что репозиторий стран не вызывался
            coVerify(exactly = 0) { mockCountriesRepository.getCountryById(any()) }
            coVerify(exactly = 0) { mockCountriesRepository.getCityById(any()) }
        }

    @Test
    fun refreshUser_whenAddressUnchanged_usesCache() =
        runTest {
            val userId = 123L
            val country = Country(id = "1", name = "Russia", cities = emptyList())
            val city = City(id = "100", name = "Moscow", lat = "0", lon = "0")
            val user = User(id = userId, name = "test", image = null, countryID = 1, cityID = 100)

            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery { mockCountriesRepository.getCountryById("1") } returns country
            coEvery { mockCountriesRepository.getCityById("100") } returns city

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            // Первый вызов — загружаем адрес
            coVerify(exactly = 1) { mockCountriesRepository.getCountryById("1") }

            // Refresh с теми же ID
            viewModel.refreshUser()
            advanceUntilIdle()

            // Второй вызов — используем кэш (getCountryById вызван только 1 раз)
            coVerify(exactly = 1) { mockCountriesRepository.getCountryById("1") }
        }

    // === Friend action tests ===

    @Test
    fun performFriendAction_whenNotFriend_thenCallsAddFriend() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery {
                mockSwRepository.friendAction(
                    userId,
                    ApiFriendAction.ADD
                )
            } returns Result.success(Unit)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.performFriendAction()
            advanceUntilIdle()

            coVerify { mockSwRepository.friendAction(userId, ApiFriendAction.ADD) }
        }

    @Test
    fun performFriendAction_whenStarts_isFriendActionLoadingIsFalseInitially() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            // До вызова - не в состоянии загрузки
            assertEquals(false, viewModel.isFriendActionLoading.value)
        }

    @Test
    fun performFriendAction_whenSuccess_thenIsFriendActionLoadingIsFalseAfterCompletion() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery {
                mockSwRepository.friendAction(
                    userId,
                    ApiFriendAction.ADD
                )
            } returns Result.success(Unit)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.performFriendAction()
            advanceUntilIdle()

            assertEquals(false, viewModel.isFriendActionLoading.value)
        }

    @Test
    fun performFriendAction_whenError_thenIsFriendActionLoadingIsFalseAfterCompletion() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery {
                mockSwRepository.friendAction(
                    userId,
                    ApiFriendAction.ADD
                )
            } returns Result.failure(Exception("Network error"))

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.performFriendAction()
            advanceUntilIdle()

            assertEquals(false, viewModel.isFriendActionLoading.value)
        }

    // === Blacklist action tests ===

    @Test
    fun performBlacklistAction_whenNotBlocked_thenCallsAddToBlacklist() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery {
                mockSwRepository.blacklistAction(
                    user,
                    ApiBlacklistOption.ADD
                )
            } returns Result.success(Unit)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            var onBlockedCalled = false
            viewModel.performBlacklistAction { onBlockedCalled = true }
            advanceUntilIdle()

            coVerify { mockSwRepository.blacklistAction(user, ApiBlacklistOption.ADD) }
            assertTrue(onBlockedCalled) // После блокировки вызывается callback
        }

    // === Current user timeout test ===

    @Test
    fun waitForCurrentUser_whenTimeout_showsAuthErrorWithoutRetry() =
        runTest {
            val userId = 123L
            // currentUser всегда null (эмуляция ошибки авторизации)
            currentUserFlow.tryEmit(null)
            every { mockSwRepository.getCurrentUserFlow() } returns flowOf(null)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )

            // Используем advanceTimeBy для срабатывания timeout (10 сек + запас)
            advanceTimeBy(OtherUserProfileViewModel.CURRENT_USER_LOAD_TIMEOUT_MS + 100)
            advanceUntilIdle()

            // После timeout показывается ошибка авторизации без возможности retry
            val state = viewModel.uiState.value
            assertTrue(state is OtherUserProfileUiState.Error)
            assertTrue(!(state as OtherUserProfileUiState.Error).canRetry)
        }

    // === Runtime exception handling tests ===

    @Test
    fun loadProfileAddress_whenRuntimeException_thenCallsHandleError() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null, countryID = 1, cityID = 100)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)

            // Мокируем выброс RuntimeException при загрузке страны
            coEvery { mockCountriesRepository.getCountryById("1") } throws RuntimeException("Unexpected error")

            OtherUserProfileViewModel(
                userId,
                mockCountriesRepository,
                mockSwRepository,
                mockLogger,
                mockUserNotifier,
                mockResources,
                mockAnalyticsService
            )

            advanceUntilIdle()

            // Then - userNotifier.handleError должен быть вызван
            coVerify { mockUserNotifier.handleError(any<AppError>()) }
        }

    // === Analytics tests ===

    @Test
    fun performFriendAction_whenNotFriend_thenLogsAddFriendUserAction() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery {
                mockSwRepository.friendAction(userId, ApiFriendAction.ADD)
            } returns Result.success(Unit)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.performFriendAction()
            advanceUntilIdle()

            verify {
                mockAnalyticsService.log(AnalyticsEvent.UserAction(UserActionType.ADD_FRIEND))
            }
        }

    @Test
    fun performFriendAction_whenAlreadyFriend_thenLogsRemoveFriendUserAction() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            every { mockSwRepository.getFriendsFlow() } returns flowOf(listOf(user))
            coEvery {
                mockSwRepository.friendAction(userId, ApiFriendAction.REMOVE)
            } returns Result.success(Unit)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.friends.first()
            advanceUntilIdle()

            viewModel.performFriendAction()
            advanceUntilIdle()

            verify {
                mockAnalyticsService.log(AnalyticsEvent.UserAction(UserActionType.REMOVE_FRIEND))
            }
        }

    @Test
    fun performFriendAction_whenFailure_thenLogsFriendRequestFailedAppError() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            val error = Exception("Network error")
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery {
                mockSwRepository.friendAction(userId, ApiFriendAction.ADD)
            } returns Result.failure(error)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.performFriendAction()
            advanceUntilIdle()

            verify {
                mockAnalyticsService.log(
                    AnalyticsEvent.AppError(AppErrorOperation.FRIEND_REQUEST_FAILED, error)
                )
            }
        }

    @Test
    fun performBlacklistAction_whenNotInBlacklist_thenLogsBlockUserUserAction() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            coEvery {
                mockSwRepository.blacklistAction(user, ApiBlacklistOption.ADD)
            } returns Result.success(Unit)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.performBlacklistAction {}
            advanceUntilIdle()

            verify {
                mockAnalyticsService.log(AnalyticsEvent.UserAction(UserActionType.BLOCK_USER))
            }
        }

    @Test
    fun performBlacklistAction_whenInBlacklist_thenLogsUnblockUserUserAction() =
        runTest {
            val userId = 123L
            val user = User(id = userId, name = "test", image = null)
            coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
            every { mockSwRepository.getBlacklistFlow() } returns flowOf(listOf(user))
            coEvery {
                mockSwRepository.blacklistAction(user, ApiBlacklistOption.REMOVE)
            } returns Result.success(Unit)

            val viewModel =
                OtherUserProfileViewModel(
                    userId,
                    mockCountriesRepository,
                    mockSwRepository,
                    mockLogger,
                    mockUserNotifier,
                    mockResources,
                    mockAnalyticsService
                )
            advanceUntilIdle()

            viewModel.blacklist.first()
            advanceUntilIdle()

            viewModel.performBlacklistAction {}
            advanceUntilIdle()

            verify {
                mockAnalyticsService.log(AnalyticsEvent.UserAction(UserActionType.UNBLOCK_USER))
            }
        }
}
