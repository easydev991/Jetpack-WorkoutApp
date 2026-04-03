package com.swparks.util

/**
 * Модель уведомления приложения.
 *
 * Используется для отображения информационных сообщений пользователю
 * через глобальный Snackbar в RootScreen.
 *
 * В отличие от [AppError], предназначена для успешных операций
 * и информационных сообщений, а не для ошибок.
 *
 * @see UserNotifier
 * @see AppError
 */
sealed class AppNotification {
    /**
     * Сообщение уведомления для отображения пользователю.
     */
    abstract val message: String

    /**
     * Информационное сообщение.
     *
     * Используется для уведомлений об успешных операциях:
     * сохранение настроек, удаление элемента и т.д.
     *
     * @property message Сообщение для пользователя
     */
    data class Info(
        override val message: String
    ) : AppNotification()
}
