package com.swparks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.swparks.data.database.entity.ParkEntity
import com.swparks.data.database.entity.UserTrainingParkCacheStateEntity
import com.swparks.data.database.entity.UserTrainingParkEntity

@Dao
interface UserTrainingParkDao {
    @Query("SELECT park_id FROM user_training_parks WHERE user_id = :userId ORDER BY position ASC")
    suspend fun getParkIdsForUser(userId: Long): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM user_training_parks_cache_state WHERE user_id = :userId LIMIT 1)")
    suspend fun hasCachedParksForUser(userId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForUser(relations: List<UserTrainingParkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCacheState(state: UserTrainingParkCacheStateEntity)

    @Query("DELETE FROM user_training_parks WHERE user_id = :userId")
    suspend fun clearForUser(userId: Long)

    @Transaction
    suspend fun replaceForUser(
        userId: Long,
        relations: List<UserTrainingParkEntity>
    ) {
        clearForUser(userId)
        insertForUser(relations)
        upsertCacheState(UserTrainingParkCacheStateEntity(userId = userId))
    }

    @Query(
        """
        SELECT p.* FROM parks p
        INNER JOIN user_training_parks utp ON p.id = utp.park_id
        WHERE utp.user_id = :userId
        ORDER BY utp.position ASC
    """
    )
    suspend fun getParksForUserFromCache(userId: Long): List<ParkEntity>
}
