package com.swparks.data.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException

/**
 * Interceptor для логирования HTTP запросов и ответов в Logcat.
 *
 * Логирует:
 * - URL запроса
 * - Метод и заголовки
 * - Тело запроса (если есть)
 * - Код ответа и время выполнения
 * - Тело ответа (если есть)
 */
class LoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Логируем запрос
        val requestLog = buildString {
            append("📤 REQUEST: ${request.method} ${request.url}")
            append("\n📋 Headers: ${request.headers}")

            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                append("\n📦 Body: ${buffer.readString(charset)}")
            }
        }
        Log.d(TAG, requestLog)

        val startTime = System.nanoTime()

        return try {
            val response = chain.proceed(request)
            val durationMs = (System.nanoTime() - startTime) / NANOS_IN_MILLIS

            // Логируем ответ
            val responseLog = buildString {
                append("📥 RESPONSE: ${response.code} ${request.url}")
                append("\n⏱ Duration: ${durationMs}ms")
                append("\n📋 Headers: ${response.headers}")

                response.body?.let { body ->
                    val source = body.source()
                    source.request(Long.MAX_VALUE)
                    val buffer = source.buffer
                    val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8

                    // Проверяем, что тело не слишком большое для логирования
                    if (buffer.size > MAX_LOG_SIZE) {
                        append("\n📦 Body: [too large to log, ${buffer.size} bytes]")
                    } else {
                        append("\n📦 Body: ${buffer.clone().readString(charset)}")
                    }
                }
            }
            Log.d(TAG, responseLog)

            response
        } catch (e: IOException) {
            val durationMs = (System.nanoTime() - startTime) / NANOS_IN_MILLIS
            Log.e(TAG, "❌ ERROR after ${durationMs}ms: ${request.method} ${request.url}", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "HTTP"
        private const val MAX_LOG_SIZE = 10_000L // 10KB max для логирования тела
        private const val NANOS_IN_MILLIS = 1_000_000L
    }
}
