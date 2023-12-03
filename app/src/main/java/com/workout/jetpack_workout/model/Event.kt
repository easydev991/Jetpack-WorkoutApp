package com.workout.jetpack_workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event (
    val id: Long,
    val title: String,
    val description: String,

    @SerialName("create_date")
    val createDate: String,

    @SerialName("begin_date")
    val beginDate: String,

    @SerialName("country_id")
    val countryid: Long,

    @SerialName("city_id")
    val cityid: Long,

    @SerialName("comment_count")
    val commentCount: Long,

    val preview: String,

    @SerialName("area_id")
    val areaid: Long? = null,

    val latitude: String,
    val longitude: String,

    @SerialName("user_count")
    val userCount: Long,

    @SerialName("is_current")
    val isCurrent: Boolean,

    val photos: List<Photo>,
    val author: Author,
    val name: String
)

@Serializable
data class Author (
    val id: Long,
    val name: String,
    val image: String
)

@Serializable
data class Photo (
    val id: Long,
    val photo: String
)