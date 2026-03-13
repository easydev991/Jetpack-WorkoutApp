package com.swparks.data.repository

import android.util.Log
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.model.toEntity
import com.swparks.domain.repository.MessagesRepository
import com.swparks.network.SWApi
import com.swparks.util.Logger
import kotlinx.coroutines.flow.Flow
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
class MessagesRepositoryImpl(
    private val dialogsDao: DialogDao,
    private val swApi: SWApi,
    private val logger: Logger
) : MessagesRepository {

    companion object {
        private const val TAG = "MessagesRepository"
    }

    // UI подписывается на этот Flow
    override val dialogs: Flow<List<DialogEntity>> = dialogsDao.getDialogsFlow()

    // Вызывается при открытии экрана и pull-to-refresh
    override suspend fun refreshDialogs(): Result<Unit> = try {
        Log.i(TAG, "Загружаем диалоги с сервера")

        // Загружаем диалоги с сервера
        val remoteDialogs = swApi.getDialogs()
        Log.i(TAG, "Получено ${remoteDialogs.size} диалогов с сервера")

        // Очищаем старые данные и вставляем новые
        dialogsDao.deleteAll()
        dialogsDao.insertAll(remoteDialogs.map { it.toEntity() })

        Log.i(TAG, "Успешно сохранено ${remoteDialogs.size} диалогов в БД")
        Result.success(Unit)
    } catch (e: IOException) {
        logger.e(TAG, "Ошибка загрузки диалогов: ${e.message}")
        Result.failure(e)
    }
}
