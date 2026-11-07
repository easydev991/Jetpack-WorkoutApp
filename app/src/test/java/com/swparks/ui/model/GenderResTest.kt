package com.swparks.ui.model

import com.swparks.R
import com.swparks.domain.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Test

class GenderResTest {
    @Test
    fun description_res_when_male_is_man_string() {
        assertEquals(R.string.man, Gender.MALE.descriptionRes)
    }

    @Test
    fun description_res_when_female_is_woman_string() {
        assertEquals(R.string.woman, Gender.FEMALE.descriptionRes)
    }

    @Test
    fun sex_res_when_male_is_male_string() {
        assertEquals(R.string.male, Gender.MALE.sexRes)
    }

    @Test
    fun sex_res_when_female_is_female_string() {
        assertEquals(R.string.female, Gender.FEMALE.sexRes)
    }

    @Test
    fun gender_has_two_entries() {
        assertEquals(2, Gender.entries.size)
    }
}
