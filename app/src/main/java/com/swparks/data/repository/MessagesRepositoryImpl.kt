package com.swparks.data.repository

import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.model.MessageResponse
import com.swparks.data.model.toEntity
import com.swparks.domain.exception.NetworkException
import com.swparks.network.SWApi
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import retrofit2.HttpException
import java.io.IOException

/**
 * Реализация репозитория для работы с диалогами
 *
 * Кэширует диалоги в локальной базе данных для офлайн-доступа
 * и обновляет их с сервера при вызове метода refreshDialogs
 *
 * @property dialogsDao DAO для работы с диалогами в Room
 * @property swApi API клиент для работы с сервером
 * @property logger Логгер для записи ошибок
 */
open class MessagesRepositoryImpl(
    private val dialogsDao: DialogDao? = null,
    private val swApi: SWApi,
    logger: Logger,
    crashReporter: CrashReporter
) : BaseRepository(logger, crashReporter) {
    companion object {
        private const val TAG = "MessagesRepository"
    }

    // UI подписывается на этот Flow
    open val dialogs: Flow<List<DialogEntity>> = dialogsDao?.getDialogsFlow() ?: flowOf(emptyList())

    // Вызывается при открытии экрана и pull-to-refresh
    open suspend fun refreshDialogs(): Result<Unit> =
        try {
            logger.i(TAG, "Загружаем диалоги с сервера")

            // Загружаем диалоги с сервера
            val remoteDialogs = swApi.getDialogs()
            logger.i(TAG, "Получено ${remoteDialogs.size} диалогов с сервера")

            // Очищаем старые данные и вставляем новые
            val dao = checkNotNull(dialogsDao) { "DialogDao is required" }
            dao.deleteAll()
            dao.insertAll(remoteDialogs.map { it.toEntity() })

            logger.i(TAG, "Успешно сохранено ${remoteDialogs.size} диалогов в БД")
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке диалогов"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке диалогов"))
        }

    open suspend fun getMessages(dialogId: Long): Result<List<MessageResponse>> =
        try {
            val messages = swApi.getMessages(dialogId)
            Result.success(messages)
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "загрузке сообщений"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "загрузке сообщений"))
        }

    open suspend fun sendMessage(
        message: String,
        userId: Long
    ): Result<Unit> =
        try {
            val response = swApi.sendMessageTo(userId, message)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "отправке сообщения"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "отправке сообщения"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "отправке сообщения"))
        }

    open suspend fun markAsRead(userId: Long): Result<Unit> =
        try {
            val response = swApi.markAsRead(userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "отметке сообщений прочитанными"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "отметке сообщений прочитанными"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "отметке сообщений прочитанными"))
        }

    open suspend fun markDialogAsRead(
        dialogId: Long,
        userId: Int
    ): Result<Unit> =
        try {
            val response = swApi.markAsRead(userId.toLong())
            if (response.isSuccessful) {
                dialogsDao?.updateUnreadCount(dialogId)
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "отметке сообщений прочитанными"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "отметке сообщений прочитанными"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "отметке сообщений прочитанными"))
        } catch (e: IllegalStateException) {
            Result.failure(NetworkException("Ошибка сети: ${e.message}"))
        }

    open suspend fun deleteDialog(dialogId: Long): Result<Unit> =
        try {
            val response = swApi.deleteDialog(dialogId)
            if (response.isSuccessful) {
                dialogsDao?.deleteById(dialogId)
                Result.success(Unit)
            } else {
                Result.failure(handleResponseError(response, TAG, "удалении диалога"))
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "удалении диалога"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "удалении диалога"))
        } catch (e: IllegalStateException) {
            Result.failure(NetworkException("Ошибка сети: ${e.message}"))
        }
}
