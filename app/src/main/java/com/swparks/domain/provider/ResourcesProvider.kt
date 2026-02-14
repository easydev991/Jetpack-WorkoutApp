package com.swparks.domain.provider

/**
 * Интерфейс для доступа к строковым ресурсам.
 *
 * Используется в ViewModel для получения локализованных строк
 * без прямой зависимости от Android Context.
 */
interface ResourcesProvider {
    /**
     * Получить строку по идентификатору ресурса.
     *
     * @param resId Идентификатор строкового ресурса
     * @return Локализованная строка
     */
    fun getString(resId: Int): String

    /**
     * Получить строку с подстановкой аргументов.
     *
     * @param resId Идентификатор строкового ресурса
     * @param args Аргументы для форматирования
     * @return Локализованная строка с подставленными аргументами
     */
    fun getString(resId: Int, vararg args: Any): String
}
