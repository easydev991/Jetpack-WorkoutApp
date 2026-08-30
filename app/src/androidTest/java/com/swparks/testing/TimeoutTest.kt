package com.swparks.testing

import org.junit.Rule
import org.junit.rules.Timeout

/**
 * Базовый класс для androidTest с per-test таймаутом.
 *
 * Защита от зависания отдельного `@Test` (см. `docs/ui-tests-optimization.md`,
 * Этап 1): каждый тест обрывается через [seconds] секунд, прогон
 * `connectedDebugAndroidTest` не блокируется навсегда.
 */
abstract class TimeoutTest(
    seconds: Long = 60
) {
    @get:Rule
    val timeoutRule: Timeout = Timeout.seconds(seconds)
}
