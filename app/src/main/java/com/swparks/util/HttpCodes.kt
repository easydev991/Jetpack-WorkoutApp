package com.swparks.util

import com.swparks.domain.exception.ServerException

/**
 * Единый источник HTTP-кодов проекта.
 *
 * Все сравнения с HTTP-статусами (BaseRepository, RetryInterceptor,
 * AppErrorExt, ViewModels) используют эти константы вместо локальных копий.
 */
internal const val HTTP_UNAUTHORIZED = 401
internal const val HTTP_FORBIDDEN = 403
internal const val HTTP_NOT_FOUND = 404
internal const val HTTP_INTERNAL_SERVER_ERROR = 500
internal const val HTTP_BAD_GATEWAY = 502
internal const val HTTP_SERVICE_UNAVAILABLE = 503
internal const val HTTP_GATEWAY_TIMEOUT = 504

/**
 * Инфраструктурный 404 (HTML/чужой JSON от прокси): репозитории мапят его
 * в [ServerException] с кодом 503. ViewModels ветвятся по этому признаку,
 * чтобы не выполнять деструктивные действия (удаление кэша, NavigateBack).
 */
internal fun Throwable.isInfraNotFound(): Boolean = this is ServerException && code == HTTP_SERVICE_UNAVAILABLE
