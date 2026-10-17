package com.swparks.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit тесты для isApiNotFoundBody
 *
 * Проверяют классификацию тела 404-ответа: JSON с полями API (errors/name) —
 * реальный API-404, HTML/битый JSON/пустое тело/чужой JSON без полей API —
 * инфраструктурный 404 (null).
 * Классификация выполняется по содержимому тела, Content-Type не проверяется.
 * Используется продовый file-level json из BaseRepository — тест проверяет
 * реальную конфигурацию парсера, а не собственную копию.
 */
class BaseRepositoryIsApiNotFoundBodyTest {
    @Test
    fun isApiNotFoundBody_whenJsonBody_thenReturnsParsedErrorResponse() {
        // Given — JSON-тело как на проде (BaseController::notFound())
        val body = """{"errors": ["площадка не найдена"]}""".toResponseBody(null)

        // When
        val result = isApiNotFoundBody(body)

        // Then
        assertEquals("площадка не найдена", result?.realMessage)
    }

    @Test
    fun isApiNotFoundBody_whenHtmlBody_thenReturnsNull() {
        // Given — HTML-страница от nginx (инфраструктурный 404)
        val body =
            "<html><head><title>404 Not Found</title></head><body>404 Not Found</body></html>"
                .toResponseBody("text/html".toMediaType())

        // When
        val result = isApiNotFoundBody(body)

        // Then
        assertNull("HTML-тело не должно считаться API-404", result)
    }

    @Test
    fun isApiNotFoundBody_whenNullBody_thenReturnsNull() {
        // When
        val result = isApiNotFoundBody(null)

        // Then — реальный API всегда кладёт errors/name; пустой 404 — от инфраструктуры
        assertNull("Пустой 404 не должен считаться API-404", result)
    }

    @Test
    fun isApiNotFoundBody_whenEmptyBody_thenReturnsNull() {
        // Given — пустое и blank-тело
        val emptyBody = "".toResponseBody(null)
        val blankBody = "   \n\t ".toResponseBody(null)

        // When
        val emptyResult = isApiNotFoundBody(emptyBody)
        val blankResult = isApiNotFoundBody(blankBody)

        // Then — реальный API всегда кладёт errors/name; пустой 404 — от инфраструктуры
        assertNull("Пустой 404 не должен считаться API-404", emptyResult)
        assertNull("Blank-404 не должен считаться API-404", blankResult)
    }

    @Test
    fun isApiNotFoundBody_whenMalformedJsonBody_thenReturnsNull() {
        // Given — обрезанный JSON
        val body = """{"errors": ["незакрытый""".toResponseBody(null)

        // When
        val result = isApiNotFoundBody(body)

        // Then
        assertNull("Битый JSON не должен считаться API-404", result)
    }

    @Test
    fun isApiNotFoundBody_whenJsonWithoutApiErrorFields_thenReturnsNull() {
        // Given — валидный JSON чужого прокси (формат Spring Boot) без полей
        // реального API (errors/message/name): кэш удалять нельзя
        val body =
            """{"timestamp":"2026-10-17T10:00:00.000+00:00","status":404,"error":"Not Found","path":"/api/v3/parks/1"}"""
                .toResponseBody(null)

        // When
        val result = isApiNotFoundBody(body)

        // Then
        assertNull("JSON без полей API не должен считаться API-404", result)
    }

    @Test
    fun isApiNotFoundBody_whenBodyParsesAsJsonRegardlessOfHeaders_thenReturnsParsedErrorResponse() {
        // Given — JSON-тело с Content-Type: text/html (классификация по содержимому)
        val body =
            """{"name": "Not Found", "message": "Страница не найдена.", "status": 404}"""
                .toResponseBody("text/html".toMediaType())

        // When
        val result = isApiNotFoundBody(body)

        // Then
        assertEquals("Страница не найдена.", result?.realMessage)
    }

    @Test
    fun isApiNotFoundBody_whenOnlyMessageField_thenReturnsNull() {
        // Given — message есть у любого прокси (Spring Boot, API Gateway): не признак нашего API
        val body =
            """{"message": "No endpoint /api/areas/5", "status": 404}"""
                .toResponseBody(null)

        // When
        val result = isApiNotFoundBody(body)

        // Then
        assertNull("message без errors/name не должен считаться API-404", result)
    }
}
