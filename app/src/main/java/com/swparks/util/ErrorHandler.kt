package com.swparks.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Реализация интерфейса ErrorReporter.
 *
 * Логирует все ошибки и отправляет их в SharedFlow для отображения в UI.
 * Использует MutableSharedFlow с буфером для неблокирующей отправки ошибок.
 *
 * @property logger Логгер для записи ошибок
 */
class ErrorHandler(
    private val logger: Logger,
) : ErrorReporter {
    private companion object {
        private const val TAG = "ErrorHandler"
        private const val ERROR_BUFFER_CAPACITY = 10
    }

    /**
     * Поток ошибок для подписки из UI-слоя.
     * Использует SharedFlow для поддержки множества подписчиков.
     */
    private val _errorFlow = MutableSharedFlow<AppError>(
        extraBufferCapacity = ERROR_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Публичный поток для подписки из UI-слоя.
     */
    override val errorFlow: SharedFlow<AppError> = _errorFlow.asSharedFlow()

    /**
     * Обрабатывает ошибку: логирует и отправляет в поток.
     *
     * Использует tryEmit для неблокирующей отправки без необходимости в launch.
     * Если буфер переполнен, старые ошибки отбрасываются (DROP_OLDEST).
     *
     * Логирует ошибки в соответствии с их типом:
     * - Network: ERROR уровень с throwable
     * - Validation: WARN уровень
     * - Server: ERROR уровень с кодом статуса
     * - Generic: ERROR уровень с throwable
     *
     * @param error Ошибка для обработки
     * @return true если ошибка успешно отправлена, false если буфер переполнен
     */
    override fun handleError(error: AppError): Boolean {
        // Логируем ошибку
        when (error) {
            is AppError.Network -> {
                logger.e(TAG, "Сетевая ошибка: ${error.message}", error.throwable)
            }

            is AppError.Validation -> {
                logger.w(TAG, "Ошибка валидации: ${error.message}")
            }

            is AppError.Server -> {
                logger.e(TAG, "Ошибка сервера (${error.code}): ${error.message}")
            }

            is AppError.Generic -> {
                logger.e(TAG, "Общая ошибка: ${error.message}", error.throwable)
            }
        }

        // Отправляем в поток (неблокирующая операция)
        return _errorFlow.tryEmit(error)
    }
}
