package com.swparks.util

/**
 * Очищает строку от HTML-тегов.
 * @param compactMode Если true, заменяет переносы строк на пробелы и схлопывает whitespace (для превью).
 *                    Если false, сохраняет структуру текста (параграфы, переносы).
 */
fun String.parseHtml(compactMode: Boolean = false): String {
    // 1. Предварительная обработка структурных тегов: в компактном режиме переносы — в пробелы
    val lineBreak = if (compactMode) " " else "\n"
    val paragraph = if (compactMode) " " else "\n\n"
    val text =
        this
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), lineBreak)
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), paragraph)
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), lineBreak)

    return text
        // 2. Удаляем все оставшиеся теги
        .replace(Regex("<[^>]*>"), "")
        // 3. Декодируем сущности
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        // 4. Финальная зачистка
        .let {
            if (compactMode) {
                // Схлопываем множественные пробелы в один
                it.trim().replace(Regex("\\s+"), " ")
            } else {
                // Убираем только лишние пробелы по краям и множественные пустые строки,
                // но оставляем одиночные переносы
                it.trim().replace(Regex("\n{3,}"), "\n\n")
            }
        }
}

/**
 * Безопасная версия для nullable строк
 */
fun String?.parseHtmlOrNull(compactMode: Boolean = false): String? = this?.parseHtml(compactMode)
