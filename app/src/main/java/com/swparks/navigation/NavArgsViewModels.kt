package com.swparks.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry

internal class EditEventNavArgsViewModel(
    val args: EditEventNavArgs?
) : ViewModel() {
    companion object {
        fun factory(navBackStackEntry: NavBackStackEntry): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EditEventNavArgsViewModel::class.java)) {
                        "Неизвестный класс ViewModel: ${modelClass.name}, " +
                            "ожидается: ${EditEventNavArgsViewModel::class.java.name}"
                    }
                    val viewModel =
                        EditEventNavArgsViewModel(navBackStackEntry.consumeEditEventArgs())
                    return checkNotNull(modelClass.cast(viewModel)) {
                        "Не удалось привести ${EditEventNavArgsViewModel::class.java.name} к ${modelClass.name}"
                    }
                }
            }
    }
}

internal class EventParticipantsNavArgsViewModel(
    val args: EventParticipantsNavArgs?
) : ViewModel() {
    companion object {
        fun factory(navBackStackEntry: NavBackStackEntry): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EventParticipantsNavArgsViewModel::class.java)) {
                        "Неизвестный класс ViewModel: ${modelClass.name}, " +
                            "ожидается: ${EventParticipantsNavArgsViewModel::class.java.name}"
                    }
                    val viewModel =
                        EventParticipantsNavArgsViewModel(navBackStackEntry.consumeEventParticipantsArgs())
                    return checkNotNull(modelClass.cast(viewModel)) {
                        "Не удалось привести ${EventParticipantsNavArgsViewModel::class.java.name} к ${modelClass.name}"
                    }
                }
            }
    }
}

internal class ParkTraineesNavArgsViewModel(
    val args: ParkTraineesNavArgs?
) : ViewModel() {
    companion object {
        fun factory(navBackStackEntry: NavBackStackEntry): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ParkTraineesNavArgsViewModel::class.java)) {
                        "Неизвестный класс ViewModel: ${modelClass.name}, " +
                            "ожидается: ${ParkTraineesNavArgsViewModel::class.java.name}"
                    }
                    val viewModel =
                        ParkTraineesNavArgsViewModel(navBackStackEntry.consumeParkTraineesArgs())
                    return checkNotNull(modelClass.cast(viewModel)) {
                        "Не удалось привести ${ParkTraineesNavArgsViewModel::class.java.name} к ${modelClass.name}"
                    }
                }
            }
    }
}
