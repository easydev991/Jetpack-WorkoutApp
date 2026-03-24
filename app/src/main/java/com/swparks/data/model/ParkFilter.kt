package com.swparks.data.model

data class ParkFilter(
    val sizes: Set<ParkSize> = ParkSize.entries.toSet(),
    val types: Set<ParkType> = ParkType.entries.toSet(),
    val selectedCityId: Int? = null
) {
    val isDefault: Boolean get() = this == ParkFilter()
}