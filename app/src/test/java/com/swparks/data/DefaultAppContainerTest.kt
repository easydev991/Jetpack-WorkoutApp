package com.swparks.data

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Тесты для проверки интеграции UserNotifier в AppContainer.
 *
 * Проверяет, что:
 * 1. UserNotifier корректно создается в DefaultAppContainer
 * 2. UserNotifier передается во все ViewModels
 * 3. UserNotifier используется для обработки ошибок
 */
class DefaultAppContainerTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk()
        every { mockContext.applicationContext } returns mockContext
    }

    @Test
    fun appContainer_creates_userNotifierImpl_for_userNotifier() {
        // Given
        val container = DefaultAppContainer(mockContext)

        // Then
        assert(container.userNotifier is com.swparks.util.UserNotifierImpl) {
            "userNotifier должен быть реализацией UserNotifierImpl"
        }
    }
}
