package com.swparks.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.entity.JournalEntity

/**
 * Room Database для локального хранения данных приложения
 *
 * @property userDao DAO для работы с пользователями
 * @property journalDao DAO для работы с дневниками
 */
@Database(
    entities = [
        UserEntity::class,
        JournalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SWDatabase : RoomDatabase() {
    /**
     * Получить DAO для работы с пользователями
     * @return UserDao
     */
    abstract fun userDao(): UserDao

    /**
     * Получить DAO для работы с дневниками
     * @return JournalDao
     */
    abstract fun journalDao(): JournalDao
}
