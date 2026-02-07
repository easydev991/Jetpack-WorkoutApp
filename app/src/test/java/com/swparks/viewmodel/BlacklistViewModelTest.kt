package com.swparks.viewmodel

import com.swparks.data.repository.SWRepository
import com.swparks.model.ApiBlacklistOption
import com.swparks.model.User
import com.swparks.ui.viewmodel.MainDispatcherRule
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
 * Unit тесты для BlacklistViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlacklistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var errorReporter: ErrorReporter
    private lateinit var blacklistViewModel: BlacklistViewModel

    @Before
    fun setup() {
        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        errorReporter = mockk(relaxed = true)
        blacklistViewModel = BlacklistViewModel(
            swRepository,
            logger,
            errorReporter
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
        val state = blacklistViewModel.uiState.value
        assertTrue("Начальное состояние должно быть Loading", state is BlacklistUiState.Loading)
    }

    @Test
    fun uiState_whenDataLoaded_shouldReturnSuccess() = runTest {
        // Given
        val testUser1 = mockk<User>(relaxed = true)
        val testUser2 = mockk<User>(relaxed = true)
        val testList = listOf(testUser1, testUser2)

        coEvery { swRepository.getBlacklistFlow() } returns flowOf(testList)

        // When
        val viewModel = BlacklistViewModel(
            swRepository,
            logger,
            errorReporter
        )
        advanceUntilIdle()

        // Then
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
    fun showRemoveDialog_shouldSetItemToRemoveAndShowDialog() = runTest {
        // Given
        val testUser = mockk<User>(relaxed = true).apply {
            every { id } returns 789L
        }

        // When
        blacklistViewModel.showRemoveDialog(testUser)
        advanceUntilIdle()

        // Then
        assertEquals(
            "itemToRemove должен быть установлен",
            testUser,
            blacklistViewModel.itemToRemove.value
        )
        assertEquals(
            "showRemoveDialog должно быть true",
            true,
            blacklistViewModel.showRemoveDialog.value
        )
    }

    @Test
    fun removeFromBlacklist_shouldCallRepositoryAndClearState() = runTest {
        // Given
        val testUser = mockk<User>(relaxed = true).apply {
            every { id } returns 101L
        }
        coEvery {
            swRepository.blacklistAction(testUser, ApiBlacklistOption.REMOVE)
        } returns Result.success(Unit)

        blacklistViewModel.showRemoveDialog(testUser)
        advanceUntilIdle()

        // When
        blacklistViewModel.removeFromBlacklist(testUser)

        // Then - проверяем СРАЗУ после вызова (до advanceUntilIdle)
        assertTrue(
            "isRemoving должен быть true сразу после вызова",
            blacklistViewModel.isRemoving.value
        )

        // Ждем завершения операции
        advanceUntilIdle()

        // После завершения
        coVerify(exactly = 1) {
            swRepository.blacklistAction(testUser, ApiBlacklistOption.REMOVE)
        }
        assertEquals(
            "itemToRemove должен быть null после удаления",
            null,
            blacklistViewModel.itemToRemove.value
        )
        assertEquals(
            "showRemoveDialog должно быть false после удаления",
            false,
            blacklistViewModel.showRemoveDialog.value
        )
        assertEquals(
            "isRemoving должен быть false после завершения",
            false,
            blacklistViewModel.isRemoving.value
        )
    }

    @Test
    fun removeFromBlacklist_onError_shouldClearStateAndReportError() = runTest {
        // Given
        val testUser = mockk<User>(relaxed = true).apply {
            every { id } returns 202L
        }
        val testException = Exception("API Error")
        coEvery {
            swRepository.blacklistAction(testUser, ApiBlacklistOption.REMOVE)
        } returns Result.failure(testException)

        blacklistViewModel.showRemoveDialog(testUser)
        advanceUntilIdle()

        // When
        blacklistViewModel.removeFromBlacklist(testUser)

        // Then - проверяем СРАЗУ после вызова (до advanceUntilIdle)
        assertTrue(
            "isRemoving должен быть true сразу после вызова",
            blacklistViewModel.isRemoving.value
        )

        // Ждем завершения операции (включая задержку delay(2000))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            swRepository.blacklistAction(testUser, ApiBlacklistOption.REMOVE)
        }
        assertEquals(
            "itemToRemove должен быть очищен даже при ошибке",
            null,
            blacklistViewModel.itemToRemove.value
        )
        assertEquals(
            "showRemoveDialog должно быть false даже при ошибке",
            false,
            blacklistViewModel.showRemoveDialog.value
        )
        assertEquals(
            "isRemoving должен быть false после ошибки",
            false,
            blacklistViewModel.isRemoving.value
        )
    }

    @Test
    fun cancelRemove_shouldClearState() {
        // Given
        val testUser = mockk<User>(relaxed = true).apply {
            every { id } returns 303L
        }
        blacklistViewModel.showRemoveDialog(testUser)

        // When
        blacklistViewModel.cancelRemove()

        // Then
        assertEquals("itemToRemove должен быть null", null, blacklistViewModel.itemToRemove.value)
        assertEquals(
            "showRemoveDialog должно быть false",
            false,
            blacklistViewModel.showRemoveDialog.value
        )
    }

    @Test
    fun removeFromBlacklist_onSuccess_shouldShowSuccessAlert() = runTest {
        // Given
        val testName = "Test User"
        val testUser = mockk<User>(relaxed = true).apply {
            every { id } returns 404L
            every { name } returns testName
        }
        coEvery {
            swRepository.blacklistAction(testUser, ApiBlacklistOption.REMOVE)
        } returns Result.success(Unit)

        blacklistViewModel.showRemoveDialog(testUser)
        advanceUntilIdle()

        // When
        blacklistViewModel.removeFromBlacklist(testUser)
        advanceUntilIdle()

        // Then
        assertEquals(
            "showSuccessAlert должно быть true после успешного разблокирования",
            true,
            blacklistViewModel.showSuccessAlert.value
        )
        assertEquals(
            "unblockedUserName должно содержать имя пользователя",
            testName,
            blacklistViewModel.unblockedUserName.value
        )
    }

    @Test
    fun dismissSuccessAlert_shouldClearSuccessState() {
        // Given
        blacklistViewModel.showRemoveDialog(mockk(relaxed = true))
        // Устанавливаем состояние, как будто алерт уже показан
        // Прямая установка состояния для теста
        val testViewModel = BlacklistViewModel(swRepository, logger, errorReporter)
        testViewModel.showRemoveDialog(mockk(relaxed = true))
        testViewModel.removeFromBlacklist(mockk(relaxed = true))

        // When
        testViewModel.dismissSuccessAlert()

        // Then
        assertEquals(
            "showSuccessAlert должно быть false",
            false,
            testViewModel.showSuccessAlert.value
        )
        assertEquals(
            "unblockedUserName должно быть null",
            null,
            testViewModel.unblockedUserName.value
        )
    }
}
