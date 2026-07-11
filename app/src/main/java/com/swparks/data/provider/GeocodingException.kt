package com.swparks.data.provider

import com.swparks.util.AppError

/**
 * Исключение геокодирования, содержащее доменную ошибку [AppError.GeocodingFailed].
 *
 * Используется внутри [GeocodingService] для передачи доменной ошибки через [Result.failure].
 *
 * @property appError Ошибка геокодирования
 */
class GeocodingException(
    val appError: AppError.GeocodingFailed
) : Exception(appError.message)
