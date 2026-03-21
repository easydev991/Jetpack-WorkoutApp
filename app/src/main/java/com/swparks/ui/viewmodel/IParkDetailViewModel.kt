package com.swparks.ui.viewmodel

import com.swparks.data.model.Photo
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.MapUriSet
import com.swparks.ui.state.ParkDetailUIState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для ParkDetailViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IParkDetailViewModel {
    /**
     * Состояние UI экрана детальной информации о площадке.
     */
    val uiState: StateFlow<ParkDetailUIState>

    /**
     * Одноразовые события UI (диалоги, системные intent и т.д.).
     */
    val events: SharedFlow<ParkDetailEvent>

    /**
     * Состояние pull-to-refresh (true во время обновления).
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Состояние авторизации пользователя.
     */
    val isAuthorized: StateFlow<Boolean>

    /**
     * Является ли текущий пользователь автором площадки.
     */
    val isParkAuthor: StateFlow<Boolean>

    /**
     * Идентификатор текущего авторизованного пользователя.
     */
    val currentUserId: StateFlow<Long?>

    /**
     * Набор URI для работы с картой (geo, browser, navigation, route).
     * Вычисляется на основе координат площадки.
     */
    val mapUriSet: MapUriSet?

    /**
     * Обработка нажатия на кнопку редактирования площадки.
     */
    fun onEditClick()

    /**
     * Обработка нажатия на кнопку удаления площадки.
     */
    fun onDeleteClick()

    /**
     * Обработка нажатия на кнопку подтверждения удаления площадки.
     * Вызывается после подтверждения в alert/dialog.
     */
    fun onDeleteConfirm()

    /**
     * Обработка нажатия на кнопку отмены удаления площадки.
     * Вызывается при отмене в alert/dialog.
     */
    fun onDeleteDismiss()

    /**
     * Обработка нажатия на кнопку "Поделиться".
     */
    fun onShareClick()

    /**
     * Обработка переключения toggle "Тренируюсь здесь".
     */
    fun onTrainHereToggle()

    /**
     * Обработка нажатия на количество тренирующихся.
     */
    fun onTraineesCountClick()

    /**
     * Обработка нажатия на кнопку "Открыть на карте".
     */
    fun onOpenMapClick()

    /**
     * Обработка нажатия на кнопку "Построить маршрут".
     */
    fun onRouteClick()

    /**
     * Обработка нажатия на кнопку "Создать мероприятие".
     */
    fun onCreateEventClick()

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
     * Обработка подтверждения удаления комментария.
     */
    fun onCommentDeleteConfirm()

    /**
     * Обработка отмены удаления комментария.
     */
    fun onCommentDeleteDismiss()

    /**
     * Обработка удаления фото из PhotoDetailSheetHost.
     * Удаляет фото из локального состояния без запроса к серверу.
     *
     * @param photoId Идентификатор удаленного фото
     */
    fun onPhotoDeleted(photoId: Long)

    /**
     * Обработка обновления площадки после возврата с ParkFormScreen.
     *
     * @param parkId Идентификатор обновленной площадки
     */
    fun onParkUpdated(parkId: Long)

    /**
     * Обновление данных площадки (pull-to-refresh).
     */
    fun refresh()
}
