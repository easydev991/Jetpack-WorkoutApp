package com.swparks.util

import kotlinx.coroutines.flow.SharedFlow

/**
 * Интерфейс для обработки и отправки ошибок и уведомлений в UI-слой.
 *
 * Используется во всех ViewModels для централизованной обработки ошибок
 * и информационных сообщений.
 * Реализация (ErrorHandler) логирует и отправляет их в SharedFlow
 * для отображения пользователю через Snackbar/Toast/Alert.
 *
 * @see ErrorHandler
 * @see AppError
 * @see AppNotification
 */
interface ErrorReporter {
    /**
     * Поток ошибок для подписки из UI-слоя.
     *
     * Используется в RootScreen для отображения ошибок пользователю
     * через Snackbar или другой UI компонент.
     */
    val errorFlow: SharedFlow<AppError>

    /**
     * Поток уведомлений для подписки из UI-слоя.
     *
     * Используется в RootScreen для отображения информационных сообщений
     * (например, об успешных операциях) пользователю через Snackbar.
     */
    val notificationFlow: SharedFlow<AppNotification>

    /**
     * Обрабатывает ошибку: логирует и отправляет в поток.
     *
     * Метод предназначен для вызова из ViewModels при возникновении ошибок.
     * Логирует ошибку через Logger и отправляет её в errorFlow для отображения в UI.
     *
     * @param error Ошибка для обработки
     * @return true если ошибка успешно отправлена в поток, false если буфер переполнен
     */
    fun handleError(error: AppError): Boolean

    /**
     * Показывает информационное уведомление.
     *
     * Метод предназначен для вызова из ViewModels при успешных операциях
     * или других событиях, о которых нужно уведомить пользователя.
     * Логирует уведомление через Logger и отправляет его в notificationFlow.
     *
     * @param message Сообщение для отображения пользователю
     * @return true если уведомление успешно отправлено в поток, false если буфер переполнен
     */
    fun showInfo(message: String): Boolean
}
