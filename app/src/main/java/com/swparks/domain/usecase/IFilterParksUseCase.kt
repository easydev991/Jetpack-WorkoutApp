package com.swparks.domain.usecase

import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter

interface IFilterParksUseCase {
    operator fun invoke(
        allParks: List<Park>,
        filter: ParkFilter
    ): List<Park>
}
