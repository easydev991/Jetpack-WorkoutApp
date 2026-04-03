package com.swparks.ui.screens.photos

import com.swparks.ui.state.PhotoDetailConfig
import com.swparks.ui.state.PhotoOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PhotoDetailSheetHostTest {
    @Test
    fun buildPhotoDetailViewModelKey_whenSamePhotoIdButDifferentOwnerType_thenKeysDifferent() {
        val eventConfig =
            createConfig(
                photoId = 1L,
                parentId = 10L,
                ownerType = PhotoOwner.Event
            )
        val parkConfig =
            createConfig(
                photoId = 1L,
                parentId = 10L,
                ownerType = PhotoOwner.Park
            )

        val eventKey = buildPhotoDetailViewModelKey(eventConfig)
        val parkKey = buildPhotoDetailViewModelKey(parkConfig)

        assertNotEquals(eventKey, parkKey)
    }

    @Test
    fun buildPhotoDetailViewModelKey_whenSamePhotoIdAndOwnerButDifferentParentId_thenKeysDifferent() {
        val firstConfig =
            createConfig(
                photoId = 1L,
                parentId = 10L,
                ownerType = PhotoOwner.Event
            )
        val secondConfig =
            createConfig(
                photoId = 1L,
                parentId = 11L,
                ownerType = PhotoOwner.Event
            )

        val firstKey = buildPhotoDetailViewModelKey(firstConfig)
        val secondKey = buildPhotoDetailViewModelKey(secondConfig)

        assertNotEquals(firstKey, secondKey)
    }

    @Test
    fun buildPhotoDetailViewModelKey_whenSameConfig_thenStableKey() {
        val config =
            createConfig(
                photoId = 7L,
                parentId = 42L,
                ownerType = PhotoOwner.Park
            )

        val firstKey = buildPhotoDetailViewModelKey(config)
        val secondKey = buildPhotoDetailViewModelKey(config)

        assertEquals(firstKey, secondKey)
        assertEquals("photo_Park_42_7", firstKey)
    }

    private fun createConfig(
        photoId: Long,
        parentId: Long,
        ownerType: PhotoOwner
    ) = PhotoDetailConfig(
        photoId = photoId,
        parentId = parentId,
        parentTitle = "Title",
        isAuthor = true,
        photoUrl = "https://example.com/photo.jpg",
        ownerType = ownerType
    )
}
