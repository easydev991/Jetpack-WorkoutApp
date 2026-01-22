package com.swparks.domain.usecase

/**
 * Интерфейс для use case выхода из системы.
 * Создан для удобства тестирования ViewModels.
 */
interface ILogoutUseCase {
    suspend operator fun invoke()
}
