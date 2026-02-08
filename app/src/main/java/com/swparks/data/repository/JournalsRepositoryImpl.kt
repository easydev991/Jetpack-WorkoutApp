package com.swparks.data.repository

import android.util.Log
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.model.toDomain
import com.swparks.domain.model.Journal
import com.swparks.domain.repository.JournalsRepository
import com.swparks.network.SWApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Реализация репозитория для работы с дневниками
 *
 * Кэширует дневники в локальной базе данных для офлайн-доступа
 * и обновляет их с сервера при вызове метода refreshJournals
 *
 * @property swApi API клиент для работы с сервером
 * @property journalDao DAO для работы с дневниками в Room
 */
class JournalsRepositoryImpl(
    private val swApi: SWApi,
    private val journalDao: JournalDao
) : JournalsRepository {

    companion object {
        private const val TAG = "JournalsRepository"
    }

    override fun observeJournals(userId: Long): Flow<List<Journal>> {
        return journalDao.getJournalsByUserId(userId)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun refreshJournals(userId: Long): Result<Unit> = try {
        Log.i(TAG, "Загружаем дневники пользователя с id: $userId")

        // Загружаем дневники с сервера
        val responses = swApi.getJournals(userId)
        Log.i(TAG, "Получено ${responses.size} дневников с сервера")

        // Мапим JournalResponse -> Journal -> JournalEntity
        val entities = responses.map { response ->
            val journal = response.toDomain()
            journal.toEntity()
        }

        // Используем транзакцию для очистки старых данных перед вставкой
        journalDao.deleteByUserId(userId)
        journalDao.insertAll(entities)

        Log.i(TAG, "Успешно сохранено ${entities.size} дневников в БД")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Ошибка при загрузке дневников: ${e.message}", e)
        Result.failure(e)
    }
}
