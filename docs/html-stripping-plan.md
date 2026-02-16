# План: Обработка HTML-тегов в текстовых полях

## Проблема

Сервер возвращает текстовые поля с HTML-тегами:
- `<i data-ed3="3"></i>` - служебный тег редактирования
- `<p>...</p>` - параграфы
- `<strong>...</strong>` - жирный текст
- `<br>` - перенос строки
- `<div class="bbcode_quote">...</div>` - цитаты
- `<img class="bbcode_img" src="...">` - изображения

Примеры:
```json
// Сообщение в диалоге
{
    "message": "<i data-ed3=\"3\"></i><p>Ответил на всё. Сборку сегодня сделаю.</p>"
}

// Последняя запись в дневнике
{
    "last_message_text": "<i data-ed3=\"3\"></i><p><strong>Что я успел сделать в 2026 году?</strong></p><p>+ Собрать паззл на 1000 деталей</p>"
}
```

## Цель

Отображать в UI чистый текст без HTML-тегов, сохраняя структуру там, где это нужно.

## Затрагиваемые поля

| Модель | Поле | Экран | Режим | Вызов |
|--------|------|-------|-------|-------|
| `DialogResponse` | `lastMessageText` | Список диалогов | **Compact** | `.parseHtml(compactMode = true)` |
| `MessageResponse` | `message` | Чат | **Detail** | `.parseHtml(compactMode = false)` |
| `JournalResponse` | `lastMessageText` | Список дневников | **Compact** | `.parseHtml(compactMode = true)` |
| `JournalEntryResponse` | `message` | Записи дневника | **Detail** | `.parseHtml(compactMode = false)` |
| `Comment` | `body` | Комментарии | **Detail** | `.parseHtml(compactMode = false)` |
| `Event` | `description` | Детали мероприятия | **Detail** | `.parseHtml(compactMode = false)` |

### Режимы

- **Compact** - для превью в списках: схлопывает переносы строк в пробелы, убирает лишние пробелы
- **Detail** - для полного просмотра: сохраняет структуру текста (параграфы, переносы)

## Решение

Extension-функция `String.parseHtml()` с параметром `compactMode`:

---

## Этап 1: Утилита для обработки HTML

- [ ] Создать extension-функцию `String.parseHtml()` в `util/HtmlUtils.kt`
- [ ] Добавить параметр `compactMode` для управления сохранением переносов
- [ ] Добавить обработку HTML-сущностей (`&amp;`, `&lt;`, `&gt;`, `&nbsp;`, `&quot;`)
- [ ] Написать unit-тесты для функции

**Файлы:**
- `app/src/main/java/com/swparks/util/HtmlUtils.kt`
- `app/src/test/java/com/swparks/util/HtmlUtilsTest.kt`

## Этап 2: Обновление моделей данных

Применить `parseHtml()` в моделях при получении текста:

### 2.1 DialogResponse

- [ ] Обновить `DialogResponse` - обработка `lastMessageText` (compact mode)

### 2.2 MessageResponse

- [ ] Обновить `MessageResponse` - обработка `message` (detail mode)

### 2.3 JournalResponse

- [ ] Обновить `JournalResponse` - обработка `lastMessageText` (compact mode)

### 2.4 JournalEntryResponse

- [ ] Обновить `JournalEntryResponse` - обработка `message` (detail mode)

### 2.5 Comment

- [ ] Обновить `Comment` - обработка `body` (detail mode)

### 2.6 Event

- [ ] Обновить `Event` - обработка `description` (detail mode)

**Файлы:**
- `app/src/main/java/com/swparks/data/model/DialogResponse.kt`
- `app/src/main/java/com/swparks/data/model/MessageResponse.kt`
- `app/src/main/java/com/swparks/data/model/JournalResponse.kt`
- `app/src/main/java/com/swparks/data/model/JournalEntryResponse.kt`
- `app/src/main/java/com/swparks/data/model/Comment.kt`
- `app/src/main/java/com/swparks/data/model/Event.kt`

## Этап 3: Обновление тестов моделей

- [ ] Добавить тесты с HTML-тегами в тесты моделей
- [ ] Проверить что теги корректно удаляются

## Этап 4: Проверка UI

- [ ] Запустить приложение
- [ ] Проверить отображение в списке диалогов (compact)
- [ ] Проверить отображение в чате (detail - переносы сохраняются)
- [ ] Проверить отображение в списке дневников (compact)
- [ ] Проверить отображение записей дневника (detail)
- [ ] Проверить отображение комментариев (detail)
- [ ] Проверить отображение описания мероприятий (detail)

---

## Реализация String.parseHtml()

```kotlin
package com.swparks.util

/**
 * Очищает строку от HTML-тегов.
 * @param compactMode Если true, заменяет переносы строк на пробелы и схлопывает whitespace (для превью).
 *                    Если false, сохраняет структуру текста (параграфы, переносы).
 */
fun String.parseHtml(compactMode: Boolean = false): String {
    var text = this

    // 1. Предварительная обработка структурных тегов
    if (compactMode) {
        // В компактном режиме всё, что похоже на перенос, меняем на пробел
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), " ")
    } else {
        // В детальном режиме сохраняем переносы
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
    }

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
```

