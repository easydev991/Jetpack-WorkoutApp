package com.swparks.data.repository

import com.swparks.domain.exception.ServerException
import com.swparks.util.CrashReporter
import com.swparks.util.HTTP_NOT_FOUND
import com.swparks.util.Logger
import io.mockk.mockk
import io.mockk.verify
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/**
 * Unit тесты для BaseRepository.handleHttpException
 *
 * Проверяют различение реального API-404 и инфраструктурного 404:
 * JSON/пустое тело — обычный ServerException, HTML от прокси —
 * ServerException с кодом 503, мимо crash-отчётов.
 */
class BaseRepositoryHandleHttpExceptionTest {
    private lateinit var logger: Logger
    private lateinit var crashReporter: CrashReporter
    private lateinit var repository: TestRepository

    @Before
    fun setUp() {
        logger = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        repository = TestRepository(logger, crashReporter)
    }

    /** Доступ к protected handleHttpException для юнит-теста */
    private class TestRepository(
        logger: Logger,
        crashReporter: CrashReporter
    ) : BaseRepository(logger, crashReporter) {
        fun handle(e: HttpException): Exception = handleHttpException(e, "TestTag", "тест")
    }

    @Test
    fun handleHttpException_when404WithJsonBody_thenReturnsServerExceptionWithRealMessage() {
        // Given — JSON-тело как на проде (BaseController::notFound())
        val exception =
            testHttpException(404, """{"errors": ["площадка не найдена"]}""".toResponseBody(null))

        // When
        val result = repository.handle(exception)

        // Then
        assertTrue("Ожидался ServerException", result is ServerException)
        assertEquals("площадка не найдена", result.message)
        assertEquals(HTTP_NOT_FOUND, (result as ServerException).code)
    }

    @Test
    fun handleHttpException_when404WithHtmlBody_thenReturnsServerExceptionWithCode503() {
        // Given — HTML-страница от nginx (инфраструктурный 404)
        val html =
            "<html><head><title>404 Not Found</title></head><body>404 Not Found</body></html>"
                .toResponseBody("text/html".toMediaType())
        val exception = testHttpException(404, html)

        // When
        val result = repository.handle(exception)

        // Then — «Сервер недоступен», код 503 и мимо crash-отчётов (только breadcrumb)
        assertTrue("Ожидался ServerException", result is ServerException)
        assertEquals("Сервер недоступен", result.message)
        assertEquals(INFRA_NOT_FOUND_CODE, (result as ServerException).code)
        verify(exactly = 0) { crashReporter.logException(any(), any()) }
        verify { logger.e(any(), match<String> { it.contains("[infra_html_404]") }) }
        verify { crashReporter.log(match<String> { it.contains("[infra_html_404]") }) }
    }

    @Test
    fun handleHttpException_when404WithEmptyBody_thenReturnsInfraNotFound() {
        // Given — пустое тело: реальный API всегда кладёт errors/name, значит это инфраструктура
        val exception = testHttpException(404, "".toResponseBody(null))

        // When
        val result = repository.handle(exception)

        // Then
        assertTrue("Ожидался ServerException", result is ServerException)
        assertEquals("Сервер недоступен", result.message)
        assertEquals(INFRA_NOT_FOUND_CODE, (result as ServerException).code)
    }

    @Test
    fun handleHttpException_when500WithJsonBody_thenReturnsServerExceptionWithRealMessage() {
        // Given — регрессия: не-404 ответы продолжают парситься как раньше
        val exception =
            testHttpException(500, """{"errors": ["Внутренняя ошибка сервера"]}""".toResponseBody(null))

        // When
        val result = repository.handle(exception)

        // Then
        assertTrue("Ожидался ServerException", result is ServerException)
        assertEquals("Внутренняя ошибка сервера", result.message)
    }

    private companion object {
        const val INFRA_NOT_FOUND_CODE = 503
    }
}
