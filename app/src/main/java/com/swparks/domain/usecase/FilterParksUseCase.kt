package com.swparks.domain.usecase

import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter

class FilterParksUseCase : IFilterParksUseCase {
    override operator fun invoke(allParks: List<Park>, filter: ParkFilter): List<Park> {
        val allowedSizes = filter.sizes.map { it.rawValue }.toSet()
        val allowedTypes = filter.types.map { it.rawValue }.toSet()
        return allParks.filter { park ->
            allowedSizes.contains(park.sizeID) &&
                allowedTypes.contains(park.typeID) &&
                (filter.selectedCityId == null || park.cityID == filter.selectedCityId)
        }
    }
}