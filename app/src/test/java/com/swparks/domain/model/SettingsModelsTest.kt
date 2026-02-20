package com.swparks.domain.model

import org.junit.Test

/**
 * Unit-тесты для моделей настроек приложения.
 *
 * Тестируются:
 * - AppIcon enum - проверка всех значений иконок SWParks
 * - AppTheme enum - проверка всех значений тем
 */
class SettingsModelsTest {

    // ========== AppIcon Tests ==========

    @Test
    fun appIcon_shouldContainAllElevenValues() {
        val expectedIcons = listOf(
            AppIcon.DEFAULT,
            AppIcon.ICON_2,
            AppIcon.ICON_3,
            AppIcon.ICON_4,
            AppIcon.ICON_5,
            AppIcon.ICON_6,
            AppIcon.ICON_7,
            AppIcon.ICON_8,
            AppIcon.ICON_9,
            AppIcon.ICON_10,
            AppIcon.ICON_11,
        )

        val actualIcons = AppIcon.entries

        assertListsEqual(actualIcons, expectedIcons)
    }

    @Test
    fun appIcon_getComponentName_whenDefault_thenReturnsCorrectComponentName() {
        val result = AppIcon.DEFAULT.getComponentName()

        assertStringsEqual(result, "com.swparks.MainActivityAliasIcon1")
    }

    @Test
    fun appIcon_getComponentName_whenIcon2_thenReturnsCorrectComponentName() {
        val result = AppIcon.ICON_2.getComponentName()

        assertStringsEqual(result, "com.swparks.MainActivityIcon2")
    }

    @Test
    fun appIcon_getComponentName_whenIcon11_thenReturnsCorrectComponentName() {
        val result = AppIcon.ICON_11.getComponentName()

        assertStringsEqual(result, "com.swparks.MainActivityIcon11")
    }

    @Test
    fun appIcon_getComponentName_forAllIcons_thenReturnsUniqueValues() {
        val componentNames = AppIcon.entries.map { it.getComponentName() }

        assertListUnique(componentNames)
    }

    // ========== AppTheme Tests ==========

    @Test
    fun appTheme_shouldContainAllThreeValues() {
        val expectedThemes = listOf(
            AppTheme.LIGHT,
            AppTheme.DARK,
            AppTheme.SYSTEM,
        )

        val actualThemes = AppTheme.entries

        assertListsEqual(actualThemes, expectedThemes)
    }

    @Test
    fun appTheme_LIGHT_shouldHaveCorrectName() {
        assertStringsEqual(AppTheme.LIGHT.name, "LIGHT")
    }

    @Test
    fun appTheme_DARK_shouldHaveCorrectName() {
        assertStringsEqual(AppTheme.DARK.name, "DARK")
    }

    @Test
    fun appTheme_SYSTEM_shouldHaveCorrectName() {
        assertStringsEqual(AppTheme.SYSTEM.name, "SYSTEM")
    }

    @Test
    fun appTheme_valueOf_shouldParseCorrectly() {
        assertThemesEqual(AppTheme.valueOf("LIGHT"), AppTheme.LIGHT)
        assertThemesEqual(AppTheme.valueOf("DARK"), AppTheme.DARK)
        assertThemesEqual(AppTheme.valueOf("SYSTEM"), AppTheme.SYSTEM)
    }
}

// ========== Helper Assertions ==========

/**
 * Вспомогательная функция для проверки равенства списков.
 */
private fun <T> assertListsEqual(actual: List<T>, expected: List<T>) {
    if (actual != expected) {
        throw AssertionError(
            "Ожидался список: $expected\n" +
                "Получен список: $actual"
        )
    }
}

/**
 * Вспомогательная функция для проверки равенства строк.
 */
private fun assertStringsEqual(actual: String, expected: String) {
    if (actual != expected) {
        throw AssertionError(
            "Ожидалась строка: $expected\n" +
                "Получена строка: $actual"
        )
    }
}

/**
 * Вспомогательная функция для проверки равенства тем.
 */
private fun assertThemesEqual(actual: AppTheme, expected: AppTheme) {
    if (actual != expected) {
        throw AssertionError(
            "Ожидалась тема: $expected\n" +
                "Получена тема: $actual"
        )
    }
}

/**
 * Вспомогательная функция для проверки уникальности элементов списка.
 */
private fun assertListUnique(list: List<*>) {
    val duplicates = list.groupingBy { it }.eachCount().filter { it.value > 1 }
    if (duplicates.isNotEmpty()) {
        throw AssertionError(
            "Найдены дубликаты: $duplicates"
        )
    }
}
