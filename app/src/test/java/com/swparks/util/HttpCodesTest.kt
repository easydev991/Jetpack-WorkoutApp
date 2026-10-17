package com.swparks.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guard-тест канонических значений HTTP-кодов: защищает единственный
 * источник констант от случайной порчи (локальные копии в проекте удалены).
 */
class HttpCodesTest {
    @Test
    fun httpCodes_whenRead_thenMatchCanonicalValues() {
        assertEquals(401, HTTP_UNAUTHORIZED)
        assertEquals(403, HTTP_FORBIDDEN)
        assertEquals(404, HTTP_NOT_FOUND)
        assertEquals(500, HTTP_INTERNAL_SERVER_ERROR)
        assertEquals(502, HTTP_BAD_GATEWAY)
        assertEquals(503, HTTP_SERVICE_UNAVAILABLE)
        assertEquals(504, HTTP_GATEWAY_TIMEOUT)
    }
}
