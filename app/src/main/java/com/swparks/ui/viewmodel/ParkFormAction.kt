package com.swparks.ui.viewmodel

sealed class ParkFormAction {
    data class AddressChange(
        val value: String
    ) : ParkFormAction()

    data class TypeChange(
        val typeId: Int
    ) : ParkFormAction()

    data class SizeChange(
        val sizeId: Int
    ) : ParkFormAction()
}
