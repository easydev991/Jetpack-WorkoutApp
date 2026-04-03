package com.swparks.data.model

import kotlinx.serialization.Serializable

/**
 * Модель черновика для создания новой площадки.
 * Android-аналог iOS NewParkMapModel.
 *
 * Содержит координаты и данные геокодирования (адрес, cityId) для предзаполнения формы создания площадки.
 *
 * @property latitude Широта
 * @property longitude Долгота
 * @property lastLocationRequestDate Дата последнего запроса локации
 * @property address Адрес площадки (получен через геокодирование)
 * @property cityId Идентификатор города (получен через геокодирование)
 */
@Serializable
data class NewParkDraft(
    val latitude: Double,
    val longitude: Double,
    val lastLocationRequestDate: Long? = null,
    val address: String = "",
    val cityId: Int? = null
) {
    /**
     * Пустая ли модель (координаты не заданы)
     */
    val isEmpty: Boolean
        get() = latitude == 0.0 || longitude == 0.0

    /**
     * Нужно ли запрашивать новую локацию
     */
    val shouldRequestLocation: Boolean
        get() {
            if (isEmpty) return true
            val lastDate = lastLocationRequestDate ?: return true
            val timeSinceLastRequest = System.currentTimeMillis() - lastDate
            return timeSinceLastRequest > LOCATION_STALE_THRESHOLD_MILLIS
        }

    /**
     * Нужно ли выполнять геокодирование
     */
    val shouldPerformGeocode: Boolean
        get() = address.isEmpty() || cityId == null || cityId == 0

    companion object {
        private const val LOCATION_STALE_THRESHOLD_MILLIS = 10_000L

        val EMPTY =
            NewParkDraft(
                latitude = 0.0,
                longitude = 0.0,
                lastLocationRequestDate = null,
                address = "",
                cityId = null
            )
    }

    /**
     * Создает копию с обновленными координатами
     */
    fun withCoordinates(
        newLatitude: Double,
        newLongitude: Double
    ): NewParkDraft =
        copy(
            latitude = newLatitude,
            longitude = newLongitude,
            lastLocationRequestDate = System.currentTimeMillis()
        )

    /**
     * Создает копию с данными геокодирования
     */
    fun withGeocodingData(
        newAddress: String,
        newCityId: Int
    ): NewParkDraft =
        copy(
            address = newAddress,
            cityId = newCityId
        )

    /**
     * Создает копию с обнуленным адресом для принудительного геокодирования
     */
    fun withoutAddress(): NewParkDraft = copy(address = "")

    /**
     * Обновляет дату последнего запроса локации
     */
    fun updatingLastLocationRequestDate(): NewParkDraft =
        copy(lastLocationRequestDate = System.currentTimeMillis())
}
