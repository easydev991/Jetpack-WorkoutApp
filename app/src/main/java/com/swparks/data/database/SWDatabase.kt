package com.swparks.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database для локального хранения данных приложения
 *
 * @property userDao DAO для работы с пользователями
 */
@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SWDatabase : RoomDatabase() {
    /**
     * Получить DAO для работы с пользователями
     * @return UserDao
     */
    abstract fun userDao(): UserDao
}
