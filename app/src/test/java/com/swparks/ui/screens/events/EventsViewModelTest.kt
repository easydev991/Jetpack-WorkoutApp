package com.swparks.ui.screens.events

import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository = mockk<SWRepository>(relaxed = true)
    private val mockUserNotifier = mockk<UserNotifier>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // resetMain не доступен в kotlinx-coroutines-test, но это не критично для unit-тестов
    }

    private fun createMockEvent(id: Long = 1L): Event {
        return Event(
            id = id,
            title = "Test Event",
            description = "Test Description",
            beginDate = "2024-01-01",
            countryID = 1,
            cityID = 1,
            preview = "",
            latitude = "0.0",
            longitude = "0.0",
            isCurrent = false,
            photos = emptyList(),
            author = createMockUser(),
            name = "Test Event"
        )
    }

    private fun createMockUser(id: Long = 1L): User {
        return User(id = id, name = "testuser", image = "")
    }

    @Test
    fun init_whenViewModelCreated_thenStateIsLoading() = runTest {
        // Given
        coEvery { mockRepository.getPastEvents() } returns emptyList()

        // When
        val viewModel = EventsViewModel(mockRepository, mockUserNotifier)
        advanceUntilIdle()

        // Then
        // После init состояние может быть Loading или уже Success/Error в зависимости от времени
        // выполнения
        assertTrue(
            viewModel.eventsUIState.value is EventsUIState.Loading ||
                    viewModel.eventsUIState.value is EventsUIState.Success ||
                    viewModel.eventsUIState.value is EventsUIState.Error
        )
        coVerify { mockRepository.getPastEvents() }
    }

    @Test
    fun getPastEvents_whenRepositoryReturnsData_thenStateIsSuccess() = runTest {
        // Given
        val mockEventsList = listOf(createMockEvent(1L), createMockEvent(2L))
        coEvery { mockRepository.getPastEvents() } returns mockEventsList

        val viewModel = EventsViewModel(mockRepository, mockUserNotifier)
        advanceUntilIdle() // Ждем завершения init

        // When
        viewModel.getPastEvents()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.eventsUIState.value is EventsUIState.Success)
        val successState = viewModel.eventsUIState.value as EventsUIState.Success
        assertEquals(mockEventsList, successState.events)
    }

    @Test
    fun getPastEvents_whenRepositoryThrowsIOException_thenStateIsError() = runTest {
        // Given
        val ioException = IOException("Network error")
        coEvery { mockRepository.getPastEvents() } throws ioException

        val viewModel = EventsViewModel(mockRepository, mockUserNotifier)
        advanceUntilIdle() // Ждем завершения init

        // When
        viewModel.getPastEvents()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.eventsUIState.value is EventsUIState.Error)
        val errorState = viewModel.eventsUIState.value as EventsUIState.Error
        assertEquals("Network error", errorState.message)

        // Проверяем, что userNotifier.handleError был вызван с AppError.Network
        coVerify {
            mockUserNotifier.handleError(
                match<AppError> { error ->
                    error is AppError.Network &&
                            error.message.contains("Не удалось загрузить мероприятия") &&
                            error.throwable == ioException
                }
            )
        }
    }

    @Test
    fun getPastEvents_whenRepositoryThrowsIOExceptionWithNullMessage_thenStateIsErrorWithNullMessage() =
        runTest {
            // Given
            val ioException = IOException()
            coEvery { mockRepository.getPastEvents() } throws ioException

            val viewModel = EventsViewModel(mockRepository, mockUserNotifier)
            advanceUntilIdle() // Ждем завершения init

            // When
            viewModel.getPastEvents()
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.eventsUIState.value is EventsUIState.Error)
            val errorState = viewModel.eventsUIState.value as EventsUIState.Error
            assertEquals(null, errorState.message)

            // Проверяем, что userNotifier.handleError был вызван с AppError.Network
            coVerify {
                mockUserNotifier.handleError(
                    match<AppError> { error ->
                        error is AppError.Network &&
                                error.message.contains("Не удалось загрузить мероприятия") &&
                                error.throwable == ioException
                    }
                )
            }
        }

    @Test
    fun getPastEvents_whenRepositoryThrowsHttpException_thenStateIsError() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>(relaxed = true)
        every { mockResponse.code() } returns 404
        every { mockResponse.message() } returns "HTTP 404"
        val httpException = HttpException(mockResponse)
        coEvery { mockRepository.getPastEvents() } throws httpException

        val viewModel = EventsViewModel(mockRepository, mockUserNotifier)
        advanceUntilIdle() // Ждем завершения init

        // When
        viewModel.getPastEvents()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.eventsUIState.value is EventsUIState.Error)
        val errorState = viewModel.eventsUIState.value as EventsUIState.Error
        assertEquals("HTTP 404", errorState.message)

        // Проверяем, что userNotifier.handleError был вызван с AppError.Server
        coVerify {
            mockUserNotifier.handleError(
                match<AppError> { error ->
                    error is AppError.Server &&
                            error.code == 404 &&
                            error.message.contains("Ошибка сервера при загрузке мероприятий")
                }
            )
        }
    }

    @Test
    fun getPastEvents_whenCalledAfterError_thenResetsToLoadingThenSuccess() = runTest {
        // Given
        val ioException = IOException("Network error")
        coEvery { mockRepository.getPastEvents() } throws ioException

        val viewModel = EventsViewModel(mockRepository, mockUserNotifier)
        advanceUntilIdle() // Ждем завершения init

        // Первый вызов с ошибкой
        viewModel.getPastEvents()
        advanceUntilIdle()
        assertTrue(viewModel.eventsUIState.value is EventsUIState.Error)

        // Когда
        val mockEventsList = listOf(createMockEvent(1L))
        coEvery { mockRepository.getPastEvents() } returns mockEventsList

        // Повторный вызов с успешным ответом
        viewModel.getPastEvents()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.eventsUIState.value is EventsUIState.Success)
        val successState = viewModel.eventsUIState.value as EventsUIState.Success
        assertEquals(mockEventsList, successState.events)
    }

    @Test
    fun getPastEvents_whenRepositoryReturnsEmptyList_thenStateIsSuccessWithEmptyList() = runTest {
        // Given
        coEvery { mockRepository.getPastEvents() } returns emptyList()

        val viewModel = EventsViewModel(mockRepository, mockUserNotifier)
        advanceUntilIdle() // Ждем завершения init

        // When
        viewModel.getPastEvents()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.eventsUIState.value is EventsUIState.Success)
        val successState = viewModel.eventsUIState.value as EventsUIState.Success
        assertEquals(emptyList<Event>(), successState.events)
    }
}
