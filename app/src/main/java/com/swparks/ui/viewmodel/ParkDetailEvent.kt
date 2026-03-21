package com.swparks.ui.viewmodel

import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.ui.model.TextEntryMode
import com.swparks.util.Complaint

/**
 * События UI экрана детальной информации о площадке.
 *
 * Используются для одноразовых действий, которые должны быть обработаны в UI
 * (например, показ диалогов подтверждения, навигация).
 */
sealed class ParkDetailEvent {
    /**
     * Показать диалог подтверждения удаления площадки.
     */
    data object ShowDeleteConfirmDialog : ParkDetailEvent()

    /**
     * Показать диалог подтверждения удаления фото.
     *
     * @param photo Фотография для удаления
     */
    data class ShowDeletePhotoConfirmDialog(val photo: Photo) : ParkDetailEvent()

    /**
     * Показать диалог подтверждения удаления комментария.
     */
    data object ShowDeleteCommentConfirmDialog : ParkDetailEvent()

    /**
     * Площадка успешно удалена, нужно закрыть экран.
     *
     * @param parkId Идентификатор удаленной площадки
     */
    data class ParkDeleted(val parkId: Long) : ParkDetailEvent()

    /**
     * Площадка обновлена после редактирования.
     * Вызывается при возврате с ParkFormScreen.
     *
     * @param parkId Идентификатор обновленной площадки
     */
    data class ParkUpdated(val parkId: Long) : ParkDetailEvent()

    /**
     * Навигация к экрану редактирования площадки.
     *
     * @param park Площадка для редактирования
     */
    data class NavigateToEditPark(val park: com.swparks.data.model.Park) : ParkDetailEvent()

    /**
     * Фото успешно удалено.
     */
    data class PhotoDeleted(val photoId: Long) : ParkDetailEvent()

    /**
     * Открыть карту по координатам площадки.
     */
    data object OpenMap : ParkDetailEvent()

    /**
     * Построить маршрут до площадки.
     */
    data object BuildRoute : ParkDetailEvent()

    /**
     * Навигация к списку тренирующихся на площадке.
     *
     * @param parkId Идентификатор площадки
     * @param users Список тренирующихся
     */
    data class NavigateToTrainees(
        val parkId: Long,
        val users: List<User>
    ) : ParkDetailEvent()

    /**
     * Навигация к экрану создания мероприятия для площадки.
     *
     * @param parkId Идентификатор площадки
     * @param parkName Название площадки
     */
    data class NavigateToCreateEvent(
        val parkId: Long,
        val parkName: String
    ) : ParkDetailEvent()

    /**
     * Отправить жалобу на комментарий через почтовый клиент.
     *
     * @param complaint Готовая модель жалобы на комментарий к площадке
     */
    data class SendCommentComplaint(
        val complaint: Complaint.ParkComment
    ) : ParkDetailEvent()

    /**
     * Открыть TextEntrySheet для создания/редактирования комментария к площадке.
     *
     * @param mode Режим экрана ввода текста
     */
    data class OpenCommentTextEntry(
        val mode: TextEntryMode
    ) : ParkDetailEvent()

    /**
     * Навигация к экрану детального просмотра фотографии.
     *
     * @param photo Фотография для просмотра
     * @param parkId Идентификатор площадки
     * @param parkTitle Название площадки
     * @param isParkAuthor Является ли пользователь автором площадки
     */
    data class NavigateToPhotoDetail(
        val photo: Photo,
        val parkId: Long,
        val parkTitle: String,
        val isParkAuthor: Boolean
    ) : ParkDetailEvent()
}
