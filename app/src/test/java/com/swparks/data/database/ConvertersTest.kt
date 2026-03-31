package com.swparks.data.database

import com.swparks.data.model.Comment
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val testUser = User(
        id = 1L,
        name = "testuser",
        image = "https://example.com/avatar.jpg"
    )

    private val testPhoto = Photo(id = 10L, photo = "https://example.com/photo.jpg")

    private val testComment = Comment(
        id = 100L,
        body = "Test comment",
        date = "2024-01-15",
        user = testUser
    )

    @Test
    fun photoListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult() {
        val photos = listOf(testPhoto, Photo(id = 11L, photo = "https://example.com/photo2.jpg"))

        val json = AppConverters.fromPhotoList(photos)
        val restored = AppConverters.toPhotoList(json)

        assertNotNull(restored)
        assertEquals(2, restored!!.size)
        assertEquals(testPhoto, restored[0])
        assertEquals(11L, restored[1].id)
    }

    @Test
    fun commentListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult() {
        val comments = listOf(testComment)

        val json = AppConverters.fromCommentList(comments)
        val restored = AppConverters.toCommentList(json)

        assertNotNull(restored)
        assertEquals(1, restored!!.size)
        assertEquals(testComment.id, restored[0].id)
        assertEquals(testComment.body, restored[0].body)
    }

    @Test
    fun userListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult() {
        val users = listOf(testUser, User(id = 2L, name = "user2", image = null))

        val json = AppConverters.fromUserList(users)
        val restored = AppConverters.toUserList(json)

        assertNotNull(restored)
        assertEquals(2, restored!!.size)
        assertEquals(testUser, restored[0])
        assertEquals(2L, restored[1].id)
    }

    @Test
    fun intListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult() {
        val ints = listOf(1, 2, 3, 42)

        val json = AppConverters.fromIntList(ints)
        val restored = AppConverters.toIntList(json)

        assertNotNull(restored)
        assertEquals(ints, restored)
    }

    @Test
    fun userConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult() {
        val json = AppConverters.fromUser(testUser)
        val restored = AppConverters.toUser(json)

        assertNotNull(restored)
        assertEquals(testUser, restored)
    }

    @Test
    fun converters_whenNullInput_thenReturnNull() {
        assertNull(AppConverters.fromPhotoList(null))
        assertNull(AppConverters.toPhotoList(null))
        assertNull(AppConverters.fromCommentList(null))
        assertNull(AppConverters.toCommentList(null))
        assertNull(AppConverters.fromUserList(null))
        assertNull(AppConverters.toUserList(null))
        assertNull(AppConverters.fromIntList(null))
        assertNull(AppConverters.toIntList(null))
        assertNull(AppConverters.fromUser(null))
        assertNull(AppConverters.toUser(null))
    }
}
