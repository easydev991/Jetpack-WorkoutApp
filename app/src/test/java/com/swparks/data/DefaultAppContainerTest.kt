package com.swparks.data

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Тесты для проверки интеграции UserNotifier в AppContainer.
 *
 * Проверяет, что UserNotifier корректно создается в DefaultAppContainer.
 *
 * Примечание: полноценное тестирование DI-контейнера требует instrumented-тестов,
 * т.к. DefaultAppContainer создает реальные экземпляры с Android-зависимостями.
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
        val container = DefaultAppContainer(mockContext)
        assert(container.userNotifier is com.swparks.util.UserNotifierImpl) {
            "userNotifier должен быть реализацией UserNotifierImpl"
        }
    }
}
