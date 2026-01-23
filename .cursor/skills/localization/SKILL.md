---
name: localization
description: Правильно работай с локализацией в Android-проекте. Используй этот навык при добавлении новых строковых ресурсов, работе с plurals, форматировании дат и текстов.
---

# Локализация

## When to Use

- Используй этот навык, когда добавляешь новые строковые ресурсы в проект
- Используй этот навык, когда работаешь с множественным числом (plurals)
- Используй этот навык, когда форматируешь даты, числа или тексты
- Используй этот навык, когда локализуешь сообщения об ошибках
- Используй этот навык, когда обновляешь или существующие строки в проекте
- Этот навык полезен, когда нужно обеспечить консистентность локализации между всеми языками

## Instructions

### Языки проекта

- **Русский (ru)** - основной язык, все строки должны быть на русском
- **Английский (en)** - дополнительный язык, перевод должен соответствовать русскому оригиналу

### Ресурсы строк

**Основные правила:**

1. Все строковые ресурсы размещаются в `res/values/strings.xml` (русский) и `res/values-en/strings.xml` (английский)
2. Каждая строка имеет уникальное `name` в нижнем регистре с подчеркиваниями
3. Добавляй новые строки параллельно в оба файла (ru и en)

**Пример добавления строки:**

```xml
<!-- res/values/strings.xml -->
<string name="welcome_message">Добро пожаловать!</string>

<!-- res/values-en/strings.xml -->
<string name="welcome_message">Welcome!</string>
```

**Использование в коде:**

```kotlin
context.getString(R.string.welcome_message)
// или в XML
android:text="@string/welcome_message"
```

### Множественное число (Plurals)

**Когда использовать:**

- Для слов, меняющих форму в зависимости от количества (день/дня/дней)
- Для счетчиков, списков, показателей количества

**Правило для русского языка:**

- `one` - 1, 21, 31, 41...
- `few` - 2-4, 22-24, 32-34...
- `many` - 0, 5-20, 25-30, 35-40...
- `other` - для всех остальных случаев (обычно не используется в русском)

**Пример:**

```xml
<!-- res/values/strings.xml -->
<plurals name="days_count">
    <item quantity="one">%d день</item>
    <item quantity="few">%d дня</item>
    <item quantity="many">%d дней</item>
    <item quantity="other">%d дней</item>
</plurals>

<!-- res/values-en/strings.xml -->
<plurals name="days_count">
    <item quantity="one">%d day</item>
    <item quantity="other">%d days</item>
</plurals>
```

**Использование в коде:**

```kotlin
val days = 5
val text = resources.getQuantityString(R.plurals.days_count, days, days)
// Результат: "5 дней" (ru) или "5 days" (en)
```

### Форматирование

**Основные инструменты:**

- `String.format()` - для форматирования строк с подстановками
- `MessageFormat` - для сложного форматирования (если нужен)
- `DateFormat` - для дат с учетом локали пользователя

**Пример форматирования строки:**

```xml
<!-- res/values/strings.xml -->
<string name="items_count_format">Найдено %d элементов</string>

<!-- res/values-en/strings.xml -->
<string name="items_count_format">Found %d items</string>
```

**Использование в коде:**

```kotlin
val count = 42
val text = getString(R.string.items_count_format, count)
// Результат: "Найдено 42 элементов"
```

**Пример форматирования даты:**

```kotlin
val date = LocalDate.now()
val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
val formattedDate = date.format(formatter)
// Результат зависит от локали устройства
```

### Логирование

**Важное правило:** Все логи в коде пишутся на русском языке независимо от локализации приложения

```kotlin
// ✅ Правильно
Log.e(TAG, "Ошибка загрузки данных: ${error.message}")
Log.i(TAG, "Загружено $count элементов")

// ❌ Неправильно
Log.e(TAG, "Error loading data: ${error.message}")
```

### Организация ресурсов

**Группировка строк:**

- Используй префиксы для группировки строк (например, `auth_`, `parks_`, `events_`)
- Связанные строки должны иметь похожие имена

**Пример группировки:**

```xml
<!-- Авторизация -->
<string name="auth_login">Войти</string>
<string name="auth_password">Пароль</string>
<string name="auth_forgot_password">Забыли пароль?</string>

<!-- Площадки -->
<string name="parks_title">Площадки</string>
<string name="parks_empty">Нет площадок</string>
<string name="parks_filter">Фильтр</string>
```

### Работа с параметрами

**Правила:**

1. Используй позиционные параметры (`%1$s`, `%2$d`) для строк с несколькими подстановками
2. Сохраняй порядок параметров одинаковым для всех языков

**Пример с несколькими параметрами:**

```xml
<!-- res/values/strings.xml -->
<string name="user_profile">%1$s, %2$d лет</string>

<!-- res/values-en/strings.xml -->
<string name="user_profile">%1$s, %2$d years old</string>
```

**Использование:**

```kotlin
val name = "Иван"
val age = 25
val text = getString(R.string.user_profile, name, age)
// Результат: "Иван, 25 лет"
```

### Обработка ошибок

**Локализованные сообщения об ошибках:**

```xml
<!-- Ошибки сети -->
<string name="error_network">Ошибка сети. Проверьте подключение к интернету.</string>
<string name="error_timeout">Время ожидания истекло. Попробуйте еще раз.</string>

<!-- Ошибки авторизации -->
<string name="error_invalid_credentials">Неверный логин или пароль</string>
<string name="error_unauthorized">Необходима авторизация</string>

<!-- Ошибки данных -->
<string name="error_not_found">Данные не найдены</string>
<string name="error_invalid_data">Неверные данные</string>
```

**Использование в коде:**

```kotlin
try {
    loadData()
} catch (e: IOException) {
    Log.e(TAG, "Ошибка сети: ${e.message}")
    showError(getString(R.string.error_network))
}
```

### Ссылки на примеры

Более подробные примеры см. в файле [references/EXAMPLES.md](references/EXAMPLES.md).

### Чек-лист при добавлении локализации

1. ✅ Добавил строку в `res/values/strings.xml` (русский)
2. ✅ Добавил строку в `res/values-en/strings.xml` (английский)
3. ✅ Проверил, что `name` уникален и соответствует конвенции именования
4. ✅ Использовал правильный формат (простая строка, plurals, форматирование)
5. ✅ Проверил использование в коде или XML
6. ✅ Убедился, что логи на русском языке (если нужны)
7. ✅ Протестировал на разных языках устройства (если возможно)
