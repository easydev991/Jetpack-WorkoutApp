package com.swparks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swparks.data.model.Comment
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User

@Entity(tableName = "parks")
data class ParkEntity(
    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "size_id")
    val sizeID: Int,

    @ColumnInfo(name = "type_id")
    val typeID: Int,

    @ColumnInfo(name = "longitude")
    val longitude: String,

    @ColumnInfo(name = "latitude")
    val latitude: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "city_id")
    val cityID: Int,

    @ColumnInfo(name = "country_id")
    val countryID: Int,

    @ColumnInfo(name = "preview")
    val preview: String,

    @ColumnInfo(name = "comments_count")
    val commentsCount: Int? = null,

    @ColumnInfo(name = "training_users_count")
    val trainingUsersCount: Int? = null,

    @ColumnInfo(name = "author")
    val author: User? = null,

    @ColumnInfo(name = "photos")
    val photos: List<Photo>? = null,

    @ColumnInfo(name = "comments")
    val comments: List<Comment>? = null,

    @ColumnInfo(name = "training_users")
    val trainingUsers: List<User>? = null,

    @ColumnInfo(name = "train_here")
    val trainHere: Boolean? = null,

    @ColumnInfo(name = "mine")
    val mine: Boolean? = null,

    @ColumnInfo(name = "can_edit")
    val canEdit: Boolean? = null,

    @ColumnInfo(name = "equipment_ids")
    val equipmentIDS: List<Int>? = null,

    @ColumnInfo(name = "create_date")
    val createDate: String? = null,

    @ColumnInfo(name = "modify_date")
    val modifyDate: String? = null
)

fun Park.toEntity(): ParkEntity = ParkEntity(
    id = id,
    name = name,
    sizeID = sizeID,
    typeID = typeID,
    longitude = longitude,
    latitude = latitude,
    address = address,
    cityID = cityID,
    countryID = countryID,
    preview = preview,
    commentsCount = commentsCount,
    trainingUsersCount = trainingUsersCount,
    author = author,
    photos = photos,
    comments = comments,
    trainingUsers = trainingUsers,
    trainHere = trainHere,
    mine = mine,
    canEdit = canEdit,
    equipmentIDS = equipmentIDS,
    createDate = createDate,
    modifyDate = modifyDate
)

fun ParkEntity.toPark(): Park = Park(
    id = id,
    name = name,
    sizeID = sizeID,
    typeID = typeID,
    longitude = longitude,
    latitude = latitude,
    address = address,
    cityID = cityID,
    countryID = countryID,
    commentsCount = commentsCount,
    preview = preview,
    trainingUsersCount = trainingUsersCount,
    createDate = createDate,
    modifyDate = modifyDate,
    author = author,
    photos = photos,
    comments = comments,
    trainHere = trainHere,
    equipmentIDS = equipmentIDS,
    mine = mine,
    canEdit = canEdit,
    trainingUsers = trainingUsers
)
