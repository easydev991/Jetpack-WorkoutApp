package com.swparks.ui.state

import android.net.Uri
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.ui.model.MainUserForm

/**
 * UI State для экрана редактирования профиля.
 *
 * @param userForm Форма с данными пользователя
 * @param initialForm Снимок исходных значений для определения изменений
 * @param countries Список стран
 * @param cities Список городов для выбранной страны
 * @param selectedCountry Выбранная страна
 * @param selectedCity Выбранный город
 * @param initialCountry Исходная страна (для отката изменений)
 * @param initialCity Исходный город (для отката изменений)
 * @param selectedAvatarUri URI выбранного фото для превью (null если не выбрано)
 * @param avatarError Ошибка при обработке фото
 * @param emailError Ошибка валидации email
 * @param isLoading Индикатор загрузки
 * @param isSaving Индикатор сохранения
 * @param isUploadingAvatar Индикатор загрузки аватара
 */
data class EditProfileUiState(
    val userForm: MainUserForm = MainUserForm(
        name = "",
        fullname = "",
        email = "",
        password = "",
        birthDate = "",
        genderCode = 0,
        countryId = null,
        cityId = null
    ),
    val initialForm: MainUserForm = userForm,
    val countries: List<Country> = emptyList(),
    val cities: List<City> = emptyList(),
    val selectedCountry: Country? = null,
    val selectedCity: City? = null,
    val initialCountry: Country? = null,
    val initialCity: City? = null,
    val selectedAvatarUri: Uri? = null,
    val avatarError: String? = null,
    val emailError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false
) {
    /**
     * Вычисляемое свойство - есть ли изменения в форме.
     * Сравнивает текущую форму с исходным снимком и учитывает изменение аватара.
     */
    val hasChanges: Boolean
        get() = userForm != initialForm || selectedAvatarUri != null

    /**
     * Вычисляемое свойство - можно ли сохранить форму.
     * Требует наличия изменений и отсутствия ошибок валидации.
     */
    val canSave: Boolean
        get() = hasChanges && emailError == null
}

/**
 * События экрана редактирования профиля.
 */
sealed interface EditProfileEvent {
    /**
     * Навигация назад.
     */
    data object NavigateBack : EditProfileEvent

    /**
     * Навигация на экран смены пароля.
     */
    data class NavigateToChangePassword(val userId: Long) : EditProfileEvent

    /**
     * Навигация на экран выбора страны.
     */
    data class NavigateToSelectCountry(val currentCountryId: Int?) : EditProfileEvent

    /**
     * Навигация на экран выбора города.
     */
    data class NavigateToSelectCity(val currentCityId: Int?, val countryId: Int) : EditProfileEvent
}
