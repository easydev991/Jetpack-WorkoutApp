package com.swparks.screenshots

import com.swparks.JetpackWorkoutApplication
import kotlinx.coroutines.runBlocking

/**
 * Test-only Application for screenshot instrumentation.
 *
 * Important: must inherit [JetpackWorkoutApplication] because production UI code
 * casts `applicationContext` to this type to access `container`.
 */
class ScreenshotTestApplication : JetpackWorkoutApplication() {
    override fun onCreate() {
        super.onCreate()
        container = ScreenshotAppContainer(this)

        // Для сценария профиля в скриншотах стартуем как неавторизованный пользователь,
        // затем выполняем авторизацию через UI, как в iOS.
        runBlocking {
            container.userPreferencesRepository.clearCurrentUserId()
        }
    }
}
