package com.swparks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swparks.data.database.entity.ParkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с площадками в Room
 */
@Dao
interface ParkDao {
    /**
     * Получить поток всех площадок
     *
     * @return Flow со списком площадок, отсортированных по id
     */
    @Query("SELECT * FROM parks ORDER BY id ASC")
    fun getAllParks(): Flow<List<ParkEntity>>

    /**
     * Получить площадку по ID
     *
     * @param id Идентификатор площадки
     * @return ParkEntity или null если не найден
     */
    @Query("SELECT * FROM parks WHERE id = :id")
    suspend fun getParkById(id: Long): ParkEntity?

    /**
     * Вставить или обновить список площадок
     *
     * При конфликте ID выполняется замена (REPLACE).
     *
     * @param parks Список площадок для сохранения
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parks: List<ParkEntity>)

    /**
     * Удалить все площадки
     */
    @Query("DELETE FROM parks")
    suspend fun deleteAll()

    /**
     * Проверить, есть ли площадки в базе
     *
     * @return true если база пуста
     */
    @Query("SELECT COUNT(*) FROM parks")
    suspend fun isEmpty(): Boolean
}
