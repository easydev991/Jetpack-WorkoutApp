package com.swparks.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.dao.UserTrainingParkDao
import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.database.entity.EventEntity
import com.swparks.data.database.entity.JournalEntity
import com.swparks.data.database.entity.JournalEntryEntity
import com.swparks.data.database.entity.ParkEntity
import com.swparks.data.database.entity.UserEntity
import com.swparks.data.database.entity.UserTrainingParkCacheStateEntity
import com.swparks.data.database.entity.UserTrainingParkEntity

/**
 * Room Database для локального хранения данных приложения
 *
 * @property userDao DAO для работы с пользователями
 * @property journalDao DAO для работы с дневниками
 * @property journalEntryDao DAO для работы с записями дневника
 * @property dialogDao DAO для работы с диалогами
 * @property eventDao DAO для работы с мероприятиями
 */
@Database(
    entities = [
        UserEntity::class,
        JournalEntity::class,
        JournalEntryEntity::class,
        DialogEntity::class,
        EventEntity::class,
        ParkEntity::class,
        UserTrainingParkEntity::class,
        UserTrainingParkCacheStateEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
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

    /**
     * Получить DAO для работы с мероприятиями
     * @return EventDao
     */
    abstract fun eventDao(): EventDao

    /**
     * Получить DAO для работы с площадками
     * @return ParkDao
     */
    abstract fun parkDao(): ParkDao

    /**
     * Получить DAO для работы с тренировочными площадками пользователя
     * @return UserTrainingParkDao
     */
    abstract fun userTrainingParkDao(): UserTrainingParkDao
}
