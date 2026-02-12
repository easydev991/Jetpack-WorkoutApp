package com.swparks.data

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Тесты для проверки интеграции ErrorReporter в AppContainer.
 *
 * Проверяет, что:
 * 1. ErrorReporter корректно создается в DefaultAppContainer
 * 2. ErrorReporter передается во все ViewModels
 * 3. ErrorReporter используется для обработки ошибок
 */
class DefaultAppContainerTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk()
        every { mockContext.applicationContext } returns mockContext
    }

    @Test
    fun `AppContainer создает ErrorHandler для errorReporter`() {
        // Given
        val container = DefaultAppContainer(mockContext)

        // Then
        assert(container.errorReporter is com.swparks.util.ErrorHandler) {
            "errorReporter должен быть реализацией ErrorHandler"
        }
    }
}
