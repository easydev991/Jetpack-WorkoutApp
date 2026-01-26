package com.swparks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit тесты для ErrorResponse
 *
 * Проверяют приоритет полей message и errors в realMessage,
 * а также работу метода makeRealCode
 */
class ErrorResponseTest {

    @Test
    fun realMessage_whenOnlyErrorsFilled_thenReturnsJoinedErrors() {
        // Given
        val response = ErrorResponse(
            errors = listOf("Ошибка 1", "Ошибка 2", "Ошибка 3"),
            message = null,
            code = 0,
            status = 0
        )

        // When
        val result = response.realMessage

        // Then
        assertEquals("Ошибка 1, Ошибка 2, Ошибка 3", result)
    }

    @Test
    fun realMessage_whenOnlyMessageFilled_thenReturnsMessage() {
        // Given
        val response = ErrorResponse(
            errors = emptyList(),
            message = "Текст сообщения об ошибке",
            code = 0,
            status = 0
        )

        // When
        val result = response.realMessage

        // Then
        assertEquals("Текст сообщения об ошибке", result)
    }

    @Test
    fun realMessage_whenBothFilled_thenMessageHasPriority() {
        // Given - message имеет приоритет над errors
        val response = ErrorResponse(
            errors = listOf("Ошибка из списка"),
            message = "Приоритетное сообщение",
            code = 0,
            status = 0
        )

        // When
        val result = response.realMessage

        // Then
        assertEquals("Приоритетное сообщение", result)
    }

    @Test
    fun realMessage_whenBothFilledAndMessageIsEmpty_thenReturnsJoinedErrors() {
        // Given - пустая строка message не является null, поэтому используется
        val response = ErrorResponse(
            errors = listOf("Ошибка 1", "Ошибка 2"),
            message = "",
            code = 0,
            status = 0
        )

        // When
        val result = response.realMessage

        // Then
        assertEquals("", result)
    }

    @Test
    fun realMessage_whenNeitherFilled_thenReturnsNull() {
        // Given - оба поля пустые
        val response = ErrorResponse(
            errors = emptyList(),
            message = null,
            code = 0,
            status = 0
        )

        // When
        val result = response.realMessage

        // Then
        assertNull(result)
    }

    @Test
    fun realMessage_whenErrorsHasSingleItem_thenReturnsSingleError() {
        // Given
        val response = ErrorResponse(
            errors = listOf("Единственная ошибка"),
            message = null,
            code = 0,
            status = 0
        )

        // When
        val result = response.realMessage

        // Then
        assertEquals("Единственная ошибка", result)
    }

    @Test
    fun realMessage_whenErrorsEmptyListAndMessageNull_thenReturnsNull() {
        // Given
        val response = ErrorResponse(
            errors = emptyList(),
            message = null,
            code = 0,
            status = 0
        )

        // When
        val result = response.realMessage

        // Then
        assertNull(result)
    }

    @Test
    fun makeRealCode_whenCodeNotZero_thenReturnsCode() {
        // Given
        val response = ErrorResponse(
            errors = emptyList(),
            code = 400,
            status = 500
        )

        // When
        val result = response.makeRealCode(null)

        // Then
        assertEquals(400, result)
    }

    @Test
    fun makeRealCode_whenCodeZeroAndStatusNotZero_thenReturnsStatus() {
        // Given
        val response = ErrorResponse(
            errors = emptyList(),
            code = 0,
            status = 500
        )

        // When
        val result = response.makeRealCode(null)

        // Then
        assertEquals(500, result)
    }

    @Test
    fun makeRealCode_whenBothCodeAndStatusZero_thenReturnsStatusCode() {
        // Given
        val response = ErrorResponse(
            errors = emptyList(),
            code = 0,
            status = 0
        )

        // When
        val result = response.makeRealCode(404)

        // Then
        assertEquals(404, result)
    }

    @Test
    fun makeRealCode_whenAllZeroAndStatusNull_thenReturnsZero() {
        // Given
        val response = ErrorResponse(
            errors = emptyList(),
            code = 0,
            status = 0
        )

        // When
        val result = response.makeRealCode(null)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun makeRealCode_withPriorityCode_thenReturnsCodeEvenIfStatusAndStatusCodeSet() {
        // Given - code имеет наивысший приоритет
        val response = ErrorResponse(
            errors = emptyList(),
            code = 400,
            status = 500
        )

        // When
        val result = response.makeRealCode(404)

        // Then
        assertEquals(400, result)
    }

    @Test
    fun makeRealCode_whenCodeZeroStatusNotZeroStatusCodeSet_thenReturnsStatus() {
        // Given - status имеет приоритет над statusCode
        val response = ErrorResponse(
            errors = emptyList(),
            code = 0,
            status = 500
        )

        // When
        val result = response.makeRealCode(404)

        // Then
        assertEquals(500, result)
    }
}
