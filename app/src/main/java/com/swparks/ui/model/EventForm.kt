package com.swparks.ui.model

import com.swparks.data.datetime.FlexibleDateDeserializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class EventForm(
    val title: String = "",
    val description: String = "",
    @Serializable(with = FlexibleDateDeserializer::class)
    val date: String = "",
    @SerialName("area_id")
    val parkId: Long = 0L,
    @Transient
    val parkName: String = "",
    @Transient
    val photosCount: Int = 0
) {
    val isReadyToCreate: Boolean
        get() = title.isNotBlank() && parkId > 0 && date.isNotBlank()

    fun isReadyToUpdate(old: EventForm): Boolean {
        return isReadyToCreate && (
            title != old.title ||
                description != old.description ||
                date != old.date ||
                parkId != old.parkId
            )
    }
}
