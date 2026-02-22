package com.swparks.ui.viewmodel

import android.net.Uri
import com.swparks.ui.model.Gender
import com.swparks.ui.state.EditProfileEvent
import com.swparks.ui.state.EditProfileUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для EditProfileViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IEditProfileViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<EditProfileUiState>

    /**
     * Поток событий (side-effects) для навигации и показа ошибок.
     */
    val events: SharedFlow<EditProfileEvent>

    /**
     * Обновить логин.
     */
    fun onLoginChange(value: String)

    /**
     * Обновить email.
     */
    fun onEmailChange(value: String)

    /**
     * Обновить полное имя.
     */
    fun onFullNameChange(value: String)

    /**
     * Обновить пол.
     */
    fun onGenderChange(gender: Gender)

    /**
     * Обновить дату рождения.
     * @param timestamp Unix timestamp в миллисекундах
     */
    fun onBirthDateChange(timestamp: Long)

    /**
     * Обработчик клика по стране.
     */
    fun onCountryClick()

    /**
     * Обработчик клика по городу.
     */
    fun onCityClick()

    /**
     * Обработчик клика по кнопке смены пароля.
     */
    fun onChangePasswordClick()

    /**
     * Обработчик клика по кнопке изменения фото.
     */
    fun onChangeAvatarClick()

    /**
     * Обработчик выбора фото из галереи.
     *
     * @param uri URI выбранного фото или null если пользователь отменил выбор
     */
    fun onAvatarSelected(uri: Uri?)

    /**
     * Обработчик клика по кнопке сохранения.
     */
    fun onSaveClick()

    /**
     * Обрабатывает выбор страны.
     *
     * @param countryName Имя выбранной страны
     */
    fun onCountrySelected(countryName: String)

    /**
     * Обрабатывает выбор города.
     *
     * @param cityName Имя выбранного города
     */
    fun onCitySelected(cityName: String)

    /**
     * Сбрасывает несохранённые изменения к исходным значениям.
     *
     * Вызывается при закрытии экрана без сохранения для восстановления
     * исходных значений формы, страны и города.
     */
    fun resetChanges()

    /**
     * Обработчик клика по кнопке удаления профиля.
     */
    fun onDeleteProfileClick()
}
