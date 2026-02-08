package com.swparks.ui.viewmodel

import com.swparks.ui.state.JournalsUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake-реализация JournalsViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможность установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeJournalsViewModel(
    override val uiState: StateFlow<JournalsUiState>,
    override val isRefreshing: StateFlow<Boolean>
) : IJournalsViewModel {

    /**
     * Функция-заглушка для повторной загрузки.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun retry() {
        // Заглушка - не делает ничего в тестах
    }

    /**
     * Функция-заглушка для загрузки.
     * В тестах состояние устанавливается напрямую через конструктор.
     */
    override fun loadJournals() {
        // Заглушка - не делает ничего в тестах
    }
}
