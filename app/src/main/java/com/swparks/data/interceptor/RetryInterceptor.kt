package com.swparks.data.interceptor

import com.swparks.util.Logger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor для автоматического повторения запросов при временных ошибках.
 * Повторяет запросы только при ошибках сервера (502, 503, 504).
 * Сетевые ошибки (DNS, connection refused) НЕ ретраятся - они постоянные.
 *
 * @param logger Логгер для записи попыток retry
 */
class RetryInterceptor(private val logger: Logger) : Interceptor {

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 500L
        private const val HTTP_BAD_GATEWAY = 502
        private const val HTTP_SERVICE_UNAVAILABLE = 503
        private const val HTTP_GATEWAY_TIMEOUT = 504
        private val RETRY_CODES =
            setOf(HTTP_BAD_GATEWAY, HTTP_SERVICE_UNAVAILABLE, HTTP_GATEWAY_TIMEOUT)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null

        repeat(MAX_RETRIES) { attempt ->
            // Выполняем запрос
            response?.close()
            response = chain.proceed(request)

            // Проверяем код ответа
            val statusCode = response.code
            val shouldRetry = statusCode in RETRY_CODES

            if (!shouldRetry) {
                // Не требуется retry - возвращаем response
                return response
            }

            // Временная ошибка сервера - retry
            logger.w(
                "RetryInterceptor",
                "Ошибка сервера $statusCode, попытка ${attempt + 1}/$MAX_RETRIES"
            )

            if (attempt < MAX_RETRIES - 1) {
                response.close()
                Thread.sleep(RETRY_DELAY_MS)
            }
        }

        // Все попытки исчерпаны - возвращаем последний response
        return checkNotNull(response) { "No response after retries" }
    }
}
