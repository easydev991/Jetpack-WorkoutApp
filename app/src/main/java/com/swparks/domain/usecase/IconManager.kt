package com.swparks.domain.usecase

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.swparks.domain.model.AppIcon

/**
 * Use Case для управления сменой иконки приложения.
 *
 * Смена иконки реализуется через Activity Aliases и PackageManager API.
 * Поддерживается на Android 8.0 (API 26) и выше.
 *
 * Примечание: Минимальная версия приложения - Android 8.0 (API 26),
 * поэтому проверки версии не требуются.
 *
 * @property context Контекст приложения
 */
class IconManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "IconManager"
    }

    /**
     * Изменяет иконку приложения.
     *
     * Метод активирует Activity Alias выбранной иконки и деактивирует остальные.
     * Использует PackageManager.setComponentEnabledSetting() для управления видимостью aliases.
     *
     * @param icon Иконка, которую нужно активировать
     */
    @Suppress("ThrowsCount")
    fun changeIcon(icon: AppIcon) {
        val packageName = context.packageName

        // Сначала АКТИВИРУЕМ новый Activity Alias
        val targetComponentName = getIconComponentName(icon, packageName)
        val targetComponentClassName = icon.getComponentName()
        try {
            context.packageManager.setComponentEnabledSetting(
                targetComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            Log.d(TAG, "Иконка $targetComponentClassName АКТИВИРОВАНА")
        } catch (e: SecurityException) {
            Log.e(TAG, "Ошибка при активации иконки: нет прав", e)
            throw e
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Компонент не найден при активации иконки", e)
            throw e
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Неверный аргумент при активации иконки", e)
            throw e
        }

        // Затем деактивируем ВСЕ остальные Activity Aliases
        val allIcons = AppIcon.entries
        allIcons.forEach { currentIcon ->
            if (currentIcon != icon) {
                disableComponent(getIconComponentName(currentIcon, packageName))
            }
        }
    }

    /**
     * Деактивирует Activity Alias с обработкой ошибок.
     *
     * @param componentName Компонент для деактивации
     */
    @Suppress("TooGenericExceptionCaught")
    private fun disableComponent(componentName: ComponentName) {
        try {
            // Используем KILL_IF_NEEDED вместо DONT_KILL_APP для более надежной деактивации
            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            Log.d(TAG, "Иконка ${componentName.className} ДЕАКТИВИРОВАНА")
        } catch (e: SecurityException) {
            Log.e(TAG, "Ошибка при деактивации иконки: нет прав", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Компонент не найден при деактивации иконки", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Неверный аргумент при деактивации иконки", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Неожиданная ошибка при деактивации иконки", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка при деактивации иконки", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        }
    }
}

/**
 * Формирует ComponentName для Activity Alias иконки.
 *
 * @param icon Иконка
 * @param packageName Имя пакета приложения
 * @return ComponentName для Activity Alias иконки
 */
private fun getIconComponentName(
    icon: AppIcon,
    packageName: String,
): ComponentName {
    val className = icon.getComponentName()
    return ComponentName(packageName, className)
}
