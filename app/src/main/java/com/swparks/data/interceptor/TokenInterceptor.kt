package com.swparks.data.interceptor

import com.swparks.data.SecureTokenRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor для добавления токена авторизации в заголовки HTTP запросов.
 *
 * Аналог iOS-реализации (RequestComponents.swift):
 * - Токен добавляется в заголовок Authorization: Basic {token}
 * - Заголовок добавляется только если токен существует и не пустой
 * - Без токена: если токена нет (пользователь не авторизован), заголовок не добавляется
 *
 * @param secureTokenRepository Репозиторий для получения токена авторизации
 */
class TokenInterceptor(
    private val secureTokenRepository: SecureTokenRepository
) : Interceptor {

    private companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val AUTHORIZATION_PREFIX = "Basic"
    }

    /**
     * Перехватывает HTTP-запросы и добавляет токен авторизации в заголовки.
     *
     * @param chain цепочка перехватчиков
     * @return HTTP-ответ
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Получаем токен авторизации
        val token = secureTokenRepository.getAuthTokenSync()?.trim()

        // Создаём новый запрос с токеном (если токен существует и не пустой)
        val requestWithToken = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header(AUTHORIZATION_HEADER, "$AUTHORIZATION_PREFIX $token")
                .build()
        } else {
            originalRequest
        }

        // Продолжаем выполнение запроса
        return chain.proceed(requestWithToken)
    }
}
