package com.swparks.ui.screens.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.SWRepository
import com.swparks.model.Event
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUIState {
    data class Success(val events: List<Event>): EventsUIState
    data class Error(val message: String?): EventsUIState
    object Loading: EventsUIState
}

class EventsViewModel(private val swRepository: SWRepository): ViewModel() {
    var eventsUIState: EventsUIState by mutableStateOf(EventsUIState.Loading)
        private set

    init {
        getPastEvents()
    }

    fun getPastEvents() {
        viewModelScope.launch {
            eventsUIState = EventsUIState.Loading
            eventsUIState = try {
                val pastEvents = swRepository.getPastEvents()
                EventsUIState.Success(events = pastEvents)
            } catch (e: IOException) {
                EventsUIState.Error(message = e.message)
            } catch (e: HttpException) {
                EventsUIState.Error(message = e.message())
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication)
                val swRepository = application.container.swRepository
                EventsViewModel(swRepository = swRepository)
            }
        }
    }
}