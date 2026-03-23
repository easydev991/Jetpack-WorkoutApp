package com.swparks.data.model

data class ParkFilter(
    val sizes: Set<ParkSize> = ParkSize.entries.toSet(),
    val types: Set<ParkType> = ParkType.entries.toSet()
) {
    val isDefault: Boolean get() = this == ParkFilter()
}