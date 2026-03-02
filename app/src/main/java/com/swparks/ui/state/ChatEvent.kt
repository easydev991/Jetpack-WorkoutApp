package com.swparks.ui.state

/**
 * События экрана чата для коммуникации между экранами.
 *
 * Используется для уведомления родительских экранов о событиях в чате.
 */
sealed class ChatEvent {
    /**
     * Сообщение успешно отправлено.
     *
     * @param dialogId ID диалога, в который отправлено сообщение
     */
    data class MessageSent(val dialogId: Long) : ChatEvent()
}
