package com.swparks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swparks.data.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с записями дневника в Room
 */
@Dao
interface JournalEntryDao {
    /**
     * Получить поток записей дневника
     *
     * @param journalId Идентификатор дневника
     * @return Flow с списком записей, отсортированным по дате изменения (от новых к старым)
     */
    @Query("SELECT * FROM journal_entries WHERE journalId = :journalId ORDER BY modifyDate DESC")
    fun getJournalEntriesByJournalId(journalId: Long): Flow<List<JournalEntryEntity>>

    /**
     * Вставить или обновить список записей дневника
     *
     * @param entries Список записей для сохранения
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntryEntity>)

    /**
     * Удалить все записи дневника
     *
     * @param journalId Идентификатор дневника
     */
    @Query("DELETE FROM journal_entries WHERE journalId = :journalId")
    suspend fun deleteByJournalId(journalId: Long)

    /**
     * Удалить запись по ID
     *
     * @param entryId Идентификатор записи
     */
    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    /**
     * Получить минимальный id записи в дневнике (первую запись)
     *
     * @param journalId Идентификатор дневника
     * @return Минимальный id записи или null, если записей нет
     */
    @Query("SELECT MIN(id) FROM journal_entries WHERE journalId = :journalId")
    suspend fun getMinEntryId(journalId: Long): Long?
}
