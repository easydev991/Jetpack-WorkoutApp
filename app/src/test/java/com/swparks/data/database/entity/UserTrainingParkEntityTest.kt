package com.swparks.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class UserTrainingParkEntityTest {

    @Test
    fun creation_withUserIdAndParkId_storesValues() {
        val userId = 1L
        val parkId = 100L

        val entity = UserTrainingParkEntity(userId = userId, parkId = parkId)

        assertEquals(userId, entity.userId)
        assertEquals(parkId, entity.parkId)
    }

    @Test
    fun creation_withPosition_storesPosition() {
        val entity = UserTrainingParkEntity(
            userId = 1L,
            parkId = 100L,
            position = 5
        )

        assertEquals(5, entity.position)
    }

    @Test
    fun primaryKey_isCompositeOfUserIdAndParkId() {
        val entity1 = UserTrainingParkEntity(userId = 1L, parkId = 100L)
        val entity2 = UserTrainingParkEntity(userId = 1L, parkId = 100L)

        assertEquals(entity1, entity2)
    }

    @Test
    fun sameParkDifferentUsers_areNotEqual() {
        val entity1 = UserTrainingParkEntity(userId = 1L, parkId = 100L)
        val entity2 = UserTrainingParkEntity(userId = 2L, parkId = 100L)

        assertEquals(entity1.userId, 1L)
        assertEquals(entity2.userId, 2L)
        assertEquals(entity1.parkId, entity2.parkId)
    }

    @Test
    fun sameUserDifferentParks_areNotEqual() {
        val entity1 = UserTrainingParkEntity(userId = 1L, parkId = 100L)
        val entity2 = UserTrainingParkEntity(userId = 1L, parkId = 200L)

        assertEquals(entity1.userId, entity2.userId)
        assertEquals(entity1.parkId, 100L)
        assertEquals(entity2.parkId, 200L)
    }
}