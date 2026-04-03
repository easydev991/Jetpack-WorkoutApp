package com.swparks.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoTest {
    private fun createPhoto(
        id: Long,
        url: String = "https://example.com/photo$id.jpg"
    ) = Photo(id = id, photo = url)

    @Test
    fun removePhotoById_whenDeletingFirstPhoto_thenReturnsRenumberedList() {
        val photos =
            listOf(
                createPhoto(id = 1),
                createPhoto(id = 2),
                createPhoto(id = 3)
            )

        val result = photos.removePhotoById(photoId = 1)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("https://example.com/photo2.jpg", result[0].photo)
        assertEquals(2L, result[1].id)
        assertEquals("https://example.com/photo3.jpg", result[1].photo)
    }

    @Test
    fun removePhotoById_whenDeletingMiddlePhoto_thenReturnsRenumberedList() {
        val photos =
            listOf(
                createPhoto(id = 1),
                createPhoto(id = 2),
                createPhoto(id = 3)
            )

        val result = photos.removePhotoById(photoId = 2)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("https://example.com/photo1.jpg", result[0].photo)
        assertEquals(2L, result[1].id)
        assertEquals("https://example.com/photo3.jpg", result[1].photo)
    }

    @Test
    fun removePhotoById_whenDeletingLastPhoto_thenReturnsRenumberedList() {
        val photos =
            listOf(
                createPhoto(id = 1),
                createPhoto(id = 2),
                createPhoto(id = 3)
            )

        val result = photos.removePhotoById(photoId = 3)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("https://example.com/photo1.jpg", result[0].photo)
        assertEquals(2L, result[1].id)
        assertEquals("https://example.com/photo2.jpg", result[1].photo)
    }

    @Test
    fun removePhotoById_whenDeletingNonExistentId_thenReturnsSameList() {
        val photos =
            listOf(
                createPhoto(id = 1),
                createPhoto(id = 2),
                createPhoto(id = 3)
            )

        val result = photos.removePhotoById(photoId = 999)

        assertEquals(3, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
        assertEquals(3L, result[2].id)
    }

    @Test
    fun removePhotoById_whenDeletingFromSingleItemList_thenReturnsEmptyList() {
        val photos = listOf(createPhoto(id = 1))

        val result = photos.removePhotoById(photoId = 1)

        assertEquals(0, result.size)
    }

    @Test
    fun removePhotoById_whenListIsEmpty_thenReturnsEmptyList() {
        val photos = emptyList<Photo>()

        val result = photos.removePhotoById(photoId = 1)

        assertEquals(0, result.size)
    }

    @Test
    fun removePhotoById_whenDeletingFromFourPhotos_thenReturnsCorrectlyRenumbered() {
        val photos =
            listOf(
                createPhoto(id = 1),
                createPhoto(id = 2),
                createPhoto(id = 3),
                createPhoto(id = 4)
            )

        val result = photos.removePhotoById(photoId = 2)

        assertEquals(3, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("https://example.com/photo1.jpg", result[0].photo)
        assertEquals(2L, result[1].id)
        assertEquals("https://example.com/photo3.jpg", result[1].photo)
        assertEquals(3L, result[2].id)
        assertEquals("https://example.com/photo4.jpg", result[2].photo)
    }
}
