package com.swparks.viewmodel

import com.swparks.data.model.ApiBlacklistOption
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.BlacklistAction
import com.swparks.ui.state.BlacklistUiState
import com.swparks.ui.viewmodel.BlacklistViewModel
import com.swparks.ui.viewmodel.MainDispatcherRule
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
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

@OptIn(ExperimentalCoroutinesApi::class)
class BlacklistViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var userNotifier: UserNotifier
    private lateinit var blacklistViewModel: BlacklistViewModel

    @Before
    fun setup() {
        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        blacklistViewModel =
            BlacklistViewModel(
                swRepository,
                logger,
                userNotifier
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun uiState_initial_shouldBeLoading() {
        val state = blacklistViewModel.uiState.value
        assertTrue("Начальное состояние должно быть Loading", state is BlacklistUiState.Loading)
    }

    @Test
    fun uiState_whenDataLoaded_shouldReturnSuccess() =
        runTest {
            val testUser1 = User(id = 1L, name = "user1", image = null)
            val testUser2 = User(id = 2L, name = "user2", image = null)
            val testList = listOf(testUser1, testUser2)

            coEvery { swRepository.getBlacklistFlow() } returns flowOf(testList)

            val viewModel =
                BlacklistViewModel(
                    swRepository,
                    logger,
                    userNotifier
                )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Состояние должно быть Success", state is BlacklistUiState.Success)
            val successState = state as BlacklistUiState.Success
            assertEquals(
                "Черный список должен содержать 2 пользователя",
                2,
                successState.blacklist.size
            )
        }

    @Test
    fun showRemoveDialog_shouldSetItemToRemoveAndShowDialog() =
        runTest {
            val testUser = User(id = 789L, name = "test", image = null)
            coEvery { swRepository.getBlacklistFlow() } returns flowOf(listOf(testUser))
            val viewModel = BlacklistViewModel(swRepository, logger, userNotifier)
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.ShowRemoveDialog(testUser))
            advanceUntilIdle()

            val state = viewModel.uiState.value as BlacklistUiState.Success
            assertEquals("itemToRemove должен быть установлен", testUser, state.itemToRemove)
            assertTrue("showRemoveDialog должно быть true", state.showRemoveDialog)
        }

    @Test
    fun removeFromBlacklist_shouldCallRepositoryAndClearState() =
        runTest {
            val testUser = User(id = 101L, name = "test", image = null)
            coEvery { swRepository.getBlacklistFlow() } returns flowOf(listOf(testUser))
            coEvery {
                swRepository.blacklistAction(any<User>(), any<ApiBlacklistOption>())
            } returns Result.success(Unit)

            val viewModel = BlacklistViewModel(swRepository, logger, userNotifier)
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.ShowRemoveDialog(testUser))
            advanceUntilIdle()

            val beforeRemoveState = viewModel.uiState.value as BlacklistUiState.Success
            assertTrue(
                "showRemoveDialog должно быть true перед Remove",
                beforeRemoveState.showRemoveDialog
            )

            viewModel.onAction(BlacklistAction.Remove(testUser))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            if (state !is BlacklistUiState.Success) {
                throw AssertionError("Состояние должно быть Success, но получено: $state")
            }

            coVerify(exactly = 1) {
                swRepository.blacklistAction(testUser, ApiBlacklistOption.REMOVE)
            }
            assertEquals("itemToRemove должен быть null после удаления", null, state.itemToRemove)
            assertEquals(
                "showRemoveDialog должно быть false после удаления, " +
                    "но: itemToRemove=${state.itemToRemove}, " +
                    "isRemoving=${state.isRemoving}, isLoading=${state.isLoading}",
                false,
                state.showRemoveDialog
            )
            assertEquals("isRemoving должен быть false после завершения", false, state.isRemoving)
        }

    @Test
    fun removeFromBlacklist_onError_shouldClearStateAndReportError() =
        runTest {
            val testUser = User(id = 202L, name = "test", image = null)
            val testException = Exception("API Error")
            coEvery { swRepository.getBlacklistFlow() } returns flowOf(listOf(testUser))
            coEvery {
                swRepository.blacklistAction(any<User>(), any<ApiBlacklistOption>())
            } returns Result.failure(testException)

            val viewModel = BlacklistViewModel(swRepository, logger, userNotifier)
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.ShowRemoveDialog(testUser))
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.Remove(testUser))
            advanceUntilIdle()

            coVerify(exactly = 1) {
                swRepository.blacklistAction(testUser, ApiBlacklistOption.REMOVE)
            }
            val state = viewModel.uiState.value as BlacklistUiState.Success
            assertEquals(
                "itemToRemove должен быть очищен даже при ошибке",
                null,
                state.itemToRemove
            )
            assertEquals(
                "showRemoveDialog должно быть false даже при ошибке",
                false,
                state.showRemoveDialog
            )
            assertEquals("isRemoving должен быть false после ошибки", false, state.isRemoving)
        }

    @Test
    fun cancelRemove_shouldClearState() =
        runTest {
            val testUser = User(id = 303L, name = "test", image = null)
            coEvery { swRepository.getBlacklistFlow() } returns flowOf(listOf(testUser))

            val viewModel = BlacklistViewModel(swRepository, logger, userNotifier)
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.ShowRemoveDialog(testUser))
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.CancelRemove)

            val state = viewModel.uiState.value as BlacklistUiState.Success
            assertEquals("itemToRemove должен быть null", null, state.itemToRemove)
            assertEquals("showRemoveDialog должно быть false", false, state.showRemoveDialog)
        }

    @Test
    fun removeFromBlacklist_onSuccess_shouldShowSuccessAlert() =
        runTest {
            val testName = "Test User"
            val testUser = User(id = 404L, name = testName, image = null)
            coEvery { swRepository.getBlacklistFlow() } returns flowOf(listOf(testUser))
            coEvery {
                swRepository.blacklistAction(any<User>(), any<ApiBlacklistOption>())
            } returns Result.success(Unit)

            val viewModel = BlacklistViewModel(swRepository, logger, userNotifier)
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.ShowRemoveDialog(testUser))
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.Remove(testUser))
            advanceUntilIdle()

            val state = viewModel.uiState.value as BlacklistUiState.Success
            assertTrue(
                "showSuccessAlert должно быть true после успешного разблокирования",
                state.showSuccessAlert
            )
            assertEquals(
                "unblockedUserName должно содержать имя пользователя",
                testName,
                state.unblockedUserName
            )
        }

    @Test
    fun dismissSuccessAlert_shouldClearSuccessState() =
        runTest {
            val testName = "Test User"
            val testUser = User(id = 505L, name = testName, image = null)
            coEvery { swRepository.getBlacklistFlow() } returns flowOf(listOf(testUser))
            coEvery {
                swRepository.blacklistAction(any<User>(), ApiBlacklistOption.REMOVE)
            } returns Result.success(Unit)

            val viewModel = BlacklistViewModel(swRepository, logger, userNotifier)
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.ShowRemoveDialog(testUser))
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.Remove(testUser))
            advanceUntilIdle()

            viewModel.onAction(BlacklistAction.DismissSuccessAlert)

            val state = viewModel.uiState.value as BlacklistUiState.Success
            assertEquals("showSuccessAlert должно быть false", false, state.showSuccessAlert)
            assertEquals("unblockedUserName должно быть null", null, state.unblockedUserName)
        }
}
