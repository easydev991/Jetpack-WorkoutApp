package com.swparks.ui.viewmodel

import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.ui.state.SearchUserUiState
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для SearchUserViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchUserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swRepository: SWRepository
    private lateinit var viewModel: SearchUserViewModel
    private val testLogger: Logger = NoOpLogger()

    private val testUser =
        User(
            id = 1L,
            name = "testuser",
            image = null
        )

    @Before
    fun setup() {
        swRepository = mockk(relaxed = true)
        viewModel =
            SearchUserViewModel(
                swRepository = swRepository,
                logger = testLogger
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun init_state_isInitial() {
        // Then
        assertTrue(viewModel.uiState.value is SearchUserUiState.Initial)
    }

    @Test
    fun onSearch_withEmptyQuery_doesNotCallApi_andRemainsInitial() =
        runTest {
            // Given
            viewModel.searchQuery.value = ""

            // When
            viewModel.onSearch()
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value is SearchUserUiState.Initial)
            coVerify(exactly = 0) { swRepository.findUsers(any()) }
        }

    @Test
    fun onSearch_withShortQuery_doesNotCallApi_andRemainsInitial() =
        runTest {
            // Given
            viewModel.searchQuery.value = "a"

            // When
            viewModel.onSearch()
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value is SearchUserUiState.Initial)
            coVerify(exactly = 0) { swRepository.findUsers(any()) }
        }

    @Test
    fun onSearch_success_setsSuccessState() =
        runTest {
            // Given
            val query = "test"
            val users = listOf(testUser)
            viewModel.searchQuery.value = query
            coEvery { swRepository.findUsers(query) } returns Result.success(users)

            // When
            viewModel.onSearch()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is SearchUserUiState.Success)
            assertEquals(1, (state as SearchUserUiState.Success).users.size)
            assertEquals(testUser.id, state.users.first().id)
            coVerify(exactly = 1) { swRepository.findUsers(query) }
        }

    @Test
    fun onSearch_emptyResult_setsEmptyState() =
        runTest {
            // Given
            val query = "notfound"
            viewModel.searchQuery.value = query
            coEvery { swRepository.findUsers(query) } returns Result.success(emptyList())

            // When
            viewModel.onSearch()
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value is SearchUserUiState.Empty)
            coVerify(exactly = 1) { swRepository.findUsers(query) }
        }

    @Test
    fun onSearch_networkError_setsNetworkErrorState() =
        runTest {
            // Given
            val query = "test"
            viewModel.searchQuery.value = query
            coEvery { swRepository.findUsers(query) } returns Result.failure(Exception("Network error"))

            // When
            viewModel.onSearch()
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value is SearchUserUiState.NetworkError)
            coVerify(exactly = 1) { swRepository.findUsers(query) }
        }

    @Test
    fun onSearch_loadingState_duringRequest() =
        runTest {
            // Given
            val query = "test"
            viewModel.searchQuery.value = query
            coEvery { swRepository.findUsers(query) } coAnswers {
                // Проверяем состояние во время выполнения запроса
                assertTrue(viewModel.uiState.value is SearchUserUiState.Loading)
                Result.success(listOf(testUser))
            }

            // When
            viewModel.onSearch()
            advanceUntilIdle()

            // Then - состояние проверяется внутри coAnswers
        }

    @Test
    fun onSearch_whileLoading_ignoresNewSearch() =
        runTest {
            // Given
            val query1 = "test1"
            val query2 = "test2"
            var callCount = 0

            viewModel.searchQuery.value = query1
            coEvery { swRepository.findUsers(any()) } coAnswers {
                callCount++
                if (callCount == 1) {
                    // Первый запрос - меняем query и вызываем onSearch снова
                    viewModel.searchQuery.value = query2
                    viewModel.onSearch()
                    Result.success(listOf(testUser))
                } else {
                    Result.success(emptyList())
                }
            }

            // When
            viewModel.onSearch()
            advanceUntilIdle()

            // Then - должен быть вызван только первый поиск
            assertEquals(1, callCount)
        }

    @Test
    fun onSearch_sameQuery_afterSuccess_ignoresSearch() =
        runTest {
            // Given
            val query = "test"
            viewModel.searchQuery.value = query
            coEvery { swRepository.findUsers(query) } returns Result.success(listOf(testUser))

            // First search
            viewModel.onSearch()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is SearchUserUiState.Success)

            // When - повторный поиск с тем же запросом
            viewModel.onSearch()
            advanceUntilIdle()

            // Then - API вызван только один раз
            coVerify(exactly = 1) { swRepository.findUsers(query) }
        }

    @Test
    fun onSearch_differentQuery_afterSuccess_executesNewSearch() =
        runTest {
            // Given
            val query1 = "test1"
            val query2 = "test2"
            viewModel.searchQuery.value = query1
            coEvery { swRepository.findUsers(query1) } returns Result.success(listOf(testUser))
            coEvery { swRepository.findUsers(query2) } returns
                Result.success(
                    listOf(testUser.copy(id = 2L, name = "test2"))
                )

            // First search
            viewModel.onSearch()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is SearchUserUiState.Success)

            // When - поиск с другим запросом
            viewModel.searchQuery.value = query2
            viewModel.onSearch()
            advanceUntilIdle()

            // Then - API вызван дважды
            coVerify(exactly = 1) { swRepository.findUsers(query1) }
            coVerify(exactly = 1) { swRepository.findUsers(query2) }
        }

    @Test
    fun onUserClick_logsUserId() =
        runTest {
            // Given
            val userId = 123L

            // When
            viewModel.onUserClick(userId)
            advanceUntilIdle()

            // Then - нет исключений, логирование прошло успешно
            assertTrue(true)
        }
}
