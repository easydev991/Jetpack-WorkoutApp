package com.swparks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swparks.data.model.Park

/**
 * Entity для хранения площадок в Room
 *
 * Хранит основную информацию о площадках, полученную с сервера.
 * Используется для локального кэширования и offline-first стратегии.
 *
 * @property id Идентификатор площадки (Primary Key)
 * @property name Название площадки
 * @property sizeID Идентификатор размера площадки (модель ParkSize)
 * @property typeID Идентификатор типа площадки (модель ParkType)
 * @property longitude Долгота
 * @property latitude Широта
 * @property address Адрес площадки
 * @property cityID Идентификатор города
 * @property countryID Идентификатор страны
 * @property preview URL превью изображения
 * @property commentsCount Количество комментариев
 * @property trainingUsersCount Количество пользователей, тренирующихся на площадке
 */
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
    val commentsCount: Int = 0,

    @ColumnInfo(name = "training_users_count")
    val trainingUsersCount: Int = 0
)

/**
 * Маппер для преобразования модели [Park] в [ParkEntity]
 */
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
    commentsCount = commentsCount ?: 0,
    trainingUsersCount = trainingUsersCount ?: 0
)

/**
 * Маппер для преобразования [ParkEntity] в модель [Park]
 */
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
    preview = preview,
    commentsCount = commentsCount,
    trainingUsersCount = trainingUsersCount
)
