package com.swparks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swparks.data.database.entity.DialogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с диалогами в Room
 */
@Dao
interface DialogDao {
    /**
     * Получить поток списка диалогов, отсортированный по дате последнего сообщения
     * @return Flow со списком диалогов
     */
    @Query("SELECT * FROM dialogs ORDER BY lastMessageDate DESC")
    fun getDialogsFlow(): Flow<List<DialogEntity>>

    /**
     * Вставить или обновить список диалогов
     * @param dialogs Список диалогов для сохранения
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dialogs: List<DialogEntity>)

    /**
     * Удалить диалог по ID
     * @param dialogId Идентификатор диалога для удаления
     */
    @Query("DELETE FROM dialogs WHERE id = :dialogId")
    suspend fun deleteById(dialogId: Long)

    /**
     * Удалить все диалоги
     */
    @Query("DELETE FROM dialogs")
    suspend fun deleteAll()
}
