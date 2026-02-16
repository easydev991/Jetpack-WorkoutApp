package com.swparks.util

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit-тесты для UserNotifierImpl.
 *
 * Проверяют логирование и отправку ошибок в SharedFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserNotifierImplTest {
    private lateinit var mockLogger: Logger
    private lateinit var userNotifier: UserNotifierImpl

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        userNotifier = UserNotifierImpl(mockLogger)
    }

    @Test
    fun handle_error_logs_network_error() = runTest {
        val error = AppError.Network("Нет подключения")

        userNotifier.handleError(error)

        verify {
            mockLogger.e("UserNotifier", "Сетевая ошибка: Нет подключения", null)
        }
    }

    @Test
    fun handle_error_logs_network_error_with_throwable() = runTest {
        val exception = IOException("Connection timeout")
        val error = AppError.Network("Таймаут соединения", throwable = exception)

        userNotifier.handleError(error)

        verify {
            mockLogger.e("UserNotifier", "Сетевая ошибка: Таймаут соединения", exception)
        }
    }

    @Test
    fun handle_error_logs_validation_error() = runTest {
        val error = AppError.Validation("Обязательное поле", field = "email")

        userNotifier.handleError(error)

        verify {
            mockLogger.w("UserNotifier", "Ошибка валидации: Обязательное поле")
        }
    }

    @Test
    fun handle_error_logs_server_error() = runTest {
        val error = AppError.Server("Внутренняя ошибка сервера", code = 500)

        userNotifier.handleError(error)

        verify {
            mockLogger.e("UserNotifier", "Ошибка сервера (500): Внутренняя ошибка сервера")
        }
    }

    @Test
    fun handle_error_logs_generic_error() = runTest {
        val error = AppError.Generic("Неизвестная ошибка")

        userNotifier.handleError(error)

        verify {
            mockLogger.e("UserNotifier", "Общая ошибка: Неизвестная ошибка", null)
        }
    }

    @Test
    fun handle_error_logs_generic_error_with_throwable() = runTest {
        val exception = RuntimeException("Unexpected error")
        val error = AppError.Generic("Критическая ошибка", throwable = exception)

        userNotifier.handleError(error)

        verify {
            mockLogger.e("UserNotifier", "Общая ошибка: Критическая ошибка", exception)
        }
    }

    @Test
    fun handle_error_returns_true_when_emitted_successfully() = runTest {
        val error = AppError.Generic("Ошибка")

        val result = userNotifier.handleError(error)

        assertTrue(result)
    }

    @Test
    fun show_info_logs_message() = runTest {
        val message = "Операция успешно выполнена"

        userNotifier.showInfo(message)

        verify {
            mockLogger.i("UserNotifier", "Инфо: $message")
        }
    }

    @Test
    fun show_info_returns_true_when_emitted_successfully() = runTest {
        val message = "Операция успешно выполнена"

        val result = userNotifier.showInfo(message)

        assertTrue(result)
    }
}
