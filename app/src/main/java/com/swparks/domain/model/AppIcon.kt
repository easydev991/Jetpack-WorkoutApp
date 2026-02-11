package com.swparks.domain.model


/**
 * Перечисление доступных иконок приложения.
 *
 * @property DEFAULT Основная иконка (по умолчанию)
 * @property ICON_2 Второй вариант иконки
 * @property ICON_3 Третий вариант иконки
 * @property ICON_4 Четвёртый вариант иконки
 * @property ICON_5 Пятый вариант иконки
 * @property ICON_6 Шестой вариант иконки
 * @property ICON_7 Седьмой вариант иконки
 * @property ICON_8 Восьмой вариант иконки
 * @property ICON_9 Девятый вариант иконки
 * @property ICON_10 Десятый вариант иконки
 * @property ICON_11 Одиннадцатый вариант иконки
 */
enum class AppIcon {
    DEFAULT,
    ICON_2,
    ICON_3,
    ICON_4,
    ICON_5,
    ICON_6,
    ICON_7,
    ICON_8,
    ICON_9,
    ICON_10,
    ICON_11,
    ;

    /**
     * Возвращает имя компонента Activity Alias для текущей иконки.
     *
     * @return Имя класса Activity Alias
     */
    fun getComponentName(): String =
        when (this) {
            DEFAULT -> "com.swparks.MainActivityAliasIcon1"
            ICON_2 -> "com.swparks.MainActivityIcon2"
            ICON_3 -> "com.swparks.MainActivityIcon3"
            ICON_4 -> "com.swparks.MainActivityIcon4"
            ICON_5 -> "com.swparks.MainActivityIcon5"
            ICON_6 -> "com.swparks.MainActivityIcon6"
            ICON_7 -> "com.swparks.MainActivityIcon7"
            ICON_8 -> "com.swparks.MainActivityIcon8"
            ICON_9 -> "com.swparks.MainActivityIcon9"
            ICON_10 -> "com.swparks.MainActivityIcon10"
            ICON_11 -> "com.swparks.MainActivityIcon11"
        }
}
