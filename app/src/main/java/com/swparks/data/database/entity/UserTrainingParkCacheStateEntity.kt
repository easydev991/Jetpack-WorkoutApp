package com.swparks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_training_parks_cache_state")
data class UserTrainingParkCacheStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long
)
