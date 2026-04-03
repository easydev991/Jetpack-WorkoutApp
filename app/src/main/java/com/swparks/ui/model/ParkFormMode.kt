package com.swparks.ui.model

import com.swparks.data.model.Park

sealed class ParkFormMode {
    data class Create(
        val initialAddress: String,
        val initialLatitude: String,
        val initialLongitude: String,
        val initialCityId: Int?
    ) : ParkFormMode()

    data class Edit(
        val parkId: Long,
        val park: Park
    ) : ParkFormMode()
}
