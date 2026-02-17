package com.swparks.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты для HTML-утилит
 *
 * Тестирует функции parseHtml и parseHtmlOrNull для удаления HTML-тегов
 *
 * @see parseHtml
 * @see parseHtmlOrNull
 */
class HtmlUtilsTest {

    // === Базовые тесты ===

    @Test
    fun parseHtml_whenParagraphTags_thenRemovesTags() {
        val html = "<p>Простой текст</p>"
        assertEquals("Простой текст", html.parseHtml())
    }

    @Test
    fun parseHtml_whenMultipleTags_thenRemovesAllTags() {
        val html = "<i data-ed3=\"3\"></i><p>Ответил на всё.</p>"
        assertEquals("Ответил на всё.", html.parseHtml())
    }

    @Test
    fun parseHtml_whenStrongTag_thenRemovesTag() {
        val html = "<p><strong>Жирный текст</strong></p>"
        assertEquals("Жирный текст", html.parseHtml())
    }

    @Test
    fun parseHtml_whenBrTagInDetailMode_thenConvertsToNewline() {
        val html = "<p>Строка 1<br>Строка 2</p>"
        assertEquals("Строка 1\nСтрока 2", html.parseHtml(compactMode = false))
    }

    @Test
    fun parseHtml_whenBrTagInCompactMode_thenConvertsToSpace() {
        val html = "<p>Строка 1<br>Строка 2</p>"
        assertEquals("Строка 1 Строка 2", html.parseHtml(compactMode = true))
    }

    @Test
    fun parseHtml_whenHtmlEntities_thenDecodesThem() {
        val html = "&lt;script&gt;alert('xss')&lt;/script&gt;"
        assertEquals("<script>alert('xss')</script>", html.parseHtml())
    }

    @Test
    fun parseHtml_whenNestedTags_thenRemovesAllTags() {
        val html = "<div><p><strong>Вложенный</strong> текст</p></div>"
        assertEquals("Вложенный текст", html.parseHtml())
    }

    @Test
    fun parseHtml_whenTagsOnly_thenReturnsEmptyString() {
        val html = "<i data-ed3=\"3\"></i>"
        assertEquals("", html.parseHtml())
    }

    @Test
    fun parseHtml_whenPlainText_thenReturnsSameText() {
        val text = "Обычный текст без тегов"
        assertEquals("Обычный текст без тегов", text.parseHtml())
    }

    // === Тесты режимов Compact vs Detail ===

    @Test
    fun parseHtml_whenCompactMode_thenCollapsesLineBreaks() {
        val html = "<p>Line 1</p><p>Line 2</p>"
        assertEquals("Line 1 Line 2", html.parseHtml(compactMode = true))
    }

    @Test
    fun parseHtml_whenDetailMode_thenPreservesLineBreaks() {
        val html = "<p>Line 1</p><p>Line 2</p>"
        assertEquals("Line 1\n\nLine 2", html.parseHtml(compactMode = false))
    }

    @Test
    fun parseHtml_whenCompactMode_thenCollapsesMultipleSpaces() {
        val html = "<p>Text   with    spaces</p>"
        assertEquals("Text with spaces", html.parseHtml(compactMode = true))
    }

    @Test
    fun parseHtml_whenDetailMode_thenCollapsesExcessiveLineBreaks() {
        val html = "<p>Line 1</p><p></p><p>Line 2</p>"
        assertEquals("Line 1\n\nLine 2", html.parseHtml(compactMode = false))
    }

    // === Тесты nullable версии ===

    @Test
    fun parseHtmlOrNull_whenNullInput_thenReturnsNull() {
        val html: String? = null
        assertNull(html.parseHtmlOrNull())
    }

    @Test
    fun parseHtmlOrNull_whenNonNullInput_thenReturnsParsedString() {
        val html: String = "<p>Test</p>"
        assertEquals("Test", html.parseHtmlOrNull())
    }

    // === Реальные примеры с сервера ===

    @Test
    fun parseHtml_whenRealDialogMessage_thenRemovesTags() {
        val html = "<i data-ed3=\"3\"></i><p>Ответил на всё. Сборку сегодня сделаю.</p>"
        assertEquals("Ответил на всё. Сборку сегодня сделаю.", html.parseHtml(compactMode = true))
    }

    @Test
    fun parseHtml_whenRealJournalEntry_thenPreservesStructure() {
        val html =
            "<i data-ed3=\"3\"></i><p><strong>Что я успел сделать в 2026 году?</strong></p>" +
                    "<p>+ Собрать паззл на 1000 деталей</p>"
        val expected = "Что я успел сделать в 2026 году?\n\n+ Собрать паззл на 1000 деталей"
        assertEquals(expected, html.parseHtml(compactMode = false))
    }

    // === Тесты HTML-сущностей ===

    @Test
    fun parseHtml_whenAmpEntity_thenDecodes() {
        val html = "Tom &amp; Jerry"
        assertEquals("Tom & Jerry", html.parseHtml())
    }

    @Test
    fun parseHtml_whenNbspEntity_thenDecodes() {
        val html = "Word1&nbsp;Word2"
        assertEquals("Word1 Word2", html.parseHtml())
    }

    @Test
    fun parseHtml_whenQuotEntity_thenDecodes() {
        val html = "Say &quot;Hello&quot;"
        assertEquals("Say \"Hello\"", html.parseHtml())
    }

    // === Тесты edge cases ===

    @Test
    fun parseHtml_whenEmptyString_thenReturnsEmptyString() {
        assertEquals("", "".parseHtml())
    }

    @Test
    fun parseHtml_whenSelfClosingBrTag_thenConvertsToNewline() {
        val html = "Line 1<br/>Line 2"
        assertEquals("Line 1\nLine 2", html.parseHtml(compactMode = false))
    }

    @Test
    fun parseHtml_whenBrTagWithSpace_thenConvertsToNewline() {
        val html = "Line 1<br />Line 2"
        assertEquals("Line 1\nLine 2", html.parseHtml(compactMode = false))
    }

    @Test
    fun parseHtml_whenDivTags_thenConvertsToNewlines() {
        val html = "<div>Block 1</div><div>Block 2</div>"
        assertEquals("Block 1\nBlock 2", html.parseHtml(compactMode = false))
    }
}
