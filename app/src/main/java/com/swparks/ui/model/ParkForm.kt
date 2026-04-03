package com.swparks.ui.model

import com.swparks.data.model.Park
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ParkForm(
    val address: String = "",
    val latitude: String = "",
    val longitude: String = "",
    @SerialName("city_id")
    val cityId: Int? = null,
    @SerialName("type_id")
    val typeId: Int = ParkType.SOVIET.rawValue,
    @SerialName("class_id")
    val sizeId: Int = ParkSize.SMALL.rawValue,
    @Transient
    val photosCount: Int = 0,
    @Transient
    val selectedPhotos: List<String> = emptyList()
) {
    val isReadyToCreate: Boolean
        get() = hasValidStrings && cityId != null && cityId != 0 && selectedPhotos.isNotEmpty()

    fun isReadyToUpdate(old: ParkForm): Boolean {
        val canSaveUpdated = hasValidStrings
        return canSaveUpdated && this != old
    }

    val imagesLimit: Int
        get() = (PHOTOS_LIMIT - selectedPhotos.size - photosCount).coerceAtLeast(0)

    val gradeStringRes: Int
        get() =
            ParkType.entries.firstOrNull { it.rawValue == typeId }?.description
                ?: 0

    val sizeStringRes: Int
        get() =
            ParkSize.entries.firstOrNull { it.rawValue == sizeId }?.description
                ?: 0

    private val hasValidStrings: Boolean
        get() = address.isNotBlank() && latitude.isNotBlank() && longitude.isNotBlank()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParkForm) return false
        return address == other.address &&
            latitude == other.latitude &&
            longitude == other.longitude &&
            cityId == other.cityId &&
            typeId == other.typeId &&
            sizeId == other.sizeId &&
            selectedPhotos == other.selectedPhotos
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + (cityId ?: 0)
        result = 31 * result + typeId
        result = 31 * result + sizeId
        result = 31 * result + selectedPhotos.hashCode()
        return result
    }

    companion object {
        const val PHOTOS_LIMIT = 15

        fun fromPark(park: Park): ParkForm =
            ParkForm(
                address = park.address,
                latitude = park.latitude,
                longitude = park.longitude,
                cityId = park.cityID,
                typeId = park.typeID,
                sizeId = park.sizeID,
                photosCount = park.photos?.size ?: 0,
                selectedPhotos = emptyList()
            )

        fun create(
            address: String,
            latitude: Double,
            longitude: Double,
            cityId: Int?
        ): ParkForm =
            ParkForm(
                address = address,
                latitude = latitude.toString(),
                longitude = longitude.toString(),
                cityId = cityId,
                typeId = ParkType.SOVIET.rawValue,
                sizeId = ParkSize.SMALL.rawValue,
                photosCount = 0,
                selectedPhotos = emptyList()
            )
    }
}
