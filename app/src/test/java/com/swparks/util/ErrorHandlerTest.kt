package com.swparks.util

import com.swparks.model.AppError
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit-тесты для ErrorHandler.
 *
 * Проверяют логирование и отправку ошибок в SharedFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ErrorHandlerTest {
    private lateinit var mockLogger: Logger
    private lateinit var errorHandler: ErrorHandler

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        errorHandler = ErrorHandler(mockLogger)
    }

    @Test
    fun handle_error_logs_network_error() = runTest {
        val error = AppError.Network("Нет подключения")

        errorHandler.handleError(error)

        verify {
            mockLogger.e("ErrorHandler", "Сетевая ошибка: Нет подключения", null)
        }
    }

    @Test
    fun handle_error_logs_network_error_with_throwable() = runTest {
        val exception = IOException("Connection timeout")
        val error = AppError.Network("Таймаут соединения", throwable = exception)

        errorHandler.handleError(error)

        verify {
            mockLogger.e("ErrorHandler", "Сетевая ошибка: Таймаут соединения", exception)
        }
    }

    @Test
    fun handle_error_logs_validation_error() = runTest {
        val error = AppError.Validation("Обязательное поле", field = "email")

        errorHandler.handleError(error)

        verify {
            mockLogger.w("ErrorHandler", "Ошибка валидации: Обязательное поле")
        }
    }

    @Test
    fun handle_error_logs_server_error() = runTest {
        val error = AppError.Server("Внутренняя ошибка сервера", code = 500)

        errorHandler.handleError(error)

        verify {
            mockLogger.e("ErrorHandler", "Ошибка сервера (500): Внутренняя ошибка сервера")
        }
    }

    @Test
    fun handle_error_logs_generic_error() = runTest {
        val error = AppError.Generic("Неизвестная ошибка")

        errorHandler.handleError(error)

        verify {
            mockLogger.e("ErrorHandler", "Общая ошибка: Неизвестная ошибка", null)
        }
    }

    @Test
    fun handle_error_logs_generic_error_with_throwable() = runTest {
        val exception = RuntimeException("Unexpected error")
        val error = AppError.Generic("Критическая ошибка", throwable = exception)

        errorHandler.handleError(error)

        verify {
            mockLogger.e("ErrorHandler", "Общая ошибка: Критическая ошибка", exception)
        }
    }

    @Test
    fun handle_error_returns_true_when_emitted_successfully() = runTest {
        val error = AppError.Generic("Ошибка")

        val result = errorHandler.handleError(error)

        assertTrue(result)
    }
}
