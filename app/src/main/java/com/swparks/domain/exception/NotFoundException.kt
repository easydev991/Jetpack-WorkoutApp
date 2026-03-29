package com.swparks.domain.exception

/**
 * Исключение для случаев, когда запрашиваемый ресурс не найден на сервере (HTTP 404).
 *
 * Используется для обработки 404 ошибок при загрузке детальной информации о park/event.
 *
 * @property resourceId ID ресурса, который не был найден
 */
sealed class NotFoundException : Exception() {
    abstract val resourceId: Long

    data class ParkNotFound(override val resourceId: Long) : NotFoundException()
    data class EventNotFound(override val resourceId: Long) : NotFoundException()
}
