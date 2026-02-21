package com.swparks.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Тесты для ImageUtils.
 */
class ImageUtilsTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // MARK: - isSupportedMimeType tests

    @Test
    fun isSupportedMimeType_jpeg_returnsTrue() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns "image/jpeg"

        // Act
        val result = ImageUtils.isSupportedMimeType(context, uri)

        // Assert
        Assert.assertTrue(result)
    }

    @Test
    fun isSupportedMimeType_png_returnsTrue() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns "image/png"

        // Act
        val result = ImageUtils.isSupportedMimeType(context, uri)

        // Assert
        Assert.assertTrue(result)
    }

    @Test
    fun isSupportedMimeType_webp_returnsTrue() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns "image/webp"

        // Act
        val result = ImageUtils.isSupportedMimeType(context, uri)

        // Assert
        Assert.assertTrue(result)
    }

    @Test
    fun isSupportedMimeType_gif_returnsFalse() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns "image/gif"

        // Act
        val result = ImageUtils.isSupportedMimeType(context, uri)

        // Assert
        Assert.assertFalse(result)
    }

    @Test
    fun isSupportedMimeType_nullMimeType_returnsFalse() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns null

        // Act
        val result = ImageUtils.isSupportedMimeType(context, uri)

        // Assert
        Assert.assertFalse(result)
    }

    @Test
    fun isSupportedMimeType_unsupportedType_returnsFalse() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns "video/mp4"

        // Act
        val result = ImageUtils.isSupportedMimeType(context, uri)

        // Assert
        Assert.assertFalse(result)
    }

    // MARK: - compressIfNeeded tests

    @Test
    fun compressIfNeeded_smallImage_returnsSameData() {
        // Arrange
        val smallData = ByteArray(1000) { it.toByte() }

        // Act
        val result = ImageUtils.compressIfNeeded(smallData)

        // Assert
        Assert.assertSame(smallData, result)
    }

    @Test
    fun compressIfNeeded_exactMaxSize_returnsSameData() {
        // Arrange
        val maxSize = 1024
        val exactMaxData = ByteArray(maxSize) { it.toByte() }

        // Act
        val result = ImageUtils.compressIfNeeded(exactMaxData, maxSize)

        // Assert
        Assert.assertSame(exactMaxData, result)
    }

    @Test
    fun compressIfNeeded_belowMaxSize_returnsSameData() {
        // Arrange
        val maxSize = 1024
        val belowMaxData = ByteArray(maxSize - 1) { it.toByte() }

        // Act
        val result = ImageUtils.compressIfNeeded(belowMaxData, maxSize)

        // Assert
        Assert.assertSame(belowMaxData, result)
    }

    // MARK: - Constants tests

    @Test
    fun maxImageSizeBytes_is5MB() {
        Assert.assertEquals(5 * 1024 * 1024, ImageUtils.MAX_IMAGE_SIZE_BYTES)
    }

    @Test
    fun supportedMimeTypes_containsJpeg() {
        Assert.assertTrue(ImageUtils.SUPPORTED_MIME_TYPES.contains("image/jpeg"))
    }

    @Test
    fun supportedMimeTypes_containsPng() {
        Assert.assertTrue(ImageUtils.SUPPORTED_MIME_TYPES.contains("image/png"))
    }

    @Test
    fun supportedMimeTypes_containsWebp() {
        Assert.assertTrue(ImageUtils.SUPPORTED_MIME_TYPES.contains("image/webp"))
    }

    @Test
    fun supportedMimeTypes_doesNotContainGif() {
        Assert.assertFalse(ImageUtils.SUPPORTED_MIME_TYPES.contains("image/gif"))
    }
}
