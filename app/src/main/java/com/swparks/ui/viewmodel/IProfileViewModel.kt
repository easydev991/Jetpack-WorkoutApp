package com.swparks.ui.viewmodel

import com.swparks.data.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для ProfileViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IProfileViewModel {
    /**
     * Текущий пользователь (подписка на Flow из репозитория).
     */
    val currentUser: StateFlow<User?>

    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<ProfileUiState>

    /**
     * Индикатор обновления (pull-to-refresh).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Индикатор загрузки профиля после авторизации.
     * True когда идет загрузка данных профиля с сервера.
     */
    val isLoadingProfile: StateFlow<Boolean>

    /**
     * Черный список пользователя.
     */
    val blacklist: StateFlow<List<User>>

    /**
     * Загружает профиль пользователя и социальные данные с сервера по userId.
     * Используется после успешной авторизации для загрузки свежих данных.
     *
     * @param userId ID пользователя для загрузки профиля и социальных данных
     */
    fun loadProfileFromServer(userId: Long)

    /**
     * Обновляет данные профиля пользователя с сервера (для pull-to-refresh).
     * Использует текущего пользователя из репозитория.
     */
    fun refreshProfile()
}
