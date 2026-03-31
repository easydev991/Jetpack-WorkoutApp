package com.swparks.data.database

import android.util.Log
import androidx.room.TypeConverter
import com.swparks.data.model.Comment
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object AppConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @TypeConverter
    fun fromParksList(parks: List<Park>?): String? {
        return parks?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toParksList(data: String?): List<Park>? {
        return data?.let {
            try {
                json.decodeFromString<List<Park>>(it)
            } catch (e: SerializationException) {
                Log.e("AppConverters", "Ошибка десериализации площадок: ${e.message}")
                null
            }
        }
    }

    @TypeConverter
    fun fromPhotoList(photos: List<Photo>?): String? {
        return photos?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toPhotoList(data: String?): List<Photo>? {
        return data?.let {
            try {
                json.decodeFromString<List<Photo>>(it)
            } catch (e: SerializationException) {
                Log.e("AppConverters", "Ошибка десериализации фото: ${e.message}")
                null
            }
        }
    }

    @TypeConverter
    fun fromCommentList(comments: List<Comment>?): String? {
        return comments?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toCommentList(data: String?): List<Comment>? {
        return data?.let {
            try {
                json.decodeFromString<List<Comment>>(it)
            } catch (e: SerializationException) {
                Log.e("AppConverters", "Ошибка десериализации комментариев: ${e.message}")
                null
            }
        }
    }

    @TypeConverter
    fun fromUserList(users: List<User>?): String? {
        return users?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toUserList(data: String?): List<User>? {
        return data?.let {
            try {
                json.decodeFromString<List<User>>(it)
            } catch (e: SerializationException) {
                Log.e("AppConverters", "Ошибка десериализации пользователей: ${e.message}")
                null
            }
        }
    }

    @TypeConverter
    fun fromUser(user: User?): String? {
        return user?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toUser(data: String?): User? {
        return data?.let {
            try {
                json.decodeFromString<User>(it)
            } catch (e: SerializationException) {
                Log.e("AppConverters", "Ошибка десериализации пользователя: ${e.message}")
                null
            }
        }
    }

    @TypeConverter
    fun fromIntList(ints: List<Int>?): String? {
        return ints?.let { json.encodeToString(ListSerializer(Int.serializer()), it) }
    }

    @TypeConverter
    fun toIntList(data: String?): List<Int>? {
        return data?.let {
            try {
                json.decodeFromString<List<Int>>(it)
            } catch (e: SerializationException) {
                Log.e("AppConverters", "Ошибка десериализации списка Int: ${e.message}")
                null
            }
        }
    }
}

@Deprecated("Используйте AppConverters", ReplaceWith("AppConverters"))
typealias UserConverters = AppConverters
