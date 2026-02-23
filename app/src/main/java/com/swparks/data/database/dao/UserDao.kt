package com.swparks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swparks.data.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с пользователями в Room
 */
@Suppress("TooManyFunctions")
@Dao
interface UserDao {
    // Основной пользователь

    /**
     * Получить поток текущего авторизованного пользователя
     * @return Flow с текущим пользователем или null
     */
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    /**
     * Получить поток пользователя по ID
     * @param userId Идентификатор пользователя
     * @return Flow с пользователем или null
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: Long): Flow<UserEntity?>

    // Друзья

    /**
     * Получить поток списка друзей
     * @return Flow с списком друзей, отсортированным по имени
     */
    @Query("SELECT * FROM users WHERE isFriend = 1 ORDER BY name ASC")
    fun getFriendsFlow(): Flow<List<UserEntity>>

    /**
     * Получить поток количества друзей
     * @return Flow с количеством друзей
     */
    @Query("SELECT COUNT(*) FROM users WHERE isFriend = 1")
    fun getFriendsCountFlow(): Flow<Int>

    // Заявки в друзья

    /**
     * Получить поток списка заявок в друзья
     * @return Flow с списком заявок, отсортированным по имени
     */
    @Query("SELECT * FROM users WHERE isFriendRequest = 1 ORDER BY name ASC")
    fun getFriendRequestsFlow(): Flow<List<UserEntity>>

    // Черный список

    /**
     * Получить поток черного списка
     * @return Flow с черным списком, отсортированным по имени
     */
    @Query("SELECT * FROM users WHERE isBlacklisted = 1 ORDER BY name ASC")
    fun getBlacklistFlow(): Flow<List<UserEntity>>

    // Вставка и обновление

    /**
     * Вставить или обновить пользователя
     * @param user Пользователь для сохранения
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    /**
     * Вставить или обновить список пользователей
     * @param users Список пользователей для сохранения
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    /**
     * Удалить пользователя по ID
     * @param userId Идентификатор пользователя
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: Long)

    // Обновление флагов

    /**
     * Пометить пользователя как друга
     * @param userId Идентификатор пользователя
     */
    @Query("UPDATE users SET isFriend = 1 WHERE id = :userId")
    suspend fun markAsFriend(userId: Long)

    /**
     * Удалить пользователя из друзей
     * @param userId Идентификатор пользователя
     */
    @Query("UPDATE users SET isFriend = 0 WHERE id = :userId")
    suspend fun removeFriend(userId: Long)

    /**
     * Удалить заявку на добавление в друга
     * @param userId Идентификатор пользователя
     */
    @Query("UPDATE users SET isFriendRequest = 0 WHERE id = :userId")
    suspend fun removeFriendRequest(userId: Long)

    /**
     * Добавить пользователя в черный список
     * @param userId Идентификатор пользователя
     */
    @Query("UPDATE users SET isBlacklisted = 1 WHERE id = :userId")
    suspend fun addToBlacklist(userId: Long)

    /**
     * Удалить пользователя из черного списка
     * @param userId Идентификатор пользователя
     */
    @Query("UPDATE users SET isBlacklisted = 0 WHERE id = :userId")
    suspend fun removeFromBlacklist(userId: Long)

    /**
     * Пометить пользователя как текущего авторизованного
     * @param userId Идентификатор пользователя
     */
    @Query("UPDATE users SET isCurrentUser = 1 WHERE id = :userId")
    suspend fun markAsCurrentUser(userId: Long)

    /**
     * Снять пометку текущего пользователя со всех пользователей
     */
    @Query("UPDATE users SET isCurrentUser = 0 WHERE isCurrentUser = 1")
    suspend fun clearCurrentUserFlags()

    /**
     * Сбросить флаги дружбы для всех пользователей (кроме текущего).
     * Используется перед синхронизацией списка друзей с сервера.
     */
    @Query("UPDATE users SET isFriend = 0 WHERE isFriend = 1")
    suspend fun clearAllFriendFlags()

    /**
     * Сбросить флаги заявок в друзья для всех пользователей.
     * Используется перед синхронизацией списка заявок с сервера.
     */
    @Query("UPDATE users SET isFriendRequest = 0 WHERE isFriendRequest = 1")
    suspend fun clearAllFriendRequestFlags()

    /**
     * Сбросить флаги черного списка для всех пользователей.
     * Используется перед синхронизацией черного списка с сервера.
     */
    @Query("UPDATE users SET isBlacklisted = 0 WHERE isBlacklisted = 1")
    suspend fun clearAllBlacklistFlags()

    /**
     * Увеличить счетчик друзей текущего пользователя на 1
     */
    @Query(
        """
        UPDATE users SET friendsCount = COALESCE(friendsCount, 0) + 1 
        WHERE isCurrentUser = 1
        """
    )
    suspend fun incrementFriendsCount()

    /**
     * Уменьшить счетчик друзей текущего пользователя на 1
     */
    @Query(
        """
        UPDATE users SET friendsCount = 
        CASE WHEN friendsCount > 0 THEN friendsCount - 1 ELSE 0 END 
        WHERE isCurrentUser = 1
        """
    )
    suspend fun decrementFriendsCount()

    /**
     * Уменьшить счетчик заявок в друзья текущего пользователя на 1
     */
    @Query(
        """
        UPDATE users SET friendRequestCount =
        CASE WHEN CAST(friendRequestCount AS INTEGER) > 0
        THEN CAST(friendRequestCount AS INTEGER) - 1 ELSE 0 END
        WHERE isCurrentUser = 1
        """
    )
    suspend fun decrementFriendRequestCount()

    /**
     * Увеличить счетчик дневников текущего пользователя на 1
     */
    @Query(
        """
        UPDATE users SET journalCount = COALESCE(journalCount, 0) + 1
        WHERE isCurrentUser = 1
        """
    )
    suspend fun incrementJournalCount()

    /**
     * Уменьшить счетчик дневников текущего пользователя на 1
     */
    @Query(
        """
        UPDATE users SET journalCount =
        CASE WHEN journalCount > 0 THEN journalCount - 1 ELSE 0 END
        WHERE isCurrentUser = 1
        """
    )
    suspend fun decrementJournalCount()

    // Очистка всех данных пользователя при logout

    /**
     * Удалить всех пользователей
     */
    @Query("DELETE FROM users")
    suspend fun clearAll()
}