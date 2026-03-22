package com.swparks.domain.usecase

import com.swparks.data.model.NewParkDraft

/**
 * Интерфейс для обработки create-flow после получения разрешения на геолокацию.
 * Создан для удобства тестирования и разделения ответственности.
 */
interface ICreateParkLocationHandler {
    /**
     * Обработать предоставление разрешения на геолокацию.
     * Получает текущие координаты и создаёт NewParkDraft для формы создания площадки.
     *
     * @return Result.success(NewParkDraft) с координатами в случае успеха
     *         Result.failure при ошибке (координаты критичны, навигация без них невозможна)
     */
    suspend operator fun invoke(): Result<NewParkDraft>
}
