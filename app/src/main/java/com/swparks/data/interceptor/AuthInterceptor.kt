package com.swparks.data.interceptor

import android.util.Log
import com.swparks.data.UserPreferencesRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor для обработки ошибок авторизации (401)
 * 
 * При получении ответа со статусом 401 (Unauthorized):
 * - Логирует ошибку авторизации
 * - Очищает токен авторизации
 * - Вызывает принудительный логаут пользователя
 */
class AuthInterceptor(
    private val preferencesRepository: UserPreferencesRepository
) : Interceptor {

    private companion object {
        private const val TAG = "AuthInterceptor"
        private const val UNAUTHORIZED_STATUS_CODE = 401
    }

    /**
     * Перехватывает HTTP-ответы и обрабатывает ошибку 401
     * 
     * @param chain цепочка перехватчиков
     * @return HTTP-ответ
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // Проверяем статус код 401 (Unauthorized)
        if (response.code == UNAUTHORIZED_STATUS_CODE) {
            Log.e(TAG, "Ошибка авторизации (401): токен недействителен или истек")
            
            // Очищаем токен авторизации синхронно
            clearToken()
        }

        return response
    }

    /**
     * Очищает токен авторизации
     * Устанавливает флаг isAuthorized в false
     */
    private fun clearToken() {
        preferencesRepository.clearToken()
        Log.i(TAG, "Токен авторизации очищен")
    }
}
