package com.swparks.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.database.entity.JournalEntity
import com.swparks.data.database.entity.JournalEntryEntity
import com.swparks.data.database.entity.UserEntity

/**
 * Room Database для локального хранения данных приложения
 *
 * @property userDao DAO для работы с пользователями
 * @property journalDao DAO для работы с дневниками
 * @property journalEntryDao DAO для работы с записями дневника
 * @property dialogDao DAO для работы с диалогами
 */
@Database(
    entities = [
        UserEntity::class,
        JournalEntity::class,
        JournalEntryEntity::class,
        DialogEntity::class
    ],
    version = 2,
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

    /**
     * Получить DAO для работы с записями дневника
     * @return JournalEntryDao
     */
    abstract fun journalEntryDao(): JournalEntryDao

    /**
     * Получить DAO для работы с диалогами
     * @return DialogDao
     */
    abstract fun dialogDao(): DialogDao
}
