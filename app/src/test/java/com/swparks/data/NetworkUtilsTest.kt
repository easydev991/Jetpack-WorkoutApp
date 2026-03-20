package com.swparks.data

import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun createImagePart_withDefaultParams_usesCorrectMimeTypeAndFilename() {
        val data = ByteArray(10) { 0xFF.toByte() }
        val part = NetworkUtils.createImagePart(data, "photo1")

        assertPartHasName(part, "photo1")
        assertPartHasContentType(part, "image/jpeg")
        assertPartHasFilename(part, "photo1.jpg")
    }

    @Test
    fun createImagePart_withExplicitFilename_usesProvidedFilename() {
        val data = ByteArray(10) { 0xFF.toByte() }
        val part = NetworkUtils.createImagePart(data, "photo1", "custom.jpg")

        assertPartHasFilename(part, "custom.jpg")
        assertPartHasContentType(part, "image/jpeg")
    }

    @Test
    fun createImagePart_withExplicitMimeType_usesProvidedMimeType() {
        val data = ByteArray(10) { 0xFF.toByte() }
        val part = NetworkUtils.createImagePart(data, "photo1", mimeType = "image/png")

        assertPartHasContentType(part, "image/png")
    }

    @Test
    fun createOptionalImagePart_withNullData_returnsNull() {
        val part = NetworkUtils.createOptionalImagePart(null, "photo1")
        assertNull(part)
    }

    @Test
    fun createOptionalImagePart_withData_returnsPart() {
        val data = ByteArray(10) { 0xFF.toByte() }
        val part = NetworkUtils.createOptionalImagePart(data, "photo1")

        assertNotNull(part)
        assertPartHasName(part!!, "photo1")
    }

    private fun assertPartHasName(part: MultipartBody.Part, expectedName: String) {
        val contentDisposition = part.headers?.get("Content-Disposition")
        assertNotNull("Content-Disposition should not be null", contentDisposition)
        assertTrue(
            "Content-Disposition should contain name=$expectedName, but was: $contentDisposition",
            contentDisposition?.contains("name=\"$expectedName\"") == true
        )
    }

    private fun assertPartHasContentType(part: MultipartBody.Part, expectedType: String) {
        val body = part.body
        val contentType = body.contentType()
        assertNotNull("Content-Type should not be null", contentType)
        assertEquals(expectedType, contentType.toString())
    }

    private fun assertPartHasFilename(part: MultipartBody.Part, expectedFilename: String) {
        val contentDisposition = part.headers?.get("Content-Disposition")
        assertTrue(
            "Content-Disposition should contain filename=$expectedFilename, but was: $contentDisposition",
            contentDisposition?.contains("filename=\"$expectedFilename\"") == true
        )
    }
}