## Тесты

```kotlin
package com.swparks.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlUtilsTest {
    
    // === Базовые тесты ===
    
    @Test
    fun `parseHtml removes paragraph tags`() {
        val html = "<p>Простой текст</p>"
        assertEquals("Простой текст", html.parseHtml())
    }

    @Test
    fun `parseHtml removes multiple tags`() {
        val html = "<i data-ed3=\"3\"></i><p>Ответил на всё.</p>"
        assertEquals("Ответил на всё.", html.parseHtml())
    }

    @Test
    fun `parseHtml handles strong tag`() {
        val html = "<p><strong>Жирный текст</strong></p>"
        assertEquals("Жирный текст", html.parseHtml())
    }

    @Test
    fun `parseHtml handles br tags in detail mode`() {
        val html = "<p>Строка 1<br>Строка 2</p>"
        assertEquals("Строка 1\nСтрока 2", html.parseHtml(compactMode = false))
    }

    @Test
    fun `parseHtml handles br tags in compact mode`() {
        val html = "<p>Строка 1<br>Строка 2</p>"
        assertEquals("Строка 1 Строка 2", html.parseHtml(compactMode = true))
    }

    @Test
    fun `parseHtml handles html entities`() {
        val html = "&lt;script&gt;alert('xss')&lt;/script&gt;"
        assertEquals("<script>alert('xss')</script>", html.parseHtml())
    }

    @Test
    fun `parseHtml handles nested tags`() {
        val html = "<div><p><strong>Вложенный</strong> текст</p></div>"
        assertEquals("Вложенный текст", html.parseHtml())
    }

    @Test
    fun `parseHtml returns empty for tags only`() {
        val html = "<i data-ed3=\"3\"></i>"
        assertEquals("", html.parseHtml())
    }

    @Test
    fun `parseHtml handles plain text`() {
        val text = "Обычный текст без тегов"
        assertEquals("Обычный текст без тегов", text.parseHtml())
    }

    // === Тесты режимов Compact vs Detail ===

    @Test
    fun `parseHtml handles line breaks differently based on mode`() {
        val html = "<p>Line 1</p><p>Line 2</p>"

        // Compact mode: схлопывает в одну строку
        assertEquals("Line 1 Line 2", html.parseHtml(compactMode = true))

        // Detail mode: сохраняет переносы
        assertEquals("Line 1\n\nLine 2", html.parseHtml(compactMode = false))
    }

    @Test
    fun `parseHtml compact mode collapses multiple spaces`() {
        val html = "<p>Text   with    spaces</p>"
        assertEquals("Text with spaces", html.parseHtml(compactMode = true))
    }

    @Test
    fun `parseHtml detail mode preserves multiple line breaks`() {
        val html = "<p>Line 1</p><p></p><p>Line 2</p>"
        // Более 3 переносов схлопываются в 2
        assertEquals("Line 1\n\nLine 2", html.parseHtml(compactMode = false))
    }

    // === Тесты nullable версии ===

    @Test
    fun `parseHtmlOrNull returns null for null input`() {
        val html: String? = null
        assertEquals(null, html.parseHtmlOrNull())
    }

    @Test
    fun `parseHtmlOrNull returns parsed string for non-null input`() {
        val html: String? = "<p>Test</p>"
        assertEquals("Test", html.parseHtmlOrNull())
    }

    // === Реальные примеры с сервера ===

    @Test
    fun `parseHtml handles real dialog message`() {
        val html = "<i data-ed3=\"3\"></i><p>Ответил на всё. Сборку сегодня сделаю.</p>"
        assertEquals("Ответил на всё. Сборку сегодня сделаю.", html.parseHtml(compactMode = true))
    }

    @Test
    fun `parseHtml handles real journal entry`() {
        val html = "<i data-ed3=\"3\"></i><p><strong>Что я успел сделать в 2026 году?</strong></p><p>+ Собрать паззл на 1000 деталей</p>"
        val expected = "Что я успел сделать в 2026 году?\n\n+ Собрать паззл на 1000 деталей"
        assertEquals(expected, html.parseHtml(compactMode = false))
    }
}
```

---

## Зависимости

Нет новых зависимостей - используется стандартный Kotlin.

## Риски

1. **Производительность**: regex на больших текстах может быть медленным
   - Решение: тексты сообщений обычно короткие

2. **Неполная обработка**: могут быть пропущены редкие HTML-конструкции
   - Решение: добавить тесты для edge cases по мере обнаружения

3. **Форматирование**: теряется визуальное форматирование (жирный, курсив)
   - Решение: в будущем можно использовать `AnnotatedString` для сохранения форматирования

## Критерии завершения

- [ ] Создана утилита `String.parseHtml()` с параметром `compactMode`
- [ ] Написаны unit-тесты
- [ ] Обновлены все модели из таблицы
- [ ] Проверено отображение на всех затронутых экранах
- [ ] Проект собирается без ошибок
- [ ] Тесты проходят успешно
- [ ] Выполнен `make format`
