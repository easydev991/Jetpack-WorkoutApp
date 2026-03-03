package com.swparks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.swparks.data.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с мероприятиями в Room
 *
 * Хранит только прошедшие мероприятия (past events).
 */
@Dao
interface EventDao {
    /**
     * Получить поток всех прошедших мероприятий
     *
     * Сортировка по дате начала от новых к старым.
     *
     * @return Flow со списком мероприятий
     */
    @Query("SELECT * FROM events ORDER BY begin_date DESC")
    fun getAllPastEvents(): Flow<List<EventEntity>>

    /**
     * Вставить или обновить список мероприятий
     *
     * @param events Список мероприятий для сохранения
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    /**
     * Удалить все мероприятия
     */
    @Query("DELETE FROM events")
    suspend fun deleteAll()

    /**
     * Заменить все мероприятия (транзакция)
     *
     * Удаляет старые и вставляет новые мероприятия атомарно.
     *
     * @param events Список мероприятий для сохранения
     */
    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        deleteAll()
        insertEvents(events)
    }
}
