package com.swparks.domain.usecase

import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterParksUseCaseTest {

    private val filterParksUseCase: IFilterParksUseCase = FilterParksUseCase()

    private fun createPark(sizeID: Int, typeID: Int) = Park(
        id = 1L,
        name = "Test Park",
        sizeID = sizeID,
        typeID = typeID,
        longitude = "0.0",
        latitude = "0.0",
        address = "Test Address",
        cityID = 1,
        countryID = 1,
        preview = ""
    )

    @Test
    fun filterParks_whenNoFilter_returnsAllParks() {
        val parks = listOf(
            createPark(ParkSize.SMALL.rawValue, ParkType.SOVIET.rawValue),
            createPark(ParkSize.MEDIUM.rawValue, ParkType.MODERN.rawValue),
            createPark(ParkSize.LARGE.rawValue, ParkType.COLLARS.rawValue)
        )
        val filter = ParkFilter()

        val result = filterParksUseCase(parks, filter)

        assertEquals(3, result.size)
    }

    @Test
    fun filterParks_whenSizeFilter_filtersBySize() {
        val parks = listOf(
            createPark(ParkSize.SMALL.rawValue, ParkType.SOVIET.rawValue),
            createPark(ParkSize.MEDIUM.rawValue, ParkType.SOVIET.rawValue),
            createPark(ParkSize.LARGE.rawValue, ParkType.SOVIET.rawValue)
        )
        val filter = ParkFilter(sizes = setOf(ParkSize.SMALL, ParkSize.MEDIUM))

        val result = filterParksUseCase(parks, filter)

        assertEquals(2, result.size)
        assertTrue(result.all { it.sizeID == ParkSize.SMALL.rawValue || it.sizeID == ParkSize.MEDIUM.rawValue })
    }

    @Test
    fun filterParks_whenTypeFilter_filtersByType() {
        val parks = listOf(
            createPark(ParkSize.SMALL.rawValue, ParkType.SOVIET.rawValue),
            createPark(ParkSize.SMALL.rawValue, ParkType.MODERN.rawValue),
            createPark(ParkSize.SMALL.rawValue, ParkType.COLLARS.rawValue)
        )
        val filter = ParkFilter(types = setOf(ParkType.SOVIET, ParkType.MODERN))

        val result = filterParksUseCase(parks, filter)

        assertEquals(2, result.size)
        assertTrue(result.all { it.typeID == ParkType.SOVIET.rawValue || it.typeID == ParkType.MODERN.rawValue })
    }

    @Test
    fun filterParks_whenBothFilters_usesAndLogic() {
        val parks = listOf(
            createPark(ParkSize.SMALL.rawValue, ParkType.SOVIET.rawValue),
            createPark(ParkSize.MEDIUM.rawValue, ParkType.SOVIET.rawValue),
            createPark(ParkSize.SMALL.rawValue, ParkType.MODERN.rawValue),
            createPark(ParkSize.MEDIUM.rawValue, ParkType.MODERN.rawValue)
        )
        val filter = ParkFilter(
            sizes = setOf(ParkSize.SMALL),
            types = setOf(ParkType.SOVIET)
        )

        val result = filterParksUseCase(parks, filter)

        assertEquals(1, result.size)
        assertEquals(ParkSize.SMALL.rawValue, result[0].sizeID)
        assertEquals(ParkType.SOVIET.rawValue, result[0].typeID)
    }

    @Test
    fun filterParks_whenNoMatch_returnsEmptyList() {
        val parks = listOf(
            createPark(ParkSize.SMALL.rawValue, ParkType.SOVIET.rawValue)
        )
        val filter = ParkFilter(
            sizes = setOf(ParkSize.LARGE),
            types = setOf(ParkType.LEGENDARY)
        )

        val result = filterParksUseCase(parks, filter)

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterParks_with9000Parks_completesQuickly() {
        val parks = List(9000) { index ->
            createPark(
                sizeID = (index % 3) + 1,
                typeID = when (index % 4) {
                    0 -> ParkType.SOVIET.rawValue
                    1 -> ParkType.MODERN.rawValue
                    2 -> ParkType.COLLARS.rawValue
                    else -> ParkType.LEGENDARY.rawValue
                }
            )
        }
        val filter = ParkFilter(
            sizes = setOf(ParkSize.SMALL, ParkSize.MEDIUM),
            types = setOf(ParkType.SOVIET, ParkType.MODERN)
        )

        val startTime = System.currentTimeMillis()
        val result = filterParksUseCase(parks, filter)
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Filter took ${duration}ms, should be under 100ms", duration < 100)
        assertTrue(result.size < 9000)
    }
}