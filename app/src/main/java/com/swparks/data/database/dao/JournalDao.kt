package com.swparks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swparks.data.database.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с дневниками в Room
 */
@Dao
interface JournalDao {
    /**
     * Получить поток дневников пользователя
     *
     * @param userId Идентификатор пользователя
     * @return Flow с списком дневников, отсортированным по дате изменения
     */
    @Query("SELECT * FROM journals WHERE ownerId = :userId ORDER BY modifyDate DESC")
    fun getJournalsByUserId(userId: Long): Flow<List<JournalEntity>>

    /**
     * Вставить или обновить список дневников
     *
     * @param journals Список дневников для сохранения
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(journals: List<JournalEntity>)

    /**
     * Удалить все дневники пользователя
     *
     * @param userId Идентификатор пользователя
     */
    @Query("DELETE FROM journals WHERE ownerId = :userId")
    suspend fun deleteByUserId(userId: Long)

    /**
     * Удалить дневник по ID
     *
     * @param journalId Идентификатор дневника
     */
    @Query("DELETE FROM journals WHERE id = :journalId")
    suspend fun deleteById(journalId: Long)
}
