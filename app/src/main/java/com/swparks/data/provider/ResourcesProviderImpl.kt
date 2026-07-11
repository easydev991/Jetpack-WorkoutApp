package com.swparks.data.provider

import android.content.Context

/**
 * Реализация ResourcesProvider на основе Android Context.
 *
 * @param context Application Context для доступа к ресурсам
 */
class ResourcesProviderImpl(
    private val context: Context
) {
    fun getString(resId: Int): String = context.getString(resId)

    fun getString(
        resId: Int,
        vararg args: Any
    ): String = context.getString(resId, *args)
}
