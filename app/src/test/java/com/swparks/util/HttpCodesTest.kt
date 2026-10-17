package com.swparks.util

import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты isInfraNotFound — признака инфраструктурного 404 (ServerException с кодом 503),
 * по которому ViewModels ветвятся вместо деструктивных действий.
 */
class HttpCodesTest {
    @Test
    fun isInfraNotFound_whenServerException503_thenReturnsTrue() {
        // Given — инфра-404: то, что возвращают infraNotFound() и handleHttpException
        val error = ServerException(message = "Сервер недоступен", code = HTTP_SERVICE_UNAVAILABLE)

        // Then
        assertTrue(error.isInfraNotFound())
    }

    @Test
    fun isInfraNotFound_whenServerExceptionOtherCode_thenReturnsFalse() {
        // Given — настоящий API-404 и прочие серверные коды — не инфраструктура
        // Then
        assertFalse(ServerException(message = "Пользователь не найден", code = HTTP_NOT_FOUND).isInfraNotFound())
        assertFalse(ServerException(message = "Ошибка сервера", code = HTTP_INTERNAL_SERVER_ERROR).isInfraNotFound())
    }

    @Test
    fun isInfraNotFound_whenOtherExceptionType_thenReturnsFalse() {
        // Given — сетевая ошибка без кода вообще
        // Then
        assertFalse(NetworkException(message = "Ошибка сети").isInfraNotFound())
    }
}
