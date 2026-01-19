package com.swparks.utils

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReadJSONFromAssetsTest {
    private lateinit var mockContext: Context
    private lateinit var mockAssetManager: AssetManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAssetManager = mockk(relaxed = true)
        every { mockContext.assets } returns mockAssetManager
    }

    @Test
    fun readJSONFromAssets_whenFileExists_thenReturnsContent() {
        // Given
        val testJsonContent = """{"test": "data"}"""
        val inputStream = ByteArrayInputStream(testJsonContent.toByteArray())
        every { mockAssetManager.open("test.json") } returns inputStream

        mockkStatic(android.util.Log::class)
        every { android.util.Log.i(any(), any()) } returns 0

        // When
        val result = ReadJSONFromAssets(mockContext, "test.json")

        // Then
        assertEquals(testJsonContent, result)
        verify { mockAssetManager.open("test.json") }
    }

    @Test
    fun readJSONFromAssets_whenFileDoesNotExist_thenReturnsEmptyString() {
        // Given
        every { mockAssetManager.open("nonexistent.json") } throws IOException("File not found")

        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0

        // When
        val result = ReadJSONFromAssets(mockContext, "nonexistent.json")

        // Then
        assertEquals("", result)
        verify { mockAssetManager.open("nonexistent.json") }
    }

    @Test
    fun readJSONFromAssets_whenExceptionOccurs_thenReturnsEmptyString() {
        // Given
        val exception = RuntimeException("Unexpected error")
        every { mockAssetManager.open("test.json") } throws exception

        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0

        // When
        val result = ReadJSONFromAssets(mockContext, "test.json")

        // Then
        assertEquals("", result)
        verify { mockAssetManager.open("test.json") }
    }
}
