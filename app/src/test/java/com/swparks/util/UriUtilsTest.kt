package com.swparks.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Тесты для UriUtils.
 */
class UriUtilsTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun uriToByteArray_success() {
        // Arrange
        val testData = "test image data".toByteArray()
        val uri = mockk<Uri>()
        val inputStream = ByteArrayInputStream(testData)

        every { contentResolver.openInputStream(uri) } returns inputStream

        // Act
        val result = UriUtils.uriToByteArray(context, uri)

        // Assert
        Assert.assertTrue(result.isSuccess)
        Assert.assertArrayEquals(testData, result.getOrNull())
    }

    @Test
    fun uriToByteArray_nullInputStream_returnsFailure() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.openInputStream(uri) } returns null

        // Act
        val result = UriUtils.uriToByteArray(context, uri)

        // Assert
        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun uriToByteArray_securityException_returnsFailure() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.openInputStream(uri) } throws SecurityException("No permission")

        // Act
        val result = UriUtils.uriToByteArray(context, uri)

        // Assert
        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun uriToByteArray_ioException_returnsFailure() {
        // Arrange
        val uri = mockk<Uri>()
        every { contentResolver.openInputStream(uri) } throws IOException("IO error")

        // Act
        val result = UriUtils.uriToByteArray(context, uri)

        // Assert
        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is IOException)
    }
}
