package com.swparks.data.interceptor

import com.swparks.util.Logger
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor для автоматического повторения запросов при временных ошибках.
 * Повторяет запросы при ошибках сервера (502, 503, 504) и сетевых ошибках (IOException).
 *
 * @param logger Логгер для записи попыток retry
 * @throws IOException Если все попытки исчерпаны
 */
class RetryInterceptor(private val logger: Logger) : Interceptor {

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val HTTP_BAD_GATEWAY = 502
        private const val HTTP_SERVICE_UNAVAILABLE = 503
        private const val HTTP_GATEWAY_TIMEOUT = 504
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                // Выполняем запрос
                response?.close()  // Закрываем предыдущий response если есть
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
                        // continue на следующую итерацию
                    }
                } else {
                    // Другие коды ошибок (404, 401, 500 и т.д.) - НЕ retry
                    return response
                }

            } catch (e: CancellationException) {
                // Отмена корутины - НЕ ретраить, пробрасываем дальше
                // Это означает, что пользователь ушел или экран закрыт
                throw e
            } catch (e: IOException) {
                // Сетевая ошибка - retry
                lastException = e
                logger.w(
                    "RetryInterceptor",
                    "Ошибка сети: ${e.message}, попытка ${attempt + 1}/$MAX_RETRIES"
                )

                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAY_MS)
                    // continue на следующую итерацию
                }
            }
        }

        // Все попытки исчерпаны
        // Возвращаем последний response или выбрасываем исключение
        return response ?: throw (lastException as Throwable)
    }
}
