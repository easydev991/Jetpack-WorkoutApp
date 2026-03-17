package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.swparks.data.AppContainer
import com.swparks.ui.model.EventFormMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFormViewModelFactoryTest {

    @Test
    fun create_whenExpectedModelClass_thenReturnsEventFormViewModel() {
        val appContainer = mockk<AppContainer>(relaxed = true) {
            every { eventFormViewModelFactory(any()) } returns mockk<EventFormViewModel>(relaxed = true)
        }
        val mode = EventFormMode.RegularCreate
        val factory = EventFormViewModel.factory(mode, appContainer)

        val result = factory.create(EventFormViewModel::class.java)

        assertNotNull(result)
        assertTrue(result is EventFormViewModel)
    }

    @Test
    fun create_whenUnexpectedModelClass_thenThrowsIllegalArgumentException() {
        val appContainer = mockk<AppContainer>(relaxed = true) {
            every { eventFormViewModelFactory(any()) } returns mockk<EventFormViewModel>(relaxed = true)
        }
        val mode = EventFormMode.RegularCreate
        val factory = EventFormViewModel.factory(mode, appContainer)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnexpectedViewModel::class.java)
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Неизвестный класс ViewModel"))
        assertTrue(message.contains(UnexpectedViewModel::class.java.name))
    }

    private class UnexpectedViewModel : ViewModel()
}
