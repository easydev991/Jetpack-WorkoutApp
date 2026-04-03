package com.swparks.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit тесты для [MapUriSet].
 * Проверяют корректность формирования URI для различных сценариев работы с картой.
 */
@RunWith(RobolectricTestRunner::class)
class MapUriSetTest {
    // ==================== Тесты geoUri ====================

    @Test
    fun geoUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals("geo:55.7558,37.6173?q=55.7558,37.6173", set.geoUri.toString())
    }

    @Test
    fun geoUri_with_negative_coordinates_is_formatted_correctly() {
        val set = MapUriSet(latitude = -33.8688, longitude = -151.2093)
        assertEquals("geo:-33.8688,-151.2093?q=-33.8688,-151.2093", set.geoUri.toString())
    }

    @Test
    fun geoUri_with_zero_coordinates_is_formatted_correctly() {
        val set = MapUriSet(latitude = 0.0, longitude = 0.0)
        assertEquals("geo:0.0,0.0?q=0.0,0.0", set.geoUri.toString())
    }

    // ==================== Тесты browserUri ====================

    @Test
    fun browserUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals("https://maps.google.com/?q=55.7558,37.6173", set.browserUri.toString())
    }

    @Test
    fun browserUri_with_negative_coordinates_is_formatted_correctly() {
        val set = MapUriSet(latitude = -33.8688, longitude = -151.2093)
        assertEquals("https://maps.google.com/?q=-33.8688,-151.2093", set.browserUri.toString())
    }

    // ==================== Тесты navigationUri ====================

    @Test
    fun navigationUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals("google.navigation:q=55.7558,37.6173", set.navigationUri.toString())
    }

    @Test
    fun navigationUri_with_negative_coordinates_is_formatted_correctly() {
        val set = MapUriSet(latitude = -33.8688, longitude = -151.2093)
        assertEquals("google.navigation:q=-33.8688,-151.2093", set.navigationUri.toString())
    }

    // ==================== Тесты browserRouteUri ====================

    @Test
    fun browserRouteUri_is_formatted_correctly() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)
        assertEquals(
            "https://maps.google.com/?daddr=55.7558,37.6173",
            set.browserRouteUri.toString()
        )
    }

    @Test
    fun browserRouteUri_with_negative_coordinates_is_formatted_correctly() {
        val set = MapUriSet(latitude = -33.8688, longitude = -151.2093)
        assertEquals(
            "https://maps.google.com/?daddr=-33.8688,-151.2093",
            set.browserRouteUri.toString()
        )
    }

    // ==================== Тесты всех URI вместе ====================

    @Test
    fun all_uris_are_formatted_correctly_for_same_coordinates() {
        val set = MapUriSet(latitude = 55.7558, longitude = 37.6173)

        assertEquals("geo:55.7558,37.6173?q=55.7558,37.6173", set.geoUri.toString())
        assertEquals("https://maps.google.com/?q=55.7558,37.6173", set.browserUri.toString())
        assertEquals("google.navigation:q=55.7558,37.6173", set.navigationUri.toString())
        assertEquals(
            "https://maps.google.com/?daddr=55.7558,37.6173",
            set.browserRouteUri.toString()
        )
    }

    @Test
    fun all_uris_contain_same_coordinates() {
        val latitude = 55.7558
        val longitude = 37.6173
        val set = MapUriSet(latitude = latitude, longitude = longitude)

        val geoUriString = set.geoUri.toString()
        val browserUriString = set.browserUri.toString()
        val navigationUriString = set.navigationUri.toString()
        val browserRouteUriString = set.browserRouteUri.toString()

        // Все URI должны содержать координаты
        listOf(
            geoUriString,
            browserUriString,
            navigationUriString,
            browserRouteUriString
        ).forEach { uri ->
            assertEquals(
                "URI должен содержать latitude=$latitude и longitude=$longitude",
                true,
                uri.contains(latitude.toString()) && uri.contains(longitude.toString())
            )
        }
    }
}
