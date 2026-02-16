package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.TextEntryOption

/**
 * Use case для добавления/редактирования комментариев и записей в дневнике.
 *
 * Делегирует вызовы в SWRepository, используя TextEntryOption для определения типа операции.
 * Добавляет логирование операций.
 *
 * @param swRepository Репозиторий для работы с API
 * @param createJournalUseCase Use case для создания дневника
 */
class TextEntryUseCase(
    private val swRepository: SWRepository,
    private val createJournalUseCase: ICreateJournalUseCase
) : ITextEntryUseCase {
    override suspend fun addJournalEntry(
        ownerId: Long,
        journalId: Long,
        text: String
    ): Result<Unit> {
        val option = TextEntryOption.Journal(ownerId, journalId, null)
        val result = swRepository.addComment(option, text)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Запись добавлена в дневник $journalId")
        }
        return result
    }

    override suspend fun editJournalEntry(
        ownerId: Long,
        journalId: Long,
        entryId: Long,
        text: String
    ): Result<Unit> {
        val option = TextEntryOption.Journal(ownerId, journalId, entryId)
        val result = swRepository.editComment(option, entryId, text)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Запись $entryId обновлена в дневнике $journalId")
        }
        return result
    }

    override suspend fun createJournal(userId: Long, title: String): Result<Unit> {
        val result = createJournalUseCase(userId, title)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Дневник создан для пользователя $userId")
        }
        return result
    }

    override suspend fun addParkComment(
        parkId: Long,
        text: String
    ): Result<Unit> {
        val option = TextEntryOption.Park(parkId)
        val result = swRepository.addComment(option, text)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Комментарий добавлен к площадке $parkId")
        }
        return result
    }

    override suspend fun editParkComment(
        parkId: Long,
        commentId: Long,
        text: String
    ): Result<Unit> {
        val option = TextEntryOption.Park(parkId)
        val result = swRepository.editComment(option, commentId, text)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Комментарий $commentId обновлен на площадке $parkId")
        }
        return result
    }

    override suspend fun addEventComment(
        eventId: Long,
        text: String
    ): Result<Unit> {
        val option = TextEntryOption.Event(eventId)
        val result = swRepository.addComment(option, text)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Комментарий добавлен к мероприятию $eventId")
        }
        return result
    }

    override suspend fun editEventComment(
        eventId: Long,
        commentId: Long,
        text: String
    ): Result<Unit> {
        val option = TextEntryOption.Event(eventId)
        val result = swRepository.editComment(option, commentId, text)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Комментарий $commentId обновлен на мероприятии $eventId")
        }
        return result
    }

    override suspend fun sendMessageTo(userId: Long, message: String): Result<Unit> {
        val result = swRepository.sendMessage(message, userId)
        result.onSuccess {
            Log.i("TextEntryUseCase", "Сообщение отправлено пользователю $userId")
        }
        return result
    }
}
