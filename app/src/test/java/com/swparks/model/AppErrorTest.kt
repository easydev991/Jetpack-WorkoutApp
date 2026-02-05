package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 * Unit-тесты для AppError модели ошибок.
 *
 * Проверяет создание разных типов ошибок и их свойства.
 */
class AppErrorTest {

    @Test
    fun testNetworkError() {
        val error = AppError.Network("Нет подключения")

        // Проверяем сообщение
        assertEquals("Нет подключения", error.message)

        // Проверяем, что throwable равен null по умолчанию
        assertNull(error.throwable)
    }

    @Test
    fun testNetworkErrorWithThrowable() {
        val exception = IOException("Connection timeout")
        val error = AppError.Network("Таймаут соединения", throwable = exception)

        // Проверяем сообщение
        assertEquals("Таймаут соединения", error.message)

        // Проверяем, что throwable сохранен
        assertNotNull(error.throwable)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun testValidationErrorWithField() {
        val error = AppError.Validation("Обязательное поле", field = "email")

        // Проверяем сообщение
        assertEquals("Обязательное поле", error.message)

        // Проверяем поле
        assertEquals("email", error.field)
    }

    @Test
    fun testValidationErrorWithoutField() {
        val error = AppError.Validation("Некорректный формат данных")

        // Проверяем сообщение
        assertEquals("Некорректный формат данных", error.message)

        // Проверяем, что field равен null по умолчанию
        assertNull(error.field)
    }

    @Test
    fun testServerErrorWithCode() {
        val error = AppError.Server("Внутренняя ошибка сервера", code = 500)

        // Проверяем сообщение
        assertEquals("Внутренняя ошибка сервера", error.message)

        // Проверяем код ошибки
        assertEquals(500, error.code)
    }

    @Test
    fun testServerErrorWithoutCode() {
        val error = AppError.Server("Ошибка сервера")

        // Проверяем сообщение
        assertEquals("Ошибка сервера", error.message)

        // Проверяем, что code равен null по умолчанию
        assertNull(error.code)
    }

    @Test
    fun testGenericError() {
        val error = AppError.Generic("Неизвестная ошибка")

        // Проверяем сообщение
        assertEquals("Неизвестная ошибка", error.message)

        // Проверяем, что throwable равен null по умолчанию
        assertNull(error.throwable)
    }

    @Test
    fun testGenericErrorWithThrowable() {
        val exception = RuntimeException("Unexpected error")
        val error = AppError.Generic("Критическая ошибка", throwable = exception)

        // Проверяем сообщение
        assertEquals("Критическая ошибка", error.message)

        // Проверяем, что throwable сохранен
        assertNotNull(error.throwable)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun testServerErrorWithDifferentCodesNotEqual() {
        val error500 = AppError.Server("Ошибка", code = 500)
        val error404 = AppError.Server("Ошибка", code = 404)

        // Проверяем, что ошибки с разными кодами не равны
        assertNotEquals(error500, error404)
    }

    @Test
    fun testValidationErrorWithDifferentFieldsNotEqual() {
        val errorEmail = AppError.Validation("Ошибка", field = "email")
        val errorPassword = AppError.Validation("Ошибка", field = "password")

        // Проверяем, что ошибки с разными полями не равны
        assertNotEquals(errorEmail, errorPassword)
    }

    @Test
    fun testSameErrorsEqual() {
        val error1 = AppError.Network("Ошибка сети")
        val error2 = AppError.Network("Ошибка сети")

        // Проверяем, что одинаковые ошибки равны
        assertEquals(error1, error2)
    }

    @Test
    fun testErrorHashCode() {
        val error1 = AppError.Validation("Ошибка", field = "email")
        val error2 = AppError.Validation("Ошибка", field = "email")
        val error3 = AppError.Validation("Ошибка", field = "password")

        // Проверяем, что одинаковые ошибки имеют одинаковый hashCode
        assertEquals(error1.hashCode(), error2.hashCode())

        // Проверяем, что разные ошибки имеют разные hashCode
        assertNotEquals(error1.hashCode(), error3.hashCode())
    }
}
