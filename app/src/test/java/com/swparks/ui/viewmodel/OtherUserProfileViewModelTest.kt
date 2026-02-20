package com.swparks.ui.viewmodel

import com.swparks.R
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
    fun init_whenCreated_thenLoadsUser() = runTest {
        val userId = 123L
        val user = User(id = userId, name = "test", image = null)
        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
        )
        advanceUntilIdle()

        assertEquals(user, viewModel.viewedUser.value)
        coVerify { mockSwRepository.getUser(userId) }
    }

    @Test
    fun loadUser_whenNotFound_thenShowsUserNotFound() = runTest {
        val userId = 999L
        val error = HttpException(Response.error<Any>(404, "".toResponseBody()))
        coEvery { mockSwRepository.getUser(userId) } returns Result.failure(error)

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is OtherUserProfileUiState.UserNotFound)
    }

    @Test
    fun loadUser_whenNetworkError_thenShowsErrorWithRetry() = runTest {
        val userId = 999L
        coEvery { mockSwRepository.getUser(userId) } returns Result.failure(Exception("Network error"))

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OtherUserProfileUiState.Error)
        assertTrue((state as OtherUserProfileUiState.Error).canRetry)
    }

    // === refreshUser tests ===

    @Test
    fun refreshUser_whenNetworkError_doesNotChangeUiStateToError() = runTest {
        val userId = 123L
        val user = User(id = userId, name = "test", image = null)
        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user) andThen
            Result.failure(Exception("Network error"))

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
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
    fun loadUser_whenNoCountryIdOrCityId_doesNotLoadAddress() = runTest {
        val userId = 123L
        val user = User(id = userId, name = "test", image = null, countryID = null, cityID = null)
        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
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
    fun refreshUser_whenAddressUnchanged_usesCache() = runTest {
        val userId = 123L
        val country = Country(id = "1", name = "Russia", cities = emptyList())
        val city = City(id = "100", name = "Moscow", lat = "0", lon = "0")
        val user = User(id = userId, name = "test", image = null, countryID = 1, cityID = 100)

        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
        coEvery { mockCountriesRepository.getCountryById("1") } returns country
        coEvery { mockCountriesRepository.getCityById("100") } returns city

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
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
    fun performFriendAction_whenNotFriend_thenCallsAddFriend() = runTest {
        val userId = 123L
        val user = User(id = userId, name = "test", image = null)
        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
        coEvery {
            mockSwRepository.friendAction(
                userId,
                ApiFriendAction.ADD
            )
        } returns Result.success(Unit)

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
        )
        advanceUntilIdle()

        viewModel.performFriendAction()
        advanceUntilIdle()

        coVerify { mockSwRepository.friendAction(userId, ApiFriendAction.ADD) }
    }

    @Test
    fun performFriendAction_whenStarts_isFriendActionLoadingIsFalseInitially() = runTest {
        val userId = 123L
        val user = User(id = userId, name = "test", image = null)
        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
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

            val viewModel = OtherUserProfileViewModel(
                userId,
                mockCountriesRepository,
                mockSwRepository,
                mockLogger,
                mockUserNotifier,
                mockResources
            )
            advanceUntilIdle()

            viewModel.performFriendAction()
            advanceUntilIdle()

            assertEquals(false, viewModel.isFriendActionLoading.value)
        }

    @Test
    fun performFriendAction_whenError_thenIsFriendActionLoadingIsFalseAfterCompletion() = runTest {
        val userId = 123L
        val user = User(id = userId, name = "test", image = null)
        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
        coEvery {
            mockSwRepository.friendAction(
                userId,
                ApiFriendAction.ADD
            )
        } returns Result.failure(Exception("Network error"))

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
        )
        advanceUntilIdle()

        viewModel.performFriendAction()
        advanceUntilIdle()

        assertEquals(false, viewModel.isFriendActionLoading.value)
    }

    // === Blacklist action tests ===

    @Test
    fun performBlacklistAction_whenNotBlocked_thenCallsAddToBlacklist() = runTest {
        val userId = 123L
        val user = User(id = userId, name = "test", image = null)
        coEvery { mockSwRepository.getUser(userId) } returns Result.success(user)
        coEvery {
            mockSwRepository.blacklistAction(
                user,
                ApiBlacklistOption.ADD
            )
        } returns Result.success(Unit)

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
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
    fun waitForCurrentUser_whenTimeout_showsAuthErrorWithoutRetry() = runTest {
        val userId = 123L
        // currentUser всегда null (эмуляция ошибки авторизации)
        currentUserFlow.tryEmit(null)
        every { mockSwRepository.getCurrentUserFlow() } returns flowOf(null)

        val viewModel = OtherUserProfileViewModel(
            userId,
            mockCountriesRepository,
            mockSwRepository,
            mockLogger,
            mockUserNotifier,
            mockResources
        )

        // Используем advanceTimeBy для срабатывания timeout (10 сек + запас)
        advanceTimeBy(OtherUserProfileViewModel.CURRENT_USER_LOAD_TIMEOUT_MS + 100)
        advanceUntilIdle()

        // После timeout показывается ошибка авторизации без возможности retry
        val state = viewModel.uiState.value
        assertTrue(state is OtherUserProfileUiState.Error)
        assertTrue(!(state as OtherUserProfileUiState.Error).canRetry)
    }
}
