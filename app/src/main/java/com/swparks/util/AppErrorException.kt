package com.swparks.util

/**
 * Обёртка для `AppError`, позволяющая использовать его в `Result.failure()`.
 *
 * `Result.failure()` требует `Throwable`, поэтому `AppError` нельзя передать напрямую.
 * Эта обёртка решает проблему транспортировки `AppError` через `Result`.
 *
 * @property appError Ошибка приложения
 */
class AppErrorException(
    val appError: AppError
) : Exception(appError.message)
