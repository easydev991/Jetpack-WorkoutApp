package com.swparks.util

/**
 * Интерфейс для отправки событий продуктовой аналитики.
 * Позволяет подменять реализацию в тестах и изолировать Firebase SDK.
 */
interface AnalyticsReporter {
    /**
     * Логирует просмотр экрана.
     *
     * @param screenName Короткое имя экрана
     * @param screenClass Полный маршрут/класс экрана
     */
    fun logScreenView(
        screenName: String,
        screenClass: String? = null
    )
}
