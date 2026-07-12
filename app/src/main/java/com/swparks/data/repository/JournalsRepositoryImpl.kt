package com.swparks.data.repository

import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.toDomain
import com.swparks.data.database.entity.toEntity
import com.swparks.data.model.JournalEntryResponse
import com.swparks.data.model.JournalResponse
import com.swparks.data.model.toDomain
import com.swparks.domain.model.Journal
import com.swparks.network.SWApi
import com.swparks.ui.model.EditJournalSettingsRequest
import com.swparks.ui.model.JournalAccess
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

/**
 * Реализация репозитория для работы с дневниками
 *
 * Кэширует дневники в локальной базе данных для офлайн-доступа
 * и обновляет их с сервера при вызове метода refreshJournals
 *
 * @property swApi API клиент для работы с сервером
 * @property journalDao DAO для работы с дневниками в Room
 */
open class JournalsRepositoryImpl(
    private val swApi: SWApi,
    private val journalDao: JournalDao,
    private val userDao: UserDao,
    logger: Logger,
    crashReporter: CrashReporter
) : BaseRepository(logger, crashReporter) {
    companion object {
        private const val TAG = "JournalsRepository"
        private const val HTTP_NOT_FOUND = 404
    }

    fun observeJournals(userId: Long): Flow<List<Journal>> =
        journalDao
            .getJournalsByUserId(userId)
            .map { entities ->
                entities.map { it.toDomain() }
            }

    suspend fun refreshJournals(userId: Long): Result<Unit> =
        try {
            logger.i(TAG, "Загружаем дневники пользователя с id: $userId")

            // Загружаем дневники с сервера
            val responses = swApi.getJournals(userId)
            logger.i(TAG, "Получено ${responses.size} дневников с сервера")

            // Мапим JournalResponse -> Journal -> JournalEntity
            val entities =
                responses.map { response ->
                    val journal = response.toDomain()
                    journal.toEntity()
                }

            // Используем транзакцию для очистки старых данных перед вставкой
            journalDao.deleteByUserId(userId)
            journalDao.insertAll(entities)

            logger.i(TAG, "Успешно сохранено ${entities.size} дневников в БД")
            Result.success(Unit)
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка при загрузке дневников: ${e.message}", e)
            crashReporter.logException(e, "Ошибка загрузки дневников")
            Result.failure(e)
        } catch (e: HttpException) {
            logger.e(TAG, "HTTP ошибка при загрузке дневников: ${e.code()} ${e.message()}", e)
            crashReporter.logException(e, "HTTP ошибка загрузки дневников: ${e.code()}")
            Result.failure(e)
        }

    open suspend fun getJournals(userId: Long): Result<List<JournalResponse>> =
        try {
            val journals = swApi.getJournals(userId)
            Result.success(journals)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке дневников"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке дневников"))
        }

    open suspend fun getJournal(
        userId: Long,
        journalId: Long
    ): Result<JournalResponse> =
        try {
            val journal = swApi.getJournal(userId, journalId)
            Result.success(journal)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке дневника"))
        }

    open suspend fun editJournalSettings(
        journalId: Long,
        title: String,
        userId: Long?,
        viewAccess: JournalAccess,
        commentAccess: JournalAccess
    ): Result<Unit> =
        try {
            val request =
                EditJournalSettingsRequest.create(
                    title = title,
                    viewAccess = viewAccess,
                    commentAccess = commentAccess
                )

            logger.i(
                TAG,
                "Запрос редактирования настроек дневника: journalId=$journalId, userId=$userId, " +
                    "title=$title, viewAccess=${request.viewAccess}, commentAccess=${request.commentAccess}"
            )

            val response =
                swApi.editJournalSettings(
                    userId = userId ?: 1L,
                    journalId = journalId,
                    title = title,
                    viewAccess = request.viewAccess,
                    commentAccess = request.commentAccess
                )

            if (response.isSuccessful) {
                logger.i(
                    TAG,
                    "Настройки дневника успешно обновлены на сервере: journalId=$journalId"
                )
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                logger.e(
                    TAG,
                    "Ошибка сервера при редактировании настроек дневника: code=${response.code()}, body=$errorBody"
                )
                Result.failure(
                    com.swparks.domain.exception.ServerException(
                        message = errorBody ?: "Ошибка сервера: ${response.code()}",
                        cause = Exception("HTTP ${response.code()}")
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "редактировании настроек дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "редактировании настроек дневника"))
        }

    open suspend fun getJournalEntries(
        userId: Long,
        journalId: Long
    ): Result<List<JournalEntryResponse>> =
        try {
            val entries = swApi.getJournalEntries(userId, journalId)
            Result.success(entries)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке записей дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке записей дневника"))
        }

    open fun observeJournalById(journalId: Long): Flow<Journal?> =
        journalDao
            .observeById(journalId)
            .map { entity -> entity?.toDomain() }
            .flowOn(Dispatchers.IO)

    @Suppress("TooGenericExceptionCaught")
    open suspend fun saveJournalToCache(journal: Journal) {
        try {
            journalDao.insert(journal.toEntity())
            logger.i(TAG, "Дневник сохранен в кэш: journalId=${journal.id}")
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка сохранения дневника в кэш: ${e.message}")
            crashReporter.logException(e, "Ошибка сохранения дневника в кэш")
            throw e
        }
    }

    open suspend fun createJournal(
        title: String,
        userId: Long?
    ): Result<Unit> =
        try {
            val finalUserId = userId ?: 1L
            logger.i(TAG, "Создание дневника: userId=$finalUserId, title=$title")
            val response =
                swApi.createJournal(
                    userId = finalUserId,
                    title = title
                )
            logger.i(
                TAG,
                "Ответ сервера при создании дневника: код=${response.code()}, успешно=${response.isSuccessful}"
            )
            if (response.isSuccessful) {
                userDao.incrementJournalCount()
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "создании дневника"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "создании дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "создании дневника"))
        }

    open suspend fun deleteJournal(
        journalId: Long,
        userId: Long?
    ): Result<Unit> =
        try {
            val response =
                swApi.deleteJournal(
                    userId = userId ?: 1L,
                    journalId = journalId
                )

            when {
                response.isSuccessful -> {
                    logger.i(TAG, "Дневник успешно удален на сервере")
                    journalDao.deleteById(journalId)
                    // Обновляем счётчик дневников текущего пользователя
                    userDao.decrementJournalCount()
                    Result.success(Unit)
                }

                response.code() == HTTP_NOT_FOUND -> {
                    // Дневник уже удален на сервере — синхронизируем локальный кэш
                    logger.i(TAG, "Дневник уже удален на сервере (404), удаляем из локального кэша")
                    journalDao.deleteById(journalId)
                    // Обновляем счётчик дневников текущего пользователя
                    userDao.decrementJournalCount()
                    Result.success(Unit)
                }

                else -> {
                    val statusCode = response.code()
                    val errorBody = response.errorBody()?.string()
                    logger.e(TAG, "Ошибка при удалении дневника: код=$statusCode, тело=$errorBody")
                    Result.failure(
                        handleHttpException(
                            HttpException(response),
                            TAG,
                            "удалении дневника"
                        )
                    )
                }
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "удалении дневника"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "удалении дневника"))
        }
}
