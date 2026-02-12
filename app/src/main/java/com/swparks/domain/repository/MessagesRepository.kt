package com.swparks.domain.repository

import com.swparks.data.database.entity.DialogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с диалогами
 *
 * Предоставляет Single Source of Truth (SSOT) для диалогов через Flow
 * и методы для обновления данных с сервера
 */
interface MessagesRepository {
    /**
     * Получить поток диалогов пользователя (SSOT)
     *
     * @return Flow со списком диалогов, отсортированным по дате последнего сообщения
     */
    val dialogs: Flow<List<DialogEntity>>

    /**
     * Обновить диалоги пользователя с сервера
     *
     * @return Result успеха или ошибки операции
     */
    suspend fun refreshDialogs(): Result<Unit>
}
