package com.swparks.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Реализация интерфейса UserNotifier.
 *
 * Логирует все ошибки и уведомления и отправляет их в SharedFlow для отображения в UI.
 * Использует MutableSharedFlow с буфером для неблокирующей отправки.
 *
 * @property logger Логгер для записи ошибок и уведомлений
 */
class UserNotifierImpl(
    private val logger: Logger,
) : UserNotifier {
    private companion object {
        private const val TAG = "UserNotifier"
        private const val ERROR_BUFFER_CAPACITY = 10
        private const val NOTIFICATION_BUFFER_CAPACITY = 10
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
     * Публичный поток ошибок для подписки из UI-слоя.
     */
    override val errorFlow: SharedFlow<AppError> = _errorFlow.asSharedFlow()

    /**
     * Поток уведомлений для подписки из UI-слоя.
     * Использует SharedFlow для поддержки множества подписчиков.
     */
    private val _notificationFlow = MutableSharedFlow<AppNotification>(
        extraBufferCapacity = NOTIFICATION_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Публичный поток уведомлений для подписки из UI-слоя.
     */
    override val notificationFlow: SharedFlow<AppNotification> = _notificationFlow.asSharedFlow()

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

            is AppError.LocationFailed -> {
                logger.e(TAG, "Ошибка геолокации: ${error.message}")
            }

            is AppError.LocationDisabled -> {
                logger.e(TAG, "Геолокация устройства отключена: ${error.message}")
            }

            is AppError.GeocodingFailed -> {
                logger.e(TAG, "Ошибка геокодирования: ${error.message}")
            }

            is AppError.ResourceNotFound -> {
                logger.w(TAG, "Ресурс не найден: ${error.message}")
            }
        }

        // Отправляем в поток (неблокирующая операция)
        return _errorFlow.tryEmit(error)
    }

    /**
     * Показывает информационное уведомление.
     *
     * Использует tryEmit для неблокирующей отправки без необходимости в launch.
     * Если буфер переполнен, старые уведомления отбрасываются (DROP_OLDEST).
     *
     * @param message Сообщение для отображения пользователю
     * @return true если уведомление успешно отправлено, false если буфер переполнен
     */
    override fun showInfo(message: String): Boolean {
        logger.i(TAG, "Инфо: $message")
        return _notificationFlow.tryEmit(AppNotification.Info(message))
    }
}
