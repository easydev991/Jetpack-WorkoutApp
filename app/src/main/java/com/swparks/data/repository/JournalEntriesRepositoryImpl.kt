package com.swparks.data.repository

import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.model.JournalEntryResponse
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.model.JournalEntry
import com.swparks.domain.repository.JournalEntriesRepository
import com.swparks.network.SWApi
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import com.swparks.domain.model.toDomain as journalEntryToDomain

/**
 * Реализация репозитория для работы с записями в дневнике
 *
 * Кэширует записи в локальной базе данных для офлайн-доступа
 * и обновляет их с сервера при вызове метода refreshJournalEntries
 *
 * @property swApi API клиент для работы с сервером
 * @property journalEntryDao DAO для работы с записями в Room
 */
class JournalEntriesRepositoryImpl(
    private val swApi: SWApi,
    private val journalEntryDao: JournalEntryDao,
    private val crashReporter: CrashReporter,
    private val logger: Logger
) : JournalEntriesRepository {
    private companion object {
        const val TAG = "JournalEntriesRepository"
        const val HTTP_NOT_FOUND = 404
    }

    override fun observeJournalEntries(
        userId: Long,
        journalId: Long
    ): Flow<List<JournalEntry>> {
        // Примечание: userId сохраняется для будущей синхронизации
        return journalEntryDao
            .getJournalEntriesByJournalId(journalId)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun refreshJournalEntries(
        userId: Long,
        journalId: Long
    ): Result<Unit> =
        try {
            logger.i(TAG, "Загружаем записи дневника: userId=$userId, journalId=$journalId")

            // Загружаем записи с сервера
            val responses = swApi.getJournalEntries(userId, journalId)
            logger.i(TAG, "Получено ${responses.size} записей с сервера")

            // Мапим JournalEntryResponse -> JournalEntry -> JournalEntryEntity
            val entities =
                responses.map { response: JournalEntryResponse ->
                    val entry = response.journalEntryToDomain()
                    entry.toEntity()
                }

            // Удаляем старые записи и вставляем новые
            journalEntryDao.deleteByJournalId(journalId)
            journalEntryDao.insertAll(entities)

            logger.i(TAG, "Успешно сохранено ${entities.size} записей в БД")
            Result.success(Unit)
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка при загрузке записей: ${e.message}", e)
            crashReporter.logException(e, "Ошибка загрузки записей дневника")
            Result.failure(e)
        } catch (e: HttpException) {
            logger.e(TAG, "HTTP ошибка при загрузке записей: ${e.code()} ${e.message()}", e)
            crashReporter.logException(e, "HTTP ошибка загрузки записей дневника: ${e.code()}")
            Result.failure(e)
        }

    override suspend fun deleteJournalEntry(
        userId: Long,
        journalId: Long,
        entryId: Long
    ): Result<Unit> =
        try {
            logger.i(TAG, "Удаление записи: userId=$userId, journalId=$journalId, entryId=$entryId")

            val response = swApi.deleteJournalEntry(userId, journalId, entryId)

            when {
                response.isSuccessful -> {
                    logger.i(TAG, "Запись успешно удалена на сервере")
                    journalEntryDao.deleteById(entryId)
                    Result.success(Unit)
                }

                response.code() == HTTP_NOT_FOUND -> {
                    // Запись уже удалена на сервере — синхронизируем локальный кэш
                    logger.i(TAG, "Запись уже удалена на сервере (404), удаляем из локального кэша")
                    journalEntryDao.deleteById(entryId)
                    Result.success(Unit)
                }

                else -> {
                    val errorMessage = "Ошибка удаления записи: код ${response.code()}"
                    logger.e(TAG, errorMessage)
                    Result.failure(NetworkException(errorMessage))
                }
            }
        } catch (e: IOException) {
            val errorMessage = "Ошибка сети при удалении записи"
            logger.e(TAG, "$errorMessage: ${e.message}")
            crashReporter.logException(e, "Ошибка сети при удалении записи дневника")
            Result.failure(NetworkException(errorMessage, e))
        } catch (e: HttpException) {
            val errorMessage = "HTTP ошибка при удалении записи: ${e.code()}"
            logger.e(TAG, "$errorMessage: ${e.message()}")
            crashReporter.logException(e, "HTTP ошибка при удалении записи дневника: ${e.code()}")
            Result.failure(NetworkException(errorMessage, e))
        }

    override suspend fun canDeleteEntry(
        entryId: Long,
        journalId: Long
    ): Boolean {
        val minEntryId = journalEntryDao.getMinEntryId(journalId)
        return minEntryId == null || entryId != minEntryId
    }
}
