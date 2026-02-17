package com.swparks.data.interceptor

import com.swparks.util.Logger
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.UnknownHostException

/**
 * Interceptor для автоматического повторения запросов при временных ошибках.
 * Повторяет запросы только при ошибках сервера (502, 503, 504).
 * Сетевые ошибки (DNS, connection refused) НЕ ретраятся - они постоянные.
 *
 * @param logger Логгер для записи попыток retry
 * @throws IOException Если все попытки исчерпаны
 */
class RetryInterceptor(private val logger: Logger) : Interceptor {

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 500L
        private const val HTTP_BAD_GATEWAY = 502
        private const val HTTP_SERVICE_UNAVAILABLE = 503
        private const val HTTP_GATEWAY_TIMEOUT = 504
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                // Выполняем запрос
                response?.close()
                response = chain.proceed(request)

                // Успешный ответ - возвращаем сразу
                if (response.isSuccessful) {
                    return response
                }

                // Проверяем код ошибки
                val statusCode = response.code
                if (statusCode == HTTP_BAD_GATEWAY ||
                    statusCode == HTTP_SERVICE_UNAVAILABLE ||
                    statusCode == HTTP_GATEWAY_TIMEOUT
                ) {
                    // Временная ошибка сервера - retry
                    logger.w(
                        "RetryInterceptor",
                        "Ошибка сервера $statusCode, попытка ${attempt + 1}/$MAX_RETRIES"
                    )

                    if (attempt < MAX_RETRIES - 1) {
                        response.close()
                        Thread.sleep(RETRY_DELAY_MS)
                    }
                } else {
                    // Другие коды ошибок (404, 401, 500 и т.д.) - НЕ retry
                    return response
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                // DNS ошибка - НЕ ретраить
                throw e
            } catch (e: IOException) {
                // Другие сетевые ошибки - НЕ ретраим
                throw e
            }
        }

        // Все попытки исчерпаны - возвращаем последний response
        return checkNotNull(response) { "No response after retries" }
    }
}
