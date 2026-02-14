package com.swparks.data.provider

import android.content.Context
import com.swparks.domain.provider.ResourcesProvider

/**
 * Реализация ResourcesProvider на основе Android Context.
 *
 * @param context Application Context для доступа к ресурсам
 */
class ResourcesProviderImpl(
    private val context: Context
) : ResourcesProvider {
    override fun getString(resId: Int): String = context.getString(resId)
    override fun getString(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
