package com.swparks.util

/**
 * Модель ошибки приложения.
 *
 * Используется для централизованной обработки ошибок во ViewModels
 * через UserNotifier интерфейс. Разные типы ошибок позволяют
 * различным образом обрабатывать и логировать ошибки.
 *
 * @see UserNotifier
 */
sealed class AppError {
    /**
     * Сообщение об ошибке для отображения пользователю.
     */
    abstract val message: String

    /**
     * Сетевая ошибка (отсутствие подключения, таймаут и т.д.).
     *
     * Используется для ошибок ввода-вывода сети: отсутствие интернета,
     * таймауты соединения, недоступность сервера.
     *
     * @property message Сообщение об ошибке для пользователя
     * @property throwable Оригинальное исключение для логирования (опционально)
     */
    data class Network(
        override val message: String,
        val throwable: Throwable? = null
    ) : AppError()

    /**
     * Ошибка валидации пользовательского ввода.
     *
     * Используется для ошибок при проверке данных, введенных пользователем:
     * пустые поля, некорректный формат email, короткий пароль и т.д.
     *
     * @property message Сообщение об ошибке для пользователя
     * @property field Имя поля, в котором обнаружена ошибка (опционально)
     */
    data class Validation(
        override val message: String,
        val field: String? = null
    ) : AppError()

    /**
     * Ошибка сервера (4xx, 5xx статусы).
     *
     * Используется для ошибок, возвращаемых сервером:
     * 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Internal Server Error.
     *
     * @property message Сообщение об ошибке для пользователя
     * @property code HTTP код ошибки (опционально)
     */
    data class Server(
        override val message: String,
        val code: Int? = null
    ) : AppError()

    /**
     * Общая ошибка (непредвиденная).
     *
     * Используется для всех остальных ошибок, которые не попадают
     * в категории Network, Validation или Server.
     *
     * @property message Сообщение об ошибке для пользователя
     * @property throwable Оригинальное исключение для логирования (опционально)
     */
    data class Generic(
        override val message: String,
        val throwable: Throwable? = null
    ) : AppError()

    /**
     * Ошибка получения геолокации.
     *
     * Используется, когда не удалось получить текущие координаты пользователя.
     * Отображается через `R.string.location_fetch_failed` или `R.string.location_permission_need_in_settings`.
     *
     * @property message Сообщение для логирования
     * @property cause Оригинальное исключение для маршрутизации
     */
    data class LocationFailed(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError()

    data class LocationDisabled(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError()

    /**
     * Ошибка геокодирования.
     *
     * Используется, когда не удалось определить адрес по координатам.
     * Отображается через `R.string.geocoding_address_build_fail` или `R.string.error_network_io`.
     *
     * @property message Сообщение для логирования
     * @property kind Тип ошибки геокодирования
     * @property cause Оригинальное исключение для логирования (опционально)
     */
    data class GeocodingFailed(
        override val message: String,
        val kind: GeocodingFailureKind,
        val cause: Throwable? = null
    ) : AppError()

    /**
     * Типы ошибок геокодирования.
     *
     * @property ADDRESS_BUILD_FAIL Не удалось построить адрес (объединяет noPlacemark и addressBuildFail)
     * @property IO_ERROR Ошибка ввода-вывода при геокодировании
     */
    enum class GeocodingFailureKind {
        ADDRESS_BUILD_FAIL,
        IO_ERROR
    }

    /**
     * Типы ресурсов для ошибки ResourceNotFound.
     */
    enum class ResourceType {
        PARK,
        EVENT
    }

    /**
     * Ресурс не найден (HTTP 404).
     *
     * Используется, когда запрашиваемый park или event не найден на сервере.
     *
     * @property resourceType Тип ресурса (PARK или EVENT)
     */
    data class ResourceNotFound(
        override val message: String,
        val resourceType: ResourceType
    ) : AppError()
}
