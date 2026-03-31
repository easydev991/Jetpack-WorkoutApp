package com.swparks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user_training_parks",
    primaryKeys = ["user_id", "park_id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["park_id"])
    ]
)
data class UserTrainingParkEntity(
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "park_id")
    val parkId: Long,

    @ColumnInfo(name = "position")
    val position: Int = 0
)