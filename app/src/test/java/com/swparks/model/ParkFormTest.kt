package com.swparks.model

import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.ui.model.ParkForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkFormTest {
    private val testPhotoUri = "content://test/photo1"

    @Suppress("LongParameterList")
    private fun createTestForm(
        address: String = "address",
        latitude: String = "latitude",
        longitude: String = "longitude",
        cityId: Int? = 1,
        typeId: Int = 1,
        sizeId: Int = 1,
        photosCount: Int = 0,
        selectedPhotos: List<String> = listOf(testPhotoUri)
    ) = ParkForm(
        address = address,
        latitude = latitude,
        longitude = longitude,
        cityId = cityId,
        typeId = typeId,
        sizeId = sizeId,
        photosCount = photosCount,
        selectedPhotos = selectedPhotos
    )

    private fun createTestPark(
        id: Long = 1,
        typeId: Int = 1,
        sizeId: Int = 1,
        address: String = "address",
        cityId: Int = 1,
        latitude: String = "latitude",
        longitude: String = "longitude",
        photos: List<Photo>? = listOf(Photo(1, "photo1"))
    ) = Park(
        id = id,
        name = "Park Name",
        sizeID = sizeId,
        typeID = typeId,
        longitude = longitude,
        latitude = latitude,
        address = address,
        cityID = cityId,
        countryID = 1,
        preview = "preview",
        photos = photos
    )

    @Test
    fun address_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(address = "123 Test Street")

        // When & Then
        assertEquals("123 Test Street", form.address)
    }

    @Test
    fun latitude_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(latitude = "59.9343")

        // When & Then
        assertEquals("59.9343", form.latitude)
    }

    @Test
    fun longitude_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(longitude = "30.3351")

        // When & Then
        assertEquals("30.3351", form.longitude)
    }

    @Test
    fun cityId_whenValueIsNotNull_thenReturnsValue() {
        // Given
        val form = createTestForm(cityId = 1)

        // When & Then
        assertEquals(1, form.cityId)
    }

    @Test
    fun cityId_whenValueIsNull_thenReturnsNull() {
        // Given
        val form = createTestForm(cityId = null)

        // When & Then
        assertNull(form.cityId)
    }

    @Test
    fun typeId_whenValueIs1_thenReturns1() {
        // Given
        val form = createTestForm(typeId = 1)

        // When & Then
        assertEquals(1, form.typeId)
    }

    @Test
    fun typeId_whenValueIs2_thenReturns2() {
        // Given
        val form = createTestForm(typeId = 2)

        // When & Then
        assertEquals(2, form.typeId)
    }

    @Test
    fun sizeId_whenValueIs1_thenReturns1() {
        // Given
        val form = createTestForm(sizeId = 1)

        // When & Then
        assertEquals(1, form.sizeId)
    }

    @Test
    fun sizeId_whenValueIs2_thenReturns2() {
        // Given
        val form = createTestForm(sizeId = 2)

        // When & Then
        assertEquals(2, form.sizeId)
    }

    @Test
    fun sizeId_whenValueIs3_thenReturns3() {
        // Given
        val form = createTestForm(sizeId = 3)

        // When & Then
        assertEquals(3, form.sizeId)
    }

    @Test
    fun form_whenAllFieldsArePresent_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            address = "123 Fitness Street",
            latitude = "55.7558",
            longitude = "37.6173",
            cityId = 1,
            typeId = 2,
            sizeId = 2
        )

        // When & Then
        assertEquals("123 Fitness Street", form.address)
        assertEquals("55.7558", form.latitude)
        assertEquals("37.6173", form.longitude)
        assertEquals(1, form.cityId)
        assertEquals(2, form.typeId)
        assertEquals(2, form.sizeId)
    }

    @Test
    fun form_whenCityIdIsNull_thenCreatesCorrectly() {
        // Given
        val form = createTestForm(
            address = "123 Fitness Street",
            latitude = "55.7558",
            longitude = "37.6173",
            cityId = null,
            typeId = 1,
            sizeId = 1
        )

        // When & Then
        assertEquals("123 Fitness Street", form.address)
        assertEquals("55.7558", form.latitude)
        assertEquals("37.6173", form.longitude)
        assertNull(form.cityId)
        assertEquals(1, form.typeId)
        assertEquals(1, form.sizeId)
    }

    @Test
    fun isReadyToCreate_whenAllFieldsValid_thenReturnsTrue() {
        val form = createTestForm()
        assertTrue(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenAddressEmpty_thenReturnsFalse() {
        val form = createTestForm(address = "")
        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenAddressSpaces_thenReturnsFalse() {
        val form1 = createTestForm(address = " ")
        val form2 = createTestForm(address = "   ")
        assertFalse(form1.isReadyToCreate)
        assertFalse(form2.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenLatitudeEmpty_thenReturnsFalse() {
        val form = createTestForm(latitude = "")
        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenLongitudeEmpty_thenReturnsFalse() {
        val form = createTestForm(longitude = "")
        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenCityIdZero_thenReturnsFalse() {
        val form = createTestForm(cityId = 0)
        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenCityIdNull_thenReturnsFalse() {
        val form = createTestForm(cityId = null)
        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToCreate_whenSelectedPhotosEmpty_thenReturnsFalse() {
        val form = createTestForm(selectedPhotos = emptyList())
        assertFalse(form.isReadyToCreate)
    }

    @Test
    fun isReadyToUpdate_whenAllFieldsValidAndChanged_thenReturnsTrue() {
        val oldForm = createTestForm(address = "old")
        val newForm = createTestForm()
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenAddressChanged_thenReturnsTrue() {
        val oldForm = createTestForm(address = "old")
        val newForm = createTestForm()
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenLatitudeChanged_thenReturnsTrue() {
        val oldForm = createTestForm(latitude = "old")
        val newForm = createTestForm()
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenLongitudeChanged_thenReturnsTrue() {
        val oldForm = createTestForm(longitude = "old")
        val newForm = createTestForm()
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenCityIdChanged_thenReturnsTrue() {
        val oldForm = createTestForm(cityId = 123)
        val newForm = createTestForm()
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenTypeIdChanged_thenReturnsTrue() {
        val oldForm = createTestForm(typeId = 123)
        val newForm = createTestForm()
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenSizeIdChanged_thenReturnsTrue() {
        val oldForm = createTestForm(sizeId = 123)
        val newForm = createTestForm()
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenSelectedPhotosChanged_thenReturnsTrue() {
        val oldForm = createTestForm(selectedPhotos = emptyList())
        val newForm = createTestForm(selectedPhotos = listOf(testPhotoUri))
        assertTrue(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenNothingChanged_thenReturnsFalse() {
        val oldForm = createTestForm()
        val newForm = createTestForm()
        assertFalse(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenAddressEmpty_thenReturnsFalse() {
        val oldForm = createTestForm()
        val newForm = createTestForm(address = "")
        assertFalse(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenLatitudeEmpty_thenReturnsFalse() {
        val oldForm = createTestForm()
        val newForm = createTestForm(latitude = "")
        assertFalse(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun isReadyToUpdate_whenLongitudeEmpty_thenReturnsFalse() {
        val oldForm = createTestForm()
        val newForm = createTestForm(longitude = "")
        assertFalse(newForm.isReadyToUpdate(oldForm))
    }

    @Test
    fun imagesLimit_whenNoPhotos_thenReturns15() {
        val form = createTestForm(photosCount = 0, selectedPhotos = emptyList())
        assertEquals(15, form.imagesLimit)
    }

    @Test
    fun imagesLimit_whenHasExistingPhotos_thenReturnsMinusPhotos() {
        val form = createTestForm(photosCount = 3, selectedPhotos = emptyList())
        assertEquals(12, form.imagesLimit)
    }

    @Test
    fun imagesLimit_whenHasSelectedPhotos_thenReturnsMinusSelected() {
        val form = createTestForm(photosCount = 0, selectedPhotos = listOf(testPhotoUri))
        assertEquals(14, form.imagesLimit)
    }

    @Test
    fun imagesLimit_whenAtLimit_thenReturnsZero() {
        val form = createTestForm(photosCount = 15, selectedPhotos = emptyList())
        assertEquals(0, form.imagesLimit)
    }

    @Test
    fun fromPark_createsCorrectForm() {
        val park = createTestPark(
            id = 1,
            typeId = 2,
            sizeId = 3,
            address = "Park Address",
            cityId = 5,
            latitude = "55.5",
            longitude = "37.5",
            photos = listOf(Photo(1, "photo1"), Photo(2, "photo2"))
        )
        val form = ParkForm.fromPark(park)

        assertEquals("Park Address", form.address)
        assertEquals("55.5", form.latitude)
        assertEquals("37.5", form.longitude)
        assertEquals(5, form.cityId)
        assertEquals(2, form.typeId)
        assertEquals(3, form.sizeId)
        assertEquals(2, form.photosCount)
        assertTrue(form.selectedPhotos.isEmpty())
    }

    @Test
    fun create_createsCorrectForm() {
        val form = ParkForm.create(
            address = "New Address",
            latitude = 55.5,
            longitude = 37.5,
            cityId = 3
        )

        assertEquals("New Address", form.address)
        assertEquals("55.5", form.latitude)
        assertEquals("37.5", form.longitude)
        assertEquals(3, form.cityId)
        assertEquals(1, form.typeId)
        assertEquals(1, form.sizeId)
        assertEquals(0, form.photosCount)
        assertTrue(form.selectedPhotos.isEmpty())
    }

    @Test
    fun equals_formsWithSameValuesAreEqual() {
        val form1 = createTestForm()
        val form2 = createTestForm()
        assertEquals(form1, form2)
    }

    @Test
    fun equals_selectedPhotosNotEmpty_differentForms() {
        val form1 = createTestForm(selectedPhotos = listOf(testPhotoUri))
        val form2 = createTestForm(selectedPhotos = emptyList())
        assertTrue(form1 != form2)
    }

    @Test
    fun equals_photosCountNotConsidered() {
        val form1 = createTestForm(photosCount = 1)
        val form2 = createTestForm(photosCount = 2)
        assertEquals(form1, form2)
    }
}
