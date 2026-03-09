package com.swparks.ui.viewmodel

import com.swparks.data.model.Photo
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.MapUriSet
import com.swparks.ui.state.EventDetailUIState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для EventDetailViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IEventDetailViewModel {
    /**
     * Состояние UI экрана детальной информации о мероприятии.
     */
    val uiState: StateFlow<EventDetailUIState>

    /**
     * Одноразовые события UI (диалоги, системные intent и т.д.).
     */
    val events: SharedFlow<EventDetailEvent>

    /**
     * Состояние pull-to-refresh (true во время обновления).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Состояние авторизации пользователя.
     */
    val isAuthorized: StateFlow<Boolean>

    /**
     * Является ли текущий пользователь автором мероприятия.
     */
    val isEventAuthor: StateFlow<Boolean>

    /**
     * Идентификатор текущего авторизованного пользователя.
     */
    val currentUserId: StateFlow<Long?>

    /**
     * Набор URI для работы с картой (geo, browser, navigation, route).
     * Вычисляется на основе координат мероприятия.
     */
    val mapUriSet: MapUriSet?

    /**
     * Обработка нажатия на кнопку редактирования мероприятия.
     */
    fun onEditClick()

    /**
     * Обработка нажатия на кнопку удаления мероприятия.
     */
    fun onDeleteClick()

    /**
     * Обработка нажатия на кнопку подтверждения удаления мероприятия.
     * Вызывается после подтверждения в alert/dialog.
     */
    fun onDeleteConfirm()

    /**
     * Обработка нажатия на кнопку отмены удаления мероприятия.
     * Вызывается при отмене в alert/dialog.
     */
    fun onDeleteDismiss()

    /**
     * Обработка нажатия на кнопку "Поделиться".
     */
    fun onShareClick()

    /**
     * Обработка переключения toggle "Пойду на мероприятие".
     */
    fun onParticipantToggle()

    /**
     * Обработка нажатия на количество участников.
     */
    fun onParticipantsCountClick()

    /**
     * Обработка нажатия на кнопку "Открыть на карте".
     */
    fun onOpenMapClick()

    /**
     * Обработка нажатия на кнопку "Построить маршрут".
     */
    fun onRouteClick()

    /**
     * Обработка нажатия на кнопку "Добавить в календарь".
     */
    fun onAddToCalendarClick()

    /**
     * Обработка ошибки открытия системного календаря.
     */
    fun onAddToCalendarFailed()

    /**
     * Обработка нажатия на фотографию.
     *
     * @param photo Выбранная фотография
     */
    fun onPhotoClick(photo: Photo)

    /**
     * Обработка нажатия на кнопку удаления фотографии.
     *
     * @param photo Фотография для удаления
     */
    fun onPhotoDeleteClick(photo: Photo)

    /**
     * Обработка нажатия на кнопку подтверждения удаления фото.
     * Вызывается после подтверждения в alert/dialog.
     */
    fun onPhotoDeleteConfirm()

    /**
     * Обработка нажатия на кнопку отмены удаления фото.
     * Вызывается при отмене в alert/dialog.
     */
    fun onPhotoDeleteDismiss()

    /**
     * Обработка нажатия на кнопку "Добавить комментарий".
     */
    fun onAddCommentClick()

    /**
     * Обработка нажатия на действие в меню комментария.
     *
     * @param commentId Идентификатор комментария
     * @param action Выбранное действие
     */
    fun onCommentActionClick(commentId: Long, action: CommentAction)

    /**
     * Обновление данных мероприятия (pull-to-refresh).
     */
    fun refresh()
}
