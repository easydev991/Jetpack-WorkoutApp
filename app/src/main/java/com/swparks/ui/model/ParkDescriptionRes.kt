package com.swparks.ui.model

import com.swparks.R
import com.swparks.domain.model.ParkSize
import com.swparks.domain.model.ParkType

/**
 * Размер площадки в виде ресурса строки.
 * Domain-модель [ParkSize] не зависит от Android-ресурсов — маппинг выполняется в UI-слое
 */
val ParkSize.descriptionRes: Int
    get() =
        when (this) {
            ParkSize.SMALL -> R.string.small_park
            ParkSize.MEDIUM -> R.string.medium_park
            ParkSize.LARGE -> R.string.large_park
        }

/**
 * Тип площадки в виде ресурса строки.
 * Domain-модель [ParkType] не зависит от Android-ресурсов — маппинг выполняется в UI-слое
 */
val ParkType.descriptionRes: Int
    get() =
        when (this) {
            ParkType.SOVIET -> R.string.soviet_park
            ParkType.MODERN -> R.string.modern_park
            ParkType.COLLARS -> R.string.collars_park
            ParkType.LEGENDARY -> R.string.legendary_park
        }
