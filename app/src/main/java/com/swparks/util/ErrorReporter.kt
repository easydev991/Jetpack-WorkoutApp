package com.swparks.util

import com.swparks.model.AppError
import kotlinx.coroutines.flow.SharedFlow

/**
 * Интерфейс для обработки и отправки ошибок в UI-слой.
 *
 * Используется во всех ViewModels для централизованной обработки ошибок.
 * Реализация (ErrorHandler) логирует ошибки и отправляет их в SharedFlow
 * для отображения пользователю через Snackbar/Toast/Alert.
 *
 * @see ErrorHandler
 * @see AppError
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
     * Обрабатывает ошибку: логирует и отправляет в поток.
     *
     * Метод предназначен для вызова из ViewModels при возникновении ошибок.
     * Логирует ошибку через Logger и отправляет её в errorFlow для отображения в UI.
     *
     * @param error Ошибка для обработки
     * @return true если ошибка успешно отправлена в поток, false если буфер переполнен
     */
    fun handleError(error: AppError): Boolean
}
