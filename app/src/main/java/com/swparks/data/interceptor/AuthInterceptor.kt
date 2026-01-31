package com.swparks.data.interceptor

import android.util.Log
import com.swparks.data.UserPreferencesRepository
import kotlinx.coroutines.sync.Mutex
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor для обработки ошибок авторизации (401)
 *
 * При получении ответа со статусом 401 (Unauthorized):
 * - Логирует ошибку авторизации
 * - Очищает токен авторизации
 * - Вызывает принудительный логаут пользователя
 *
 * ВАЖНО: AuthInterceptor срабатывает только на реальный HTTP 401,
 * не на эндпоинты авторизации (login, register, reset-password).
 */
class AuthInterceptor(
    private val preferencesRepository: UserPreferencesRepository
) : Interceptor {

    private companion object {
        private const val TAG = "AuthInterceptor"
        private const val UNAUTHORIZED_STATUS_CODE = 401
    }

    // Защита от гонок при логауте
    private var isLoggingOut = false
    private val logoutMutex = Mutex()

    /**
     * Перехватывает HTTP-ответы и обрабатывает ошибку 401
     *
     * @param chain цепочка перехватчиков
     * @return HTTP-ответ
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Проверяем статус код 401 (Unauthorized)
        if (response.code == UNAUTHORIZED_STATUS_CODE) {
            // Проверяем путь запроса - НЕ логаут для эндпоинтов авторизации
            val path = request.url.encodedPath
            val isAuthEndpoint = path.contains("/login") ||
                    path.contains("/register") ||
                    path.contains("/reset-password")

            if (!isAuthEndpoint) {
                Log.e(TAG, "Ошибка авторизации (401): токен недействителен или истек")
                Log.e(TAG, "URL запроса: ${request.url}")
                Log.e(TAG, "Путь запроса: $path")

                // Очищаем токен авторизации синхронно с защитой от гонок
                clearToken()
            } else {
                Log.d(TAG, "Игнорируем 401 для эндпоинта авторизации: $path")
            }
        }

        return response
    }

    /**
     * Очищает токен авторизации с защитой от гонок
     * Устанавливает флаг isAuthorized в false
     */
    private fun clearToken() {
        // Защита от гонок
        logoutMutex.tryLock().let { locked ->
            if (!locked) {
                Log.w(TAG, "Выполняется другой логаут, пропускаем")
                return
            }

            try {
                if (isLoggingOut) {
                    Log.w(TAG, "Уже выполняется логаут, пропускаем")
                    return
                }

                isLoggingOut = true
                preferencesRepository.clearToken()
                Log.i(TAG, "Токен авторизации очищен")
            } finally {
                isLoggingOut = false
                logoutMutex.unlock()
            }
        }
    }
}
