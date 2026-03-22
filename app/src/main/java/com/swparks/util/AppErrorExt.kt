package com.swparks.util

import android.content.Context
import com.swparks.R
import java.io.IOException

/**
 * Расширение для преобразования AppError в локализованное сообщение.
 *
 * Использует строковые ресурсы для получения локализованных сообщений.
 *
 * @param context Контекст для доступа к строковым ресурсам
 * @return Локализованное сообщение об ошибке
 */
fun AppError.toUiText(context: Context): String {
    return when (this) {
        is AppError.Network -> toNetworkUiText(context)
        is AppError.Validation -> toValidationUiText(context)
        is AppError.Server -> toServerUiText(context)
        is AppError.Generic -> message
        is AppError.LocationFailed -> toLocationFailedUiText(context)
        is AppError.GeocodingFailed -> toGeocodingUiText(context)
    }
}

/**
 * Преобразует сетевую ошибку в локализованное сообщение.
 */
private fun AppError.Network.toNetworkUiText(context: Context): String {
    return when {
        throwable is IOException -> context.getString(R.string.error_network_io)
        else -> context.getString(R.string.error_network_general)
    }
}

/**
 * Преобразует ошибку валидации в локализованное сообщение.
 */
private fun AppError.Validation.toValidationUiText(context: Context): String {
    return when (field) {
        "email" -> context.getString(R.string.error_validation_email)
        "password" -> context.getString(R.string.error_validation_password)
        else -> {
            if (field != null) {
                context.getString(R.string.error_validation_field, field)
            } else {
                message
            }
        }
    }
}

/**
 * Преобразует ошибку сервера в локализованное сообщение.
 */
private fun AppError.Server.toServerUiText(context: Context): String {
    return when (code) {
        HTTP_UNAUTHORIZED -> context.getString(R.string.error_server_unauthorized)
        HTTP_FORBIDDEN -> context.getString(R.string.error_server_forbidden)
        HTTP_NOT_FOUND -> context.getString(R.string.error_server_not_found)
        HTTP_INTERNAL_SERVER_ERROR -> context.getString(R.string.error_server_internal)
        HTTP_SERVICE_UNAVAILABLE -> context.getString(R.string.error_server_unavailable)
        else -> context.getString(R.string.error_server_general)
    }
}

/**
 * Преобразует ошибку геокодирования в локализованное сообщение.
 */
private fun AppError.GeocodingFailed.toGeocodingUiText(context: Context): String {
    return when (kind) {
        AppError.GeocodingFailureKind.ADDRESS_BUILD_FAIL -> context.getString(R.string.geocoding_address_build_fail)
        AppError.GeocodingFailureKind.IO_ERROR -> context.getString(R.string.error_network_io)
    }
}

/**
 * Преобразует ошибку геолокации в локализованное сообщение.
 */
private fun AppError.LocationFailed.toLocationFailedUiText(context: Context): String {
    return if (cause is SecurityException) {
        context.getString(R.string.location_permission_need_in_settings)
    } else {
        context.getString(R.string.location_fetch_failed)
    }
}

/**
 * HTTP коды ошибок.
 */
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_INTERNAL_SERVER_ERROR = 500
private const val HTTP_SERVICE_UNAVAILABLE = 503
