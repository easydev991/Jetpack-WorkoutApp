package com.swparks.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import com.swparks.ui.viewmodel.IEventsViewModel
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUIState {
    data class Success(val events: List<Event>) : EventsUIState
    data class Error(val message: String?) : EventsUIState
    object Loading : EventsUIState
}

/**
 * ViewModel для экрана списка мероприятий.
 *
 * Управляет загрузкой и отображением списка прошедших мероприятий.
 * Обрабатывает сетевые ошибки и ошибки сервера через UserNotifier.
 *
 * @param swRepository Репозиторий для загрузки мероприятий
 * @param userNotifier Интерфейс для обработки и отправки ошибок в UI-слой
 */
class EventsViewModel(
    private val swRepository: SWRepository,
    private val userNotifier: UserNotifier,
) : ViewModel(), IEventsViewModel {
    private val _eventsUIState = MutableStateFlow<EventsUIState>(EventsUIState.Loading)
    override val eventsUIState: StateFlow<EventsUIState> = _eventsUIState

    init {
        getPastEvents()
    }

    /**
     * Загружает список прошедших мероприятий.
     *
     * При успешной загрузке обновляет состояние UI списком мероприятий.
     * При ошибке сети или сервера отправляет ошибку через UserNotifier
     * и сохраняет сообщение об ошибке в UI state.
     */
    override fun getPastEvents() {
        viewModelScope.launch {
            _eventsUIState.value = EventsUIState.Loading
            _eventsUIState.value = try {
                val pastEvents = swRepository.getPastEvents()
                EventsUIState.Success(events = pastEvents)
            } catch (e: IOException) {
                userNotifier.handleError(
                    AppError.Network(
                        message = "Не удалось загрузить мероприятия. Проверьте подключение к интернету.",
                        throwable = e
                    )
                )
                EventsUIState.Error(message = e.message)
            } catch (e: HttpException) {
                val errorMessage = e.message()
                userNotifier.handleError(
                    AppError.Server(
                        code = e.code(),
                        message = "Ошибка сервера при загрузке мероприятий: $errorMessage"
                    )
                )
                EventsUIState.Error(message = errorMessage)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JetpackWorkoutApplication
                val swRepository = application.container.swRepository
                val userNotifier = application.container.userNotifier
                EventsViewModel(
                    swRepository = swRepository,
                    userNotifier = userNotifier
                )
            }
        }
    }
}