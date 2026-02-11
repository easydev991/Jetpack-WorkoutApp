package com.swparks.domain.usecase

import com.swparks.ui.model.JournalAccess

/**
 * Интерфейс use case для редактирования настроек дневника
 */
interface IEditJournalSettingsUseCase {
    /**
     * Редактировать настройки дневника
     *
     * После успешного редактирования необходимо перезагрузить дневники,
     * чтобы получить обновленные данные с сервера.
     *
     * @param journalId ID дневника
     * @param title Новое название дневника
     * @param userId ID пользователя
     * @param viewAccess Новый уровень доступа для просмотра
     * @param commentAccess Новый уровень доступа для комментариев
     * @return Result<Unit> результат операции
     */
    suspend operator fun invoke(
        journalId: Long,
        title: String,
        userId: Long?,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ): Result<Unit>
}
